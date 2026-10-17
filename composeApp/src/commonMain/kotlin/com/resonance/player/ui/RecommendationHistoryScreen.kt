package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resonance.player.model.*

@Composable
internal fun RecommendationHistoryScreen(compact: Boolean, onBack: () -> Unit) {
    val feature = LocalMusicFeatures.current ?: return
    val assistant = feature.assistant ?: return
    val state by assistant.state.collectAsState()
    var filter by remember { mutableStateOf("全部") }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Track?>(null) }
    var comment by remember { mutableStateOf("") }
    val songs = (state.recommendationHistory + state.recommendationFeedback.values.map {
        RecommendationHistoryItem(it.title, it.artist, it.album, it.updatedAt)
    }).sortedByDescending { it.recommendedAt }.distinctBy { it.artist.trim().lowercase() + ":" + it.title.trim().lowercase() }
    val shown = songs.filter { song ->
        val track = Track("history", song.title, song.artist, song.album, "0:00", 0)
        val feedback = assistant.feedbackFor(track)
        (query.isBlank() || (song.title + " " + song.artist + " " + feedback?.comment.orEmpty()).contains(query, true)) && when (filter) {
            "点赞" -> feedback?.vote == RecommendationVote.Like
            "踩" -> feedback?.vote == RecommendationVote.Dislike
            "有评论" -> !feedback?.comment.isNullOrBlank()
            else -> true
        }
    }
    Column(Modifier.fillMaxSize().padding(if (compact) 16.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Text("推荐历史与我的反馈", style = MaterialTheme.typography.titleLarge)
        }
        Text("反馈保存在本机，不随临时音频清理。可以撤销态度、编辑评论或重新查找歌曲。", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(query, { query = it.take(180) }, label = { Text("搜索歌名、歌手或评论") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("全部", "点赞", "踩", "有评论").forEach { label -> FilterChip(filter == label, { filter = label }, label = { Text(label) }) }
        }
        if (feature.message.isNotBlank()) Text(feature.message, color = MaterialTheme.colorScheme.primary)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (shown.isEmpty()) item { Text("暂无符合条件的记录。新推荐和你的反馈会出现在这里。") }
            items(shown, key = { it.artist + ":" + it.title }) { song ->
                val track = feature.library.firstOrNull { it.title.equals(song.title, true) && it.artist.equals(song.artist, true) }
                    ?: Track("history", song.title, song.artist, song.album, "0:00", 0)
                val feedback = assistant.feedbackFor(track)
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium)
                        Text(song.artist + " · " + song.album.ifBlank { "专辑未提供" })
                        if (song.recommendedAt > 0) Text("最近记录：" + feature.services.formatDateTime(song.recommendedAt), style = MaterialTheme.typography.labelSmall)
                        if (!feedback?.comment.isNullOrBlank()) Text("你的评论：" + feedback.comment, style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { feature.launch { assistant.setRecommendationVote(track, if (feedback?.vote == RecommendationVote.Like) RecommendationVote.Neutral else RecommendationVote.Like) } }, enabled = !state.task.running) { Text(if (feedback?.vote == RecommendationVote.Like) "已点赞 · 撤销" else "点赞") }
                            TextButton(onClick = { feature.launch { assistant.setRecommendationVote(track, if (feedback?.vote == RecommendationVote.Dislike) RecommendationVote.Neutral else RecommendationVote.Dislike) } }, enabled = !state.task.running) { Text(if (feedback?.vote == RecommendationVote.Dislike) "已踩 · 撤销" else "踩") }
                            TextButton(onClick = { editing = track; comment = feedback?.comment.orEmpty() }, enabled = !state.task.running) { Text("编辑评论") }
                            TextButton(onClick = { feature.searchQuery = song.title + " " + song.artist; feature.searchOnline = true; feature.onNavigate(LibraryDestination.Discover) }) { Text("重新查找") }
                            if (track.id != "history") TextButton(onClick = { feature.onPlay(track) }) { Text("播放／恢复下载") }
                        }
                    }
                }
            }
        }
    }
    editing?.let { track ->
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("评价「${track.title}」") }, text = {
            OutlinedTextField(comment, { comment = it.take(500) }, label = { Text("评论，留空可删除") }, supportingText = { Text("${comment.length}/500") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        }, confirmButton = { TextButton(onClick = { val value = comment; editing = null; feature.launch { assistant.saveRecommendationComment(track, value) } }, enabled = !state.task.running) { Text("保存") } }, dismissButton = { TextButton(onClick = { editing = null }) { Text("取消") } })
    }
}
