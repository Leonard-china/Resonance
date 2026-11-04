package com.resonance.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.resonance.player.design.*
import com.resonance.player.model.*
import com.resonance.player.ui.NowPlayingOverlay
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Exercise Android's independent dialog window over a real host Haze source, without user audio. */
class PlaybackSettingsDialogDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()

    @Test(timeout = 60_000) fun volumeOpensAdjustsReopensAndLeavesPlaybackResponsive() {
        var volumeChanges = 0
        var speedChanges = 0
        var balanceChanges = 0
        ui.setContent {
            val glass = rememberHazeState()
            var player by remember { mutableStateOf(PlayerState(
                currentTrack = Track("volume-device-fixture", "音量弹窗测试", "测试歌手", "测试专辑", "4:01", 0),
                volume = 0.8f,
            )) }
            CompositionLocalProvider(LocalGlassState provides glass) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.matchParentSize().background(ResonanceColors.Canvas).hazeSource(glass))
                        NowPlayingOverlay(
                            playerState = player, queue = listOf(player.currentTrack!!), lyricsState = LyricsUiState.Idle,
                            onDismiss = {}, onTogglePlay = { player = player.copy(isPlaying = !player.isPlaying) },
                            onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {}, onSeek = {},
                            onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {},
                            onVolumeChange = { volumeChanges++; player = player.copy(volume = it) },
                            onSpeedChange = { speedChanges++; player = player.copy(playbackSpeed = it) },
                            onToggleAutoLoudness = { balanceChanges++; player = player.copy(autoLoudnessEnabled = !player.autoLoudnessEnabled) },
                        )
                    }
                }
            }
        }
        ui.onNodeWithText("音量").performClick()
        ui.onNodeWithText("播放设置").assertIsDisplayed()
        ui.runOnIdle {
            assertEquals(0, volumeChanges)
            assertEquals(0, speedChanges)
            assertEquals(0, balanceChanges)
        }
        ui.onNodeWithContentDescription("音量").performSemanticsAction(SemanticsActions.SetProgress) { it(0.4f) }
        ui.onNodeWithText("40%").assertIsDisplayed()
        ui.onNodeWithText("1.0x").performClick()
        ui.onNodeWithText("1.25x 倍速").performClick()
        ui.onNodeWithText("1.25x").assertIsDisplayed()
        ui.onNodeWithText("音量平衡").performClick()
        ui.onNodeWithText("完成").performClick()
        ui.onNodeWithText("播放设置").assertDoesNotExist()
        ui.runOnIdle {
            assertEquals(1, volumeChanges)
            assertEquals(1, speedChanges)
            assertEquals(1, balanceChanges)
        }
        repeat(3) {
            ui.onNodeWithText("音量").performClick()
            ui.onNodeWithText("40%").assertIsDisplayed()
            ui.onNodeWithText("完成").performClick()
        }
        ui.onNodeWithContentDescription("播放").performClick()
        ui.onNodeWithContentDescription("暂停").assertIsDisplayed()
    }
}
