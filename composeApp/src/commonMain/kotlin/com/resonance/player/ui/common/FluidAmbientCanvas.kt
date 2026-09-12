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
import kotlin.math.cos
import kotlin.math.sin

/**
 * 氛围光色板定义：用于全屏沉浸播放器根据歌曲封面 Seed 衍生专属光晕流体。
 */
val FluidPalettes = listOf(
    // 0: 暖阳珊瑚（温暖活力）
    listOf(Color(0xFFFF6B55), Color(0xFFFFA07A), Color(0xFF6366F1), Color(0xFF38BDF8)),
    // 1: 薄荷碧海（清爽空灵）
    listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF4EE3B0)),
    // 2: 暮色极光（静谧神秘）
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF3B82F6), Color(0xFF6366F1)),
    // 3: 琥珀金芒（复古醇厚）
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFF97316)),
)

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
    val transition = rememberInfiniteTransition(label = "fluidAmbientTransition")

    // 缓慢平滑漂移的相位（不同周期形成永不重复的有机漫游路径）
    val phaseOne by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseOne",
    )

    val phaseTwo by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseTwo",
    )

    // 水波呼吸伸缩（播放时节律更生动）
    val breathePulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 2600 else 4800,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathePulse",
    )

    val paletteIndex = ((seed % FluidPalettes.size) + FluidPalettes.size) % FluidPalettes.size
    val colors = FluidPalettes[paletteIndex]

    val baseAlpha = (if (isDark) 0.22f else 0.12f) * intensity
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
