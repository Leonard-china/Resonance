package com.resonance.player.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.*

@Composable
actual fun ResonanceBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val key = remember { Any() }
    val callback by rememberUpdatedState(onBack)
    val active = LocalBackHandlerActive.current
    DisposableEffect(enabled, active) {
        if (enabled && active) KeyboardBackNavigation.register(key) { callback() }
        onDispose { KeyboardBackNavigation.unregister(key) }
    }
}
