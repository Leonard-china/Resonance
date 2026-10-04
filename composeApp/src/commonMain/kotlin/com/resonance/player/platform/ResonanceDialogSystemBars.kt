package com.resonance.player.platform

import androidx.compose.runtime.Composable

/** A full-screen player dialog owns a separate Android window. */
@Composable
expect fun ResonanceDialogSystemBars(isDark: Boolean)
