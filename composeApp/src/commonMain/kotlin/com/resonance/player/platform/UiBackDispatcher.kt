package com.resonance.player.platform

import androidx.compose.runtime.staticCompositionLocalOf

/** A window-scoped bridge from toolbar Back to the same active native back handlers. */
class UiBackDispatcher {
    private val callbacks = linkedMapOf<Any, () -> Unit>()
    fun register(key: Any, callback: () -> Unit) { callbacks[key] = callback }
    fun unregister(key: Any) { callbacks.remove(key) }
    fun handle(): Boolean {
        val callback = callbacks.values.lastOrNull() ?: return false
        callback()
        return true
    }
}

val LocalUiBackDispatcher = staticCompositionLocalOf<UiBackDispatcher?> { null }
