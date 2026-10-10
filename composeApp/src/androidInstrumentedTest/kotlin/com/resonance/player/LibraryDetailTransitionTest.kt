package com.resonance.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.ThemeMode
import com.resonance.player.ui.LibraryDetailTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Samples rendered pixels mid-transition, where transparent panels previously bled through. */
class LibraryDetailTransitionTest {
    @get:Rule val ui = createComposeRule()
    private val detail = mutableStateOf<String?>(null)
    private var canvas = Color.Black

    private fun show(mode: ThemeMode = ThemeMode.Light, reducedMotion: Boolean = false) {
        ui.mainClock.autoAdvance = false
        ui.setContent {
            ResonanceTheme(mode, reducedMotion = reducedMotion) {
                canvas = ResonanceColors.Canvas
                Box(Modifier.size(280.dp).background(Color.Black).testTag("root")) {
                    LibraryDetailTransition(detail.value, Modifier.size(240.dp).testTag("viewport")) { page ->
                        if (page == null) Box(Modifier.fillMaxSize().requiredWidth(400.dp).background(Color.Red))
                        else Box(Modifier.size(24.dp).background(Color.Blue))
                    }
                }
            }
        }
    }

    private fun color(tag: String, x: Float, y: Float): Color {
        val pixels = ui.onNodeWithTag(tag).captureToImage().toPixelMap()
        return pixels[(pixels.width * x).toInt(), (pixels.height * y).toInt()]
    }

    private fun assertColor(expected: Color, actual: Color) {
        assertEquals(expected.red, actual.red, 0.02f)
        assertEquals(expected.green, actual.green, 0.02f)
        assertEquals(expected.blue, actual.blue, 0.02f)
    }

    @Test fun openingCoversTheListAndClipsAtTheViewport() {
        show()
        ui.runOnIdle { detail.value = "playlist" }
        ui.mainClock.advanceTimeByFrame()
        ui.mainClock.advanceTimeBy(80)
        assertColor(canvas, color("viewport", 0.5f, 0.8f))
        assertColor(Color.Black, color("root", 0.95f, 0.8f))
        ui.mainClock.advanceTimeBy(400)
        // The opaque transition background must disappear so the ambient canvas returns.
        assertColor(Color.Black, color("viewport", 0.5f, 0.8f))
    }

    @Test fun returningKeepsTheDetailAboveTheListInDarkMode() {
        show(ThemeMode.Dark)
        ui.runOnIdle { detail.value = "playlist" }
        ui.mainClock.advanceTimeBy(400)
        ui.runOnIdle { detail.value = null }
        ui.mainClock.advanceTimeByFrame()
        ui.mainClock.advanceTimeBy(48)
        assertColor(canvas, color("viewport", 0.9f, 0.8f))
        ui.mainClock.advanceTimeBy(400)
        assertColor(Color.Red, color("viewport", 0.5f, 0.8f))
    }

    @Test fun immediateBackInterruptsTheOpeningWithoutLeavingAStaleLayer() {
        show()
        ui.runOnIdle { detail.value = "playlist" }
        ui.mainClock.advanceTimeBy(48)
        ui.runOnIdle { detail.value = null }
        ui.mainClock.advanceTimeBy(400)
        assertColor(Color.Red, color("viewport", 0.5f, 0.8f))
        assertColor(Color.Black, color("root", 0.95f, 0.8f))
    }

    @Test fun reducedMotionDoesNotCrossfadeBothPagesTogether() {
        show(reducedMotion = true)
        ui.runOnIdle { detail.value = "playlist" }
        ui.mainClock.advanceTimeByFrame()
        ui.mainClock.advanceTimeBy(112)
        val mid = color("viewport", 0.5f, 0.8f)
        assertTrue("Old red list bleeds through: $mid", mid.red <= canvas.red + 0.02f)
        assertTrue("Old list must no longer tint the incoming page", kotlin.math.abs(mid.red - mid.green) < 0.08f)
        ui.mainClock.advanceTimeBy(400)
        assertColor(Color.Black, color("viewport", 0.5f, 0.8f))
    }
}
