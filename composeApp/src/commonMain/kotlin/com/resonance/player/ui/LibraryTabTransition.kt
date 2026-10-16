package com.resonance.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.ResonanceMotionTokens

/** Keep one lazy tree alive. Reading motion only in the layer avoids per-frame layout. */
@Composable
internal fun LibraryTabTransition(index: Int, content: @Composable () -> Unit) {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(1f) }
    var previous by remember { mutableIntStateOf(index) }
    var direction by remember { mutableIntStateOf(1) }
    LaunchedEffect(index) {
        if (previous != index) {
            direction = if (index > previous) 1 else -1
            previous = index
            progress.snapTo(0f)
            progress.animateTo(1f, tween(160, easing = ResonanceMotionTokens.ExitEasing))
        }
    }
    Box(Modifier.fillMaxSize().clipToBounds()) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            val p = if (previous != index) 0f else progress.value
            val d = if (previous != index) { if (index > previous) 1 else -1 } else direction
            alpha = 0.65f + 0.35f * p
            translationX = if (reduced) 0f else d * 24.dp.toPx() * (1f - p)
        }) { content() }
    }
}
