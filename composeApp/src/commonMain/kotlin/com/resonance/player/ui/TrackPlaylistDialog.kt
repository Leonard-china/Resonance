package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.design.FrostedAlertDialog
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.Playlist
import com.resonance.player.model.Track

@Composable
internal fun TrackPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onMembershipChange: (Track, String, Boolean) -> Unit,
    onCreatePlaylist: ((String) -> Unit)? = null,
) {
    val initialIds = remember(track.id) {
        playlists.filter { playlist -> playlist.tracks.any { it.id == track.id } }.map { it.id }.toSet()
    }
    var selectedIds by remember(track.id) { mutableStateOf(initialIds) }
    var query by remember(track.id) { mutableStateOf("") }
    var creating by remember(track.id) { mutableStateOf(false) }
    var newName by remember(track.id) { mutableStateOf("") }
    val filtered = remember(playlists, query) { playlists.filter { it.name.contains(query.trim(), ignoreCase = true) } }

    FrostedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入歌单") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                item(key = "description") {
                    Text(track.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("勾选加入，取消勾选移出，完成后保存。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                    Spacer(Modifier.height(12.dp))
                    if (playlists.size >= 8) {
                        OutlinedTextField(value = query, onValueChange = { query = it.take(100) },
                            label = { Text("搜索歌单") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                    }
                    if (onCreatePlaylist != null) {
                        if (creating) {
                            OutlinedTextField(value = newName, onValueChange = { newName = it.take(60) },
                                label = { Text("歌单名称") }, supportingText = { Text("完成后新建歌单并加入这首歌") },
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                            TextButton(onClick = { creating = false; newName = "" }) { Text("取消新建") }
                        } else TextButton(onClick = { creating = true }) { Text("新建歌单") }
                    }
                }
                if (filtered.isEmpty()) item(key = "empty") {
                    Text(if (playlists.isEmpty()) { if (onCreatePlaylist != null) "还没有歌单，可直接新建。" else "还没有歌单，可先在音乐库新建一个。" } else "没有匹配的歌单", color = ResonanceColors.Muted)
                }

                items(filtered, key = { "playlist:${it.id}" }) { playlist ->
                    val included = playlist.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .toggleable(value = included, role = Role.Checkbox, onValueChange = { checked ->
                                selectedIds = if (checked) selectedIds + playlist.id else selectedIds - playlist.id
                            })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = included, onCheckedChange = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${playlist.tracks.size} 首", style = MaterialTheme.typography.bodySmall,
                                color = ResonanceColors.Muted)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !creating || newName.isNotBlank(), onClick = {
                onDismiss()
                playlists.forEach { playlist ->
                    val included = playlist.id in selectedIds
                    val wasIncluded = playlist.tracks.any { it.id == track.id }
                    if (included != (playlist.id in initialIds) && included != wasIncluded) {
                        onMembershipChange(track, playlist.id, included)
                    }
                }
                if (creating) onCreatePlaylist?.invoke(newName.trim())
            }) { Text(if (playlists.isEmpty() && onCreatePlaylist == null) "关闭" else "完成") }
        },
        dismissButton = if (playlists.isEmpty() && onCreatePlaylist == null) null else {
            { TextButton(onClick = onDismiss) { Text("取消") } }
        },
    )
}
