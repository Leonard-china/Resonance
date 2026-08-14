package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.resonance.player.model.Track

@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    DialogScrim(onDismiss = onDismiss) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("重命名歌单", style = MaterialTheme.typography.headlineMedium)
                    Text("歌曲不会受到影响", color = ResonanceColors.Muted)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text("歌单名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(
                    onClick = { onConfirm(name.trim()) },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
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
    DialogScrim(onDismiss = onDismiss) {
        Column(Modifier.padding(24.dp)) {
            Text("删除「$playlistName」？", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("只会删除歌单，不会删除本地音乐文件。", color = ResonanceColors.Muted)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceColors.Coral,
                        contentColor = Color.White,
                    ),
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
    DialogScrim(onDismiss = onDismiss) {
        Column(Modifier.padding(24.dp)) {
            Text("删除本地 MP3？", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("「${track.title}」将从设备和全部自建歌单中删除。此操作不可撤销。", color = ResonanceColors.Muted)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceColors.Coral,
                        contentColor = Color.White,
                    ),
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
        Surface(
            color = ResonanceColors.Raised,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 18.dp,
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            content = content,
        )
    }
}
