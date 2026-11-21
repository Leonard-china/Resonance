package com.resonance.player.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Borderless luminous focus band with a quiet vertical accent bar.
 * Changes drawing only, so wrapped lyrics never reflow or shift height on focus changes.
 */
@Composable
fun Modifier.lyricFocus(active: Boolean): Modifier {
    val accent = ResonanceColors.Notice
    val dark = ResonanceColors.isDark
    val reduced = LocalReducedMotion.current
    val spec = if (reduced) tween<Color>(0) else spring(dampingRatio = 0.86f, stiffness = 360f)
    val fillStart by animateColorAsState(
        if (active) accent.copy(alpha = if (dark) 0.14f else 0.10f) else Color.Transparent,
        spec, label = "lyricFocusFillStart",
    )
    val fillEnd by animateColorAsState(
        if (active) accent.copy(alpha = if (dark) 0.03f else 0.02f) else Color.Transparent,
        spec, label = "lyricFocusFillEnd",
    )
    val barColor by animateColorAsState(
        if (active) accent.copy(alpha = if (dark) 0.90f else 0.80f) else Color.Transparent,
        spec, label = "lyricFocusBar",
    )
    return drawBehind {
        if (fillStart.alpha > 0.001f || barColor.alpha > 0.001f) {
            val corner = CornerRadius(14.dp.toPx())
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(fillStart, fillEnd)),
                cornerRadius = corner,
            )
            val barWidth = 3.dp.toPx()
            val insetY = (size.height * 0.20f).coerceAtMost(12.dp.toPx())
            val barHeight = (size.height - insetY * 2f).coerceAtLeast(12.dp.toPx())
            drawRoundRect(
                color = barColor,
                topLeft = Offset(3.dp.toPx(), (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
