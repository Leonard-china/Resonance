package com.resonance.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun BilibiliWebLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        onDismiss()
    }
}
