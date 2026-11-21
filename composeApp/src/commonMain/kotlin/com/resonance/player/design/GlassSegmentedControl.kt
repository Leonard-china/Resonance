package com.resonance.player.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** A single moving selection surface; position is read in layout, never recomposes the labels per frame. */
@Composable
fun GlassSegmentedControl(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier,
    tabModifier: @Composable (Int) -> Modifier = { Modifier }) {
    if (labels.isEmpty()) return
    val outerShape = RoundedCornerShape(14.dp)
    val innerShape = RoundedCornerShape(10.dp)
    val shadowTint = ResonanceColors.Shadow.copy(alpha = if (ResonanceColors.isDark) 0.32f else 0.08f)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(outerShape)
            .background(ResonanceColors.Soft)
            .border(1.dp, ResonanceColors.Divider.copy(alpha = if (ResonanceColors.isDark) 0.65f else 0.85f), outerShape)
            .padding(4.dp)
            .selectableGroup()
    ) {
        val cell = maxWidth / labels.size
        val instant = LocalInputModeManager.current.inputMode == InputMode.Keyboard || LocalReducedMotion.current
        val position = animateDpAsState(
            cell * selectedIndex.coerceIn(labels.indices),
            if (instant) tween(0) else spring(dampingRatio = 0.82f, stiffness = 420f),
            label = "segmentSelection",
        )
        Box(
            Modifier
                .offset { IntOffset(position.value.roundToPx(), 0) }
                .width(cell)
                .fillMaxHeight()
                .shadow(if (ResonanceColors.isDark) 2.dp else 1.5.dp, innerShape, spotColor = shadowTint, ambientColor = shadowTint)
                .background(ResonanceColors.Raised, innerShape)
                .border(1.dp, if (ResonanceColors.isDark) ResonanceColors.GlassBorderSubtle else ResonanceColors.Divider.copy(alpha = 0.55f), innerShape)
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(tabModifier(index))
                        .selectable(selected, role = Role.Tab, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) ResonanceColors.Primary else ResonanceColors.Muted,
                    )
                }
            }
        }
    }
}
