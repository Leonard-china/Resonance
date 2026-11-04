package com.resonance.player.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue

@Composable
actual fun ResonanceBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val active = enabled && LocalBackHandlerActive.current
    val toolbar = LocalUiBackDispatcher.current
    val key = remember { Any() }
    val callback by rememberUpdatedState(onBack)
    DisposableEffect(active, toolbar) {
        if (active) toolbar?.register(key) { callback() }
        onDispose { toolbar?.unregister(key) }
    }
    BackHandler(enabled = active, onBack = onBack)
}
