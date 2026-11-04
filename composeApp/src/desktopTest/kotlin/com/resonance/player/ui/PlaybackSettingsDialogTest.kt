package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import com.resonance.player.design.*
import com.resonance.player.model.*
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class PlaybackSettingsDialogTest {
    private fun ImageComposeScene.nodes(): List<SemanticsNode> {
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
    }

    private fun ImageComposeScene.node(label: String): SemanticsNode = nodes().firstOrNull {
        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true ||
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true
    } ?: error("Missing $label")

    private fun ImageComposeScene.click(label: String) {
        assertTrue(assertNotNull(node(label).config.getOrNull(SemanticsActions.OnClick)?.action).invoke())
    }

    @Test fun dialogIsolatesEveryContentSlotFromHostGlassWithoutChangingHostState() {
        SwingUtilities.invokeAndWait {
            val observed = mutableMapOf<String, Boolean>()
            val scene = ImageComposeScene(390, 844) {
                val glass = rememberHazeState()
                CompositionLocalProvider(LocalGlassState provides glass) {
                    ResonanceTheme(reducedMotion = true) {
                        observed["host"] = LocalGlassState.current === glass
                        FrostedAlertDialog(
                            onDismissRequest = {},
                            icon = { observed["icon"] = LocalGlassState.current == null; Text("图标") },
                            title = { observed["title"] = LocalGlassState.current == null; Text("播放设置") },
                            text = { observed["text"] = LocalGlassState.current == null; Text("窗口内容") },
                            confirmButton = {
                                observed["confirm"] = LocalGlassState.current == null
                                TextButton(onClick = {}) { Text("完成") }
                            },
                            dismissButton = {
                                observed["dismiss"] = LocalGlassState.current == null
                                TextButton(onClick = {}) { Text("取消") }
                            },
                        )
                        observed["hostAfter"] = LocalGlassState.current === glass
                    }
                }
            }
            try {
                scene.render(0).close()
                for (slot in listOf("host", "icon", "title", "text", "confirm", "dismiss", "hostAfter")) {
                    assertEquals(true, observed[slot], "$slot must have window-local glass state")
                }
            } finally { scene.close() }
        }
    }

    @Test fun volumeOpensAdjustsAndClosesWithPlaybackControlsStillResponsive() {
        SwingUtilities.invokeAndWait {
            for (mode in listOf(ThemeMode.Light, ThemeMode.Dark)) {
                for ((height, scale) in listOf(844 to 1f, 640 to 1.5f)) {
                    var volumeChanges = 0
                    var speedChanges = 0
                    var balanceChanges = 0
                    val scene = ImageComposeScene(390, height, density = Density(1f, scale)) {
                        val glass = rememberHazeState()
                        var player by remember { mutableStateOf(PlayerState(
                            currentTrack = Track("volume-fixture", "音量测试", "测试歌手", "测试专辑", "4:01", 0),
                            volume = 0.8f,
                        )) }
                        CompositionLocalProvider(LocalGlassState provides glass) {
                            ResonanceTheme(mode, reducedMotion = true) {
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
                    try {
                        scene.render(0).close()
                        if (height == 844) scene.click("音量") else {
                            scene.click("播放选项"); scene.render(100_000_000).close()
                            scene.click("播放设置 · 音量与速度")
                        }
                        scene.render(500_000_000).close()
                        scene.node("播放设置")
                        assertEquals(0, volumeChanges)
                        assertEquals(0, speedChanges)
                        assertEquals(0, balanceChanges)
                        val slider = scene.nodes().single {
                            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("音量") == true &&
                                it.config.getOrNull(SemanticsActions.SetProgress) != null
                        }
                        val progress = assertNotNull(slider.config.getOrNull(SemanticsActions.SetProgress)?.action)
                        assertTrue(progress(0.4f))
                        scene.render(600_000_000).close()
                        assertEquals(1, volumeChanges)
                        scene.node("40%")
                        scene.click("1.0x"); scene.render(700_000_000).close()
                        scene.click("1.25x 倍速"); scene.render(800_000_000).close()
                        assertEquals(1, speedChanges)
                        scene.node("1.25x")
                        scene.click("音量平衡"); scene.render(900_000_000).close()
                        assertEquals(1, balanceChanges)
                        scene.click("完成"); scene.render(1_000_000_000).close()
                        assertTrue(scene.nodes().none { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == "播放设置" } == true })
                        scene.click("播放"); scene.render(1_100_000_000).close()
                        scene.node("暂停")
                    } finally { scene.close() }
                }
            }
        }
    }
}
