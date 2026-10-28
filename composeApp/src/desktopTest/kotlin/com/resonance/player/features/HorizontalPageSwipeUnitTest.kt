package com.resonance.player.features

import com.resonance.player.model.APP_RELEASE_DATE
import com.resonance.player.model.APP_RELEASE_HIGHLIGHTS
import com.resonance.player.model.APP_VERSION
import com.resonance.player.ui.swipePageTarget
import kotlin.test.*

class HorizontalPageSwipeUnitTest {
    @Test
    fun swipePageTargetAdvancesToNextOnLeftSwipeAndPreviousOnRightSwipe() {
        // Distance < 0 means swiping left -> advance to next page
        assertEquals(1, swipePageTarget(index = 0, count = 3, distance = -100f, threshold = 50f))
        assertEquals(2, swipePageTarget(index = 1, count = 3, distance = -100f, threshold = 50f))

        // Distance > 0 means swiping right -> retreat to previous page
        assertEquals(1, swipePageTarget(index = 2, count = 3, distance = 100f, threshold = 50f))
        assertEquals(0, swipePageTarget(index = 1, count = 3, distance = 100f, threshold = 50f))
    }

    @Test
    fun swipePageTargetRejectsAtBoundariesWithoutWrapping() {
        // At start (0), swiping right should NOT wrap to end
        assertNull(swipePageTarget(index = 0, count = 3, distance = 100f, threshold = 50f))

        // At end (count - 1), swiping left should NOT wrap to start
        assertNull(swipePageTarget(index = 2, count = 3, distance = -100f, threshold = 50f))
    }

    @Test
    fun swipePageTargetRejectsSubThresholdOrZeroThreshold() {
        // Distance below threshold without velocity
        assertNull(swipePageTarget(index = 0, count = 3, distance = -30f, threshold = 50f))
        assertNull(swipePageTarget(index = 1, count = 3, distance = 30f, threshold = 50f))

        // Threshold zero or negative
        assertNull(swipePageTarget(index = 0, count = 3, distance = -100f, threshold = 0f))
        assertNull(swipePageTarget(index = 0, count = 3, distance = -100f, threshold = -10f))
    }

    @Test
    fun swipePageTargetSupportsFlingVelocityWhenDistanceIsSubThreshold() {
        // Distance is sub-threshold (-20f < 50f), but fling velocity exceeds threshold with matching direction
        assertEquals(1, swipePageTarget(index = 0, count = 3, distance = -20f, threshold = 50f, velocity = -600f, velocityThreshold = 400f))
        assertEquals(0, swipePageTarget(index = 1, count = 3, distance = 20f, threshold = 50f, velocity = 600f, velocityThreshold = 400f))

        // Opposing velocity does not satisfy fling
        assertNull(swipePageTarget(index = 0, count = 3, distance = -20f, threshold = 50f, velocity = 600f, velocityThreshold = 400f))

        // Insufficient velocity does not satisfy fling
        assertNull(swipePageTarget(index = 0, count = 3, distance = -20f, threshold = 50f, velocity = -200f, velocityThreshold = 400f))
    }

    @Test
    fun appVersionAndHighlightsGeneratedFromReleaseNotes() {
        assertEquals("0.6.4", APP_VERSION)
        assertEquals("2026-10-28", APP_RELEASE_DATE)
        assertEquals(5, APP_RELEASE_HIGHLIGHTS.size)
        assertTrue(APP_RELEASE_HIGHLIGHTS[0].contains("Mineradio"))
        assertTrue(APP_RELEASE_HIGHLIGHTS.last().contains("Android 16"))
    }
}
