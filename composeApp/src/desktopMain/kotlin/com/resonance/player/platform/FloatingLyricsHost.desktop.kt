package com.resonance.player.platform
import androidx.compose.runtime.Composable
import com.resonance.player.model.*
import com.resonance.player.ui.DesktopFloatingLyricsWindow
@Composable
actual fun FloatingLyricsHost(player: PlayerState, lyrics: LyricsUiState, settings: MusicFeatureSettings,
    onSettingsChange: (MusicFeatureSettings) -> Unit, onTogglePlay: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) {
    if (settings.floatingLyrics) DesktopFloatingLyricsWindow(player, lyrics, settings, onSettingsChange, onTogglePlay, onPrevious, onNext)
}
