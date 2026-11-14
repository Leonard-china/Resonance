package com.resonance.player.playback

import com.resonance.player.model.Track
import com.resonance.player.platform.PlatformServices
import java.lang.reflect.Proxy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackRecoveryBoundaryTest {
    private class Backend {
        val resumes = mutableListOf<Boolean>()
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader,
            arrayOf(PlatformServices::class.java)) { _, method, args ->
            when (method.name) {
                "loadGlobalVolume" -> 1f
                "isAutoLoudnessEnabled" -> false
                "setPlaying" -> { resumes += args!![0] as Boolean; Unit }
                else -> Unit
            }
        } as PlatformServices
    }
    private val track = Track("selected", "Selected", "", "", "0:10", 0, sourceUri = "fixture.wav")

    @Test fun resumeAfterFailedPreparationRetriesSelectionInsteadOfOldNativeTrack() = runTest {
        val backend = Backend()
        val failures = mutableListOf<Throwable>()
        val store = PlaybackStateStore(backend.services, this, failures::add)
        var preparations = 0
        var starts = 0
        store.activate(track, null, expectNativeTrack = true, prepare = {
            if (++preparations == 1) error("temporary disk failure")
        }) { starts++ }
        advanceUntilIdle()
        assertEquals(1, failures.size)
        store.refreshAudio(null)
        store.setPlaying(true)
        assertFalse(store.state.value.isPlaying)
        assertFalse(store.acceptNativeTrack("old"))
        store.requestPlaying(true)
        advanceUntilIdle()
        assertEquals(2, preparations)
        assertEquals(1, starts)
        assertFalse(true in backend.resumes, "The previous native track must never be resumed")
        store.close()
    }

    @Test fun synchronousNativeStartFailureCanRetryTheSelectedTrack() = runTest {
        val backend = Backend()
        val store = PlaybackStateStore(backend.services, this)
        var attempts = 0
        store.activate(track, null, expectNativeTrack = true) {
            if (++attempts == 1) error("controller unavailable")
        }
        advanceUntilIdle()
        store.requestPlaying(true)
        advanceUntilIdle()
        assertEquals(2, attempts)
        assertFalse(true in backend.resumes)
        store.close()
    }

    @Test fun clearingOrReplacingASelectionDiscardsItsFailedStart() = runTest {
        for (clear in listOf(false, true)) {
            val backend = Backend()
            val store = PlaybackStateStore(backend.services, this)
            var failedPreparations = 0
            var latestStarts = 0
            store.activate(track, null, expectNativeTrack = true, prepare = {
                failedPreparations++; error("unavailable")
            }) { error("failed selection started") }
            advanceUntilIdle()
            if (clear) store.clearCurrentTrack()
            else store.activate(track.copy(id = "latest"), null, expectNativeTrack = true) { latestStarts++ }
            advanceUntilIdle()
            store.requestPlaying(true)
            advanceUntilIdle()
            assertEquals(1, failedPreparations)
            assertEquals(if (clear) 0 else 1, latestStarts)
            store.close()
        }
    }
}
