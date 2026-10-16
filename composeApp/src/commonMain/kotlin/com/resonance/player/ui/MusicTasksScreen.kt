package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.*

@Composable
internal fun MusicTasksScreen(compact: Boolean, onBack: () -> Unit) {
    val feature = LocalMusicFeatures.current ?: return
    val assistant = feature.assistant ?: return
    val state by assistant.state.collectAsState()
    var paidRetry by remember { mutableStateOf<MusicTaskEntry?>(null) }
    paidRetry?.let { task ->
        AlertDialog(onDismissRequest = { paidRetry = null }, title = { Text("重试同步歌词生成") }, text = {
            Text("将重新上传音频到百炼语音识别，可能再次按音频时长收费。取消或失败的上次请求也可能已计费，因此用量按整曲时长保守记录；本月已预留/使用 ${state.speechSeconds} 秒，上限 ${feature.settings.monthlySpeechSeconds} 秒。")
        }, confirmButton = { TextButton(onClick = { paidRetry = null; feature.launch { assistant.retryTask(task.id); feature.reloadGeneratedLyrics() } }) { Text("开始重试") } }, dismissButton = { TextButton(onClick = { paidRetry = null }) { Text("取消") } })
    }
    Column(Modifier.fillMaxSize().padding(if (compact) 16.dp else 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Text("任务列表", style = MaterialTheme.typography.headlineMedium)
        }
        TextButton(onClick = { feature.launch { assistant.matchVideoCovers(); feature.onReload() } }, enabled = !state.task.running) { Text("匹配已下载歌曲的专辑封面") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.tasks.isEmpty()) item { Text("下载、封面匹配和歌词处理的进度会显示在这里。", color = ResonanceColors.Muted) }
            items(state.tasks.asReversed(), key = { it.id }) { task ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        if (task.outcome == MusicTaskOutcome.Running) {
                            MusicTaskProgress(task.progress, assistant::cancel)
                        } else {
                            Text(when (task.outcome) {
                                MusicTaskOutcome.Completed -> "已完成"
                                MusicTaskOutcome.Cancelled -> "已取消"
                                MusicTaskOutcome.Interrupted -> "上次处理被中断"
                                else -> "未完成"
                            }, style = MaterialTheme.typography.labelMedium)
                            if (task.message.isNotBlank()) Text(task.message, style = MaterialTheme.typography.bodySmall)
                            if (task.retryable && task.outcome != MusicTaskOutcome.Completed) {
                                TextButton(onClick = {
                                    if (task.kind == "syncLyrics") paidRetry = task
                                    else feature.launch { assistant.retryTask(task.id); feature.onReload(); feature.reloadGeneratedLyrics() }
                                }, enabled = !state.task.running) { Text(if (task.kind == "syncLyrics") "重新生成（可能计费）" else "重试此步骤") }
                            }
                        }
                    }
                }
            }
            if (feature.message.isNotBlank()) item { Text(feature.message, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
internal fun RecommendationsScreen(compact: Boolean, onBack: () -> Unit) {
    val feature = LocalMusicFeatures.current ?: return
    val assistant = feature.assistant ?: return
    val state by assistant.state.collectAsState()
    val tracks = state.recommendations.trackIds.mapNotNull { id -> feature.library.firstOrNull { it.id == id } }
    Column(Modifier.fillMaxSize().padding(if (compact) 16.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Text("每日推荐", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { feature.onNavigate(LibraryDestination.Settings) }) { Text("设置") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("收藏或加入自建歌单可保留，其他歌曲下次更新时清理。", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
            TextButton(onClick = { feature.launch { assistant.recommend(true); feature.onReload() } }, enabled = feature.settings.recommendationsEnabled && !state.task.running) { Text("更新推荐") }
        }
        if (!feature.settings.recommendationsEnabled) TextButton(onClick = { feature.onNavigate(LibraryDestination.Settings) }) { Text("未开启推荐 · 前往设置") }
        if (state.task.running) MusicTaskProgress(state.task.progress, assistant::cancel)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tracks.isEmpty()) item { Text("开启后按计划下载推荐，也可以点击“更新推荐”。", color = ResonanceColors.Muted) }
            items(tracks, key = Track::id) { track ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AlbumArtwork(track.artworkSeed, Modifier.size(48.dp), artworkPath = track.artworkPath)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(track.title); Text(track.artist, style = MaterialTheme.typography.bodySmall) }
                            IconButton(onClick = { feature.onPlay(track) }) { Icon(Icons.Default.PlayArrow, "播放推荐") }
                            IconButton(onClick = { feature.favorite(track) }, enabled = !state.task.running) { Icon(if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "喜欢并收藏") }
                            IconButton(onClick = { feature.dislike(track) }, enabled = !state.task.running) { Icon(Icons.Default.ThumbDownOffAlt, "不喜欢并删除") }
                        }
                        state.recommendations.reasons[track.id]?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted) }
                    }
                }
            }
            if (feature.message.isNotBlank()) item { Text(feature.message, color = MaterialTheme.colorScheme.primary) }
        }
    }
}
