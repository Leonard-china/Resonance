package com.resonance.player.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.*

@Composable
actual fun ResonanceBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val key = remember { Any() }
    val callback by rememberUpdatedState(onBack)
    val active = LocalBackHandlerActive.current
    val toolbar = LocalUiBackDispatcher.current
    DisposableEffect(enabled, active, toolbar) {
        if (enabled && active) {
            KeyboardBackNavigation.register(key) { callback() }
            toolbar?.register(key) { callback() }
        }
        onDispose { KeyboardBackNavigation.unregister(key); toolbar?.unregister(key) }
    }
}
