package com.resonance.player

import com.resonance.player.data.PersistenceQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PersistenceQueueTest {
    @Test fun diskWritesStayOrderedAndFailureDoesNotDropFollowingActions() = runTest {
        val output = mutableListOf<Int>()
        val errors = mutableListOf<Throwable>()
        val queue = PersistenceQueue(backgroundScope, errors::add)
        queue.submit { delay(100); output += 1 }
        queue.submit { error("write failed") }
        queue.submit { output += 2 }
        runCurrent()
        advanceTimeBy(101)
        runCurrent()
        assertEquals(listOf(1, 2), output)
        assertEquals(1, errors.size)
        queue.close()
    }
    @Test fun rapidSliderChangesKeepLatestValuesForEachTrackAndGlobalVolume() = runTest {
        val output = mutableMapOf<String, Float>()
        val queue = PersistenceQueue(backgroundScope) { throw it }
        for (value in listOf(0.2f, 0.3f, 0.4f)) {
            queue.submitLatest("global") { output["global"] = value }
            queue.submitLatest("a") { output["a"] = value }
        }
        queue.submitLatest("global") { output["global"] = 0.8f }
        queue.submitLatest("b") { output["b"] = 0.8f }
        runCurrent()
        assertEquals(mapOf("global" to 0.8f, "a" to 0.4f, "b" to 0.8f), output)
        queue.close()
    }

    @Test fun disposingTheUiFinishesAcceptedWrites() = runTest {
        val ui = CoroutineScope(coroutineContext + Job())
        val output = mutableListOf<Int>()
        val queue = PersistenceQueue(ui) { throw it }
        queue.submit { delay(100); output += 1 }
        queue.submit { output += 2 }
        runCurrent()
        ui.cancel()
        advanceUntilIdle()
        assertEquals(listOf(1, 2), output)
    }

    @Test fun coalescedWritesDoNotJumpAcrossOrderedActions() = runTest {
        val output = mutableListOf<String>()
        val queue = PersistenceQueue(backgroundScope) { throw it }
        queue.submitLatest("snapshot") { output += "old progress" }
        queue.submitLatest("snapshot") { output += "latest old progress" }
        queue.submit { output += "start new queue" }
        queue.submitLatest("snapshot") { output += "new progress" }
        queue.submitLatest("snapshot") { output += "latest new progress" }
        runCurrent()
        assertEquals(listOf("latest old progress", "start new queue", "latest new progress"), output)
        queue.close()
    }
}
