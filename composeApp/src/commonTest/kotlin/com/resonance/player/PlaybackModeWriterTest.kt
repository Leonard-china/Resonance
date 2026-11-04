package com.resonance.player

import com.resonance.player.data.PersistenceQueue
import com.resonance.player.data.PlaybackModeWriter
import com.resonance.player.model.PlaybackModePreference
import com.resonance.player.model.RepeatMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlaybackModeWriterTest {
    @Test fun blockedOldWriteCannotOverwriteLastActionAndCloseDrains() = runTest {
        val gate = CompletableDeferred<Unit>()
        val writes = mutableListOf<PlaybackModePreference>()
        var inFlight = 0
        val queue = PersistenceQueue(this) { throw it }
        val writer = PlaybackModeWriter(queue) { preference ->
            assertEquals(1, ++inFlight, "Preference writes must not race")
            if (writes.isEmpty()) gate.await()
            writes += preference
            inFlight--
        }
        writer.save(true, RepeatMode.Off)
        runCurrent()
        writer.save(true, RepeatMode.All)
        writer.save(false, RepeatMode.All)
        writer.save(false, RepeatMode.One)
        queue.close()
        runCurrent()
        assertTrue(writes.isEmpty())
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(PlaybackModePreference(true, RepeatMode.Off), PlaybackModePreference(false, RepeatMode.One)), writes)
    }
}
