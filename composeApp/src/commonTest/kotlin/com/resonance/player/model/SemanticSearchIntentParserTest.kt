package com.resonance.player.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SemanticSearchIntentParserTest {

    @Test
    fun parseRunningAndBpmQuery() {
        val intent = SemanticSearchIntentParser.parse("来点适合跑步的 140 BPM 燃歌")
        assertTrue("跑步" in intent.scenes)
        assertTrue("燃" in intent.moods)
        assertNotNull(intent.bpmRange)
        val range = intent.bpmRange
        assertTrue(140 in range)
    }

    @Test
    fun parseRainyHealingAcousticQuery() {
        val intent = SemanticSearchIntentParser.parse("雨天想听治愈的民谣或者纯音乐")
        assertTrue("雨天" in intent.scenes)
        assertTrue("治愈" in intent.moods)
        assertTrue("民谣" in intent.genres)
        assertTrue("纯音乐" in intent.genres)
    }

    @Test
    fun parseSlowBalladQuery() {
        val intent = SemanticSearchIntentParser.parse("深夜安静的慢歌")
        assertTrue("深夜" in intent.scenes)
        assertTrue("安静" in intent.moods)
        assertNotNull(intent.bpmRange)
        val range = intent.bpmRange
        assertTrue(70 in range)
    }
}
