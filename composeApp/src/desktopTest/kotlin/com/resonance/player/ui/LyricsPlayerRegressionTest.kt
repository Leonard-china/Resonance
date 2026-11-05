package com.resonance.player.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.test.*

/** Exercise the real immersive player: component-only tests missed the cover/stage regression. */
@OptIn(ExperimentalComposeUiApi::class)
class LyricsPlayerRegressionTest {
    private val track = Track("player-lyrics-regression", "此刻的旋律", "测试歌手", "测试专辑", "0:30", 0)
    private val lyrics = Lyrics(track.id, (0..5).map { LyricLine(it * 5000L, "原创旋律第 $it 句") }, true, false, "测试夹具")

    private fun ImageComposeScene.nodes(): List<SemanticsNode> {
        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        return semanticsOwners.flatMap { walk(it.rootSemanticsNode) + walk(it.unmergedRootSemanticsNode) }
    }

    private fun ImageComposeScene.tag(tag: String) = nodes().firstOrNull {
        it.config.getOrNull(SemanticsProperties.TestTag) == tag
    } ?: error("Missing $tag")

    private fun ImageComposeScene.label(label: String) = nodes().firstOrNull {
        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true ||
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true
    } ?: error("Missing $label")

    private fun ImageComposeScene.click(label: String) {
        assertNotNull(label(label).config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
    }

    @Test fun coverPreviewAndImmersiveFocusFollowNativePositionAtAllWindowSizes() {
        SwingUtilities.invokeAndWait {
            for ((width, height, scale) in listOf(Triple(390, 844, 1f), Triple(360, 640, 1.5f), Triple(720, 360, 1.5f), Triple(1280, 800, 1f))) {
                val position = mutableStateOf(5000L)
                val scene = ImageComposeScene(width, height, density = Density(1f, scale)) {
                    ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                        NowPlayingOverlay(playerState = PlayerState(currentTrack = track, positionMs = position.value),
                            queue = listOf(track), lyricsState = LyricsUiState.Ready(lyrics), onDismiss = {},
                            onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {},
                            onSeek = {}, onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {})
                    }
                }
                var time = 0L
                fun settle() { repeat(20) { scene.render(time).close(); time += 16_000_000L } }
                fun capture(name: String): java.awt.image.BufferedImage {
                    return scene.render(time).use { image ->
                        image.encodeToData()!!.use { data ->
                            val file = Path.of("build/lyrics-regression/$width-$height-$scale-$name.png")
                            Files.createDirectories(file.parent); Files.write(file, data.bytes)
                            ImageIO.read(ByteArrayInputStream(data.bytes))
                        }
                    }
                }
                fun assertFocus(index: Int, name: String) {
                    val row = scene.tag("lyric-line-$index")
                    assertEquals(true, row.config.getOrNull(SemanticsProperties.Selected))
                    val bounds = row.boundsInRoot
                    val image = capture(name)
                    fun light(x: Int): Double {
                        val color = java.awt.Color(image.getRGB(x, bounds.center.y.toInt()))
                        return (color.red + color.green + color.blue) / (3.0 * 255)
                    }
                    assertTrue(light((bounds.left + 5).toInt()) - light((bounds.left - 2).toInt()) > .035,
                        "Immersive current line must have a visible band: $width/$height/$index")
                }
                try {
                    settle(); scene.click("封面"); settle()
                    capture("cover")
                    val preview = scene.tag("hero-lyrics-list").boundsInRoot
                    val current = scene.tag("hero-lyric-1")
                    assertEquals(true, current.config.getOrNull(SemanticsProperties.Selected))
                    assertTrue(abs(current.boundsInRoot.center.y - preview.center.y) < 3f)
                    assertTrue(preview.height > 0 && preview.top >= 0 && preview.bottom <= height)
                    if (height >= 560) assertTrue(scene.tag("player-artwork").boundsInRoot.height > 0)
                    for (label in listOf("播放", "上一首", "下一首", "播放进度")) {
                        val bounds = scene.label(label).touchBoundsInRoot
                        assertTrue(bounds.height >= 48 && bounds.width >= 48, "$width/$height: $label $bounds")
                        assertTrue(bounds.top >= 0 && bounds.bottom <= height, "$width/$height: $label $bounds")
                    }
                    position.value = 10_000; settle()
                    assertEquals(true, scene.tag("hero-lyric-2").config.getOrNull(SemanticsProperties.Selected))
                    // A constrained preview virtualizes rows outside its viewport.
                    assertTrue(scene.nodes().none { it.config.getOrNull(SemanticsProperties.TestTag) == "hero-lyric-1" &&
                        it.config.getOrNull(SemanticsProperties.Selected) == true })
                    assertTrue(abs(scene.tag("hero-lyric-2").boundsInRoot.center.y -
                        scene.tag("hero-lyrics-list").boundsInRoot.center.y) < 3f)
                    scene.click("歌词"); settle(); assertFocus(2, "lyrics-current")
                    val beforeHeight = scene.tag("lyric-line-2").size.height
                    position.value = 15_000; settle(); assertFocus(3, "lyrics-next")
                    assertEquals(false, scene.tag("lyric-line-2").config.getOrNull(SemanticsProperties.Selected))
                    assertEquals(beforeHeight, scene.tag("lyric-line-2").size.height)
                } finally { scene.close() }
            }
        }
    }
}
