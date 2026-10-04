package com.resonance.player.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.LocalAppForeground
import kotlin.math.cos
import kotlin.math.sin

/**
 * 氛围光色板定义：用于全屏沉浸播放器根据歌曲封面 Seed 衍生专属光晕流体。
 */
/**
 * 流体光晕画布（Fluid Ambient Canvas）
 *
 * 在暗色或浅色底板下，呈现 3 团拥有自然水波张力的平滑漂移光晕。
 * 当音乐正在播放时（isPlaying = true），光球会伴随舒缓的正弦呼吸周期微微伸缩，
 * 为上层的磨砂玻璃面板提供通透、波光粼粼的声学光学底衬。
 */
@Composable
fun FluidAmbientCanvas(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    isPlaying: Boolean = false,
    intensity: Float = 1f,
) {
    val isDark = ResonanceColors.isDark
    val moving = isPlaying && LocalAppForeground.current && !LocalReducedMotion.current
    val phases = if (moving) {
        val transition = rememberInfiniteTransition(label = "ambientMotion")
        val one by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "ambientOne")
        val two by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(32000, easing = LinearEasing)), label = "ambientTwo")
        Triple(one, two, 1f)
    } else Triple(0f, 60f, 1f)
    val phaseOne = phases.first
    val phaseTwo = phases.second
    val breathePulse = phases.third
    val colors = listOf(ResonanceColors.Primary, ResonanceColors.Positive, ResonanceColors.Info)

    val baseAlpha = (if (isDark) 0.14f else 0.09f) * intensity
    val c1 = colors[0].copy(alpha = baseAlpha * 1.1f)
    val c2 = colors[1].copy(alpha = baseAlpha * 0.85f)
    val c3 = colors[2].copy(alpha = baseAlpha * 0.75f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val rad1 = (phaseOne * kotlin.math.PI / 180.0).toFloat()
        val rad2 = (phaseTwo * kotlin.math.PI / 180.0).toFloat()

        // 光斑 1（暖调/主导，右上方缓缓回旋）
        val x1 = w * (0.68f + 0.16f * cos(rad1))
        val y1 = h * (0.24f + 0.14f * sin(rad1))
        val r1 = (minOf(w, h) * 0.60f * breathePulse).coerceAtLeast(10f)

        // 光斑 2（冷调/对冲，左下方柔缓游动）
        val x2 = w * (0.28f + 0.18f * cos(rad2 + 1.8f))
        val y2 = h * (0.72f + 0.16f * sin(rad2 + 1.2f))
        val r2 = (minOf(w, h) * 0.52f * (2.0f - breathePulse)).coerceAtLeast(10f)

        // 光斑 3（次调/氛围，中间偏左上漂移）
        val x3 = w * (0.42f + 0.15f * sin(rad1 * 0.7f))
        val y3 = h * (0.48f + 0.18f * cos(rad2 * 0.8f))
        val r3 = (minOf(w, h) * 0.44f * breathePulse).coerceAtLeast(10f)

        // 绘制柔和漫射径向渐变
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c1, c1.copy(alpha = c1.alpha * 0.4f), Color.Transparent),
                center = Offset(x1, y1),
                radius = r1,
            ),
            center = Offset(x1, y1),
            radius = r1,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c2, c2.copy(alpha = c2.alpha * 0.4f), Color.Transparent),
                center = Offset(x2, y2),
                radius = r2,
            ),
            center = Offset(x2, y2),
            radius = r2,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c3, c3.copy(alpha = c3.alpha * 0.35f), Color.Transparent),
                center = Offset(x3, y3),
                radius = r3,
            ),
            center = Offset(x3, y3),
            radius = r3,
        )
    }
}
