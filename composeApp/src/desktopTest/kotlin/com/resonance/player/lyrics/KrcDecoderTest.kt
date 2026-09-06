package com.resonance.player.lyrics

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KrcDecoderTest {
    @Test
    fun decodesValidKrcData() {
        val plainLyrics = """
            [ti:Test Song]
            [ar:Test Singer]
            [al:Test Album]
            [1000,2000]<0,500,0>First <500,1500,0>Line
            [00:02.500]Second line from LRC format
            [3000,2000]<0,1000,0>Third <1000,1000,0>Line
        """.trimIndent()

        // 1. Zlib compress
        val compressed = Deflater().let { deflater ->
            val input = plainLyrics.toByteArray(StandardCharsets.UTF_8)
            deflater.setInput(input)
            deflater.finish()
            val bos = ByteArrayOutputStream()
            val buf = ByteArray(1024)
            while (!deflater.finished()) {
                val count = deflater.deflate(buf)
                bos.write(buf, 0, count)
            }
            deflater.end()
            bos.toByteArray()
        }

        // 2. XOR encrypt with Kugou key
        val key = byteArrayOf(
            0x40.toByte(), 0x47.toByte(), 0x61.toByte(), 0x77.toByte(),
            0x5e.toByte(), 0x32.toByte(), 0x74.toByte(), 0x47.toByte(),
            0x51.toByte(), 0x36.toByte(), 0x31.toByte(), 0x2d.toByte(),
            0xce.toByte(), 0xd2.toByte(), 0x6e.toByte(), 0x69.toByte(),
        )
        val encrypted = ByteArray(compressed.size) { i ->
            (compressed[i].toInt() xor key[i % key.size].toInt()).toByte()
        }

        // 3. Prepend 'krc1' magic header
        val header = byteArrayOf(0x6B, 0x72, 0x63, 0x31)
        val fullKrcBytes = header + encrypted

        val lines = KrcDecoder.decode(fullKrcBytes)

        assertTrue(lines.isNotEmpty())
        assertEquals(3, lines.size)
        assertEquals("First Line", lines[0].text)
        assertEquals(1000L, lines[0].timestampMs)
        assertEquals("Second line from LRC format", lines[1].text)
        assertEquals(2500L, lines[1].timestampMs)
        assertEquals("Third Line", lines[2].text)
        assertEquals(3000L, lines[2].timestampMs)
    }
}
