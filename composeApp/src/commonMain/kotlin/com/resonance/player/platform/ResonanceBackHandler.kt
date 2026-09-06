package com.resonance.player.platform

import androidx.compose.runtime.Composable

@Composable
expect fun ResonanceBackHandler(enabled: Boolean = true, onBack: () -> Unit)
