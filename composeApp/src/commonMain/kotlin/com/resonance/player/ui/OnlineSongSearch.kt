package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resonance.player.model.*

@Composable
internal fun OnlineSongSearch(query: String, modifier: Modifier = Modifier) {
    val feature = LocalMusicFeatures.current ?: return
    val assistant = feature.assistant ?: return
    val state by assistant.state.collectAsState()
    var pending by remember { mutableStateOf<SourceSong?>(null) }
    var playlistId by remember { mutableStateOf<String?>(null) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var alternate by remember(query) { mutableStateOf<SourceSong?>(null) }
    val busy = state.task.running
    val results = if (query.trim() == feature.onlineResultQuery) feature.onlineResults else emptyList()
    val shown = alternate?.let { song -> results.filter { it.title.equals(song.title, true) && it.artist.equals(song.artist, true) && it.sourceId != song.sourceId } } ?: results
    DisposableEffect(Unit) { onDispose { feature.stopPreview() } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { alternate = null; errors = emptyMap(); feature.searchSources(query) }, enabled = query.isNotBlank() && !busy) { Text("搜索在线歌曲") }
            TextButton(onClick = { feature.onNavigate(LibraryDestination.Sources) }) { Text("管理歌源") }
        }
        Text("内置歌源无需配置 AI。试听会先临时下载音频（最多 64 MB），最多播放 30 秒；会暂停当前播放，离开页面后停止。", style = MaterialTheme.typography.bodySmall)
        if (busy) MusicTaskProgress(state.task.progress, assistant::cancel)
        if (feature.onlineError.isNotBlank()) Text(feature.onlineError, color = MaterialTheme.colorScheme.error)
        if (feature.message.isNotBlank()) Text(feature.message, style = MaterialTheme.typography.bodySmall)
        if (alternate != null) Row {
            Text("同歌其他来源", modifier = Modifier.weight(1f))
            TextButton(onClick = { alternate = null }) { Text("显示全部") }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (shown.isEmpty()) item {
                Text(when {
                    busy -> "正在查找候选…"
                    alternate != null -> "本次结果没有同歌的其他来源，可以重新搜索或调整歌名、歌手。"
                    results.isEmpty() && query.trim() == feature.onlineResultQuery && query.isNotBlank() && feature.onlineError.isBlank() -> "没有找到候选，试试完整歌名和歌手。"
                    else -> "输入歌名或歌手，点击搜索。"
                })
            }
            items(shown, key = { it.sourceId + ":" + it.id }) { song ->
                val key = song.sourceId + ":" + song.id
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium)
                        Text(song.artist + " · " + song.album.ifBlank { "专辑未提供" })
                        Text((state.sources.firstOrNull { it.id == song.sourceId }?.name ?: "歌源") + " · " + song.version.ifBlank { "版本未核验" } +
                            if (song.durationSeconds > 0) " · ${song.durationSeconds / 60}:${(song.durationSeconds % 60).toString().padStart(2, '0')}" else "", style = MaterialTheme.typography.bodySmall)
                        errors[key]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { if (feature.previewId == key) feature.stopPreview() else feature.startPreview(song) }, enabled = !busy || feature.previewId == key) { Text(if (feature.previewId == key) "停止试听" else "试听") }
                            TextButton(onClick = { feature.stopPreview(); playlistId = null; pending = song }, enabled = !busy) { Text("核对并下载") }
                            TextButton(onClick = { feature.stopPreview(); alternate = song }, enabled = !busy) { Text("换来源") }
                        }
                    }
                }
            }
        }
    }
    pending?.let { song ->
        AlertDialog(onDismissRequest = { pending = null }, title = { Text("下载歌曲") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(song.title + "\n" + song.artist + "\n" + song.album)
                Text("目标歌单：" + (feature.playlists.firstOrNull { it.id == playlistId }?.name ?: "仅下载到曲库"))
                LazyColumn(Modifier.heightIn(max = 180.dp)) {
                    item { TextButton(onClick = { playlistId = null }) { Text("仅下载到曲库") } }
                    items(feature.playlists.filterNot { it.id == DAILY_PLAYLIST_ID }, key = Playlist::id) { list ->
                        TextButton(onClick = { playlistId = list.id }) { Text(list.name) }
                    }
                }
                Text("成功后长期保存，可在下载与空间释放本地文件。", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(onClick = {
            val target = playlistId; pending = null
            feature.launch {
                val key = song.sourceId + ":" + song.id
                try { assistant.importSourceSong(song, target); feature.onReload(); errors = errors - key; feature.message = "已下载并保存" }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { errors = errors + (key to ("下载未完成：" + error.message + "。可点“换来源”重选。")) }
            }
        }, enabled = !busy) { Text("下载") } }, dismissButton = { TextButton(onClick = { pending = null }) { Text("取消") } })
    }
}
