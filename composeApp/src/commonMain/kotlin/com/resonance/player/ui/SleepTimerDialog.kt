package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.resonanceGlass
import com.resonance.player.design.resonancePressable
import com.resonance.player.model.SleepTimerOption

@Composable
fun SleepTimerDialog(
    currentOption: SleepTimerOption,
    remainingSeconds: Int?,
    onDismiss: () -> Unit,
    onSelectOption: (SleepTimerOption) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .resonanceGlass(
                    shape = RoundedCornerShape(22.dp),
                    borderColors = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
                    shadowElevation = 16.dp,
                ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = ResonanceColors.Coral,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "睡眠定时器",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ResonanceColors.Ivory,
                    )
                }

                if (currentOption != SleepTimerOption.Off && remainingSeconds != null && remainingSeconds > 0) {
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "剩余时间：%02d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ResonanceColors.Coral,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(16.dp))

                SleepTimerOption.entries.forEach { option ->
                    val isSelected = option == currentOption
                    val interaction = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(ResonanceColors.CoralSoft)
                                        .border(1.dp, ResonanceColors.GlassBorderGlow.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                } else Modifier
                            )
                            .resonancePressable(interaction, pressedScale = 0.98f)
                            .clickable(interactionSource = interaction, indication = null, role = Role.RadioButton) {
                                onSelectOption(option)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) ResonanceColors.Ivory else ResonanceColors.Dim,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "已选择",
                                tint = ResonanceColors.Coral,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = ResonanceColors.Muted)
                    }
                }
            }
        }
    }
}
