package com.resonance.player.playback

import com.resonance.player.model.Track
import com.resonance.player.platform.PlatformServices
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackStateStoreTest {
    @Test fun clearingTemporaryPlaybackCancelsAStartThatFinishesAfterLeavingTheRoom() = runTest {
        val backend = Backend(this)
        backend.delays["loadGlobalVolume:"] = 1000
        val store = PlaybackStateStore(backend.services, this)
        var started = false
        store.activate(track("together:session:audio"), null, analyzeLoudness = false) { started = true }
        runCurrent()
        store.clearCurrentTrack()
        advanceUntilIdle()
        assertFalse(started)
        assertNull(store.state.value.currentTrack)
        assertFalse(store.state.value.isPlaying)
    }
    private fun track(id: String) = Track(id, id, "artist", "album", "3:00", 0, sourceUri = "$id.mp3")
    private class Backend(private val scope: CoroutineScope) {
        val volumes = mutableListOf<Float>()
        val gains = mutableListOf<Float>()
        val playingCommands = mutableListOf<Boolean>()
        val values = mutableMapOf<String, Any?>()
        val delays = mutableMapOf<String, Long>()
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, args ->
            val arguments = args.orEmpty()
            when (method.name) {
                "setVolume" -> { volumes += arguments[0] as Float; Unit }
                "setTrackGain" -> { gains += arguments[0] as Float; Unit }
                "cancelVolumeCalibration" -> Unit
                "setPlaying" -> { playingCommands += arguments[0] as Boolean; Unit }
                else -> {
                    val id = when (val arg = arguments.firstOrNull()) { is Track -> arg.id; is String -> arg; else -> "" }
                    val key = "${method.name}:$id"
                    val fallback: Any? = when (method.name) {
                        "isAutoLoudnessEnabled" -> true
                        "loadGlobalVolume" -> 0.4f
                        "analyzeAndCacheLoudnessGain" -> 0f
                        else -> null
                    }
                    val value = if (values.containsKey(key)) values[key] else fallback
                    if (value is Throwable) throw value
                    val wait = delays[key] ?: 0L
                    if (wait > 0) {
                        @Suppress("UNCHECKED_CAST") val continuation = arguments.last() as Continuation<Any?>
                        // Deliberately finishes even after cancellation, like a native decoder callback.
                        scope.launch { delay(wait); continuation.resume(value) }
                        COROUTINE_SUSPENDED
                    } else value
                }
            }
        } as PlatformServices
    }

    @Test fun rapidSwitchIgnoresLegacyCalibrationAndAppliesOnlyLatestBalancedGain() = runTest {
        val backend = Backend(this)
        backend.values["loadTrackVolume:a"] = 0.9f
        backend.values["loadTrackCalibratedGain:a"] = 12f
        backend.delays["loadGlobalVolume:"] = 1000
        backend.values["loadGlobalVolume:"] = 0.3f
        backend.values["loadAutoLoudnessGain:b"] = -4f
        backend.values["loadTrackCalibratedGain:b"] = -4f
        val store = PlaybackStateStore(backend.services, this)
        val started = mutableListOf<String>()
        store.activate(track("a"), null) { started += "a" }
        runCurrent()
        store.activate(track("b"), null) { started += "b" }
        advanceUntilIdle()
        assertEquals(listOf("b"), started)
        assertEquals(0.3f, store.state.value.volume)
        assertEquals(-4f, store.state.value.trackGainDb)
        assertFalse(12f in backend.gains)
        assertFalse(0.9f in backend.volumes)
    }

    @Test fun lateAnalysisCannotOverwriteNewSong() = runTest {
        val backend = Backend(this)
        backend.values["analyzeAndCacheLoudnessGain:a"] = 10f
        backend.delays["analyzeAndCacheLoudnessGain:a"] = 1000
        backend.values["loadAutoLoudnessGain:b"] = -2f
        val store = PlaybackStateStore(backend.services, this)
        store.activate(track("a"), null)
        runCurrent()
        store.activate(track("b"), null)
        advanceUntilIdle()
        assertEquals("b", store.state.value.currentTrack?.id)
        assertEquals(-2f, backend.gains.last())
        assertFalse(10f in backend.gains)
    }

    @Test fun manualVolumeAndGainWinOverPendingReadsAndAnalysis() = runTest {
        val backend = Backend(this)
        backend.values["loadTrackVolume:a"] = 0.9f
        backend.delays["loadGlobalVolume:"] = 500
        backend.values["analyzeAndCacheLoudnessGain:a"] = 14f
        val store = PlaybackStateStore(backend.services, this)
        var started = false
        store.activate(track("a"), null) { started = true }
        runCurrent()
        store.setVolume(0.2f)
        store.setGain(-3f)
        advanceUntilIdle()
        assertTrue(started)
        assertEquals(0.2f, backend.volumes.last())
        assertEquals(-3f, backend.gains.last())
    }

    @Test fun volumeAndCachedGainAreAppliedBeforePlaybackStarts() = runTest {
        val backend = Backend(this)
        backend.values["loadGlobalVolume:"] = 0.25f
        backend.values["loadAutoLoudnessGain:a"] = 3f
        val store = PlaybackStateStore(backend.services, this)
        store.activate(track("a"), null) {
            assertEquals(0.25f, backend.volumes.last())
            assertEquals(3f, backend.gains.last())
        }
        advanceUntilIdle()
    }

    @Test fun disposedStateRejectsLateAudioResults() = runTest {
        val backend = Backend(this)
        backend.delays["loadGlobalVolume:"] = 500
        val store = PlaybackStateStore(backend.services, this)
        store.activate(track("a"), null)
        runCurrent()
        store.close()
        advanceUntilIdle()
        assertTrue(backend.volumes.isEmpty())
    }

    @Test fun switchingDuringQueuePreparationCannotStartThePreviousTrack() = runTest {
        val backend = Backend(this)
        val store = PlaybackStateStore(backend.services, this)
        val started = mutableListOf<String>()
        store.activate(track("a"), null, prepare = { delay(500) }) { started += "a" }
        runCurrent()
        store.activate(track("b"), null) { started += "b" }
        advanceUntilIdle()
        assertEquals(listOf("b"), started)
    }

    @Test fun refreshingAudioWhileLoadingKeepsThePendingPlaybackRequest() = runTest {
        val backend = Backend(this)
        backend.delays["loadGlobalVolume:"] = 500
        val store = PlaybackStateStore(backend.services, this)
        var prepared = false
        var starts = 0
        store.activate(track("a"), null, prepare = { prepared = true }) { assertTrue(prepared); starts++ }
        runCurrent()
        store.refreshAudio(null)
        advanceUntilIdle()
        assertEquals(1, starts)
    }

    @Test fun oldNativeTrackEventsCannotCancelTheLatestPlaybackRequest() = runTest {
        val backend = Backend(this)
        val store = PlaybackStateStore(backend.services, this)
        store.activate(track("a"), null, expectNativeTrack = true)
        runCurrent()
        store.activate(track("b"), null, expectNativeTrack = true)
        assertFalse(store.acceptNativeTrack("a"))
        advanceUntilIdle()
        assertFalse(store.acceptNativeTrack("a"))
        assertFalse(store.acceptNativeTrack("b"))
        assertTrue(store.acceptNativeTrack("c"))
    }

    @Test fun pauseWhilePreparingPreventsPlaybackStartOnReady() = runTest {
        val backend = Backend(this)
        backend.delays["loadGlobalVolume:"] = 500
        val store = PlaybackStateStore(backend.services, this)
        var started = false
        store.activate(track("a"), null) { started = true }
        runCurrent()
        assertTrue(store.state.value.isPreparing)
        assertTrue(store.state.value.isLoading)
        // User pauses or loses focus while prepare is still pending.
        store.setUserPlayIntent(false)
        assertFalse(store.state.value.isPlaying)
        advanceUntilIdle()
        assertFalse(started)
        assertFalse(store.state.value.isPreparing)
        assertFalse(store.state.value.isPlaying)
        assertFalse(store.state.value.isLoading)
    }

    @Test fun previousNativePauseCannotCancelASingleTrackSelection() = runTest {
        val backend = Backend(this)
        val store = PlaybackStateStore(backend.services, this)
        var audible = "old"
        store.activate(track("old"), null, expectNativeTrack = true) { audible = "old" }
        advanceUntilIdle()
        store.setPlaying(true)

        backend.delays["loadGlobalVolume:"] = 500
        store.activate(track("selected"), null, expectNativeTrack = true) { audible = "selected" }
        runCurrent()
        // Android reports the pause of the old media item after the UI selects the new one.
        store.setPlaying(false)
        advanceUntilIdle()

        assertEquals("selected", audible)
        assertEquals(audible, store.state.value.currentTrack?.id)
        store.setPlaying(true)
        assertTrue(store.state.value.isPlaying)
        store.setPlaying(false)
        assertFalse(store.state.value.isPlaying)
    }

    @Test fun previousNativePlayingCannotUndoAnExplicitPauseDuringPreparation() = runTest {
        val backend = Backend(this)
        backend.delays["loadGlobalVolume:"] = 500
        val store = PlaybackStateStore(backend.services, this)
        var started = false
        store.activate(track("selected"), null, expectNativeTrack = true) { started = true }
        runCurrent()
        store.setUserPlayIntent(false)
        store.setPlaying(true) // Late event from the previous media item.
        advanceUntilIdle()
        assertFalse(store.state.value.isPlaying)
        assertFalse(started)
    }

    @Test fun audioRefreshPreservesProtectionFromPreviousNativeStatus() = runTest {
        val backend = Backend(this)
        backend.delays["loadGlobalVolume:"] = 500
        val store = PlaybackStateStore(backend.services, this)
        var started = false
        store.activate(track("selected"), null, expectNativeTrack = true) { started = true }
        runCurrent()
        store.refreshAudio(null)
        store.setPlaying(false)
        advanceUntilIdle()
        assertTrue(started)
        assertEquals("selected", store.state.value.currentTrack?.id)
    }

    @Test fun bufferingTransitionsReflectInPlayerStateAndClearOnPlayback() = runTest {
        val backend = Backend(this)
        val store = PlaybackStateStore(backend.services, this)
        store.activate(track("a"), null)
        advanceUntilIdle()
        assertFalse(store.state.value.isBuffering)
        assertFalse(store.state.value.isLoading)

        store.setBuffering(true)
        assertTrue(store.state.value.isBuffering)
        assertTrue(store.state.value.isLoading)

        // Once native player starts playing, buffering should automatically clear.
        store.setPlaying(true)
        assertTrue(store.state.value.isPlaying)
        assertFalse(store.state.value.isBuffering)
        assertFalse(store.state.value.isLoading)
    }

    @Test fun failureExitsPreparingAndBufferingStates() = runTest {
        val backend = Backend(this)
        var reportedError: Throwable? = null
        val store = PlaybackStateStore(backend.services, this) { reportedError = it }
        store.activate(track("a"), null, prepare = { error("disk read failed") })
        runCurrent()
        advanceUntilIdle()
        assertNotNull(reportedError)
        assertFalse(store.state.value.isPreparing)
        assertFalse(store.state.value.isBuffering)
        assertFalse(store.state.value.isPlaying)
        assertFalse(store.state.value.isLoading)
    }

    private fun native(id: String, playing: Boolean = true, ready: Boolean = playing, buffering: Boolean = false) =
        NativePlaybackState(id, playing, ready, buffering, 0.25f, 45_000L)

    @Test fun nativeAutoAdvanceKeepsPlayingWithoutAnotherPlayingEvent() = runTest {
        val backend = Backend(this)
        backend.delays["loadGlobalVolume:"] = 500
        val store = PlaybackStateStore(backend.services, this)
        store.setPlaying(true)
        store.adoptNativeTrack(track("next"), null, native("next"))
        assertTrue(store.state.value.isPlaying)
        assertFalse(store.state.value.isPreparing)
        assertEquals(0.25f, store.state.value.progress)
        assertEquals(45_000L, store.state.value.positionMs)
        advanceUntilIdle() // Media3 stays playing and emits no additional isPlaying=true callback.
        assertTrue(store.state.value.isPlaying)
        assertTrue(backend.playingCommands.isEmpty())
    }

    @Test fun nativeSnapshotOverridesAnOldPausedUiOnAutoAdvance() = runTest {
        val backend = Backend(this)
        val store = PlaybackStateStore(backend.services, this)
        store.setPlaying(false)
        store.adoptNativeTrack(track("next"), null, native("next"))
        advanceUntilIdle()
        assertTrue(store.state.value.isPlaying)
        assertEquals("next", store.state.value.currentTrack?.id)
        assertTrue(backend.playingCommands.isEmpty())
    }

    @Test fun bufferingNativeTransitionDoesNotTurnIntoAPauseAfterGainLoads() = runTest {
        val backend = Backend(this)
        backend.delays["loadGlobalVolume:"] = 500
        val store = PlaybackStateStore(backend.services, this)
        store.adoptNativeTrack(track("next"), null, native("next", playing = false, ready = true, buffering = true))
        store.setPlaying(false) // Not audible while buffering, but playWhenReady is still true.
        advanceUntilIdle()
        assertTrue(store.state.value.isBuffering)
        assertTrue(backend.playingCommands.isEmpty())
        store.setPlaying(true)
        assertTrue(store.state.value.isPlaying)
        assertFalse(store.state.value.isBuffering)
    }

    @Test fun pausedNativeTransitionIsAdoptedWithoutRestartOrPauseCommands() = runTest {
        val backend = Backend(this)
        val store = PlaybackStateStore(backend.services, this)
        store.setPlaying(true)
        store.adoptNativeTrack(track("next"), null, native("next", playing = false))
        advanceUntilIdle()
        assertFalse(store.state.value.isPlaying)
        assertTrue(backend.playingCommands.isEmpty())
    }

    @Test fun explicitPauseDuringNativeGainLoadingRemainsPaused() = runTest {
        val backend = Backend(this)
        backend.delays["loadGlobalVolume:"] = 500
        val store = PlaybackStateStore(backend.services, this)
        store.adoptNativeTrack(track("next"), null, native("next"))
        runCurrent()
        val requestedPlaying = !store.state.value.isPlaying // Same toggle as the main and floating controls.
        assertFalse(requestedPlaying)
        store.setUserPlayIntent(requestedPlaying)
        backend.services.setPlaying(requestedPlaying)
        store.setPlaying(false)
        advanceUntilIdle()
        assertFalse(store.state.value.isPlaying)
        assertEquals(listOf(false), backend.playingCommands)
    }

    @Test fun nextNativeTransitionCanReplacePendingGainWork() = runTest {
        val backend = Backend(this)
        backend.delays["loadAutoLoudnessGain:b"] = 500
        backend.values["loadAutoLoudnessGain:b"] = 8f
        backend.values["loadAutoLoudnessGain:c"] = -2f
        val store = PlaybackStateStore(backend.services, this)
        store.adoptNativeTrack(track("b"), null, native("b"))
        runCurrent()
        store.adoptNativeTrack(track("c"), null, native("c"))
        advanceUntilIdle()
        assertEquals("c", store.state.value.currentTrack?.id)
        assertTrue(store.state.value.isPlaying)
        assertEquals(-2f, store.state.value.trackGainDb)
        assertFalse(8f in backend.gains)
    }

    @Test fun audioRefreshDuringNativeTransitionPreservesPauseAndPosition() = runTest {
        val backend = Backend(this)
        backend.delays["loadGlobalVolume:"] = 500
        val store = PlaybackStateStore(backend.services, this)
        store.adoptNativeTrack(track("next"), null, native("next"))
        runCurrent()
        store.setUserPlayIntent(false)
        store.refreshAudio(null)
        advanceUntilIdle()
        assertFalse(store.state.value.isPlaying)
        assertFalse(store.state.value.isPreparing)
        assertEquals(45_000L, store.state.value.positionMs)
        assertTrue(backend.playingCommands.isEmpty())
    }

    @Test fun gainReadFailureCannotMarkAnAudibleNativeTrackPaused() = runTest {
        val backend = Backend(this)
        backend.values["loadGlobalVolume:"] = IllegalStateException("settings read failed")
        var failure: Throwable? = null
        val store = PlaybackStateStore(backend.services, this) { failure = it }
        store.adoptNativeTrack(track("next"), null, native("next"))
        advanceUntilIdle()
        assertNotNull(failure)
        assertTrue(store.state.value.isPlaying)
        assertFalse(store.state.value.isPreparing)
        assertTrue(backend.playingCommands.isEmpty())
    }

    @Test fun obsoleteOrUnacknowledgedNativeSnapshotsCannotReplaceUserSelection() = runTest {
        val backend = Backend(this)
        val store = PlaybackStateStore(backend.services, this)
        store.activate(track("selected"), null, expectNativeTrack = true)
        advanceUntilIdle()
        store.adoptNativeTrack(track("old"), null, native("selected")) // Queued old event; controller already moved.
        store.adoptNativeTrack(track("old"), null, native("old"))
        assertEquals("selected", store.state.value.currentTrack?.id)
        store.adoptNativeTrack(track("selected"), null, native("selected")) // Acknowledge latest explicit request.
        store.adoptNativeTrack(track("next"), null, native("next"))
        advanceUntilIdle()
        assertEquals("next", store.state.value.currentTrack?.id)
        assertTrue(store.state.value.isPlaying)
    }

    @Test fun loudnessFailureAfterPlaybackStartsCannotFalselyPauseTheButton() = runTest {
        val backend = Backend(this)
        backend.values["analyzeAndCacheLoudnessGain:next"] = IllegalStateException("analysis failed")
        var failure: Throwable? = null
        val store = PlaybackStateStore(backend.services, this) { failure = it }
        store.activate(track("next"), null, expectNativeTrack = true) { store.setPlaying(true) }
        advanceUntilIdle()
        assertNotNull(failure)
        assertTrue(store.state.value.isPlaying)
        assertFalse(store.state.value.isPreparing)
        assertTrue(backend.playingCommands.isEmpty())
    }
}
