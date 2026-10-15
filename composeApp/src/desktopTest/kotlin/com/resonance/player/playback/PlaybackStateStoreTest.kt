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
    private fun track(id: String) = Track(id, id, "artist", "album", "3:00", 0, sourceUri = "$id.mp3")
    private class Backend(private val scope: CoroutineScope) {
        val volumes = mutableListOf<Float>()
        val gains = mutableListOf<Float>()
        val values = mutableMapOf<String, Any?>()
        val delays = mutableMapOf<String, Long>()
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, args ->
            val arguments = args.orEmpty()
            when (method.name) {
                "setVolume" -> { volumes += arguments[0] as Float; Unit }
                "setTrackGain" -> { gains += arguments[0] as Float; Unit }
                "cancelVolumeCalibration" -> Unit
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
        assertEquals("b", store.state.value.currentTrack?.id)
    }
}
