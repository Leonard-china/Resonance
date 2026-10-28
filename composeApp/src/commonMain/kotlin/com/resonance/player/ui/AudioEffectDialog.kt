package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.resonanceGlass
import com.resonance.player.model.AudioEffectPreset

@Composable
fun AudioEffectDialog(
    currentPreset: AudioEffectPreset,
    onPresetSelect: (AudioEffectPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .resonanceGlass(shape = RoundedCornerShape(22.dp))
                .border(1.dp, ResonanceColors.Divider.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ResonanceColors.Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = ResonanceColors.Primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "音效调校预设",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ResonanceColors.TextPrimary,
                        )
                        Text(
                            text = "双端原生 16-bit PCM 硬件加速实时调校",
                            style = MaterialTheme.typography.bodySmall,
                            color = ResonanceColors.Muted,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(AudioEffectPreset.entries) { preset ->
                        val isSelected = preset == currentPreset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) ResonanceColors.Primary.copy(alpha = 0.12f)
                                    else ResonanceColors.CanvasElevated.copy(alpha = 0.5f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) ResonanceColors.Primary.copy(alpha = 0.5f)
                                    else ResonanceColors.Divider.copy(alpha = 0.3f),
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(),
                                    onClick = {
                                        onPresetSelect(preset)
                                        onDismiss()
                                    },
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) ResonanceColors.Primary else ResonanceColors.TextPrimary,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) ResonanceColors.TextPrimary.copy(alpha = 0.8f) else ResonanceColors.Muted,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ResonanceColors.Primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("完成", color = ResonanceColors.Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
