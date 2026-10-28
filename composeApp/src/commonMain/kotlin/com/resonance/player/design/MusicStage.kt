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

/** A quiet visual atmosphere, independent of the audio signal. All frame reads stay in drawing. */
@Composable
fun MusicStageBackdrop(seed: Int, playing: Boolean, modifier: Modifier = Modifier, atmosphere: AlbumAtmosphere = albumAtmosphere(seed)) {
    val phase = rememberPlaybackVisualPhase(playing, 48_000)
    val lights = remember(seed) {
        val random = Random(seed)
        List(64) { StageLight(random.nextFloat(), random.nextFloat(), .45f + random.nextFloat(), .25f + random.nextFloat() * .75f) }
    }
    val dark = ResonanceColors.isDark
    val canvas = ResonanceColors.Canvas
    val accent by androidx.compose.animation.animateColorAsState(atmosphere.glow,
        tween(motionDuration(ResonanceMotionTokens.TabDuration), easing = ResonanceMotionTokens.ExitEasing), label = "albumLight")
    val cool by androidx.compose.animation.animateColorAsState(atmosphere.edge,
        tween(motionDuration(ResonanceMotionTokens.TabDuration), easing = ResonanceMotionTokens.ExitEasing), label = "albumRim")
    val light = if (dark) Color(0xFFD7E7ED) else Color(0xFF375B69)
    Box(modifier.background(canvas).drawWithCache {
        val origin = Offset(size.width * .32f, size.height * .44f)
        val radius = (size.maxDimension * .70f).coerceAtLeast(1f)
        val halo = Brush.radialGradient(listOf(accent.copy(alpha = if (dark) .23f else .065f), Color.Transparent), origin, radius)
        val second = Brush.radialGradient(listOf(cool.copy(alpha = if (dark) .13f else .04f), Color.Transparent),
            Offset(size.width * .87f, size.height * .26f), radius * .8f)
        val beam = Path().apply {
            moveTo(size.width * .13f, 0f); lineTo(size.width * .37f, 0f)
            lineTo(size.width * .78f, size.height); lineTo(size.width * .35f, size.height); close()
        }
        val beamBrush = Brush.verticalGradient(listOf(light.copy(alpha = .018f), Color.Transparent))
        val floor = Brush.verticalGradient(listOf(Color.Transparent, canvas.copy(alpha = .8f)))
        onDrawBehind {
            drawRect(halo); drawRect(second); drawPath(beam, beamBrush)
            val time = phase.value
            lights.forEach { point ->
                val x = point.x * size.width + sin(time + point.y * 6f) * 9.dp.toPx() * point.depth
                val y = point.y * size.height + cos(time + point.x * 6f) * 6.dp.toPx() * point.depth
                drawCircle(light.copy(alpha = (if (dark) .22f else .13f) * point.depth),
                    point.radius.dp.toPx(), Offset(x, y))
            }
            drawRect(floor)
        }
    })
}

/** The tilt is static: scrolling and reading do not start a perpetual animation. */
fun Modifier.musicSleevePerspective() = graphicsLayer {
    rotationY = -5f
    rotationZ = -1.2f
    cameraDistance = 32f * density
}

@Composable
fun Modifier.musicDock() = galleryPlinth(corner = 24.dp, depth = 14.dp)
