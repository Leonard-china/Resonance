package com.resonance.player.playback

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow

/** One store/writer per process and data directory, shared by service and recreated Activities. */
class ListeningStatsRepository private constructor(directory: Path) {
    private sealed interface Write {
        data class Record(val id: String, val seconds: Int, val completed: Boolean) : Write
        data class Flush(val finished: CompletableDeferred<Unit>? = null) : Write
    }
    val errors = MutableStateFlow<String?>(null)
    private val snapshotState = MutableStateFlow(com.resonance.player.model.ListeningStatsSnapshot())
    val snapshots: kotlinx.coroutines.flow.StateFlow<com.resonance.player.model.ListeningStatsSnapshot> get() = snapshotState
    val store by lazy { ListeningStatsStore(directory, flushIntervalMillis = 10_000) }
    private val writes = Channel<Write>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    init {
        scope.launch {
            publishSnapshot()
            for (write in writes) {
                try {
                    when (write) {
                        is Write.Record -> store.recordPlayback(write.id, write.seconds, write.completed)
                        is Write.Flush -> store.flush()
                    }
                    if (!store.hasPendingWrites) errors.value = null
                    publishSnapshot()
                    (write as? Write.Flush)?.finished?.complete(Unit)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) {
                    errors.value = "听歌记录未能保存，请检查存储空间；原有记录已保留"
                    (write as? Write.Flush)?.finished?.completeExceptionally(failure)
                } finally { publishSnapshot() }
            }
        }
    }
    private fun publishSnapshot() {
        try { snapshotState.value = com.resonance.player.model.ListeningStatsSnapshot(store.getAllStats(), true, errors.value) }
        catch (_: Exception) {
            errors.value = "听歌记录暂不可用，原有记录已保留"
            snapshotState.value = snapshots.value.copy(error = errors.value)
        }
    }
    fun getAllStats(): com.resonance.player.model.AllListeningStats {
        snapshots.value.error?.let { error(it) }
        return snapshots.value.stats
    }
    fun getTrackStats(id: String): com.resonance.player.model.TrackListeningStats? = getAllStats().trackStats[id]
    fun record(id: String, seconds: Int, completed: Boolean) {
        check(writes.trySend(Write.Record(id, seconds, completed)).isSuccess)
    }
    fun flushSoon() { check(writes.trySend(Write.Flush()).isSuccess) }
    suspend fun flush() {
        val finished = CompletableDeferred<Unit>()
        writes.send(Write.Flush(finished)); finished.await()
    }
    fun recorder(nowMs: () -> Long = { System.nanoTime() / 1_000_000 }) = ListeningStatsRecorder(this, nowMs)

    companion object {
        private val stores = ConcurrentHashMap<Path, ListeningStatsRepository>()
        fun get(directory: Path): ListeningStatsRepository = stores.computeIfAbsent(directory.toAbsolutePath().normalize()) {
            ListeningStatsRepository(it)
        }
        /** Tests own their directory; release its idle writer after all accepted writes finish. */
        internal suspend fun release(directory: Path) {
            stores.remove(directory.toAbsolutePath().normalize())?.let { repository ->
                try { repository.flush() } finally { repository.writes.close(); repository.scope.cancel() }
            }
        }
    }
}

class ListeningStatsRecorder internal constructor(private val repository: ListeningStatsRepository, private val nowMs: () -> Long) {
    private val tracker = ListeningSessionTracker(repository::record)
    @Synchronized fun start(id: String?, positionMs: Long = 0) {
        repository.flushSoon(); tracker.start(id, positionMs, nowMs())
    }
    @Synchronized fun sample(id: String?, positionMs: Long, durationMs: Long, playing: Boolean, speed: Float,
                             discontinuity: Boolean = false) {
        tracker.sample(id, positionMs, durationMs, playing, speed, nowMs(), discontinuity)
        if (!playing) repository.flushSoon()
    }
    @Synchronized fun finish() { tracker.finish(nowMs()); repository.flushSoon() }
    fun flushSoon() = repository.flushSoon()
}
