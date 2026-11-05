package com.resonance.player

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.ui.NowPlayingOverlay
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/** Real phone/player surface with synthetic lyrics, no library access or audio requests. */
class LyricsPlayerRegressionDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    private val position = mutableStateOf(5000L)
    private val track = Track("native-player-lyrics", "此刻的旋律", "测试歌手", "测试专辑", "0:30", 0)
    private val lyrics = Lyrics(track.id, (0..5).map { LyricLine(it * 5000L, "原创旋律第 $it 句") }, true, false, "测试夹具")

    private fun show(scale: Float = 1f) {
        ui.mainClock.autoAdvance = false
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                    NowPlayingOverlay(playerState = PlayerState(currentTrack = track, positionMs = position.value),
                        queue = listOf(track), lyricsState = LyricsUiState.Ready(lyrics), onDismiss = {},
                        onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {},
                        onSeek = {}, onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {})
                }
            }
        }
        settle()
    }

    private fun settle() { ui.mainClock.advanceTimeBy(1000); ui.waitForIdle() }

    private fun assertPreview(index: Int) {
        ui.onNodeWithTag("hero-lyric-$index", useUnmergedTree = true).assertIsSelected().assertIsDisplayed()
        val current = ui.onNodeWithTag("hero-lyric-$index", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val viewport = ui.onNodeWithTag("hero-lyrics-list", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(abs(current.center.y - viewport.center.y) < 3f)
        val artwork = ui.onNodeWithTag("player-artwork", useUnmergedTree = true).fetchSemanticsNode()
        println("Native player layout: root=${ui.onRoot().fetchSemanticsNode().boundsInRoot}, preview=$viewport, artwork=${artwork.boundsInRoot}, size=${artwork.size}")
        val output = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        java.io.File(output, "lyrics-preview-$index.png").outputStream().use {
            ui.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        ui.onNodeWithTag("player-artwork", useUnmergedTree = true).assertIsDisplayed()
        for (label in listOf("播放", "上一首", "下一首", "播放进度")) {
            ui.onNodeWithContentDescription(label).assertIsDisplayed()
        }
    }

    private fun assertImmersiveBand(index: Int) {
        ui.onNodeWithTag("lyric-line-$index").assertIsSelected().assertIsDisplayed()
        val bounds = ui.onNodeWithTag("lyric-line-$index").fetchSemanticsNode().boundsInRoot
        val pixels = ui.onRoot().captureToImage().toPixelMap()
        fun light(x: Int): Float {
            val color = pixels[x, bounds.center.y.toInt()]
            return (color.red + color.green + color.blue) / 3f
        }
        assertTrue("Current lyric needs a visible band on the actual player surface",
            light((bounds.left + 5).toInt()) - light((bounds.left - 2).toInt()) > .035f)
    }

    @Test fun coverAndFullLyricsFollowNativePositionWithVisibleHighlight() {
        show(); assertPreview(1)
        ui.runOnIdle { position.value = 10_000 }; settle(); assertPreview(2)
        ui.onNodeWithContentDescription("歌词").performClick(); settle(); assertImmersiveBand(2)
        ui.runOnIdle { position.value = 15_000 }; settle(); assertImmersiveBand(3)
        ui.onNodeWithTag("lyric-line-2").assertIsNotSelected()
    }

    @Test fun largeTextKeepsCoverPreviewAndTransportReachable() {
        show(scale = 1.5f); assertPreview(1)
        ui.runOnIdle { position.value = 10_000 }; settle(); assertPreview(2)
    }
}
