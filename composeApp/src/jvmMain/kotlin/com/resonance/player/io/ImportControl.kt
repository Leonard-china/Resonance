package com.resonance.player.io

import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

class ImportControl {
    private val cancelled = AtomicBoolean(false)
    val isCancelled get() = cancelled.get()
    fun cancel() { cancelled.set(true) }
    fun check() { if (cancelled.get() || Thread.currentThread().isInterrupted) throw CancellationException("已取消；已完成的歌曲已保留") }
}
