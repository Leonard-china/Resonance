package com.resonance.player.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class StageLight(val x: Float, val y: Float, val radius: Float, val depth: Float)

@Composable
fun rememberPlaybackVisualPhase(playing: Boolean, periodMillis: Int): State<Float> {
    val moving = playing && LocalAppForeground.current && !LocalReducedMotion.current
    val phase = remember { Animatable(0f) }
    LaunchedEffect(moving, periodMillis) {
        if (moving) while (isActive) {
            phase.animateTo(phase.value + (2 * PI).toFloat(), tween(periodMillis, easing = LinearEasing))
            phase.snapTo(phase.value % (2 * PI).toFloat())
        }
    }
    return phase.asState()
}

/** Static album-derived fallback for surfaces that have no artwork source. */
@Composable
fun MusicStageBackdrop(seed: Int, playing: Boolean, modifier: Modifier = Modifier, atmosphere: AlbumAtmosphere = albumAtmosphere(seed)) {
    val canvas = ResonanceColors.Canvas
    Box(modifier.background(canvas).background(Brush.verticalGradient(listOf(
        atmosphere.edge.copy(alpha = .08f), atmosphere.glow.copy(alpha = .04f), Color.Transparent))))
}

/** Covers remain front-facing; depth comes from the shadow, not a simulated slab. */
fun Modifier.musicSleevePerspective(): Modifier = this

@Composable
fun Modifier.musicDock() = resonanceGlass(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), shadowElevation = 8.dp)
