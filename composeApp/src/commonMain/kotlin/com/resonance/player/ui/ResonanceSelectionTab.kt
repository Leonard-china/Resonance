package com.resonance.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonance.player.design.*

/** One selection treatment for the library and the player, including keyboard focus. */
@Composable
internal fun ResonanceSelectionTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contained: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val background by animateColorAsState(
        if (focused || hovered) ResonanceColors.Soft else Color.Transparent,
        tween(motionDuration(ResonanceMotionTokens.StateDuration)), label = "selectionBackground",
    )
    val foreground = if (selected) { if (contained) ResonanceColors.TextPrimary else ResonanceColors.Primary } else ResonanceColors.Muted
    val indicator = ResonanceColors.Primary
    val shape = RoundedCornerShape(if (contained) 16.dp else 6.dp)
    Row(
        modifier.clip(shape).background(background)
            .border(1.5.dp, if (focused) ResonanceColors.Primary else Color.Transparent, shape)
            .drawWithContent {
                drawContent()
                if (selected && !contained) {
                    val lineWidth = minOf((size.width - 32.dp.toPx()).coerceAtLeast(0f), 48.dp.toPx())
                    drawLine(indicator, Offset((size.width - lineWidth) / 2, size.height - 3.dp.toPx()),
                        Offset((size.width + lineWidth) / 2, size.height - 3.dp.toPx()),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            .selectable(selected, onClick = onClick, role = Role.Tab, interactionSource = interaction, indication = ripple())
            .heightIn(min = 48.dp).padding(horizontal = if (contained) 8.dp else 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = foreground)
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = foreground,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}
