package com.resonance.player.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.test.*

/** Native frame-by-frame geometry checks using synthetic lyrics and no platform services. */
@OptIn(ExperimentalComposeUiApi::class)
class LyricsMotionTest {
    @Test fun focusBandAndSelectedLineMoveTogetherInBothThemes() {
        SwingUtilities.invokeAndWait {
            for (mode in listOf(ThemeMode.Light, ThemeMode.Dark)) {
                val position = mutableStateOf(80_000L)
                val scene = ImageComposeScene(390, 600) {
                    ResonanceTheme(mode, reducedMotion = true) {
                        androidx.compose.material3.Surface {
                            LyricsContent(track, 0f, position.value, lyrics, onSeek = {})
                        }
                    }
                }
                try {
                    val frames = Frames(scene)
                    frames.advance(200)
                    fun bandPixel(tag: String, time: Long, suffix: String): Int {
                        val bounds = frames.node(tag).boundsInRoot
                        return scene.render(time).use { image ->
                            val bytes = image.encodeToData()!!.use { it.bytes }
                            val file = java.nio.file.Path.of("build/glass-coverage/lyrics-${mode.name}-$suffix.png")
                            java.nio.file.Files.createDirectories(file.parent)
                            java.nio.file.Files.write(file, bytes)
                            javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(bytes))
                                .getRGB((bounds.left + 5).toInt(), bounds.center.y.toInt())
                        }
                    }
                    assertEquals(true, frames.node("lyric-line-8").config.getOrNull(SemanticsProperties.Selected))
                    val highlighted = bandPixel("lyric-line-8", 220_000_000L, "before")
                    val plain = bandPixel("lyric-line-7", 240_000_000L, "plain")
                    assertNotEquals(plain, highlighted, "Current lyric must have a visible focus band")
                    position.value = 90_000
                    frames.advance(300)
                    assertEquals(true, frames.node("lyric-line-9").config.getOrNull(SemanticsProperties.Selected))
                    assertEquals(false, frames.node("lyric-line-8").config.getOrNull(SemanticsProperties.Selected))
                    assertEquals(highlighted, bandPixel("lyric-line-9", 600_000_000L, "after"))
                    assertEquals(plain, bandPixel("lyric-line-8", 620_000_000L, "old"))
                } finally { scene.close() }
            }
        }
    }

    private val track = Track("motion-fixture", "此刻的旋律", "Resonance", "测试", "4:00", 0)
    private val lyrics = Lyrics(track.id, (0..25).map {
        LyricLine(it * 10_000L, if (it == 9) "让那些很长很长的心事在旋律里慢慢找到属于自己的回响" else "此刻的旋律，第 $it 句")
    }, true, false, "原创测试文本")

    private class Frames(val scene: ImageComposeScene) {
        private var timeMs = 0L
        fun advance(milliseconds: Int) {
            repeat((milliseconds + 15) / 16) {
                scene.render(timeMs * 1_000_000).close()
                timeMs += 16
            }
        }
        fun node(tag: String): SemanticsNode {
            fun find(node: SemanticsNode): SemanticsNode? {
                if (node.config.getOrNull(SemanticsProperties.TestTag) == tag) return node
                return node.children.firstNotNullOfOrNull { find(it) }
            }
            return scene.semanticsOwners.firstNotNullOfOrNull { find(it.rootSemanticsNode) }
                ?: error("Missing node $tag")
        }
        fun y(tag: String) = node(tag).boundsInRoot.center.y
        fun assertCentered(row: String, viewport: String) {
            assertTrue(abs(y(row) - y(viewport)) < 3f, "$row must be centered in $viewport")
        }
    }

    @Test fun wrappedLineMovesThroughIntermediatePositionsWithoutReflow() {
        SwingUtilities.invokeAndWait {
            val position = mutableStateOf(80_000L)
            val scene = ImageComposeScene(350, 480, density = Density(1f)) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = false) {
                    LyricsContent(track, 0f, position.value, lyrics, onSeek = {})
                }
            }
            try {
                val frames = Frames(scene)
                frames.advance(500)
                frames.assertCentered("lyric-line-8", "player-lyrics-list")
                val before = frames.node("lyric-line-9").boundsInRoot
                position.value = 90_000
                frames.advance(96)
                val during = frames.node("lyric-line-9").boundsInRoot
                frames.advance(500)
                val after = frames.node("lyric-line-9").boundsInRoot
                assertEquals(before.height, after.height, 1f, "Focus must not change line wrapping")
                assertTrue(during.center.y < before.center.y - 1 && during.center.y > after.center.y + 1,
                    "Expected an intermediate position: $before -> $during -> $after")
                frames.assertCentered("lyric-line-9", "player-lyrics-list")
                position.value = 80_000
                frames.advance(96)
                position.value = 100_000
                frames.advance(500)
                frames.assertCentered("lyric-line-10", "player-lyrics-list")
            } finally { scene.close() }
        }
    }

    @Test fun previewReusesTheSameLineWhileItSlidesIntoFocus() {
        SwingUtilities.invokeAndWait {
            val index = mutableStateOf(2)
            val scene = ImageComposeScene(350, 220, density = Density(1f)) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = false) {
                    SyncedLyricsPreview(lyrics, index.value, LyricDisplay.Original, stage = false, compact = false)
                }
            }
            try {
                val frames = Frames(scene)
                frames.advance(500)
                frames.assertCentered("hero-lyric-2", "hero-lyrics-list")
                val before = frames.y("hero-lyric-3")
                val identity = frames.node("hero-lyric-3").id
                index.value = 3
                frames.advance(96)
                val during = frames.y("hero-lyric-3")
                assertEquals(identity, frames.node("hero-lyric-3").id, "A lyric must not be replaced to transition it")
                frames.advance(500)
                val after = frames.y("hero-lyric-3")
                assertTrue(during < before - 1 && during > after + 1)
                frames.assertCentered("hero-lyric-3", "hero-lyrics-list")
            } finally { scene.close() }
        }
    }

    @Test fun reducedMotionSeeksImmediatelyAndTranslationArrivalPreservesListIdentity() {
        SwingUtilities.invokeAndWait {
            val position = mutableStateOf(0L)
            val text = mutableStateOf(lyrics)
            val scene = ImageComposeScene(350, 480, density = Density(1f, 1.5f)) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                    LyricsContent(track, 0f, position.value, text.value, onSeek = {})
                }
            }
            try {
                val frames = Frames(scene)
                frames.advance(200)
                frames.assertCentered("lyric-line-0", "player-lyrics-list")
                position.value = 250_000
                frames.advance(64)
                frames.assertCentered("lyric-line-25", "player-lyrics-list")
                val identity = frames.node("lyric-line-25").id
                text.value = lyrics.copy(lines = lyrics.lines.map { it.copy(translation = "A translated line") })
                frames.advance(64)
                assertEquals(identity, frames.node("lyric-line-25").id)
                frames.assertCentered("lyric-line-25", "player-lyrics-list")
                position.value = 0
                frames.advance(64)
                frames.assertCentered("lyric-line-0", "player-lyrics-list")
            } finally { scene.close() }
        }
    }
}
