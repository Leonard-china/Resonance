package com.resonance.player.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A focus band changes drawing only: wrapped lyrics keep their height while scrolling. */
@Composable
fun Modifier.lyricFocus(active: Boolean): Modifier {
    val accent = ResonanceColors.Notice
    val fill by animateColorAsState(
        if (active) accent.copy(alpha = if (ResonanceColors.isDark) .14f else .10f) else Color.Transparent,
        tween(motionDuration(160)), label = "lyricFocusFill",
    )
    val edge by animateColorAsState(
        if (active) accent.copy(alpha = .24f) else Color.Transparent,
        tween(motionDuration(160)), label = "lyricFocusEdge",
    )
    val shape = RoundedCornerShape(14.dp)
    return background(fill, shape).border(1.dp, edge, shape)
}
