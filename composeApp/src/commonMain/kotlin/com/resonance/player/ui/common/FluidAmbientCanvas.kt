package com.resonance.player.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import com.resonance.player.design.*
import kotlin.math.cos
import kotlin.math.sin

/** Cache the light fields; only their positions change on a playing frame. */
@Composable
fun FluidAmbientCanvas(modifier: Modifier = Modifier, seed: Int = 0, isPlaying: Boolean = false, intensity: Float = 1f) {
    val moving = isPlaying && LocalAppForeground.current && !LocalReducedMotion.current
    val one: State<Float>
    val two: State<Float>
    if (moving) {
        val transition = rememberInfiniteTransition(label = "ambientMotion")
        one = transition.animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "ambientOne")
        two = transition.animateFloat(0f, 360f, infiniteRepeatable(tween(32000, easing = LinearEasing)), label = "ambientTwo")
    } else {
        one = rememberUpdatedState(0f)
        two = rememberUpdatedState(60f)
    }
    val alpha = (if (ResonanceColors.isDark) 0.14f else 0.09f) * intensity
    val primary = ResonanceColors.Primary.copy(alpha = alpha * 1.1f)
    val secondary = ResonanceColors.Positive.copy(alpha = alpha * 0.85f)
    val accent = ResonanceColors.Info.copy(alpha = alpha * 0.75f)
    val seedPhase = (seed % 360) * (kotlin.math.PI / 180).toFloat()
    Box(modifier.fillMaxSize().drawWithCache {
        val w = size.width
        val h = size.height
        val r1 = (minOf(w, h) * 0.60f).coerceAtLeast(10f)
        val r2 = (minOf(w, h) * 0.52f).coerceAtLeast(10f)
        val r3 = (minOf(w, h) * 0.44f).coerceAtLeast(10f)
        fun light(color: Color, radius: Float) = Brush.radialGradient(
            listOf(color, color.copy(alpha = color.alpha * 0.4f), Color.Transparent), Offset.Zero, radius)
        val brush1 = light(primary, r1)
        val brush2 = light(secondary, r2)
        val brush3 = light(accent, r3)
        onDrawBehind {
            val a = one.value * (kotlin.math.PI / 180).toFloat() + seedPhase
            val b = two.value * (kotlin.math.PI / 180).toFloat() + seedPhase
            translate(w * (0.68f + 0.16f * cos(a)), h * (0.24f + 0.14f * sin(a))) {
                drawCircle(brush1, r1, Offset.Zero)
            }
            translate(w * (0.28f + 0.18f * cos(b + 1.8f)), h * (0.72f + 0.16f * sin(b + 1.2f))) {
                drawCircle(brush2, r2, Offset.Zero)
            }
            translate(w * (0.42f + 0.15f * sin(a * 0.7f)), h * (0.48f + 0.18f * cos(b * 0.8f))) {
                drawCircle(brush3, r3, Offset.Zero)
            }
        }
    })
}
