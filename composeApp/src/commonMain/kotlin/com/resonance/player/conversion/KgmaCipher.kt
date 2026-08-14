package com.resonance.player.conversion

/** Pure KGMA v3/slot-1 cipher primitives, shared by Android and Windows streaming converters. */
object KgmaCipher {
    val header = byteArrayOf(
        0x7c, 0xd5.toByte(), 0x32, 0xeb.toByte(), 0x86.toByte(), 0x02, 0x7f, 0x4b,
        0xa8.toByte(), 0xaf.toByte(), 0xa6.toByte(), 0x8e.toByte(), 0x0f, 0xff.toByte(), 0x99.toByte(), 0x14,
    )
    private val slot2Key = byteArrayOf(0x6c, 0x2c, 0x2f, 0x27)
    private const val fileBoxSuffix = 0x6b

    fun createBoxes(cryptoKey: ByteArray, md5: (ByteArray) -> ByteArray): Boxes {
        require(cryptoKey.size == 16) { "KGMA crypto key must contain 16 bytes" }
        return Boxes(
            slotBox = reverseDigestPairs(md5(slot2Key)),
            fileBox = reverseDigestPairs(md5(cryptoKey)) + fileBoxSuffix.toByte(),
        )
    }

    fun decryptInPlace(buffer: ByteArray, length: Int, absoluteOffset: Long, boxes: Boxes) {
        require(length in 0..buffer.size)
        for (index in 0 until length) {
            val position = absoluteOffset + index
            var value = buffer[index].toInt() and 0xff
            value = value xor (boxes.fileBox[(position % 17).toInt()].toInt() and 0xff)
            value = value xor ((value shl 4) and 0xff)
            value = value xor (boxes.slotBox[(position % 16).toInt()].toInt() and 0xff)
            value = value xor xorCollapse(position)
            buffer[index] = value.toByte()
        }
    }

    fun detectAudioFormat(prefix: ByteArray): NativeAudioFormat = when {
        prefix.size >= 4 && prefix.copyOfRange(0, 4).decodeToString() == "fLaC" -> NativeAudioFormat.Flac
        prefix.size >= 3 && prefix.copyOfRange(0, 3).decodeToString() == "ID3" -> NativeAudioFormat.Mp3
        prefix.size >= 4 && prefix.copyOfRange(0, 4).decodeToString() == "OggS" -> NativeAudioFormat.Ogg
        prefix.size >= 2 && (prefix[0].toInt() and 0xff) == 0xff && (prefix[1].toInt() and 0xe0) == 0xe0 -> NativeAudioFormat.Mp3
        else -> NativeAudioFormat.Unknown
    }

    fun littleEndianInt(buffer: ByteArray, offset: Int): Int =
        (buffer[offset].toInt() and 0xff) or
            ((buffer[offset + 1].toInt() and 0xff) shl 8) or
            ((buffer[offset + 2].toInt() and 0xff) shl 16) or
            ((buffer[offset + 3].toInt() and 0xff) shl 24)

    private fun reverseDigestPairs(digest: ByteArray): ByteArray {
        require(digest.size == 16) { "MD5 digest must contain 16 bytes" }
        return ByteArray(16) { index ->
            val pairStart = index and 0xfe
            digest[14 - pairStart + (index and 1)]
        }
    }

    private fun xorCollapse(position: Long): Int =
        (position.toInt() and 0xff) xor
            ((position ushr 8).toInt() and 0xff) xor
            ((position ushr 16).toInt() and 0xff) xor
            ((position ushr 24).toInt() and 0xff)

    data class Boxes(val slotBox: ByteArray, val fileBox: ByteArray)
}

enum class NativeAudioFormat(val extension: String) {
    Mp3("mp3"),
    Flac("flac"),
    Ogg("ogg"),
    Unknown("bin"),
}
