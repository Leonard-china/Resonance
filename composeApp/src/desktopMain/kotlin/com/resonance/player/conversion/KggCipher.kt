package com.resonance.player.conversion

import java.io.BufferedInputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * KGG v5 (QMC2) cipher primitives.
 *
 * Algorithm ported from FlyMouseFormat kgg-format.js which itself ports from
 * unlock-music CLI derivatives (C++ AudioDecrypt + Go Kugo-Music-Converter).
 *
 * Three-layer decryption:
 *  1. Database page decryption  → handled in KggKeyDatabase
 *  2. ekey decryption (TEA-CBC) → ekeyDecrypt()
 *  3. Audio decryption (QMC2)   → decryptStream() / decryptBlock()
 */
internal object KggCipher {

    // ---- ekey constants ----
    private const val EKEY_V2_PREFIX = "UVFNdXNpYyBFbmNWMixLZXk6"
    private val EKEY_V2_KEY1 = byteArrayOf(
        0x33, 0x38, 0x36, 0x5a, 0x4a, 0x59, 0x21, 0x40,
        0x23, 0x2a, 0x24, 0x25, 0x5e, 0x26, 0x29, 0x28,
    )
    private val EKEY_V2_KEY2 = byteArrayOf(
        0x2a, 0x2a, 0x23, 0x21, 0x28, 0x23, 0x24, 0x25,
        0x26, 0x5e, 0x61, 0x31, 0x63, 0x5a, 0x2c, 0x54,
    )

    // ---- TEA constants ----
    private const val TEA_ROUNDS = 16
    private const val TEA_DELTA = 0x9e3779b9L
    private val TEA_EXPECTED_SUM: Long = ((TEA_ROUNDS * TEA_DELTA) and 0xFFFFFFFFL)

    // ---- QMC2 constants ----
    private const val QMC_MAP_BOUNDARY = 0x7fff
    private const val QMC_MAP_INDEX_OFFSET = 71214
    private const val QMC_MAP_KEY_SIZE = 128
    private const val QMC_FIRST_SEGMENT = 0x80
    private const val QMC_OTHER_SEGMENT = 0x1400
    private const val QMC_RC4_STREAM_SIZE = QMC_OTHER_SEGMENT + 512

    // ---- AES helper (used by KggKeyDatabase for DB page decryption) ----
    fun aesCbcDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    // ---- TEA ECB ----
    private fun uint32(n: Long) = n and 0xFFFFFFFFL

    private fun teaSingleRound(v: Long, sum: Long, k1: Long, k2: Long): Long =
        uint32(uint32(v shl 4) + k1 xor (v + sum) xor (uint32(v ushr 5) + k2))

    private fun teaEcbDecrypt(value: Long, key: LongArray): Long {
        var y = value ushr 32 and 0xFFFFFFFFL
        var z = value and 0xFFFFFFFFL
        var sum = TEA_EXPECTED_SUM
        repeat(TEA_ROUNDS) {
            z = uint32(z - teaSingleRound(y, sum, key[2], key[3]))
            y = uint32(y - teaSingleRound(z, sum, key[0], key[1]))
            sum = uint32(sum - TEA_DELTA)
        }
        return (y shl 32) or z
    }

    // ---- TEA CBC ----
    private fun teaCbcDecrypt(cipher: ByteArray, key: ByteArray): ByteArray {
        if (cipher.size % 8 != 0 || cipher.size < 16) return ByteArray(0)
        val teaKey = LongArray(4) { i ->
            ((key[i * 4].toLong() and 0xff) shl 24) or
                ((key[i * 4 + 1].toLong() and 0xff) shl 16) or
                ((key[i * 4 + 2].toLong() and 0xff) shl 8) or
                (key[i * 4 + 3].toLong() and 0xff)
        }
        return teaCbcDecrypt(cipher, teaKey)
    }

    private fun teaCbcDecrypt(cipher: ByteArray, teaKey: LongArray): ByteArray {
        if (cipher.size % 8 != 0 || cipher.size < 16) return ByteArray(0)
        var iv1 = 0L
        var iv2 = 0L

        fun readBE64(buf: ByteArray, off: Int): Long {
            var v = 0L
            for (i in 0 until 8) v = (v shl 8) or (buf[off + i].toLong() and 0xff)
            return v
        }

        fun writeBE64(buf: ByteArray, off: Int, v: Long) {
            for (i in 7 downTo 0) buf[off + (7 - i)] = ((v ushr (i * 8)) and 0xff).toByte()
        }

        val header = ByteArray(16)
        var pos = 0

        fun decryptRound(dst: ByteArray, dstOff: Int, src: ByteArray, srcOff: Int) {
            val iv1Next = readBE64(src, srcOff)
            val iv2Next = teaEcbDecrypt(iv1Next xor iv2, teaKey)
            val plain = iv2Next xor iv1
            iv1 = iv1Next
            iv2 = iv2Next
            writeBE64(dst, dstOff, plain)
        }

        decryptRound(header, 0, cipher, 0)
        decryptRound(header, 8, cipher, 8)
        pos = 16

        val hdrSkip = 1 + (header[0].toInt() and 7) + 2 // kFixedSaltLen = 2
        val zeroPad = 7
        val realLen = cipher.size - hdrSkip - zeroPad
        if (realLen <= 0) return ByteArray(0)
        val result = ByteArray(realLen)

        val copyLen = minOf(16 - hdrSkip, realLen).coerceAtLeast(0)
        if (copyLen > 0) System.arraycopy(header, hdrSkip, result, 0, copyLen)

        var outPos = copyLen
        var remaining = realLen - copyLen
        val block = ByteArray(8)
        while (remaining > 0 && pos + 8 <= cipher.size) {
            decryptRound(block, 0, cipher, pos)
            pos += 8
            val take = minOf(8, remaining)
            System.arraycopy(block, 0, result, outPos, take)
            outPos += take
            remaining -= take
        }
        return result
    }

    // ---- ekey decryption ----
    private fun ekeyDecryptV1(ekey: String): ByteArray {
        val raw = java.util.Base64.getDecoder().decode(ekey)
        if (raw.size < 8) return ByteArray(0)
        val b = raw
        val teaKey = longArrayOf(
            (0x69005600L or ((b[0].toLong() and 0xff) shl 16) or (b[1].toLong() and 0xff)) and 0xFFFFFFFFL,
            (0x46003800L or ((b[2].toLong() and 0xff) shl 16) or (b[3].toLong() and 0xff)) and 0xFFFFFFFFL,
            (0x2b002000L or ((b[4].toLong() and 0xff) shl 16) or (b[5].toLong() and 0xff)) and 0xFFFFFFFFL,
            (0x15000b00L or ((b[6].toLong() and 0xff) shl 16) or (b[7].toLong() and 0xff)) and 0xFFFFFFFFL,
        )
        val decrypted = teaCbcDecrypt(raw.copyOfRange(8, raw.size), teaKey)
        return raw.copyOfRange(0, 8) + decrypted
    }

    /** Decrypt an ekey string (v1 bare-Base64 or v2 with prefix) into the raw key bytes. */
    fun ekeyDecrypt(ekey: String): ByteArray {
        if (ekey.startsWith(EKEY_V2_PREFIX)) {
            val rest = ekey.substring(EKEY_V2_PREFIX.length).toByteArray(Charsets.UTF_8)
            var result = teaCbcDecrypt(rest, EKEY_V2_KEY1)
            result = teaCbcDecrypt(result, EKEY_V2_KEY2)
            return ekeyDecryptV1(result.toString(Charsets.UTF_8))
        }
        return ekeyDecryptV1(ekey)
    }

    // ---- QMC2 MAP cipher (key < 300 bytes) ----
    internal class QMC2Map(key: ByteArray) {
        private val keyMap = ByteArray(QMC_MAP_KEY_SIZE) { i ->
            val j = (i * i + QMC_MAP_INDEX_OFFSET) % key.size
            val shift = (j + 4) % 8
            val b = key[j].toInt() and 0xff
            ((b shl shift) or (b ushr shift) and 0xff).toByte()
        }

        fun decrypt(data: ByteArray, offset: Int, length: Int) {
            var cur = offset
            for (i in 0 until length) {
                val idx = if (cur <= QMC_MAP_BOUNDARY) cur else cur % QMC_MAP_BOUNDARY
                data[i] = (data[i].toInt() xor (keyMap[idx % keyMap.size].toInt() and 0xff)).toByte()
                cur++
            }
        }
    }

    // ---- QMC2 RC4 cipher (key >= 300 bytes) ----
    internal class QMC2RC4(private val key: ByteArray) {
        private val hash: Long
        private val stream: ByteArray

        init {
            var h = 1L
            for (b in key) {
                if (b.toInt() == 0) continue
                val next = (h * (b.toLong() and 0xff)) and 0xFFFFFFFFL
                if (next <= h) break
                h = next
            }
            hash = h
            stream = rc4Keystream(key, QMC_RC4_STREAM_SIZE)
        }

        private fun rc4Keystream(k: ByteArray, len: Int): ByteArray {
            val n = k.size
            val s = IntArray(n) { it and 0xff }
            var j = 0
            for (i in 0 until n) {
                j = (j + s[i] + (k[i].toInt() and 0xff)) % n
                val t = s[i]; s[i] = s[j]; s[j] = t
            }
            val out = ByteArray(len)
            var a = 0; var b = 0
            for (kk in 0 until len) {
                a = (a + 1) % n
                b = (b + s[a]) % n
                val t = s[a]; s[a] = s[b]; s[b] = t
                out[kk] = s[(s[a] + s[b]) % n].toByte()
            }
            return out
        }

        private fun segmentKey(segmentId: Long, seed: Byte): Long {
            val s = seed.toLong() and 0xff
            if (s == 0L) return 0L
            return Math.floor((hash.toDouble() / (s * (segmentId + 1))) * 100.0).toLong()
        }

        fun decrypt(data: ByteArray, audioOffset: Long, length: Int) {
            val n = key.size
            var offset = audioOffset
            var pos = 0

            // First segment: QMC_FIRST_SEGMENT bytes
            if (offset < QMC_FIRST_SEGMENT) {
                val processLen = minOf(length, QMC_FIRST_SEGMENT - offset.toInt())
                for (i in 0 until processLen) {
                    val idx = segmentKey(offset, key[(offset % n).toInt()]) % n
                    data[i] = (data[i].toInt() xor (key[idx.toInt()].toInt() and 0xff)).toByte()
                    offset++
                }
                pos = processLen
            }

            // Subsequent segments
            while (pos < length) {
                val segmentIdx = offset / QMC_OTHER_SEGMENT
                val segmentOffset = (offset % QMC_OTHER_SEGMENT).toInt()
                val skipLen = (segmentKey(segmentIdx, key[(segmentIdx % n).toInt()]) and 0x1ff).toInt()
                val processLen = minOf(length - pos, QMC_OTHER_SEGMENT - segmentOffset)
                for (i in 0 until processLen) {
                    data[pos + i] = (data[pos + i].toInt() xor
                        (stream[skipLen + segmentOffset + i].toInt() and 0xff)).toByte()
                }
                offset += processLen
                pos += processLen
            }
        }
    }

    /** Holder for a resolved QMC2 decryptor (either MAP or RC4). */
    sealed class QMC2Cipher {
        class Map(private val impl: QMC2Map) : QMC2Cipher() {
            fun decrypt(data: ByteArray, offset: Int, length: Int) = impl.decrypt(data, offset, length)
        }
        class Rc4(private val impl: QMC2RC4) : QMC2Cipher() {
            fun decrypt(data: ByteArray, audioOffset: Long, length: Int) = impl.decrypt(data, audioOffset, length)
        }
    }

    /** Decode an ekey string into a usable QMC2 cipher, or null if decoding fails. */
    fun createQMC2(ekey: String): QMC2Cipher? {
        val key = ekeyDecrypt(ekey)
        if (key.isEmpty()) return null
        return if (key.size < 300) QMC2Cipher.Map(QMC2Map(key))
        else QMC2Cipher.Rc4(QMC2RC4(key))
    }

    /**
     * Stream-decrypt a KGG audio payload into [output].
     *
     * @param input     Source path of the .kgg file.
     * @param audioOffset Byte offset where the audio payload begins in the file.
     * @param cipher    Resolved QMC2 cipher for this file.
     * @param output    Destination stream for the decrypted audio bytes.
     */
    fun decryptStream(input: Path, audioOffset: Long, cipher: QMC2Cipher, output: OutputStream) {
        BufferedInputStream(Files.newInputStream(input)).use { source ->
            skipFully(source, audioOffset)
            val buffer = ByteArray(256 * 1024)
            var absoluteOffset = 0L
            while (true) {
                val read = source.read(buffer)
                if (read < 0) break
                when (cipher) {
                    is QMC2Cipher.Map -> cipher.decrypt(buffer, absoluteOffset.toInt(), read)
                    is QMC2Cipher.Rc4 -> cipher.decrypt(buffer, absoluteOffset, read)
                }
                output.write(buffer, 0, read)
                absoluteOffset += read
            }
        }
    }

    /** Detect audio format from first bytes of decrypted payload (same magic bytes as KgmaCipher). */
    fun detectAudioFormat(prefix: ByteArray): NativeAudioFormat = when {
        prefix.size >= 4 && prefix.copyOfRange(0, 4).decodeToString() == "fLaC" -> NativeAudioFormat.Flac
        prefix.size >= 3 && prefix.copyOfRange(0, 3).decodeToString() == "ID3" -> NativeAudioFormat.Mp3
        prefix.size >= 4 && prefix.copyOfRange(0, 4).decodeToString() == "OggS" -> NativeAudioFormat.Ogg
        prefix.size >= 2 && (prefix[0].toInt() and 0xff) == 0xff && (prefix[1].toInt() and 0xe0) == 0xe0 -> NativeAudioFormat.Mp3
        else -> NativeAudioFormat.Unknown
    }

    fun littleEndianInt(buf: ByteArray, offset: Int): Int =
        (buf[offset].toInt() and 0xff) or
            ((buf[offset + 1].toInt() and 0xff) shl 8) or
            ((buf[offset + 2].toInt() and 0xff) shl 16) or
            ((buf[offset + 3].toInt() and 0xff) shl 24)

    private fun skipFully(source: java.io.InputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = source.skip(remaining)
            if (skipped > 0) remaining -= skipped
            else {
                check(source.read() >= 0) { "KGG 文件数据不完整" }
                remaining--
            }
        }
    }

    /** Minimal KGG v5 file header size (enough to read audioHash length field). */
    const val KGG_MINIMUM_HEADER = 76
}