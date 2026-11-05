package com.resonance.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.*
import com.resonance.player.ui.LyricsContent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/** Isolated native lyrics, synthetic text only; never loads the device library or a network API. */
class LyricsScrollDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<PromoCaptureActivity>()
    private val track = Track("scroll-fixture", "此刻，听见共鸣", "Resonance", "原创测试文本", "4:00", 7)
    private val currentTrack = mutableStateOf(track)
    private val position = mutableStateOf(80_000L)
    private val height = mutableStateOf(480.dp)
    private var sought: Float? = null
    private val lines = (0 until 30).map { i ->
        LyricLine(i * 10_000L, if (i == 9) "让那些很长很长的心事在旋律里慢慢找到属于自己的回响" else "此刻的旋律，第 $i 句",
            "Let every moment find its own resonance · $i")
    }

    private fun show(reduce: Boolean = false, scale: Float = 1f, synced: Boolean = true, offset: Long = 0) {
        ui.mainClock.autoAdvance = false
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ResonanceTheme(ThemeMode.Light, reducedMotion = reduce) {
                    Box(Modifier.width(350.dp).height(height.value)) {
                        LyricsContent(currentTrack.value, 0f, position.value, Lyrics(currentTrack.value.id, lines, synced, false, "原创测试文本", offsetMs = offset),
                            onSeek = { sought = it; position.value = (it * 240_000).toLong() }, onAdjustOffset = {})
                    }
                }
            }
        }
        settle()
    }

    private fun settle() { ui.mainClock.advanceTimeBy(1_500); ui.waitForIdle() }
    private fun assertCentered(index: Int) {
        val viewport = ui.onNodeWithTag("player-lyrics-list").fetchSemanticsNode().boundsInRoot
        val row = ui.onNodeWithTag("lyric-line-$index").fetchSemanticsNode().boundsInRoot
        assertTrue("$index is not centered: row=$row viewport=$viewport", abs(row.center.y - viewport.center.y) < with(ui.density) { 3.dp.toPx() })
        ui.onNodeWithTag("lyric-line-$index").assertIsSelected()
    }

    @Test fun initialAndWrappedBilingualLinesAreCentered() {
        show()
        assertCentered(8)
        ui.runOnIdle { position.value = 90_000 }
        settle()
        assertCentered(9)
        ui.runOnIdle { position.value = 100_000 }
        settle()
        assertCentered(10)
    }

    @Test fun nextLinePassesThroughIntermediatePositionsWithoutChangingRowHeight() {
        show()
        val row = ui.onNodeWithTag("lyric-line-9")
        val before = row.fetchSemanticsNode().boundsInRoot
        ui.runOnIdle { position.value = 90_000 }
        ui.mainClock.advanceTimeBy(96)
        val during = row.fetchSemanticsNode().boundsInRoot
        settle()
        val after = row.fetchSemanticsNode().boundsInRoot
        assertEquals("Focus must not reflow wrapped text", before.height, after.height, 1f)
        assertTrue("No intermediate movement: $before -> $during -> $after",
            during.center.y < before.center.y - 1 && during.center.y > after.center.y + 1)
        assertCentered(9)
    }

    @Test fun rapidlyReversingTheTargetDoesNotReplayOrSnap() {
        show()
        val initialCenter = ui.onNodeWithTag("lyric-line-8").fetchSemanticsNode().boundsInRoot.center.y
        ui.runOnIdle { position.value = 90_000 }
        ui.mainClock.advanceTimeBy(96)
        ui.runOnIdle { position.value = 80_000 }
        // The next frame first ticks the old animation, then applies this state/recomposition.
        // Sample once the new target is acknowledged instead of treating that old tick as a snap.
        ui.mainClock.advanceTimeByFrame()
        ui.onNodeWithTag("lyric-line-8").assertIsSelected()
        val retargeted = ui.onNodeWithTag("lyric-line-8").fetchSemanticsNode().boundsInRoot.center.y
        assertTrue("Accepting the target must not snap to its final center", retargeted < initialCenter - 1)
        ui.mainClock.advanceTimeByFrame()
        val firstFrame = ui.onNodeWithTag("lyric-line-8").fetchSemanticsNode().boundsInRoot.center.y
        assertTrue("Retargeting must start at the current position: before=$retargeted, after=$firstFrame",
            abs(retargeted - firstFrame) < with(ui.density) { 8.dp.toPx() })
        settle()
        assertCentered(8)
    }

    @Test fun seekingToAnOffscreenLineStartsByScrollingTheExistingRows() {
        show()
        val before = ui.onNodeWithTag("lyric-line-8").fetchSemanticsNode().boundsInRoot.center.y
        ui.runOnIdle { position.value = 190_000 }
        ui.mainClock.advanceTimeBy(64)
        val during = ui.onNodeWithTag("lyric-line-8").fetchSemanticsNode().boundsInRoot.center.y
        assertTrue("Offscreen seek must not teleport the list", during < before)
        settle()
        assertCentered(19)
    }

    @Test fun changingTracksWithTheSameLinesAndTimestampStillCentersTheNewList() {
        show()
        assertCentered(8)
        ui.runOnIdle { currentTrack.value = track.copy(id = "scroll-fixture-next") }
        settle()
        assertCentered(8)
    }

    @Test fun returnButtonImmediatelyRecentersEvenWhilePaused() {
        show()
        ui.onNodeWithTag("player-lyrics-list").performTouchInput { swipeUp(durationMillis = 500) }
        ui.mainClock.advanceTimeBy(1_000)
        ui.onNodeWithTag("lyric-return-current").assertIsDisplayed().performClick()
        settle()
        assertCentered(8)
        ui.onNodeWithTag("lyric-return-current").assertDoesNotExist()
    }

    @Test fun browsingWaitsThenResumesWithoutANewLyricTimestamp() {
        show()
        ui.onNodeWithTag("player-lyrics-list").performTouchInput { swipeUp(durationMillis = 500) }
        ui.mainClock.advanceTimeBy(2_000)
        ui.onNodeWithTag("lyric-return-current").assertIsDisplayed()
        ui.runOnIdle { position.value = 90_000 }
        ui.mainClock.advanceTimeBy(500)
        ui.onNodeWithTag("lyric-return-current").assertIsDisplayed()
        ui.mainClock.advanceTimeBy(7_000)
        settle()
        assertCentered(9)
        ui.onNodeWithTag("lyric-return-current").assertDoesNotExist()
    }

    @Test fun firstLastResizeAndLargeTextStayCenteredWithReducedMotion() {
        position.value = 0
        show(reduce = true, scale = 1.5f)
        assertCentered(0)
        ui.runOnIdle { position.value = 290_000 }
        settle()
        assertCentered(29)
        ui.runOnIdle { height.value = 360.dp }
        settle()
        assertCentered(29)
    }

    @Test fun tappingUsesOffsetAndClampsSeekToTheAudioRange() {
        show(offset = 5_000)
        ui.onNodeWithTag("lyric-line-8").performClick()
        settle()
        ui.runOnIdle { assertEquals(75_000f / 240_000, sought!!, .001f) }
    }

    @Test fun plainTextBrowsingDoesNotSeekOrShowFollowControls() {
        show(synced = false)
        ui.onNodeWithTag("lyric-line-0").performTouchInput { click() }
        ui.onNodeWithTag("player-lyrics-list").performTouchInput { swipeUp(durationMillis = 500) }
        settle()
        ui.onNodeWithTag("lyric-return-current").assertDoesNotExist()
        ui.runOnIdle { assertNull(sought) }
    }
}
