package com.resonance.player.playback

import com.resonance.player.model.Track
import com.resonance.player.platform.PlatformServices
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import java.lang.reflect.Proxy
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PausedPreparationTest {
    private class Backend {
        var nativeId = "old"
        val commands = mutableListOf<Boolean>()
        val starts = mutableListOf<String>()
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, args ->
            when (method.name) {
                "loadGlobalVolume" -> .5f
                "isAutoLoudnessEnabled" -> false
                "setPlaying" -> { commands += args!![0] as Boolean; Unit }
                "cancelVolumeCalibration", "setVolume", "setTrackGain" -> Unit
                else -> error("Unexpected ${method.name}")
            }
        } as PlatformServices
        fun select(store: PlaybackStateStore, id: String) {
            store.activate(Track(id, id, "Artist", "Album", "3:00", 0), null, expectNativeTrack = true, prepare = { delay(100) }) {
                nativeId = id; starts += id
            }
        }
    }
    @Test fun resumeAfterPreparationStartsSelectedTrackExactlyOnce() = runTest {
        val backend = Backend(); val state = PlaybackStateStore(backend.services, this)
        backend.select(state, "selected"); runCurrent()
        state.requestPlaying(false); advanceUntilIdle()
        assertEquals("old", backend.nativeId)
        assertFalse(state.state.value.isPreparing)
        assertFalse(state.acceptNativeTrack("old"))
        state.requestPlaying(true)
        assertEquals("selected", backend.nativeId)
        state.requestPlaying(true)
        assertEquals(listOf("selected"), backend.starts)
        assertEquals(listOf(false, false, true), backend.commands)
    }
    @Test fun resumeBeforePreparationNeverResumesPreviousNativeItem() = runTest {
        val backend = Backend(); val state = PlaybackStateStore(backend.services, this)
        backend.select(state, "selected"); runCurrent()
        state.requestPlaying(false); state.requestPlaying(true)
        assertFalse(true in backend.commands)
        assertEquals("old", backend.nativeId)
        advanceUntilIdle()
        assertEquals("selected", backend.nativeId)
        assertEquals(listOf("selected"), backend.starts)
    }
    @Test fun newSelectionAndAudioRefreshKeepOnlyLatestDeferredStart() = runTest {
        val backend = Backend(); val state = PlaybackStateStore(backend.services, this)
        backend.select(state, "discarded"); state.requestPlaying(false); advanceUntilIdle()
        backend.select(state, "latest"); state.requestPlaying(false); advanceUntilIdle()
        state.refreshAudio(null); advanceUntilIdle()
        state.requestPlaying(true)
        assertEquals(listOf("latest"), backend.starts)
    }
    @Test fun clearingAndClosingCannotReviveDeferredStarts() = runTest {
        for (close in listOf(false, true)) {
            val backend = Backend(); val state = PlaybackStateStore(backend.services, this)
            backend.select(state, "discarded"); state.requestPlaying(false); advanceUntilIdle()
            if (close) state.close() else state.clearCurrentTrack()
            state.requestPlaying(true)
            assertTrue(backend.starts.isEmpty())
        }
    }
    @Test fun preparationFailureCannotStartDiscardedSelection() = runTest {
        val backend = Backend(); var failure: Throwable? = null
        val state = PlaybackStateStore(backend.services, this) { failure = it }
        state.activate(Track("new", "New", "Artist", "Album", "3:00", 0), null, expectNativeTrack = true, prepare = { error("write failed") }) { backend.starts += "new" }
        advanceUntilIdle()
        assertNotNull(failure)
        assertTrue(backend.starts.isEmpty())
        assertFalse(state.state.value.isPreparing)
    }
}
