package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.resonance.player.model.*

@Composable
internal fun ScreenshotImportDialog(draft: ScreenshotImportDraft) {
    val feature = LocalMusicFeatures.current ?: return
    val assistant = feature.assistant ?: return
    val state by assistant.state.collectAsState()
    var lists by remember(draft.id) { mutableStateOf(draft.playlists) }
    var selected by remember(draft.id) { mutableStateOf(draft.playlists.flatMapIndexed { i, list -> list.tracks.indices.map { i to it } }.toSet()) }
    val chosen = lists.mapIndexedNotNull { i, list -> list.copy(tracks = list.tracks.filterIndexed { n, _ -> i to n in selected }).takeIf { it.tracks.isNotEmpty() } }
    val valid = chosen.isNotEmpty() && chosen.all { it.name.isNotBlank() && it.tracks.all { song -> song.title.isNotBlank() && song.artist.isNotBlank() } }
    fun dismiss() { if (!state.task.running) feature.launch { assistant.discardScreenshotImport() } }
    Dialog(onDismissRequest = ::dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.94f).widthIn(max = 760.dp).fillMaxHeight(0.92f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("核对截图歌单", style = MaterialTheme.typography.headlineSmall)
                Text("已选择 ${selected.size} 首 · ${draft.recognition}。可编辑歌名、歌手和专辑；确认后再匹配本地或下载。", style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = { selected = lists.flatMapIndexed { i, list -> list.tracks.indices.map { i to it } }.toSet() }) { Text("全选") }
                    TextButton(onClick = { selected = emptySet() }) { Text("全不选") }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    lists.forEachIndexed { listIndex, list ->
                        item(key = "header-$listIndex") {
                            OutlinedTextField(list.name, { name -> lists = lists.mapIndexed { n, old -> if (n == listIndex) old.copy(name = name.take(80)) else old } }, label = { Text("歌单名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        itemsIndexed(list.tracks, key = { index, _ -> "$listIndex-$index" }) { songIndex, song ->
                            val key = listIndex to songIndex
                            fun change(value: SongRequest) { lists = lists.mapIndexed { n, old -> if (n == listIndex) old.copy(tracks = old.tracks.mapIndexed { m, entry -> if (m == songIndex) value else entry }) else old } }
                            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row {
                                        Checkbox(key in selected, { checked -> selected = if (checked) selected + key else selected - key })
                                        Column(Modifier.weight(1f)) {
                                            val truncated = listOf(song.title, song.artist).any { it.endsWith("…") || it.endsWith("...") }
                                            val exists = feature.library.any { it.sourceUri != null && it.title.trim().equals(song.title.trim(), true) && it.artist.trim().equals(song.artist.trim(), true) }
                                            Text(when { truncated -> "名称待核对 · 请补充完整信息"; exists -> "本地已有 · 优先复用"; else -> "需要查找音源并下载" }, style = MaterialTheme.typography.labelMedium)
                                            OutlinedTextField(song.title, { change(song.copy(title = it.take(180))) }, label = { Text("歌名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                    OutlinedTextField(song.artist, { change(song.copy(artist = it.take(180))) }, label = { Text("歌手") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(song.album, { change(song.copy(album = it.take(180))) }, label = { Text("专辑（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = ::dismiss, enabled = !state.task.running) { Text("取消导入") }
                    Button(onClick = { feature.launch { assistant.confirmScreenshotImport(draft.id, chosen); feature.onReload() } }, enabled = valid && !state.task.running) { Text("确认导入 ${selected.size} 首") }
                }
            }
        }
    }
}
