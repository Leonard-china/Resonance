package com.resonance.player.platform

import androidx.compose.runtime.Composable
import com.resonance.player.model.*

@Composable
expect fun FloatingLyricsHost(player: PlayerState, lyrics: LyricsUiState, settings: MusicFeatureSettings,
    onSettingsChange: (MusicFeatureSettings) -> Unit, onTogglePlay: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit)

fun floatingLyricLines(player: PlayerState, state: LyricsUiState, settings: MusicFeatureSettings, positionMs: Long? = null): Pair<String, String> {
    val track = player.currentTrack ?: return "Resonance" to ""
    val lyrics = (state as? LyricsUiState.Ready)?.lyrics ?: return track.title to track.artist
    if (lyrics.instrumental) return track.title to "纯音乐"
    val ms = (positionMs ?: player.positionMs ?: (durationTextToSeconds(track.durationText) * 1000L * player.progress).toLong()) + lyrics.offsetMs
    val index = if (lyrics.synchronized) lyrics.lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= ms }.coerceAtLeast(0) else 0
    val line = lyrics.lines.getOrNull(index) ?: return track.title to track.artist
    return when (settings.lyricDisplay) {
        LyricDisplay.Original -> line.text to lyrics.lines.getOrNull(index + 1)?.text.orEmpty()
        LyricDisplay.Translation -> (line.translation ?: line.text) to ""
        LyricDisplay.Bilingual -> line.text to line.translation.orEmpty()
    }
}
