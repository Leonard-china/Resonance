package com.resonance.player.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TasteAnalyticsEngineTest {

    @Test
    fun emptyTracksReturnsDefaultPersona() {
        val persona = TasteAnalyticsEngine.analyze(emptyList())
        assertEquals(0, persona.totalTracksAnalyzed)
        assertEquals("初遇拾音人", persona.title)
    }

    @Test
    fun tracksWithEmotionalKeywordsProduceNostalgicOrDreamerPersona() {
        val tracks = listOf(
            Track("1", "安静的晚安", "治愈歌手", "吉他民谣", "3:20", 1, isFavorite = true),
            Track("2", "想念的雨夜", "治愈歌手", "经典现场", "4:10", 2, isFavorite = true),
            Track("3", "岁月温柔", "民谣诗人", "时光胶片", "3:45", 3, isFavorite = false),
        )
        val persona = TasteAnalyticsEngine.analyze(tracks)
        assertEquals(3, persona.totalTracksAnalyzed)
        assertTrue(persona.dimensions.emotionalResonance >= 70)
        assertTrue(persona.title.isNotEmpty())
    }
}
