package com.resonance.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
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
                        else Box(Modifier.size(24.dp).background(Color.Blue).testTag("detail-marker"))
                    }
                }
            }
        }
    }

    private fun color(tag: String, x: Float, y: Float): Color {
        val pixels = ui.onNodeWithTag(tag).captureToImage().toPixelMap()
        return pixels[(pixels.width * x).toInt(), (pixels.height * y).toInt()]
    }

    private fun settleTransition() {
        // Spring duration depends on displacement/density; a fixed 400ms isn't an idle signal.
        val before = ui.mainClock.currentTime
        ui.mainClock.autoAdvance = true
        try { ui.waitForIdle() } finally { ui.mainClock.autoAdvance = false }
        val elapsed = ui.mainClock.currentTime - before
        println("Detail transition settled after ${elapsed}ms of test-clock time")
        assertTrue("Transition must settle within 2 seconds, took ${elapsed}ms", elapsed <= 2000)
    }

    private fun assertColor(expected: Color, actual: Color) {
        if (kotlin.math.abs(expected.red - actual.red) > 0.02f || kotlin.math.abs(expected.green - actual.green) > 0.02f) {
            val file = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.resolve("detail-transition-failure.png")
            file.outputStream().use { ui.onNodeWithTag("root").captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
        assertEquals("expected=$expected actual=$actual marker=${runCatching { ui.onNodeWithTag("detail-marker").fetchSemanticsNode().boundsInRoot }.getOrNull()}", expected.red, actual.red, 0.02f)
        assertEquals(expected.green, actual.green, 0.02f)
        assertEquals(expected.blue, actual.blue, 0.02f)
    }

    @Test fun openingCrossfadesBothPagesAndClipsAtTheViewport() {
        show()
        ui.runOnIdle { detail.value = "playlist" }
        ui.mainClock.advanceTimeByFrame()
        ui.mainClock.advanceTimeBy(80)
        // Sample inside the incoming panel's actual visible region, not at an assumed spring position.
        val viewport = ui.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        val incoming = ui.onNodeWithTag("detail-marker").fetchSemanticsNode().boundsInRoot
        assertTrue("Incoming detail must already be visible", incoming.width > 0 && incoming.left < viewport.right)
        val insideIncoming = ((incoming.left + viewport.right) / 2 - viewport.left) / viewport.width
        val blended = color("viewport", insideIncoming, 0.8f)
        assertTrue("Old page should be fading: $blended", blended.red in 0.01f..0.99f)
        assertEquals(0f, blended.green, 0.02f)
        assertEquals(0f, blended.blue, 0.02f)
        val evidence = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.resolve("detail-transition-verified.png")
        evidence.outputStream().use { ui.onNodeWithTag("root").captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        assertColor(Color.Black, color("root", 0.95f, 0.8f))
        settleTransition()
        // After both moving layers settle, the transparent detail exposes its parent canvas.
        assertColor(Color.Black, color("viewport", 0.5f, 0.8f))
        val settled = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.resolve("detail-transition-settled.png")
        settled.outputStream().use { ui.onNodeWithTag("root").captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun returningCrossfadeSettlesToTheListInDarkMode() {
        show(ThemeMode.Dark)
        ui.runOnIdle { detail.value = "playlist" }
        settleTransition()
        ui.runOnIdle { detail.value = null }
        ui.mainClock.advanceTimeByFrame()
        ui.mainClock.advanceTimeBy(48)
        val blended = color("viewport", 0.9f, 0.8f)
        assertTrue("Returning list should be entering: $blended", blended.red in 0.01f..0.99f)
        assertEquals(0f, blended.green, 0.02f)
        assertEquals(0f, blended.blue, 0.02f)
        settleTransition()
        assertColor(Color.Red, color("viewport", 0.5f, 0.8f))
    }

    @Test fun immediateBackInterruptsTheOpeningWithoutLeavingAStaleLayer() {
        show()
        ui.runOnIdle { detail.value = "playlist" }
        ui.mainClock.advanceTimeBy(48)
        ui.runOnIdle { detail.value = null }
        settleTransition()
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
        settleTransition()
        assertColor(Color.Black, color("viewport", 0.5f, 0.8f))
    }
}
