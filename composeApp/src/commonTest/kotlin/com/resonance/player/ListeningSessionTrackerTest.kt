package com.resonance.player

import com.resonance.player.playback.ListeningSessionTracker
import kotlin.test.*

class ListeningSessionTrackerTest {
    private data class Record(val id: String, val seconds: Int, val completed: Boolean)
    @Test fun pauseNearEndDoesNotCompleteAgainAndRepeatStartsAnotherPass() {
        val records = mutableListOf<Record>()
        val tracker = ListeningSessionTracker { id, seconds, completed -> records += Record(id, seconds, completed) }
        tracker.start("a", 93_000, 0)
        tracker.sample("a", 93_000, 100_000, true, 1f, 0)
        tracker.sample("a", 95_000, 100_000, true, 1f, 2_000)
        tracker.sample("a", 96_000, 100_000, false, 1f, 3_000)
        tracker.sample("a", 96_000, 100_000, true, 1f, 23_000)
        tracker.sample("a", 99_000, 100_000, true, 1f, 26_000)
        tracker.finish(27_000); tracker.finish(27_000)
        assertEquals(7, records.sumOf { it.seconds })
        assertEquals(1, records.count { it.completed })
        tracker.start("a", 0, 28_000)
        tracker.sample("a", 0, 100_000, true, 1f, 28_000)
        tracker.sample("a", 100_000, 100_000, true, 1f, 128_000)
        tracker.finish(128_000)
        assertEquals(2, records.count { it.completed })
    }

    @Test fun bufferingSeekSpeedAndRemoteAudioDoNotInflateDuration() {
        val records = mutableListOf<Record>()
        val tracker = ListeningSessionTracker { id, seconds, completed -> records += Record(id, seconds, completed) }
        tracker.start("a", 0, 0)
        tracker.sample("a", 0, 100_000, true, 2f, 0)
        tracker.sample("a", 4_000, 100_000, true, 2f, 2_000) // two seconds at 2x
        tracker.sample("a", 4_000, 100_000, false, 2f, 12_000) // stalled
        tracker.sample("a", 99_000, 100_000, false, 2f, 13_000, discontinuity = true)
        assertEquals(2, records.sumOf { it.seconds })
        assertEquals(0, records.count { it.completed })
        tracker.start("together:remote", 0, 14_000)
        tracker.sample("together:remote", 0, 10_000, true, 1f, 14_000)
        tracker.sample("together:remote", 10_000, 10_000, true, 1f, 24_000)
        tracker.finish(24_000)
        assertEquals(2, records.sumOf { it.seconds })
    }

    @Test fun jitterBetweenWallClockAndAudioClockDoesNotLoseTimeOnEveryTick() {
        var seconds = 0
        val tracker = ListeningSessionTracker { _, delta, _ -> seconds += delta }
        tracker.start("a", 0, 0)
        tracker.sample("a", 0, 60_000, true, 1f, 0)
        tracker.sample("a", 200, 60_000, true, 1f, 250)
        tracker.sample("a", 500, 60_000, true, 1f, 500)
        tracker.sample("a", 700, 60_000, true, 1f, 750)
        tracker.sample("a", 1000, 60_000, true, 1f, 1000)
        assertEquals(1, seconds)
    }

    @Test fun quarterSecondNativeUpdatesAccumulateAcrossPauseWithoutUiTimer() {
        val records = mutableListOf<Record>()
        val tracker = ListeningSessionTracker { id, seconds, completed -> records += Record(id, seconds, completed) }
        tracker.start("a", 0, 0)
        tracker.sample("a", 0, 60_000, true, 1f, 0)
        for (tick in 1..3) tracker.sample("a", tick * 250L, 60_000, true, 1f, tick * 250L)
        tracker.sample("a", 750, 60_000, false, 1f, 750)
        tracker.sample("a", 750, 60_000, true, 1f, 10_750)
        tracker.sample("a", 1_000, 60_000, true, 1f, 11_000)
        assertEquals(listOf(Record("a", 1, false)), records)
    }
}
