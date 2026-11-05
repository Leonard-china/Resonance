package com.resonance.player

import com.resonance.player.application.PlaybackSession
import com.resonance.player.data.*
import com.resonance.player.model.*
import com.resonance.player.platform.PlatformServices
import com.resonance.player.playback.PlaybackStateStore
import java.lang.reflect.Proxy
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackSessionTest {
    private fun track(id: String) = Track(id, id, "", "", "0:10", 0, sourceUri = "synthetic-$id")
    private class Fixture(scope: CoroutineScope, tracks: List<Track>) {
        val played = mutableListOf<Pair<String, List<String>>>()
        val gates = mutableMapOf<String, CompletableDeferred<Unit>>()
        val services = Proxy.newProxyInstance(PlatformServices::class.java.classLoader, arrayOf(PlatformServices::class.java)) { _, method, args ->
            val arguments = args.orEmpty()
            when (method.name) {
                "audioFileAvailable" -> {
                    val gate = gates[(arguments[0] as Track).id]
                    if (gate == null) true else {
                        @Suppress("UNCHECKED_CAST") val continuation = arguments.last() as Continuation<Boolean>
                        scope.launch { withContext(NonCancellable) { gate.await() }; continuation.resume(true) }
                        COROUTINE_SUSPENDED
                    }
                }
                "loadGlobalVolume" -> 1f
                "isAutoLoudnessEnabled" -> false
                "play" -> {
                    @Suppress("UNCHECKED_CAST") val queue = arguments[1] as List<Track>
                    played += (arguments[0] as Track).id to queue.map(Track::id)
                    Unit
                }
                else -> Unit
            }
        } as PlatformServices
        val playback = PlaybackStateStore(services, scope)
        val writer = PersistenceQueue(scope) { throw it }
        val state = object : PlaybackSession.State {
            override var player: PlayerState
                get() = playback.state.value
                set(value) { playback.state.value = value }
            override var customQueue: List<Track>? = null
            override var activePlaylistId: String? = null
            override val selectedPlaylistId: String? = null
            override val fullLibraryQueue = tracks
            override val queue: List<Track> get() = resolvePlaybackQueue(customQueue, fullLibraryQueue)
        }
        val session = PlaybackSession(services, scope, playback, writer,
            PlaybackModeWriter(writer) { }, state, { _, _, _ -> }, {}, { _, _ -> })
        fun close() { gates.values.forEach { it.complete(Unit) }; session.close(); playback.close(); writer.close() }
    }
    @Test fun removingCurrentTrackCannotRestoreItThroughTheOldCapturedQueue() = runTest {
        val tracks = listOf(track("a"), track("b"), track("c"))
        val fixture = Fixture(this, tracks)
        try {
            fixture.state.player = PlayerState(currentTrack = tracks[0], isPlaying = true)
            fixture.session.removeFromQueue(tracks[0])
            repeat(10) { runCurrent() }
            assertEquals(listOf("b", "c"), fixture.state.customQueue!!.map(Track::id))
            assertEquals("b" to listOf("b", "c"), fixture.played.single())
        } finally { fixture.close() }
    }
    @Test fun clearingAnEmptyQueueCancelsAFileCheckThatFinishesLate() = runTest {
        val a = track("a")
        val fixture = Fixture(this, listOf(a))
        try {
            val gate = CompletableDeferred<Unit>(); fixture.gates[a.id] = gate
            fixture.session.startTrack(a); runCurrent()
            fixture.session.clearQueue()
            gate.complete(Unit)
            repeat(10) { runCurrent() }
            assertTrue(fixture.played.isEmpty())
            assertEquals(emptyList(), fixture.state.customQueue)
            assertNull(fixture.state.player.currentTrack)
        } finally { fixture.close() }
    }
    @Test fun rapidRequestsRejectAnObsoleteNonCancellableFileCheck() = runTest {
        val a = track("a"); val b = track("b")
        val fixture = Fixture(this, listOf(a, b))
        try {
            val gate = CompletableDeferred<Unit>(); fixture.gates[a.id] = gate
            fixture.session.startTrack(a); runCurrent()
            fixture.session.startTrack(b); repeat(10) { runCurrent() }
            gate.complete(Unit); repeat(10) { runCurrent() }
            assertEquals(listOf("b"), fixture.played.map { it.first })
            assertEquals("b", fixture.state.player.currentTrack?.id)
        } finally { fixture.close() }
    }
    @Test fun consecutiveQueueCommandsReadTheCommittedInMemoryState() = runTest {
        val a = track("a"); val b = track("b"); val c = track("c")
        val fixture = Fixture(this, listOf(a))
        try {
            fixture.state.player = PlayerState(currentTrack = a)
            fixture.session.playNext(b)
            fixture.session.playNext(c)
            assertEquals(listOf("a", "c", "b"), fixture.state.customQueue!!.map(Track::id))
        } finally { fixture.close() }
    }
}
