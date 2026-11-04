package com.resonance.player.design

import androidx.compose.animation.core.animateDpAsState
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
    BoxWithConstraints(modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp))
        .background(ResonanceColors.Soft.copy(alpha = .62f)).padding(4.dp).selectableGroup()) {
        val cell = maxWidth / labels.size
        val position = animateDpAsState(cell * selectedIndex.coerceIn(labels.indices),
            tween(if (LocalInputModeManager.current.inputMode == InputMode.Keyboard) 0 else motionDuration(180),
                easing = ResonanceMotionTokens.ExitEasing), label = "segmentSelection")
        Box(Modifier.offset { IntOffset(position.value.roundToPx(), 0) }.width(cell).fillMaxHeight()
            .shadow(2.dp, RoundedCornerShape(12.dp)).background(ResonanceColors.GlassUltra, RoundedCornerShape(12.dp))
            .border(1.dp, ResonanceColors.GlassBorder, RoundedCornerShape(12.dp)))
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { index, label ->
                Box(Modifier.weight(1f).fillMaxHeight().then(tabModifier(index)).selectable(index == selectedIndex, role = Role.Tab,
                    onClick = { onSelect(index) }), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (index == selectedIndex) ResonanceColors.Primary else ResonanceColors.Muted)
                }
            }
        }
    }
}
