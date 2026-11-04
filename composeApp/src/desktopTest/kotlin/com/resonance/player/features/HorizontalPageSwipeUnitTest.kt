package com.resonance.player.features

import com.resonance.player.model.APP_RELEASE_DATE
import com.resonance.player.model.APP_RELEASE_HIGHLIGHTS
import com.resonance.player.model.APP_VERSION
import com.resonance.player.ui.swipePageTarget
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
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
        val root = Path.of("..").toAbsolutePath().normalize()
        val version = Properties().apply {
            Files.newInputStream(root.resolve("version.properties")).use(::load)
        }.getProperty("versionName")
        assertEquals(version, APP_VERSION, "Generated version must follow version.properties")
        val notes = Files.readAllLines(root.resolve("docs/RELEASE_NOTES-$version.md"))
        assertTrue(APP_RELEASE_DATE.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue(notes.first().startsWith("# $APP_RELEASE_DATE"))
        val section = notes.indexOf("## 主要更新")
        assertTrue(section >= 0, "Release notes must contain current highlights")
        val nextHeading = (section + 1 until notes.size).firstOrNull { notes[it].startsWith("## ") } ?: notes.size
        val expected = notes.subList(section + 1, nextHeading).filter { it.startsWith("- ") }.map { it.substring(2) }
        assertTrue(expected.isNotEmpty(), "Release highlights must not be empty")
        assertEquals(expected, APP_RELEASE_HIGHLIGHTS, "About-page highlights must match this release")
    }
}
