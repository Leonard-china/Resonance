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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.resonance.player.design.ResonanceShapes
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
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("重命名歌单", style = MaterialTheme.typography.titleLarge, color = ResonanceColors.Ivory)
                    Spacer(Modifier.height(2.dp))
                    Text("歌曲不会受到影响", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = ResonanceColors.Muted)
                }
            }
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text("歌单名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ResonanceColors.Coral,
                    unfocusedBorderColor = ResonanceColors.Divider,
                    focusedContainerColor = ResonanceColors.Canvas,
                    unfocusedContainerColor = ResonanceColors.Canvas,
                    cursorColor = ResonanceColors.Coral,
                ),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消", color = ResonanceColors.Muted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(name.trim()) },
                    enabled = name.isNotBlank(),
                    shape = ResonanceShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceColors.Coral,
                        contentColor = Color.White,
                    ),
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
        Column(Modifier.padding(22.dp)) {
            Text("删除「$playlistName」？", style = MaterialTheme.typography.titleLarge, color = ResonanceColors.Ivory)
            Spacer(Modifier.height(6.dp))
            Text("只会删除歌单，不会删除本地音乐文件。", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消", color = ResonanceColors.Muted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    shape = ResonanceShapes.Button,
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
        Column(Modifier.padding(22.dp)) {
            Text("删除本地音频文件？", style = MaterialTheme.typography.titleLarge, color = ResonanceColors.Ivory)
            Spacer(Modifier.height(6.dp))
            Text("「${track.title}」的音频文件将直接从手机/本地设备存储中永久删除，并从音乐库和全部歌单中移除。此操作不可撤销。", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消", color = ResonanceColors.Muted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    shape = ResonanceShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceColors.Coral,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.resonancePressable(deleteInteraction),
                    interactionSource = deleteInteraction,
                ) { Text("确认删除") }
            }
        }
    }
}

@Composable
fun BatchDeleteTracksDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val deleteInteraction = remember { MutableInteractionSource() }
    DialogScrim(onDismiss = onDismiss) {
        Column(Modifier.padding(22.dp)) {
            Text("批量删除 $count 首歌曲？", style = MaterialTheme.typography.titleLarge, color = ResonanceColors.Ivory)
            Spacer(Modifier.height(6.dp))
            Text("选中的 $count 首歌曲的本地音频文件将直接从存储中永久删除，并从音乐库和全部歌单中移除。此操作不可撤销。", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消", color = ResonanceColors.Muted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    shape = ResonanceShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceColors.Coral,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.resonancePressable(deleteInteraction),
                    interactionSource = deleteInteraction,
                ) { Text("确认批量删除") }
            }
        }
    }
}

@Composable
fun BatchAddToPlaylistDialog(
    playlists: List<com.resonance.player.model.Playlist>,
    trackCount: Int,
    onDismiss: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreateNewPlaylist: () -> Unit,
) {
    DialogScrim(onDismiss = onDismiss) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("添加到歌单", style = MaterialTheme.typography.titleLarge, color = ResonanceColors.Ivory)
                    Spacer(Modifier.height(2.dp))
                    Text("将选中的 $trackCount 首歌曲加入歌单", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = ResonanceColors.Muted)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (playlists.isEmpty()) {
                Text("暂无自建歌单", style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Dim)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { onDismiss(); onCreateNewPlaylist() }, shape = ResonanceShapes.Button) {
                    Text("新建歌单")
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(playlists.size) { index ->
                        val playlist = playlists[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPlaylist(playlist.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = ResonanceColors.Coral, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.titleSmall, color = ResonanceColors.Ivory)
                                Text("${playlist.tracks.size} 首歌曲", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
                            }
                        }
                        HorizontalDivider(color = ResonanceColors.Divider, thickness = androidx.compose.ui.unit.Dp.Hairline)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("取消", color = ResonanceColors.Muted) }
            }
        }
    }
}

@Composable
private fun DialogScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    com.resonance.player.platform.ResonanceBackHandler(enabled = true, onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = ResonanceShapes.Panel,
            color = ResonanceColors.Raised,
            tonalElevation = 0.dp,
        ) {
            content()
        }
    }
}


