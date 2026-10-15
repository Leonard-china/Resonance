package com.resonance.player

import com.resonance.player.model.*
import com.resonance.player.platform.floatingLyricLines
import kotlin.test.*

class FloatingLyricsTest {
    @Test fun nativePositionKeepsBackgroundLyricsMovingWhenTrackDurationIsNotReady() {
        val track = Track("t", "Test", "Artist", "", "0:00", 0)
        val lyrics = LyricsUiState.Ready(Lyrics("t", listOf(LyricLine(0, "First"), LyricLine(2000, "Second")), true, false, "Local"))
        assertEquals("Second", floatingLyricLines(PlayerState(currentTrack = track), lyrics, MusicFeatureSettings(), 2500).first)
    }
    @Test fun translatedAndOriginalLinesShareTheOriginalTimeAxis() {
        val track = Track("t", "Test", "Artist", "", "0:10", 0)
        val player = PlayerState(currentTrack = track, progress = 0.25f)
        val lyrics = LyricsUiState.Ready(Lyrics("t", listOf(LyricLine(1000, "One", "一"), LyricLine(2000, "Two", "二")), true, false, "Local"))
        assertEquals("Two" to "二", floatingLyricLines(player, lyrics, MusicFeatureSettings()))
        assertEquals("二" to "", floatingLyricLines(player, lyrics, MusicFeatureSettings(lyricDisplay = LyricDisplay.Translation)))
        assertEquals("Two" to "", floatingLyricLines(player, lyrics, MusicFeatureSettings(lyricDisplay = LyricDisplay.Original)))
    }
    @Test fun unavailableTranslationNeverMakesOriginalLyricsDisappear() {
        val track = Track("t", "Test", "Artist", "", "0:10", 0)
        val lyrics = LyricsUiState.Ready(Lyrics("t", listOf(LyricLine(0, "Original")), true, false, "Local"))
        assertEquals("Original", floatingLyricLines(PlayerState(currentTrack = track), lyrics, MusicFeatureSettings(lyricDisplay = LyricDisplay.Translation)).first)
    }
}
