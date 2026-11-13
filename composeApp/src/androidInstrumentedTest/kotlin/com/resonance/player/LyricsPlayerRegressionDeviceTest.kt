package com.resonance.player

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
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

    private fun show(scale: Float? = 1f, fixture: Lyrics = lyrics) {
        ui.mainClock.autoAdvance = false
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides (scale?.let { Density(density.density, it) } ?: density)) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = true) {
                    NowPlayingOverlay(playerState = PlayerState(currentTrack = track, positionMs = position.value),
                        queue = listOf(track), lyricsState = LyricsUiState.Ready(fixture), onDismiss = {},
                        onTogglePlay = {}, onPrevious = {}, onNext = {}, onToggleShuffle = {}, onCycleRepeat = {},
                        onSeek = {}, onTrackSelected = {}, onToggleFavorite = {}, onRefreshLyrics = {})
                }
            }
        }
        settle()
    }

    private fun settle() { ui.mainClock.advanceTimeBy(1000); ui.waitForIdle() }

    private fun assertPreview(index: Int, checkArtwork: Boolean = true) {
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
        if (checkArtwork) ui.onNodeWithTag("player-artwork", useUnmergedTree = true).assertIsDisplayed()
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


    private fun assertWholeLine(text: String, viewportTag: String) {
        val node = ui.onNodeWithText(text, useUnmergedTree = true)
        val layout = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
        val result = layout.last()
        val visible = node.fetchSemanticsNode().boundsInRoot
        val viewport = ui.onNodeWithTag(viewportTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val ellipsized = (0 until result.lineCount).any(result::isLineEllipsized)
        val lastLineBottom = result.getLineBottom(result.lineCount - 1)
        assertTrue("Text must wrap without clipping: $text, layout=${result.size}, last baseline box=$lastLineBottom",
            !ellipsized && result.getLineEnd(result.lineCount - 1) == text.length &&
                lastLineBottom <= result.size.height + 1)
        assertTrue("Text must be fully visible: $text, text=$visible layout=${result.size}, viewport=$viewport",
            visible.height >= result.size.height - 1 && visible.top >= viewport.top && visible.bottom <= viewport.bottom)
    }

    @Test fun longBilingualLineFitsOnTheCoverAndInTheFullLyrics() {
        val original = "Hey, what are you doing so far away from home?"
        val translation = "嘿，你怎么独自走到了离家这么远的地方？"
        val fixture = lyrics.copy(lines = lyrics.lines.toMutableList().apply {
            this[1] = LyricLine(5000L, original, translation)
        })
        show(scale = null, fixture = fixture)
        val root = ui.onRoot().fetchSemanticsNode().boundsInRoot
        ui.onNodeWithContentDescription("封面").performClick()
        settle()
        assertPreview(1, checkArtwork = root.height > root.width)
        assertWholeLine(original, "hero-lyrics-list")
        assertWholeLine(translation, "hero-lyrics-list")
        ui.onNode(hasClickAction() and hasAnyChild(hasTestTag("hero-lyrics-list")), useUnmergedTree = true).performClick()
        settle()
        assertImmersiveBand(1)
        assertWholeLine(original, "player-lyrics-list")
        assertWholeLine(translation, "player-lyrics-list")
    }

    @Test fun veryLongPreviewOpensScrollableLyricsWithoutDiscardingText() {
        val original = List(18) { "The entire original stays available on the lyrics page" }.joinToString(" ")
        val translation = List(12) { "特别长的译文可以滑动读完。" }.joinToString("")
        val fixture = lyrics.copy(lines = lyrics.lines.toMutableList().apply {
            this[1] = LyricLine(5000L, original, translation)
        })
        show(scale = null, fixture = fixture)
        ui.onNodeWithText("查看完整歌词", useUnmergedTree = true).assertIsDisplayed()
        val preview = ui.onNode(hasClickAction() and hasAnyChild(hasTestTag("hero-lyrics-list")), useUnmergedTree = true)
        preview.performSemanticsAction(SemanticsActions.OnClick) { it() }
        settle()
        val list = ui.onNodeWithTag("player-lyrics-list", useUnmergedTree = true)
        list.assertExists()
        val viewport = list.fetchSemanticsNode().boundsInRoot
        assertTrue("Full lyrics must have a scrollable viewport: $viewport", viewport.height > 0)
        val current = ui.onNodeWithTag("lyric-line-1", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(current.config.getOrNull(SemanticsProperties.Selected) == true)
        ui.onNodeWithText(original, useUnmergedTree = true).assertExists()
        repeat(6) {
            val translated = ui.onAllNodesWithText(translation, useUnmergedTree = true)
                .fetchSemanticsNodes().firstOrNull()
            val viewport = list.fetchSemanticsNode().boundsInRoot
            if (translated != null && translated.boundsInRoot.height > 0 && translated.boundsInRoot.bottom < viewport.bottom - 4) {
                return
            }
            list.performTouchInput { swipeUp(durationMillis = 500) }
            ui.mainClock.advanceTimeBy(360)
            ui.waitForIdle()
        }
        throw AssertionError("Scrolling must reach the end of the translated lyric")
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
