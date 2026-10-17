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

private fun MusicTaskOutcome.toUserLabel(): String = when (this) {
    MusicTaskOutcome.Completed -> "已完成"
    MusicTaskOutcome.Running -> "处理中"
    MusicTaskOutcome.Cancelled -> "已取消"
    MusicTaskOutcome.Interrupted -> "被中断"
    MusicTaskOutcome.Failed -> "未完成"
}

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
        TextButton(onClick = { feature.launch { assistant.matchMissingCovers(); feature.onReload() } }, enabled = !state.task.running) { Text("补齐曲库缺失封面 · 不调用模型") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.tasks.isEmpty()) item { Text("下载、封面匹配和歌词处理的进度会显示在这里。", color = ResonanceColors.Muted) }
            items(groupMusicTasks(state.tasks), key = { it.root.id }) { group ->
                val task = group.root
                var expanded by remember(task.id) { mutableStateOf(false) }
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        if (task.progress.total > 0) Text("已处理 ${task.progress.completed} / ${task.progress.total} · ${task.progress.detail}", style = MaterialTheme.typography.bodySmall)
                        if (group.steps.isNotEmpty()) Text("步骤完成 ${group.steps.count { it.outcome == MusicTaskOutcome.Completed }} · 可重试异常 ${group.failures.size}", style = MaterialTheme.typography.labelMedium)
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
                        val freeFailures = group.failures.filterNot { it.kind == "syncLyrics" }
                        if (freeFailures.isNotEmpty()) TextButton(onClick = { feature.launch {
                            assistant.retryFailedTasks(freeFailures.map { it.id }); feature.onReload(); feature.reloadGeneratedLyrics()
                        } }, enabled = !state.task.running) { Text("仅重试失败项（${freeFailures.size} 个步骤）") }
                        if (group.steps.isNotEmpty()) {
                            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起详情" else "查看步骤与失败原因") }
                            if (expanded) group.steps.forEach { entry ->
                                HorizontalDivider()
                                Text(entry.title, style = MaterialTheme.typography.titleSmall)
                                Text(entry.outcome.toUserLabel() + if (entry.message.isNotBlank()) " · ${entry.message}" else "", style = MaterialTheme.typography.bodySmall)
                                if (entry in group.failures) {
                                    TextButton(onClick = {
                                        if (entry.kind == "syncLyrics") paidRetry = entry
                                        else feature.launch { assistant.retryTask(entry.id); feature.onReload(); feature.reloadGeneratedLyrics() }
                                    }, enabled = !state.task.running) { Text(if (entry.kind == "syncLyrics") "重新生成（可能计费）" else "重试此步骤") }
                                }
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
@OptIn(ExperimentalLayoutApi::class)
internal fun RecommendationsScreen(compact: Boolean, onBack: () -> Unit) {
    val feature = LocalMusicFeatures.current ?: return
    val assistant = feature.assistant ?: return
    val state by assistant.state.collectAsState()
    val tracks = state.recommendations.trackIds.mapNotNull { id -> feature.library.firstOrNull { it.id == id } }
    var saving by remember { mutableStateOf(emptyList<Track>()) }
    var choosePlaylist by remember { mutableStateOf(false) }
    var createPlaylist by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var info by remember { mutableStateOf<Track?>(null) }
    var commenting by remember { mutableStateOf<Track?>(null) }
    var commentDraft by remember { mutableStateOf("") }
    val busy = state.task.running
    Column(Modifier.fillMaxSize().padding(if (compact) 16.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Text("每日推荐", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { feature.onNavigate(LibraryDestination.Settings) }) { Text("设置") }
        }
        TextButton(onClick = { feature.onNavigate(LibraryDestination.RecommendationHistory) }) { Text("推荐历史与我的反馈") }
        Text("点赞、踩和评论帮助调整下次推荐；收藏或加入歌单才会保留音频。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
        if (state.recommendations.createdDate.isNotBlank()) Text("上次推荐：" + state.recommendations.createdDate +
            " · 近 " + feature.settings.recommendationCooldownDays + " 天不重复", style = MaterialTheme.typography.labelMedium)
        Row {
            TextButton(onClick = { feature.launch { assistant.recommend(true); feature.onReload() } }, enabled = feature.settings.recommendationsEnabled && !busy) { Text("更新推荐") }
            TextButton(onClick = { selecting = !selecting; selected = emptySet() }, enabled = !busy && tracks.isNotEmpty()) { Text(if (selecting) "取消选择" else "多选") }
            TextButton(onClick = {
                saving = if (selecting) tracks.filter { it.id in selected } else tracks
                choosePlaylist = saving.isNotEmpty()
            }, enabled = !busy && tracks.isNotEmpty() && (!selecting || selected.isNotEmpty())) { Text(if (selecting) "加入歌单" else "保存全部") }
        }
        if (!feature.settings.recommendationsEnabled) TextButton(onClick = { feature.onNavigate(LibraryDestination.Settings) }) { Text("未开启推荐 · 前往设置") }
        if (busy) MusicTaskProgress(state.task.progress, assistant::cancel)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tracks.isEmpty()) item { Text("已内置歌源，开启推荐后点击“更新推荐”即可开始；也可先收藏喜欢的歌，帮助了解你的偏好。", color = ResonanceColors.Muted) }
            items(tracks, key = Track::id) { track ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selecting) Checkbox(track.id in selected, { checked -> selected = if (checked) selected + track.id else selected - track.id }, enabled = !busy)
                            AlbumArtwork(track.artworkSeed, Modifier.size(48.dp), artworkPath = track.artworkPath, audioSource = track.sourceUri)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(track.title, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(track.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                            IconButton(onClick = {
                                if (track.sourceUri.isNullOrBlank()) feature.launch { val restored = assistant.restoreDownload(track.id); feature.onReload(); feature.onPlay(restored) }
                                else feature.onPlay(track)
                            }, enabled = !busy) { Icon(Icons.Default.PlayArrow, "播放推荐") }
                            IconButton(onClick = { feature.favorite(track) }, enabled = !busy) { Icon(if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (track.isFavorite) "已收藏" else "收藏歌曲") }
                        }
                        state.origins[track.id]?.let { origin ->
                            Text(origin.version + " · " + origin.source, style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted, maxLines = 2)
                        }
                        var expanded by remember(track.id) { mutableStateOf(false) }
                        state.recommendations.reasons[track.id]?.let { reason ->
                            Text(reason, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted,
                                maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        val saved = track.isFavorite || feature.playlists.any { list -> list.id != DAILY_PLAYLIST_ID && list.tracks.any { it.id == track.id } }
                        Text(if (saved) "已保存 · 换批保留" else "临时推荐 · 收藏或加入歌单后保留", style = MaterialTheme.typography.labelMedium)
                        val feedback = assistant.feedbackFor(track)
                        val vote = feedback?.vote ?: RecommendationVote.Neutral
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { feature.launch {
                                assistant.setRecommendationVote(track, if (vote == RecommendationVote.Like) RecommendationVote.Neutral else RecommendationVote.Like)
                            } }, enabled = !busy) {
                                Icon(Icons.Default.ThumbUp, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (vote == RecommendationVote.Like) "已点赞 · 撤销" else "点赞")
                            }
                            TextButton(onClick = { feature.launch {
                                assistant.setRecommendationVote(track, if (vote == RecommendationVote.Dislike) RecommendationVote.Neutral else RecommendationVote.Dislike)
                            } }, enabled = !busy) {
                                Icon(Icons.Default.ThumbDown, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (vote == RecommendationVote.Dislike) "已踩 · 撤销" else "踩")
                            }
                            TextButton(onClick = { commentDraft = feedback?.comment.orEmpty(); commenting = track }, enabled = !busy) {
                                Icon(Icons.Default.Comment, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (feedback?.comment.isNullOrBlank()) "评论" else "编辑评论")
                            }
                        }
                        if (!feedback?.comment.isNullOrBlank()) Text("你的评论：" + feedback.comment,
                            style = MaterialTheme.typography.bodySmall, maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        FlowRow {
                            TextButton(onClick = { saving = listOf(track); choosePlaylist = true }, enabled = !busy) { Text(if (saved) "已保存 · 加入歌单" else "加入歌单") }
                            TextButton(onClick = { info = track }) { Text("歌曲信息") }
                            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起理由" else "展开理由") }
                        }
                    }
                }
            }
            if (feature.message.isNotBlank()) item { Text(feature.message, color = MaterialTheme.colorScheme.primary) }
        }
    }
    commenting?.let { track ->
        AlertDialog(onDismissRequest = { commenting = null }, title = { Text("评价「" + track.title + "」") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("写下喜欢的地方或不想再听的特点，例如“喜欢轻快的旋律”“不要太吵的摇滚”。评论保存在本机，用于后续推荐。", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(commentDraft, { commentDraft = it.take(500) }, label = { Text("你的评论") },
                    supportingText = { Text(commentDraft.length.toString() + "/500 · 留空保存可删除评论") },
                    minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
            }
        }, confirmButton = { TextButton(onClick = {
            val comment = commentDraft; commenting = null
            feature.launch { assistant.saveRecommendationComment(track, comment); feature.message = if (comment.isBlank()) "评论已删除" else "评论已保存，下次推荐会参考" }
        }, enabled = !busy) { Text("保存") } }, dismissButton = { TextButton(onClick = { commenting = null }) { Text("取消") } })
    }
    if (choosePlaylist && saving.isNotEmpty()) BatchAddToPlaylistDialog(
        playlists = feature.playlists.filterNot { it.id == DAILY_PLAYLIST_ID }, trackCount = saving.size,
        onDismiss = { choosePlaylist = false; saving = emptyList() },
        onSelectPlaylist = { id ->
            val ids = saving.map(Track::id); choosePlaylist = false; saving = emptyList(); selecting = false; selected = emptySet()
            feature.launch { feature.message = assistant.saveToPlaylist(ids, id); feature.onReload() }
        },
        onCreateNewPlaylist = { choosePlaylist = false; createPlaylist = true; playlistName = "" },
    )
    if (createPlaylist) AlertDialog(onDismissRequest = { createPlaylist = false }, title = { Text("新建歌单并加入") },
        text = { OutlinedTextField(playlistName, { playlistName = it.take(80) }, label = { Text("歌单名称") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            val ids = saving.map(Track::id); val title = playlistName; createPlaylist = false; saving = emptyList(); selecting = false; selected = emptySet()
            feature.launch { feature.message = assistant.saveToPlaylist(ids, newName = title); feature.onReload() }
        }, enabled = playlistName.isNotBlank()) { Text("创建并保存") } }, dismissButton = { TextButton(onClick = { createPlaylist = false }) { Text("取消") } })
    info?.let { track ->
        AlertDialog(onDismissRequest = { info = null }, title = { Text(track.title) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackSourceDetails(track, state.origins[track.id])
                Text("不喜欢会删除实际音频并移出所有歌单，包括已保存的歌曲。", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(onClick = { info = null; feature.launch { assistant.matchCover(track, true); feature.onReload() } }, enabled = !busy) { Text("重新匹配专辑封面") } },
            dismissButton = { Row {
                TextButton(onClick = { info = null; feature.dislike(track) }, enabled = !busy) { Text("不喜欢并删除") }
                TextButton(onClick = { info = null }) { Text("关闭") }
            } })
    }
}
