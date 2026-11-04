package com.resonance.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** List and detail share the same short, interruptible crossfade as the approved prototype. */
@Composable
internal fun <T> LibraryDetailTransition(detail: T?, modifier: Modifier = Modifier, content: @Composable (T?) -> Unit) {
    ContinuousPageTransition(detail, modifier, direction = { from, to -> if (from != null && to == null) -1 else 1 }, content = content)
}
