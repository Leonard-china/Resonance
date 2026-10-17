package com.resonance.player.platform

val LocalBackHandlerActive = androidx.compose.runtime.staticCompositionLocalOf { true }

/** Desktop Escape runs after focused controls and dialogs have handled their own keys. */
object KeyboardBackNavigation {
    private val callbacks = linkedMapOf<Any, () -> Unit>()
    fun register(key: Any, callback: () -> Unit) { callbacks[key] = callback }
    fun unregister(key: Any) { callbacks.remove(key) }
    fun handle(): Boolean {
        val callback = callbacks.values.lastOrNull() ?: return false
        callback(); return true
    }
}
