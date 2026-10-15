package com.resonance.player.platform

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** A bounded, shared cache. Failed/denied artwork never poisons subsequent fallback attempts. */
object ArtworkMemoryCache {
    private val mutableRevision = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val revision: kotlinx.coroutines.flow.StateFlow<Long> = mutableRevision
    private val mutex = Mutex()
    private val permits = Semaphore(3)
    private val images = LinkedHashMap<String, ImageBitmap>()
    private val pending = mutableMapOf<String, CompletableDeferred<ImageBitmap?>>()
    suspend fun load(path: String): ImageBitmap? {
        var owner = false
        var generation = 0L
        val result = mutex.withLock {
            generation = mutableRevision.value
            images.remove(path)?.let { images[path] = it; return it }
            pending[path] ?: CompletableDeferred<ImageBitmap?>().also { pending[path] = it; owner = true }
        }
        if (!owner) return result.await()
        var bitmap: ImageBitmap? = null
        try {
            bitmap = permits.withPermit { decodeArtwork(path) }
            return bitmap
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    if (generation == mutableRevision.value) bitmap?.let { images[path] = it }
                    while (images.size > 40 || images.values.sumOf { it.width.toLong() * it.height } > 8_000_000) images.remove(images.keys.first())
                    if (pending[path] === result) pending.remove(path)
                    result.complete(bitmap)
                }
            }
        }
    }
    suspend fun invalidate(path: String) { mutex.withLock { images.remove(path); pending.remove(path); mutableRevision.value++ } }
    suspend fun clear() { mutex.withLock { images.clear(); pending.clear(); mutableRevision.value++ } }
}
