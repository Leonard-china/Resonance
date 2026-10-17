package com.resonance.player.playback

import com.resonance.player.model.SleepTimerOption
import kotlinx.coroutines.*
import kotlin.test.*

class PlaybackSleepTimerTest {
    @Test fun countdownUsesDeadlineAndExpiresOnlyOnce() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var now = 10_000L
        var pauses = 0
        val timer = PlaybackSleepTimer(scope, { pauses++ }, { now })
        try {
            timer.set(SleepTimerOption.Min15)
            assertEquals(900, timer.state.value.remainingSeconds)
            now += 899_001
            assertFalse(timer.tick())
            assertEquals(1, timer.state.value.remainingSeconds)
            now += 5_000
            assertTrue(timer.tick())
            assertEquals(SleepTimerOption.Off, timer.state.value.option)
            assertEquals(1, pauses)
            assertFalse(timer.tick())
            assertEquals(1, pauses)
        } finally { timer.close(); scope.cancel() }
    }
    @Test fun replacingAndCancellingTimersNeverPausesFromOldDeadline() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var now = 0L
        var pauses = 0
        val timer = PlaybackSleepTimer(scope, { pauses++ }, { now })
        try {
            timer.set(SleepTimerOption.Min15)
            now = 800_000
            timer.set(SleepTimerOption.Min30)
            now = 901_000
            assertFalse(timer.tick())
            timer.set(SleepTimerOption.Off)
            now = 9_000_000
            assertFalse(timer.tick())
            assertEquals(0, pauses)
        } finally { timer.close(); scope.cancel() }
    }
    @Test fun endOfTrackStopsOnceWithoutStartingAnotherQueueItem() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var pauses = 0
        val timer = PlaybackSleepTimer(scope, { pauses++ })
        try {
            timer.set(SleepTimerOption.EndOfTrack)
            assertNull(timer.state.value.remainingSeconds)
            assertFalse(timer.tick())
            assertTrue(timer.trackEnded())
            assertFalse(timer.trackEnded())
            assertEquals(1, pauses)
        } finally { timer.close(); scope.cancel() }
    }
}
