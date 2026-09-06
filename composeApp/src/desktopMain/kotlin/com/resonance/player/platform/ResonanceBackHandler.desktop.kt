package com.resonance.player.platform

import androidx.compose.runtime.Composable

@Composable
actual fun ResonanceBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop platform does not use back gesture handler
}
