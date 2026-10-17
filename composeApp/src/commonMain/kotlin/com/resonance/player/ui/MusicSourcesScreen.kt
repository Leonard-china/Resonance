package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.model.*

@Composable
internal fun MusicSourcesScreen(compact: Boolean, onBack: () -> Unit) {
    val feature = LocalMusicFeatures.current ?: return
    val assistant = feature.assistant ?: return
    val state by assistant.state.collectAsState()
    var address by remember { mutableStateOf("") }
    val busy = state.task.running
    Column(Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Text("歌源", style = MaterialTheme.typography.headlineMedium)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                Text("已为您内置网易云音乐、酷我音乐、酷狗音乐等主流公共歌源，开箱即用，无需到处寻找插件。", style = MaterialTheme.typography.bodyMedium)
                Text("搜索歌曲无需配置 AI，也不消耗模型额度。下载、封面和歌词优先使用同一歌曲来源。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Button(onClick = { feature.searchOnline = true; feature.onNavigate(LibraryDestination.Discover) }) { Text("搜索在线歌曲") }
            }
            item {
                OutlinedTextField(address, { address = it }, label = { Text("歌源 .js 或订阅 JSON 地址") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2, enabled = !busy)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { feature.launch {
                        val installed = assistant.restoreBuiltinSources()
                        feature.message = "已恢复并载入 " + installed.size + " 个内置歌源"
                    } }, enabled = !busy) { Text("恢复内置歌源") }
                    Button(onClick = { feature.launch {
                        val installed = assistant.installSourceUrl(address.trim())
                        feature.message = "已导入 " + installed.size + " 个歌源"
                    } }, enabled = !busy && (address.startsWith("https://") || address.startsWith("http://"))) { Text("从地址导入") }
                    OutlinedButton(onClick = { feature.launch {
                        feature.services.chooseMusicSourceFile()?.let { script ->
                            val installed = assistant.installSource(script)
                            feature.message = "已导入 " + installed.name
                        }
                    } }, enabled = !busy) { Text("选择 .js 文件") }
                }
            }
            items(state.sources, key = MusicSourceInfo::id) { source ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(source.name, style = MaterialTheme.typography.titleMedium)
                                Text(source.version.ifBlank { "未提供版本号" }, style = MaterialTheme.typography.labelSmall)
                            }
                            Switch(source.enabled, { enabled -> feature.launch { assistant.enableSource(source.id, enabled) } }, enabled = !busy)
                        }
                        Text(source.capabilities.joinToString(" · ") { sourceCapabilityLabel(it) },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { feature.launch { assistant.removeSource(source.id) } }, enabled = !busy) { Text("卸载歌源（保留已下载歌曲）") }
                    }
                }
            }
            item {
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("保留原音轨格式", modifier = Modifier.weight(1f))
                    Switch(feature.settings.preserveOriginalAudio, { feature.save(feature.settings.copy(preserveOriginalAudio = it)) })
                }
                Text("兼容时直接保存音轨；关闭后转换为 MP3。提高输出码率不会恢复原音源细节。", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("允许 B 站补充来源", modifier = Modifier.weight(1f))
                    Switch(feature.settings.bilibiliFallback, { feature.save(feature.settings.copy(bilibiliFallback = it)) })
                }
                var uids by remember(feature.settings.preferredBilibiliUids) { mutableStateOf(feature.settings.preferredBilibiliUids) }
                OutlinedTextField(uids, { uids = it.take(500) }, label = { Text("优先检索的 B 站 UID，逗号分隔") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                Text("填写后自动候选限定这些 UID；所有自动候选仍排除 MV、现场及剪辑。账号名称和 Hi-Res 标题不构成音质认证。", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { feature.save(feature.settings.copy(preferredBilibiliUids = uids.trim())); feature.message = "来源偏好已保存" }) { Text("保存账号范围") }
            }
            if (busy) item { MusicTaskProgress(state.task.progress, assistant::cancel) }
            if (feature.message.isNotBlank()) item { Text(feature.message, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

private fun sourceCapabilityLabel(value: String): String = when (value) {
    "search" -> "搜索"; "getMediaSource" -> "音频"; "getMusicInfo" -> "歌曲详情"; "getLyric" -> "歌词"
    "getAlbumInfo" -> "专辑"; "getMusicSheetInfo" -> "歌单"; "getTopLists" -> "榜单"; "getTopListDetail" -> "榜单歌曲"
    else -> value
}

@Composable
internal fun MusicStorageScreen(compact: Boolean, onBack: () -> Unit) {
    val feature = LocalMusicFeatures.current ?: return
    val assistant = feature.assistant ?: return
    val state by assistant.state.collectAsState()
    var report by remember { mutableStateOf<MusicStorageReport?>(null) }
    var pending by remember { mutableStateOf<List<ManagedAudioEntry>>(emptyList()) }
    var refreshing by remember { mutableStateOf(false) }
    fun refresh(duplicates: Boolean = false) = feature.launch {
        refreshing = true
        try { report = assistant.storageReport(duplicates) } finally { refreshing = false }
    }
    LaunchedEffect(Unit) { refresh() }
    val busy = state.task.running || refreshing
    Column(Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Text("下载与空间", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { refresh() }, enabled = !busy) { Text("刷新") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                report?.let { r ->
                    Text("已登记下载 " + formatMusicBytes(r.managedBytes), style = MaterialTheme.typography.titleLarge)
                    Text("临时 " + formatMusicBytes(r.temporaryBytes) + " · 已保存 " + formatMusicBytes(r.savedBytes) +
                        "\n设备可用 " + formatMusicBytes(r.freeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("只管理已确认由应用创建的下载；手机其他目录的音频不参与自动清理。释放本地下载会保留歌曲信息、收藏和歌单关系，重新下载取决于原歌源是否可用。", style = MaterialTheme.typography.bodySmall)
            }
            item {
                var limit by remember(feature.settings.temporaryStorageMb) { mutableStateOf(feature.settings.temporaryStorageMb.toString()) }
                OutlinedTextField(limit, { limit = it.filter(Char::isDigit).take(4) }, label = { Text("临时下载上限 MB（64–8192）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                TextButton(onClick = { feature.save(feature.settings.copy(temporaryStorageMb = limit.toIntOrNull()?.coerceIn(64, 8192) ?: 512)); feature.message = "空间上限已保存" }) { Text("保存上限") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pending = report?.entries.orEmpty().filter { !it.saved && !it.playing && it.bytes > 0 } }, enabled = !busy && (report?.temporaryBytes ?: 0L) > 0L) { Text("清理临时下载") }
                    TextButton(onClick = { refresh(true) }, enabled = !busy) { Text("检查重复文件") }
                }
            }
            items(report?.entries.orEmpty(), key = { it.track.id }) { entry ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(entry.track.title, style = MaterialTheme.typography.titleMedium)
                        Text(entry.track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (entry.bytes == 0L) "本地未下载" else formatMusicBytes(entry.bytes) + " · " +
                            if (entry.playing) "正在播放" else if (entry.saved) "长期保存／歌单保护" else "临时下载",
                            style = MaterialTheme.typography.labelMedium)
                        if (entry.duplicates.isNotEmpty()) Text("发现内容完全相同的其他已登记文件：" + entry.duplicates.size + " 份", style = MaterialTheme.typography.bodySmall)
                        Row {
                            if (entry.bytes > 0) TextButton(onClick = { pending = listOf(entry) }, enabled = !busy && !entry.playing) { Text("释放本地下载") }
                            else TextButton(onClick = { feature.launch {
                                assistant.restoreDownload(entry.track.id); feature.onReload()
                                report = assistant.storageReport()
                            } }, enabled = !busy) { Text("重新下载") }
                        }
                    }
                }
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (feature.message.isNotBlank()) item { Text(feature.message, color = MaterialTheme.colorScheme.primary) }
        }
    }
    if (pending.isNotEmpty()) {
        val chosen = pending
        AlertDialog(onDismissRequest = { pending = emptyList() }, title = { Text("释放本地下载") }, text = {
            Text("将释放 " + chosen.size + " 首，共约 " + formatMusicBytes(chosen.sumOf { it.bytes }) +
                "。保留歌曲信息、收藏和歌单关系；播放中的文件会保留。重新下载需要原歌源可用。")
        }, confirmButton = { TextButton(onClick = {
            pending = emptyList()
            feature.launch {
                val result = assistant.cleanupDownloads(chosen.map { it.track.id }, includeSaved = chosen.any { it.saved })
                feature.onReload(); report = assistant.storageReport()
                feature.message = "实际释放 " + formatMusicBytes(result.freedBytes) + "，" + result.deletedFiles + " 个文件" +
                    if (result.messages.isEmpty()) "" else "\n" + result.messages.joinToString("\n")
            }
        }) { Text("释放下载") } }, dismissButton = { TextButton(onClick = { pending = emptyList() }) { Text("取消") } })
    }
}

internal fun formatMusicBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.2f GB".format(bytes.toDouble() / (1024L * 1024 * 1024))
    else -> "%.1f MB".format(bytes.toDouble() / (1024L * 1024))
}
