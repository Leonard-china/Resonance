package com.resonance.player

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.design.LocalGlassState
import dev.chrisbanes.haze.rememberHazeState
import com.resonance.player.model.*
import com.resonance.player.ui.NowPlayingOverlay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Native dialog windows over the real player's backdrop; generated metadata, no audio or accounts. */
class PlayerDialogsDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()

    @Test(timeout = 60_000) fun audioEffectsOpenSelectAndCloseWithoutStoppingPlayerControls() {
        val track = Track("dialog-fixture", "弹窗测试", "测试歌手", "测试专辑", "4:01", 0)
        var selected = AudioEffectPreset.Off
        ui.setContent {
            var player by remember { mutableStateOf(PlayerState(currentTrack = track)) }
            CompositionLocalProvider(LocalGlassState provides rememberHazeState()) {
            ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                NowPlayingOverlay(playerState = player, queue = listOf(track), lyricsState = LyricsUiState.Unavailable("暂无歌词", false),
                    onDismiss = {}, onTogglePlay = { player = player.copy(isPlaying = !player.isPlaying) },
                    onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {},
                    onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {},
                    onAudioEffectPresetChange = { selected = it; player = player.copy(audioEffectPreset = it) })
            }
            }
        }
        ui.onNodeWithText("音效").performClick()
        ui.onNodeWithText("澎湃低音").assertIsDisplayed().performClick()
        ui.onNodeWithText("澎湃低音").assertDoesNotExist()
        ui.runOnIdle { assertEquals(AudioEffectPreset.BassBoost, selected) }
        ui.onNodeWithContentDescription("播放").performClick()
        ui.onNodeWithContentDescription("暂停").assertIsDisplayed()
        ui.onNodeWithText("音效").performClick()
        ui.onNodeWithText("完成").performClick()
        ui.onNodeWithContentDescription("暂停").assertIsDisplayed()
    }
}
