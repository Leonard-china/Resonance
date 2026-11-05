package com.resonance.player.playback

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.*

class ListeningStatsRepositoryTest {
    @Test fun serviceAndRecreatedUiShareOneStoreAndCannotOverwriteEachOther() = runBlocking {
        val root = Files.createTempDirectory("listening-repository-")
        try {
            val service = ListeningStatsRepository.get(root)
            val recreatedUi = ListeningStatsRepository.get(root.resolve(".").normalize())
            assertSame(service, recreatedUi)
            var now = 0L
            val recorder = service.recorder { now }
            recorder.start("a")
            recorder.sample("a", 0, 100_000, true, 1f)
            now = 2_000; recorder.sample("a", now, 100_000, true, 1f)
            // No UI instance/timer is involved. A later UI reads the service's accumulated data.
            now = 5_000; recorder.sample("a", now, 100_000, false, 1f)
            recreatedUi.flush()
            assertEquals(5L, recreatedUi.store.getAllStats().totalSeconds)
            assertEquals(5L, ListeningStatsStore(root).getAllStats().totalSeconds)
        } finally { ListeningStatsRepository.release(root); root.toFile().deleteRecursively() }
    }

    @Test fun uiSnapshotDoesNotWaitForTheDiskStoresMonitor() = runBlocking {
        val root = Files.createTempDirectory("listening-snapshot-")
        val repository = ListeningStatsRepository.get(root)
        try {
            repository.record("a", 3, false)
            repository.flush()
            assertTrue(repository.snapshots.value.loaded)
            val complete = java.util.concurrent.CountDownLatch(1)
            var seconds = -1L
            synchronized(repository.store) {
                Thread { seconds = repository.getAllStats().totalSeconds; complete.countDown() }.start()
                assertTrue(complete.await(500, java.util.concurrent.TimeUnit.MILLISECONDS), "Reading UI data must not acquire the disk store lock")
            }
            assertEquals(3L, seconds)
        } finally { ListeningStatsRepository.release(root); root.toFile().deleteRecursively() }
    }

    @Test fun writeFailureIsVisibleAndAcceptedIncrementIsNotDuplicatedOnFlushRetry() = runBlocking {
        val root = Files.createTempDirectory("listening-failure-")
        val blocked = root.resolve("not-directory")
        Files.writeString(blocked, "fixture")
        val repository = ListeningStatsRepository.get(blocked)
        try {
            repository.record("a", 1, false)
            assertFails { repository.flush() }
            assertNotNull(repository.errors.value)
            assertEquals(1L, repository.store.getAllStats().totalSeconds)
            assertEquals(1L, repository.snapshots.value.stats.totalSeconds)
            assertNotNull(repository.snapshots.value.error)
            Files.delete(blocked) // This test owns this temporary fixture.
            Files.createDirectory(blocked)
            repository.flush()
            assertNull(repository.errors.value)
            assertEquals(1L, ListeningStatsStore(blocked).getAllStats().totalSeconds)
        } finally { ListeningStatsRepository.release(blocked); root.toFile().deleteRecursively() }
    }
}
