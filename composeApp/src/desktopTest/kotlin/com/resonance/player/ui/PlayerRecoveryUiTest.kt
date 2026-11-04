package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.design.LocalGlassState
import dev.chrisbanes.haze.rememberHazeState
import com.resonance.player.model.*
import com.resonance.player.platform.PlatformServices
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class PlayerRecoveryUiTest {
    private val track = Track("recovery-ui", "歌词空态夹具", "测试歌手", "测试专辑", "4:01", 0)
    private fun ImageComposeScene.nodes(): List<SemanticsNode> {
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
    }
    private fun ImageComposeScene.node(label: String): SemanticsNode = nodes().firstOrNull {
        it.config.getOrNull(SemanticsProperties.Text)?.any { t -> t.text == label } == true ||
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true
    } ?: error("Missing $label")
    private fun ImageComposeScene.click(label: String) {
        assertNotNull(node(label).config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
    }
    private fun ImageComposeScene.frame(time: Long) = render(time).close()
    private fun ImageComposeScene.capture(name: String) {
        val output = Path.of("build/lyrics-repair/screenshots"); Files.createDirectories(output)
        render(2_000_000_000).use { image -> image.encodeToData()!!.use { Files.write(output.resolve(name), it.bytes) } }
    }

    @Test fun emptyLyricsHaveOneSearchActionAndReachableToolsWithLargeText() {
        SwingUtilities.invokeAndWait {
            for ((width, height, scale) in listOf(Triple(390, 844, 1f), Triple(360, 640, 1.5f), Triple(720, 360, 1.5f))) {
                val scene = ImageComposeScene(width, height, density = Density(1f, scale)) {
                    val scope = rememberCoroutineScope()
                    val feature = remember { MusicFeatureController(Proxy.newProxyInstance(PlatformServices::class.java.classLoader,
                        arrayOf(PlatformServices::class.java)) { _, _, _ -> null } as PlatformServices, scope).apply { player = PlayerState(currentTrack = track) } }
                    CompositionLocalProvider(LocalMusicFeatures provides feature) {
                    ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                        NowPlayingOverlay(playerState = PlayerState(currentTrack = track), queue = listOf(track),
                            lyricsState = LyricsUiState.Unavailable("暂未找到匹配的歌词", false),
                            onDismiss = {}, onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {},
                            onCycleRepeat = {}, onSeek = {}, onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {})
                    }
                    }
                }
                try {
                    scene.frame(0); scene.click("歌词"); scene.frame(500_000_000)
                    val search = scene.node("查找歌词").touchBoundsInRoot
                    assertTrue(search.top >= 0 && search.bottom <= height, "$width/$height: $search")
                    assertTrue(scene.nodes().none { it.config.getOrNull(SemanticsProperties.Text)?.any { t -> t.text == "AI 查找歌词" } == true })
                    scene.capture("lyrics-empty-$width-$height-$scale.png")
                    scene.click("歌词工具"); scene.frame(1_000_000_000)
                    val generate = scene.node("AI 生成歌词文本").touchBoundsInRoot
                    assertTrue(generate.height > 0 && generate.top >= 0 && generate.bottom <= height)
                    scene.node("音频识别同步歌词")
                    scene.capture("lyrics-tools-$width-$height-$scale.png")
                } finally { scene.close() }
            }
        }
    }

    @Test fun favoriteCardOnlyShowsSongCount() {
        SwingUtilities.invokeAndWait {
            val scene = ImageComposeScene(390, 844) {
                ResonanceTheme {
                    PlaylistListTab(emptyList(), {}, {}, favoriteCount = 7, contentPadding = PaddingValues(16.dp), state = rememberLazyGridState())
                }
            }
            try {
                scene.frame(0); scene.node("我喜欢的音乐"); scene.node("7 首")
                assertTrue(scene.nodes().none { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text.contains("心动") } == true })
            } finally { scene.close() }
        }
    }

    @Test fun audioDialogHasBoundedListAndSelectionDoesNotInvokePlaybackWorkUntilChosen() {
        SwingUtilities.invokeAndWait {
            var selected: AudioEffectPreset? = null
            val scene = ImageComposeScene(390, 844, density = Density(1f, 1.5f)) {
                CompositionLocalProvider(LocalGlassState provides rememberHazeState()) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                    NowPlayingOverlay(playerState = PlayerState(currentTrack = track), queue = listOf(track),
                        lyricsState = LyricsUiState.Unavailable("暂无歌词", false),
                        onDismiss = {}, onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {},
                        onCycleRepeat = {}, onSeek = {}, onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {},
                        onAudioEffectPresetChange = { selected = it })
                }
                }
            }
            try {
                scene.frame(0); scene.click("音效"); scene.frame(500_000_000)
                assertNull(selected)
                scene.node("完成")
                scene.capture("audio-effects-large-text.png")
                scene.click("澎湃低音"); scene.frame(1_000_000_000)
                assertEquals(AudioEffectPreset.BassBoost, selected)
                assertTrue(scene.nodes().none { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == "完成" } == true })
            } finally { scene.close() }
        }
    }
}
