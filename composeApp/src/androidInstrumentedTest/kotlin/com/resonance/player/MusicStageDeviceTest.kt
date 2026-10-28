package com.resonance.player

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.ui.NowPlayingOverlay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real device window and native controls, with no user library, account or network service. */
class MusicStageDeviceTest {
    @get:Rule val ui = createComposeRule()
    private val args = InstrumentationRegistry.getArguments()
    private val mode = if (args.getString("stageTheme") == "light") ThemeMode.Light else ThemeMode.Dark
    private val label = args.getString("stageLabel") ?: "device"
    private val tracks = listOf(
        Track("stage-device-a", "夜色里的电台", "Resonance 测试音乐", "原生界面调试", "4:00", 7),
        Track("stage-device-b", "一首名称很长也不应该挤走播放按钮的测试歌曲", "仅用于设备测试的歌手", "原生界面调试", "3:00", 2),
    )
    private var seek = 0f
    private var selected: Track? = null
    private var nextCount = 0

    private fun show(largeText: Boolean = false) {
        ui.mainClock.autoAdvance = false
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.5f else density.fontScale)) {
                var player by remember { mutableStateOf(PlayerState(currentTrack = if (largeText) tracks.last() else tracks.first(), progress = .38f)) }
                var open by remember { mutableStateOf(true) }
                val lyrics = LyricsUiState.Ready(Lyrics(player.currentTrack!!.id,
                    listOf(LyricLine(0, "把忙碌留在窗外"), LyricLine(50_000, "把此刻交给旋律", "Let this moment belong to music"),
                        LyricLine(130_000, "让夜色慢慢流淌")), true, false, "原创测试文本"))
                ResonanceTheme(mode, reducedMotion = false) {
                    if (open) NowPlayingOverlay(
                        playerState = player, queue = tracks, lyricsState = lyrics,
                        onDismiss = { open = false }, onTogglePlay = { player = player.copy(isPlaying = !player.isPlaying) },
                        onPrevious = { player = player.copy(currentTrack = tracks.first()) },
                        onNext = { nextCount++; player = player.copy(currentTrack = tracks.last()) },
                        onToggleShuffle = { player = player.copy(shuffleEnabled = !player.shuffleEnabled) },
                        onCycleRepeat = {}, onSeek = { seek = it; player = player.copy(progress = it) },
                        onTrackSelected = { selected = it; player = player.copy(currentTrack = it) },
                        onToggleFavorite = {}, onRefreshLyrics = {},
                    ) else Text("播放器已收起")
                }
            }
        }
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
    }

    private fun capture(name: String) {
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Dialog windows also animate on Android's real clock, outside the Compose test clock.
        instrumentation.waitForIdleSync()
        Thread.sleep(300)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "device-stage")
        check(directory.exists() || directory.mkdirs())
        // Capture only this test's native Compose content, never another foreground app.
        val captureNode = if (ui.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty()) ui.onNode(isDialog()) else ui.onRoot()
        val bitmap = captureNode.captureToImage().asAndroidBitmap()
        try {
            File(directory, "$label-${mode.name.lowercase()}-$name.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally { bitmap.recycle() }
    }

    private fun assertTransportInsideWindow() {
        val root = ui.onRoot().fetchSemanticsNode().boundsInRoot
        for (label in listOf("关闭正在播放", "播放选项", "播放进度", "上一首", "下一首")) {
            val node = ui.onNodeWithContentDescription(label).assertIsDisplayed().fetchSemanticsNode()
            val bounds = node.touchBoundsInRoot
            val minimum = with(ui.density) { 48.dp.toPx() }
            assertTrue("$label target: $bounds", bounds.width >= minimum - 1 && bounds.height >= minimum - 1)
            assertTrue("$label outside window: $bounds / $root", bounds.top >= root.top - 1 && bounds.bottom <= root.bottom + 1 &&
                bounds.left >= root.left - 1 && bounds.right <= root.right + 1)
        }
    }

    @Test fun lyricsQueueSeekAndDismissUseRealControls() {
        show()
        assertTransportInsideWindow()
        val viewport = ui.onRoot().fetchSemanticsNode().boundsInRoot
        if (viewport.height > viewport.width && with(ui.density) { viewport.height.toDp() } >= 680.dp) {
            ui.onNodeWithTag("player-artwork", useUnmergedTree = true).assertWidthIsAtLeast(180.dp)
        }
        capture("cover")
        ui.onNodeWithContentDescription("播放进度").performSemanticsAction(SemanticsActions.SetProgress) { it(.45f) }
        ui.runOnIdle { assertEquals(.45f, seek, .001f) }
        ui.onNodeWithText("歌词", useUnmergedTree = true).performClick()
        ui.mainClock.advanceTimeBy(500)
        ui.onNodeWithText("把此刻交给旋律").assertIsDisplayed()
        capture("lyrics")
        ui.onNodeWithText("队列", useUnmergedTree = true).performClick()
        ui.mainClock.advanceTimeBy(500)
        capture("queue")
        ui.onNodeWithText(tracks.last().title).performClick()
        ui.mainClock.advanceTimeBy(500)
        ui.runOnIdle { assertEquals(tracks.last(), selected) }
        ui.onNodeWithContentDescription("关闭正在播放").performClick()
        ui.mainClock.advanceTimeBy(500)
        ui.onNodeWithText("播放器已收起").assertIsDisplayed()
    }

    @Test fun largeTextKeepsTransportAndPlaybackSettingsReachable() {
        show(largeText = true)
        assertTransportInsideWindow()
        capture("large-text")
        ui.onNodeWithContentDescription("播放选项").performClick()
        ui.mainClock.advanceTimeBy(500)
        ui.onNodeWithText("播放设置 · 音量与速度").performClick()
        ui.mainClock.advanceTimeBy(500)
        ui.onNodeWithContentDescription("音量").assertIsDisplayed()
        ui.onNodeWithText("1.0x").assertIsDisplayed()
        capture("playback-settings")
    }

    @Test fun playingAtmosphereAndNextTrackRemainInteractive() {
        show()
        ui.onNodeWithContentDescription("播放").performClick()
        ui.mainClock.advanceTimeBy(1_000)
        ui.onNodeWithContentDescription("暂停").assertIsDisplayed()
        capture("playing")
        ui.onNodeWithContentDescription("下一首").performClick()
        ui.mainClock.advanceTimeBy(500)
        ui.runOnIdle { assertEquals(1, nextCount) }
        ui.onNodeWithContentDescription("暂停").performClick()
        ui.mainClock.advanceTimeBy(500)
        ui.onNodeWithContentDescription("播放").assertIsDisplayed()
        capture("paused-next")
    }
}
