package com.resonance.player.platform
import androidx.compose.runtime.Composable
import com.resonance.player.model.*
@Composable
actual fun FloatingLyricsHost(player: PlayerState, lyrics: LyricsUiState, settings: MusicFeatureSettings,
    onSettingsChange: (MusicFeatureSettings) -> Unit, onTogglePlay: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) = Unit
