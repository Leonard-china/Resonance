package com.resonance.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.ui.SyncedLyricsPreview
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/** Samples real native layout during motion, not just the settled screenshot. */
class LyricsPreviewMotionDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    private val current = mutableStateOf(2)
    private val lyrics = Lyrics("preview-motion", (0..8).map {
        LyricLine(it * 1_000L, "此刻的旋律，第 $it 句")
    }, true, false, "原创测试文本")

    private fun show(reduce: Boolean = false) {
        ui.mainClock.autoAdvance = false
        ui.setContent {
            ResonanceTheme(ThemeMode.Light, reducedMotion = reduce) {
                Box(Modifier.width(350.dp)) {
                    SyncedLyricsPreview(lyrics, current.value, LyricDisplay.Original, stage = false, compact = false)
                }
            }
        }
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
    }

    private fun center(index: Int) = ui.onNodeWithTag("hero-lyric-$index").fetchSemanticsNode().boundsInRoot.center.y
    private fun assertCentered(index: Int) {
        val viewport = ui.onNodeWithTag("hero-lyrics-list").fetchSemanticsNode().boundsInRoot
        assertTrue(abs(center(index) - viewport.center.y) < with(ui.density) { 3.dp.toPx() })
        ui.onNodeWithTag("hero-lyric-$index").assertIsSelected()
    }

    @Test fun onePersistentRowSlidesIntoFocusAcrossSeveralFrames() {
        show()
        assertCentered(2)
        val before = center(3)
        ui.runOnIdle { current.value = 3 }
        ui.mainClock.advanceTimeBy(96)
        val during = center(3)
        ui.onAllNodesWithText("此刻的旋律，第 3 句", useUnmergedTree = true).assertCountEquals(1)
        ui.mainClock.advanceTimeBy(500)
        val after = center(3)
        assertTrue("Must visibly travel, not crossfade replacement rows", during < before - 1 && during > after + 1)
        assertCentered(3)
    }

    @Test fun reducedMotionAndBackwardSeekKeepTheSameFocus() {
        show(reduce = true)
        ui.runOnIdle { current.value = 4 }
        ui.mainClock.advanceTimeBy(64)
        assertCentered(4)
        ui.runOnIdle { current.value = 1 }
        ui.mainClock.advanceTimeBy(64)
        assertCentered(1)
    }
}
