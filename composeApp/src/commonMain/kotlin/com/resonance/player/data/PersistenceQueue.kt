package com.resonance.player.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Enqueue on the UI thread to preserve user action order across suspending disk writes. */
class PersistenceQueue(scope: CoroutineScope, private val onError: (Throwable) -> Unit) {
    private class Write(val key: String?, var action: suspend () -> Unit)
    private val writes = Channel<Write>(Channel.UNLIMITED)
    private val pending = mutableMapOf<String, Write>()
    private val writerScope = CoroutineScope(scope.coroutineContext.minusKey(Job))
    init {
        scope.coroutineContext[Job]?.invokeOnCompletion { close() }
        writerScope.launch {
            try {
                for (write in writes) {
                    write.key?.let { key -> if (pending[key] === write) pending.remove(key) }
                    try { write.action() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { onError(error) }
                }
            } finally { writerScope.cancel() }
        }
    }
    fun submit(write: suspend () -> Unit) {
        // An ordered action is a barrier: later slider/snapshot values must stay after it.
        pending.clear()
        check(writes.trySend(Write(null, write)).isSuccess)
    }
    suspend fun run(write: suspend () -> Unit) {
        val finished = CompletableDeferred<Unit>()
        submit {
            try { write(); finished.complete(Unit) }
            catch (error: Exception) { finished.completeExceptionally(error); throw error }
        }
        finished.await()
    }
    fun submitLatest(key: String, write: suspend () -> Unit) {
        val queued = pending[key]
        if (queued != null) queued.action = write
        else {
            val entry = Write(key, write)
            check(writes.trySend(entry).isSuccess)
            pending[key] = entry
        }
    }
    /** Stop accepting actions; finish everything already accepted even if the UI is disposed. */
    fun close() { writes.close() }
}
