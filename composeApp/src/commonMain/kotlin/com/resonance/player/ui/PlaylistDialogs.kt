package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.resonanceGlass
import com.resonance.player.design.resonancePressable
import com.resonance.player.model.Track

@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    val saveInteraction = remember { MutableInteractionSource() }
    DialogScrim(onDismiss = onDismiss) {
        Column(Modifier.padding(26.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("重命名歌单", style = MaterialTheme.typography.headlineMedium, color = ResonanceColors.Ivory)
                    Spacer(Modifier.height(3.dp))
                    Text("歌曲不会受到影响", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = ResonanceColors.Muted)
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text("歌单名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ResonanceColors.Coral.copy(alpha = 0.6f),
                    unfocusedBorderColor = ResonanceColors.Divider,
                    focusedContainerColor = ResonanceColors.GlassLight,
                    unfocusedContainerColor = ResonanceColors.GlassLight,
                    cursorColor = ResonanceColors.Coral,
                ),
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消", color = ResonanceColors.Muted) }
                Button(
                    onClick = { onConfirm(name.trim()) },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ResonanceColors.Coral, contentColor = Color(0xFF2B0C08)),
                    modifier = Modifier.resonancePressable(saveInteraction),
                    interactionSource = saveInteraction,
                ) { Text("保存") }
            }
        }
    }
}

@Composable
fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val deleteInteraction = remember { MutableInteractionSource() }
    DialogScrim(onDismiss = onDismiss) {
        Column(Modifier.padding(26.dp)) {
            Text("删除「$playlistName」？", style = MaterialTheme.typography.headlineMedium, color = ResonanceColors.Ivory)
            Spacer(Modifier.height(8.dp))
            Text("只会删除歌单，不会删除本地音乐文件。", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消", color = ResonanceColors.Muted) }
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceColors.Coral,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.resonancePressable(deleteInteraction),
                    interactionSource = deleteInteraction,
                ) { Text("删除歌单") }
            }
        }
    }
}

@Composable
fun DeleteTrackDialog(
    track: Track,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val deleteInteraction = remember { MutableInteractionSource() }
    DialogScrim(onDismiss = onDismiss) {
        Column(Modifier.padding(26.dp)) {
            Text("删除本地 MP3？", style = MaterialTheme.typography.headlineMedium, color = ResonanceColors.Ivory)
            Spacer(Modifier.height(8.dp))
            Text("「${track.title}」将从设备和全部自建歌单中删除。此操作不可撤销。", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消", color = ResonanceColors.Muted) }
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceColors.Coral,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.resonancePressable(deleteInteraction),
                    interactionSource = deleteInteraction,
                ) { Text("删除 MP3") }
            }
        }
    }
}

@Composable
private fun DialogScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .resonanceGlass(
                    shape = RoundedCornerShape(26.dp),
                    backgroundColor = ResonanceColors.Raised,
                    shadowElevation = 24.dp,
                )
                .clickable(enabled = false) {},
        ) {
            content()
        }
    }
}

