package com.resonance.player

import com.resonance.player.application.PlaybackSession
import com.resonance.player.data.PersistenceQueue
import com.resonance.player.data.PlaybackModeWriter
import com.resonance.player.model.*
import com.resonance.player.platform.PlatformServices
import com.resonance.player.playback.PlaybackStateStore
import java.lang.reflect.Proxy
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackSessionQueueEditTest {
    @Test fun queueEditsDuringPausedPreparationReachTheNativeStart() = runTest {
        val tracks = listOf("a", "b", "c").map { Track(it, it, "", "", "0:10", 0, sourceUri = "fixture-$it") }
        var submitted: List<Track>? = null
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, args ->
            when (method.name) {
                "audioFileAvailable" -> true
                "loadGlobalVolume" -> 1f
                "isAutoLoudnessEnabled" -> false
                "play" -> { @Suppress("UNCHECKED_CAST") submitted = args!![1] as List<Track>; Unit }
                else -> Unit
            }
        } as PlatformServices
        val playback = PlaybackStateStore(services, this)
        val writes = PersistenceQueue(this) { throw it }
        val state = object : PlaybackSession.State {
            override var player: PlayerState
                get() = playback.state.value
                set(value) { playback.state.value = value }
            override var customQueue: List<Track>? = null
            override var activePlaylistId: String? = null
            override val selectedPlaylistId: String? = null
            override val fullLibraryQueue = tracks
            override val queue get() = resolvePlaybackQueue(customQueue, fullLibraryQueue)
        }
        val session = PlaybackSession(services, this, playback, writes, PlaybackModeWriter(writes) {}, state, { _, _, _ -> }, {}, { _, _ -> })
        try {
            writes.submit { delay(100) }
            session.startTrack(tracks[0], tracks)
            runCurrent()
            playback.requestPlaying(false)
            advanceUntilIdle()
            assertNull(submitted)
            session.playNext(tracks[2])
            session.removeFromQueue(tracks[1])
            playback.requestPlaying(true)
            advanceUntilIdle()
            assertEquals(listOf("a", "c"), submitted!!.map { it.id })
        } finally { session.close(); playback.close(); writes.close() }
    }
}
