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

/** Single lazy tree with a short directional reveal; saved list state stays outside. */
@Composable
internal fun LibraryTabTransition(index: Int, content: @Composable () -> Unit) {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(1f) }
    var previous by remember { mutableIntStateOf(index) }
    var direction by remember { mutableIntStateOf(1) }
    LaunchedEffect(index, reduced) {
        if (reduced) {
            previous = index
            progress.snapTo(1f)
        } else if (previous != index) {
            direction = if (index > previous) 1 else -1
            previous = index
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = ResonanceMotionTokens.TabDuration, easing = ResonanceMotionTokens.ExitEasing),
            )
        }
    }
    Box(Modifier.fillMaxSize().clipToBounds()) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            val p = if (previous != index) 0f else progress.value
            val d = if (previous != index) { if (index > previous) 1 else -1 } else direction
            alpha = if (reduced) 1f else (0.15f + 0.85f * p)
            translationX = if (reduced) 0f else d * 32.dp.toPx() * (1f - p)
        }) { content() }
    }
}
