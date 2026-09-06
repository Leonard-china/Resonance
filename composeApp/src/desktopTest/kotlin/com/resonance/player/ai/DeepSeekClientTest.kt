package com.resonance.player.ai

import com.resonance.player.lyrics.parseLrc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepSeekClientTest {
    @Test
    fun cleansAndParsesAiGeneratedLrcContent() {
        val client = DeepSeekClient()
        val mockLrc = """
            ```lrc
            [ti:California Dreamin']
            [ar:The Mamas & The Papas]
            [00:00.00]All the leaves are brown
            [00:04.50]And the sky is gray
            [00:09.20]I've been for a walk
            [00:13.80]On a winter's day
            ```
        """.trimIndent()

        val cleaned = client.cleanLrcResponse(mockLrc)
        val lines = parseLrc(cleaned)
        assertTrue(lines.isNotEmpty())
        assertEquals(4, lines.size)
        assertEquals("All the leaves are brown", lines[0].text)
        assertEquals(0L, lines[0].timestampMs)
        assertEquals("And the sky is gray", lines[1].text)
        assertEquals(4500L, lines[1].timestampMs)
        assertEquals("I've been for a walk", lines[2].text)
        assertEquals(9200L, lines[2].timestampMs)
    }
}
