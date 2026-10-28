package com.resonance.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.ResonanceMotionTokens
import kotlin.math.sin
import kotlin.math.PI

/** Single lazy tree with a short directional reveal; saved list state stays outside. */
@Composable
internal fun LibraryTabTransition(index: Int, content: @Composable () -> Unit) {
    val reduced = LocalReducedMotion.current || LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val progress = remember { Animatable(1f) }
    var previous by remember { mutableIntStateOf(index) }
    var direction by remember { mutableIntStateOf(1) }
    LaunchedEffect(index, reduced) {
        if (reduced) {
            previous = index
            progress.snapTo(1f)
        } else if (previous != index) {
            val settled = kotlin.math.abs(progress.value - 1f) < .001f
            if (settled) direction = if (index > previous) 1 else -1
            previous = index
            // A second selection keeps the current displacement instead of kicking off again.
            if (settled) progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = ResonanceMotionTokens.SelectionBounceSpec,
            )
        }
    }
    Box(Modifier.fillMaxSize().clipToBounds()) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            val p = progress.value
            val d = direction
            alpha = if (reduced) 1f else (0.15f + 0.85f * p).coerceIn(0.15f, 1f)
            translationX = if (reduced) 0f else d * 32.dp.toPx() * (1f - p)
            translationY = if (reduced) 0f else -4.dp.toPx() * sin((p * PI).toFloat())
        }) { content() }
    }
}
