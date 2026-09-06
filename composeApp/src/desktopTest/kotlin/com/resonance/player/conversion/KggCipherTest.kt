package com.resonance.player.conversion

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KggCipherTest {

    @Test
    fun detectsAudioFormatsCorrectly() {
        assertEquals(
            NativeAudioFormat.Flac,
            KggCipher.detectAudioFormat("fLaC1234".toByteArray(Charsets.ISO_8859_1))
        )
        assertEquals(
            NativeAudioFormat.Mp3,
            KggCipher.detectAudioFormat("ID3\u0003\u0000".toByteArray(Charsets.ISO_8859_1))
        )
        assertEquals(
            NativeAudioFormat.Mp3,
            KggCipher.detectAudioFormat(byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x44.toByte()))
        )
        assertEquals(
            NativeAudioFormat.Ogg,
            KggCipher.detectAudioFormat("OggS1234".toByteArray(Charsets.ISO_8859_1))
        )
        assertEquals(
            NativeAudioFormat.Unknown,
            KggCipher.detectAudioFormat(byteArrayOf(0x00, 0x01, 0x02, 0x03))
        )
    }

    @Test
    fun qmc2MapIsSelfInverting() {
        val key = ByteArray(128) { (it * 7 + 13).toByte() }
        val original = ByteArray(1024) { (it xor 42).toByte() }
        val buffer = original.copyOf()

        val mapCipher = KggCipher.QMC2Map(key)
        // Decrypt (XOR)
        mapCipher.decrypt(buffer, 0, buffer.size)
        // Decrypt again (XOR is symmetric)
        val decrypted = buffer.copyOf()
        mapCipher.decrypt(decrypted, 0, decrypted.size)

        assertContentEquals(original, decrypted, "QMC2Map applied twice must restore the original data")
    }

    @Test
    fun qmc2Rc4IsSelfInvertingAcrossMultipleSegments() {
        val key = ByteArray(512) { (it * 13 + 37).toByte() }
        // Create data spanning multiple segments (> 0x1400 = 5120 bytes)
        val original = ByteArray(16 * 1024) { (it % 251).toByte() }
        val buffer = original.copyOf()

        val rc4Cipher1 = KggCipher.QMC2RC4(key)
        rc4Cipher1.decrypt(buffer, 0L, buffer.size)

        val rc4Cipher2 = KggCipher.QMC2RC4(key)
        val decrypted = buffer.copyOf()
        rc4Cipher2.decrypt(decrypted, 0L, decrypted.size)

        assertContentEquals(original, decrypted, "QMC2RC4 applied twice must restore the original data")
    }

    @Test
    fun decryptStreamWorksCorrectly() {
        val key = ByteArray(64) { (it + 5).toByte() }
        val map = KggCipher.QMC2Cipher.Map(KggCipher.QMC2Map(key))

        // Create a fake audio payload
        val fakeAudio = "ID3-test-audio-content-for-kgg-verification".toByteArray(Charsets.UTF_8)
        val encryptedAudio = fakeAudio.copyOf()
        KggCipher.QMC2Map(key).decrypt(encryptedAudio, 0, encryptedAudio.size)

        // Write a temp file with 16 bytes of header then the encrypted audio
        val temp = Files.createTempFile("kgg-stream-test-", ".kgg")
        try {
            val fileBytes = ByteArray(16) { 0 } + encryptedAudio
            Files.write(temp, fileBytes)

            val out = ByteArrayOutputStream()
            KggCipher.decryptStream(temp, 16L, map, out)

            assertContentEquals(fakeAudio, out.toByteArray())
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    @Test
    fun parsesKggV5HeaderCorrectly() {
        val hash = "E3E0ADDF0807C052CE7F27AC3B7B7E6D"
        val hashBytes = hash.toByteArray(Charsets.UTF_8)
        val header = ByteArray(72 + hashBytes.size)
        // audioOffset at 16..19 (512 = 0x0200)
        header[16] = 0x00; header[17] = 0x02; header[18] = 0x00; header[19] = 0x00
        // mode at 20..23 (5)
        header[20] = 0x05; header[21] = 0x00; header[22] = 0x00; header[23] = 0x00
        // hashLen at 68..71 (32)
        header[68] = hashBytes.size.toByte(); header[69] = 0; header[70] = 0; header[71] = 0
        System.arraycopy(hashBytes, 0, header, 72, hashBytes.size)

        val mode = KggCipher.littleEndianInt(header, 20)
        assertEquals(5, mode)
        val audioOffset = (KggCipher.littleEndianInt(header, 16).toLong()) and 0xFFFFFFFFL
        assertEquals(512L, audioOffset)
        val hashLen = KggCipher.littleEndianInt(header, 68)
        assertEquals(32, hashLen)
        val extractedHash = header.copyOfRange(72, 72 + hashLen).toString(Charsets.UTF_8)
        assertEquals(hash, extractedHash)
    }

    @Test
    fun testKuGouKeyDatabaseLoadsWhenPresent() {
        val dbFile = KggKeyDatabase.candidateDbPath()
        if (dbFile != null && dbFile.isFile) {
            val keyMap = KggKeyDatabase.loadKeyMap(dbFile)
            assertTrue(keyMap.isNotEmpty(), "KGG key database should contain keys if db file exists")
        }
    }
}