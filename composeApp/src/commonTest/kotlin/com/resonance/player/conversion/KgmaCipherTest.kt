package com.resonance.player.conversion

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class KgmaCipherTest {
    @Test
    fun littleEndianHeaderFieldsAreDecoded() {
        val bytes = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04)
        assertEquals(0x04030201, KgmaCipher.littleEndianInt(bytes, 1))
    }

    @Test
    fun audioSignaturesAreDetected() {
        assertEquals(NativeAudioFormat.Mp3, KgmaCipher.detectAudioFormat("ID3data".encodeToByteArray()))
        assertEquals(NativeAudioFormat.Flac, KgmaCipher.detectAudioFormat("fLaCdata".encodeToByteArray()))
        assertEquals(NativeAudioFormat.Ogg, KgmaCipher.detectAudioFormat("OggSdata".encodeToByteArray()))
    }

    @Test
    fun chunkedDecryptionMatchesSinglePass() {
        val boxes = KgmaCipher.Boxes(ByteArray(16) { (it * 7).toByte() }, ByteArray(17) { (it * 11).toByte() })
        val singlePass = ByteArray(1_037) { (it * 13).toByte() }
        val chunked = singlePass.copyOf()
        KgmaCipher.decryptInPlace(singlePass, singlePass.size, 0, boxes)
        var offset = 0
        while (offset < chunked.size) {
            val size = minOf(127, chunked.size - offset)
            val block = chunked.copyOfRange(offset, offset + size)
            KgmaCipher.decryptInPlace(block, block.size, offset.toLong(), boxes)
            block.copyInto(chunked, offset)
            offset += size
        }
        assertContentEquals(singlePass, chunked)
    }

    @Test
    fun decryptingEncryptedBytesRestoresOriginal() {
        val boxes = KgmaCipher.Boxes(ByteArray(16) { (it * 3 + 1).toByte() }, ByteArray(17) { (it * 5 + 2).toByte() })
        val original = "ID3-resonance-kgma-roundtrip".encodeToByteArray()
        val encrypted = encryptForFixture(original, boxes)

        KgmaCipher.decryptInPlace(encrypted, encrypted.size, 0, boxes)

        assertContentEquals(original, encrypted)
    }

    private fun encryptForFixture(original: ByteArray, boxes: KgmaCipher.Boxes): ByteArray = ByteArray(original.size) { index ->
        val target = original[index]
        (0..255).first { candidate ->
            val probe = byteArrayOf(candidate.toByte())
            KgmaCipher.decryptInPlace(probe, 1, index.toLong(), boxes)
            probe[0] == target
        }.toByte()
    }
}
