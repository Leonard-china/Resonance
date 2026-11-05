package com.resonance.player

import com.resonance.player.application.*
import com.resonance.player.data.PersistenceQueue
import com.resonance.player.model.ThemeMode
import com.resonance.player.platform.PreferenceServices
import com.resonance.player.platform.LibraryServices
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class ApplicationSessionTest {
    @Test fun shutdownStopsProducersThenDrainsEveryWriterBeforeReleasingPlatform() = runBlocking {
        val events = mutableListOf<String>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val writer = PersistenceQueue(scope) { throw it }
        val releaseWrite = CompletableDeferred<Unit>()
        writer.submit { releaseWrite.await(); events += "write" }
        val life = ApplicationLifetime { events += "release" }
        life.registerStop("producer") { events += "stop"; scope.cancel(); releaseWrite.complete(Unit) }
        life.register("writer") { writer.closeAndJoin() }
        val first = life.close()
        assertSame(first, life.close())
        first.join()
        assertEquals(listOf("stop", "write", "release"), events)
        assertTrue(life.shutdownFailures.value.isEmpty())
    }
    @Test fun cancellationCleanupCanCommitBeforeTheWritersAreClosed() = runBlocking {
        val tasks = ApplicationTasks(Dispatchers.Default)
        val writer = PersistenceQueue(tasks.scope) { throw it }
        val entered = CompletableDeferred<Unit>()
        var acceptedWriteFinished = false
        var platformSawWrite = false
        tasks.scope.launch {
            try { entered.complete(Unit); awaitCancellation() }
            finally { withContext(NonCancellable) { delay(20); writer.submit { acceptedWriteFinished = true } } }
        }
        entered.await()
        val life = ApplicationLifetime { platformSawWrite = acceptedWriteFinished }
        life.registerStop("producer", tasks::close)
        life.registerCompletion("producer") { tasks.join() }
        life.register("writer") { writer.closeAndJoin() }
        life.close().join()
        assertTrue(platformSawWrite)
        assertTrue(life.shutdownFailures.value.isEmpty())
    }
        @Test fun oneFailedDrainDoesNotSkipOtherWritersOrNativeCleanup() = runBlocking {
        val events = mutableListOf<String>()
        val life = ApplicationLifetime { events += "release" }
        life.register("failure") { error("synthetic failure") }
        life.register("remaining") { events += "remaining" }
        life.close().join()
        assertEquals(listOf("remaining", "release"), events)
        assertEquals(1, life.shutdownFailures.value.size)
        assertFailsWith<IllegalStateException> { life.register("late") {} }
        Unit
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun failingTaskIsObservableAndDoesNotCancelOtherApplicationWork() = runTest {
        val tasks = ApplicationTasks(coroutineContext)
        var finished = false
        tasks.scope.launch { error("synthetic task failure") }
        tasks.scope.launch { delay(10); finished = true }
        advanceUntilIdle()
        assertTrue(finished)
        assertEquals("synthetic task failure", tasks.failures.value?.message)
        tasks.close()
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun cancellingOneQueuedOperationDoesNotStrandAnOrderedBarrier() = runTest {
        val errors = mutableListOf<Throwable>()
        val writer = PersistenceQueue(backgroundScope, errors::add)
        writer.submit { throw CancellationException("cancel only this operation") }
        var completed = false
        val barrier = async { writer.run { completed = true } }
        runCurrent()
        barrier.await()
        assertTrue(completed)
        assertTrue(errors.isEmpty())
        writer.closeAndJoin()
    }
}
