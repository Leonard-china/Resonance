package com.resonance.player

import com.resonance.player.model.*
import com.resonance.player.platform.*
import com.resonance.player.playback.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TogetherPlaybackDriverTest {
    private class Room : ListenTogetherService {
        override val state = MutableStateFlow(TogetherState(TogetherRole.Guest, TogetherStatus.Connected))
        override val remotePlayback = MutableStateFlow<TogetherPlayback?>(null)
        val ready = mutableListOf<String>()
        override suspend fun host(port: Int) {}
        override suspend fun join(invitation: String) {}
        override fun invitation(address: String, port: Int) = "fixture"
        override fun publishPlayback(player: PlayerState) {}
        override fun reportPlaybackReady(mediaId: String) { ready += mediaId }
        override fun leave() { state.value = TogetherState(); remotePlayback.value = null }
        override fun close() = leave()
    }
    private fun frame(id: String, sequence: Long = 1) = TogetherPlayback(
        Track("together:$id", "原创 $id", "Resonance", "", "4:00", 1, sourceUri = "fixture"), id, true, .2f, 1f, 240_000, sequence)

    @Test fun rapidReplacementNeverStartsOrAcknowledgesAnOldPreparedTrack() = runTest {
        val room = Room()
        var player = PlayerState()
        val starts = mutableListOf<String>()
        val driver = TogetherPlaybackDriver(room, backgroundScope, { player },
            prepare = { player = PlayerState(currentTrack = it.track, progress = it.progress, positionMs = 48_000) },
            seek = { player = player.copy(progress = it, positionMs = (it * 240_000).toLong()) },
            playing = { player = player.copy(isPlaying = it); if (it) starts += player.currentTrack!!.id },
            speed = { player = player.copy(playbackSpeed = it) }, ready = { delay(200); true })
        runCurrent()
        room.remotePlayback.value = frame("a"); runCurrent(); advanceTimeBy(50)
        room.remotePlayback.value = frame("b"); runCurrent(); advanceTimeBy(250); runCurrent()
        assertEquals(listOf("b"), room.ready)
        assertEquals(listOf("together:b"), starts)
        room.leave(); runCurrent()
        assertFalse(player.isPlaying)
        driver.close()
    }

    @Test fun smallTimingNoiseDoesNotSeekAndLargeCorrectionsHaveHysteresis() {
        val policy = TogetherSyncPolicy()
        assertFalse(policy.shouldSeek(10_000, 10_150, 0, true))
        assertFalse(policy.shouldSeek(10_000, 10_400, 0, true))
        assertTrue(policy.shouldSeek(10_000, 11_500, 0, true))
        assertFalse(policy.shouldSeek(10_000, 11_500, 500, true))
        assertTrue(policy.shouldSeek(10_000, 11_500, 4_000, true))
        assertTrue(policy.shouldSeek(10_000, 10_100, 4_001, false))
        assertEquals(1.025f, policy.effectiveSpeed(10_000, 10_500, 1.0f, true))
        assertEquals(0.975f, policy.effectiveSpeed(10_500, 10_000, 1.0f, true))
        assertEquals(1.0f, policy.effectiveSpeed(10_000, 10_100, 1.0f, true))
    }

    @Test fun interruptionWhilePreparingNeverResumesOrAcknowledgesTheOldAudio() = runTest {
        val room = Room()
        var player = PlayerState()
        var starts = 0
        val driver = TogetherPlaybackDriver(room, backgroundScope, { player },
            prepare = { player = PlayerState(currentTrack = it.track, positionMs = 48_000) },
            seek = {}, playing = { if (it) starts++; player = player.copy(isPlaying = it) },
            speed = {}, ready = { delay(500); true })
        runCurrent()
        room.remotePlayback.value = frame("a"); runCurrent(); advanceTimeBy(100)
        room.state.value = room.state.value.copy(status = TogetherStatus.Reconnecting)
        runCurrent(); advanceTimeBy(600); runCurrent()
        assertEquals(0, starts); assertTrue(room.ready.isEmpty())
        room.leave(); runCurrent()
        assertFalse(player.isPlaying)
        driver.close()
    }
    @Test fun localAudioInterruptionKeepsRoomButNeverAutoplaysUntilExplicitResume() = runTest {
        val room = Room()
        var player = PlayerState()
        var prepares = 0
        val driver = TogetherPlaybackDriver(room, backgroundScope, { player },
            prepare = { prepares++; player = PlayerState(currentTrack = it.track, progress = it.progress, positionMs = (it.progress * it.durationMs).toLong()) },
            seek = { player = player.copy(progress = it, positionMs = (it * 240_000).toLong()) },
            playing = { player = player.copy(isPlaying = it) }, speed = {}, ready = { true })
        runCurrent()
        room.remotePlayback.value = frame("a"); runCurrent()
        assertTrue(player.isPlaying)
        room.state.value = room.state.value.copy(localAudioPaused = true)
        runCurrent()
        assertFalse(player.isPlaying)
        repeat(5) { index -> room.remotePlayback.value = frame("a", index + 2L).copy(progress = .5f); runCurrent() }
        assertFalse(player.isPlaying)
        assertTrue(room.state.value.isGuest)
        assertEquals(1, prepares)
        room.state.value = room.state.value.copy(localAudioPaused = false)
        runCurrent()
        room.remotePlayback.value = frame("a", 10).copy(progress = .6f)
        runCurrent()
        assertTrue(player.isPlaying)
        assertEquals(2, prepares)
        assertTrue(player.progress >= .6f)
        driver.close()
    }
}
