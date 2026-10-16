package com.resonance.player.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.*

@Composable
fun MusicTaskProgress(progress: ImportProgress, onCancel: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(progress.stage, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            if (progress.total > 0) Text("${progress.completed} / ${progress.total}", style = MaterialTheme.typography.labelMedium)
            onCancel?.let { TextButton(onClick = it) { Text("取消") } }
        }
        progress.fraction?.let { LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) }
            ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (progress.fileName.isNotBlank()) Text(progress.fileName, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        if (progress.detail.isNotBlank()) Text(progress.detail, style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted)
    }
}

@Composable
private fun ToggleSetting(title: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = change)
    }
}

@Composable
internal fun MusicFeatureSettingsSection() {
    val feature = LocalMusicFeatures.current ?: return
    val settings = feature.settings
    var configureAi by rememberSaveable { mutableStateOf(false) }
    SectionHeader("播放")
    ToggleSetting("统一播放音量", feature.player.autoLoudnessEnabled) { feature.onToggleBalance() }
    SettingRow(Icons.Default.HelpOutline, "音量平衡使用说明", "了解自动分析、音量与原始文件", { feature.onNavigate(LibraryDestination.AudioHelp) })
    ToggleSetting("桌面悬浮歌词", settings.floatingLyrics) { feature.toggleFloatingLyrics() }
    if (settings.floatingLyrics) {
        ToggleSetting("锁定歌词位置并允许点击穿透", settings.floatingLyricsLocked) { feature.save(settings.copy(floatingLyricsLocked = it)) }
        Text("歌词字号 ${settings.floatingLyricsSize}", style = MaterialTheme.typography.labelMedium)
        Slider(settings.floatingLyricsSize.toFloat(), { feature.settings = feature.settings.copy(floatingLyricsSize = it.toInt()) }, onValueChangeFinished = { feature.save(feature.settings) }, valueRange = 14f..56f)
    }
    SectionHeader("歌词翻译")
    OutlinedTextField(settings.translationLanguage, { feature.save(settings.copy(translationLanguage = it.take(40))) }, label = { Text("目标语言，例如中文 / English / 日本語") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(LyricDisplay.Original to "原文", LyricDisplay.Translation to "译文", LyricDisplay.Bilingual to "双语").forEach { (mode, label) ->
            FilterChip(settings.lyricDisplay == mode, { feature.save(settings.copy(lyricDisplay = mode)) }, label = { Text(label) })
        }
    }
    SectionHeader("AI 音乐助手")
    SettingRow(Icons.Default.AutoAwesome, "打开音乐助手", "聊天管理、截图导入与每日推荐", { feature.onNavigate(LibraryDestination.Assistant) })
    TextButton(onClick = { configureAi = !configureAi }) { Text(if (configureAi) "收起模型设置" else "配置模型与 API Key") }
    if (configureAi) AiConnectionEditor(feature)
    SectionHeader("每日推荐")
    ToggleSetting("自动推荐并下载", settings.recommendationsEnabled) { feature.save(settings.copy(recommendationsEnabled = it)) }
    if (settings.recommendationsEnabled) {
        ToggleSetting("允许后台定时生成", settings.backgroundRecommendations) { feature.save(settings.copy(backgroundRecommendations = it)) }
        if (!settings.backgroundRecommendations) Text("打开应用时检查是否到期", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
        else Text("Windows 在托盘运行；Android 按系统后台调度执行，错过时间后补生成。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
        var hours by remember(settings.intervalHours) { mutableStateOf(settings.intervalHours.toString()) }
        var count by remember(settings.recommendationCount) { mutableStateOf(settings.recommendationCount.toString()) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(hours, { hours = it.filter(Char::isDigit).take(3) }, label = { Text("间隔小时（1–168）") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(count, { count = it.filter(Char::isDigit).take(2) }, label = { Text("每次首数（1–30）") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        TextButton(onClick = { feature.save(settings.copy(intervalHours = hours.toIntOrNull()?.coerceIn(1, 168) ?: 24, recommendationCount = count.toIntOrNull()?.coerceIn(1, 30) ?: 5)); feature.message = "推荐计划已保存" }) { Text("保存推荐计划") }
    }
    var limit by remember(settings.monthlyTokenLimit) { mutableStateOf(settings.monthlyTokenLimit.toString()) }
    OutlinedTextField(limit, { limit = it.filter(Char::isDigit).take(9) }, label = { Text("每月 AI Token 上限") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    TextButton(onClick = { feature.save(settings.copy(monthlyTokenLimit = limit.toLongOrNull()?.coerceIn(10_000, 100_000_000) ?: 1_000_000)); feature.message = "预算上限已保存" }) { Text("保存预算") }
    var cookie by remember(settings.bilibiliCookie) { mutableStateOf(settings.bilibiliCookie) }
    SectionHeader("同步歌词生成")
    Text("根据实际音频识别人声并生成时间轴。使用阿里云百炼 Paraformer，独立按音频时长计费，不计入聊天 Token。仅点击生成时上传音频，结果保存在本机。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
    var speechKey by remember(settings.speechApiKey) { mutableStateOf(settings.speechApiKey) }
    var speechUrl by remember(settings.speechUrl) { mutableStateOf(settings.speechUrl) }
    var speechMinutes by remember(settings.monthlySpeechSeconds) { mutableStateOf((settings.monthlySpeechSeconds / 60).toString()) }
    OutlinedTextField(speechKey, { speechKey = it }, label = { Text("百炼语音 API Key（留空复用千问 Key）") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(speechUrl, { speechUrl = it }, label = { Text("语音 WebSocket 地址（北京地域）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(speechMinutes, { speechMinutes = it.filter(Char::isDigit).take(5) }, label = { Text("每月语音处理上限（分钟）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    TextButton(onClick = { feature.save(feature.settings.copy(speechApiKey = speechKey.trim(), speechUrl = speechUrl.trim(), monthlySpeechSeconds = (speechMinutes.toIntOrNull() ?: 60).coerceIn(1, 6000) * 60)); feature.message = "语音配置已保存" }) { Text("保存语音配置") }
    SectionHeader("B 站账号（可选）")
    OutlinedTextField(cookie, { cookie = it }, label = { Text("自己的登录 Cookie") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
    TextButton(onClick = { feature.save(settings.copy(bilibiliCookie = cookie.trim())); feature.message = "B 站账号配置已保存" }) { Text("保存账号配置") }
    if (feature.message.isNotBlank()) Text(feature.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun AiConnectionEditor(feature: MusicFeatureController) {
    val settings = feature.settings
    var provider by rememberSaveable { mutableStateOf(settings.assistantProvider.name) }
    val selected = AiProvider.entries.first { it.name == provider }
    val initial = settings.connections.firstOrNull { it.provider == selected } ?: AiConnection(selected)
    var url by remember(selected, initial.baseUrl) { mutableStateOf(initial.baseUrl) }
    var model by remember(selected, initial.model) { mutableStateOf(initial.model) }
    var key by remember(selected, initial.apiKey) { mutableStateOf(initial.apiKey) }
    var vision by remember(selected, initial.vision) { mutableStateOf(initial.vision) }

    val defaultConnection = remember(selected) { AiConnection(selected) }
    val candidateModels = when (selected) {
        AiProvider.Qwen -> listOf("qwen-plus", "qwen-turbo", "qwen-max", "qwen3-vl-flash")
        AiProvider.DeepSeek -> listOf("deepseek-chat", "deepseek-reasoner")
        AiProvider.Doubao -> listOf("doubao-pro-32k", "doubao-lite-32k", "doubao-vision-pro-32k")
        AiProvider.Custom -> emptyList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiProvider.entries.forEach { p -> FilterChip(selected == p, { provider = p.name }, label = { Text(p.label) }) }
        }
        OutlinedTextField(url, { url = it }, label = { Text("HTTPS API 地址") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(model, { model = it }, label = { Text(if (selected == AiProvider.Doubao) "模型名称或推理接入点 ID" else "模型名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("常用推荐模型（点击填入）：", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            candidateModels.forEach { candidate ->
                FilterChip(
                    selected = model == candidate,
                    onClick = {
                        model = candidate
                        if (candidate.contains("vl", ignoreCase = true) || candidate.contains("vision", ignoreCase = true)) {
                            vision = true
                        }
                    },
                    label = { Text(candidate, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                url = defaultConnection.baseUrl
                model = defaultConnection.model
                vision = defaultConnection.vision
            }) {
                Text("恢复默认地址与模型")
            }
        }
        OutlinedTextField(key, { key = it }, label = { Text("API Key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        ToggleSetting("该模型支持图片输入", vision) { vision = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val c = AiConnection(selected, url.trim(), model.trim(), key.trim(), vision)
                feature.save(settings.copy(connections = settings.connections.filterNot { it.provider == selected } + c)); feature.message = "模型配置已保存"
            }) { Text("保存") }
            TextButton(onClick = { feature.launch { feature.message = feature.assistant?.testConnection(AiConnection(selected, url.trim(), model.trim(), key.trim(), vision)).orEmpty() } }, enabled = key.isNotBlank() && model.isNotBlank()) { Text("测试连接") }
        }
        Text("音乐助手：${settings.assistantProvider.label} · 图片识别：${settings.visionProvider.label}", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { feature.save(feature.settings.copy(assistantProvider = selected)) }) { Text("用于音乐管理") }
            TextButton(onClick = { feature.save(feature.settings.copy(visionProvider = selected)) }, enabled = vision) { Text("用于截图识别") }
        }
    }
}

@Composable
internal fun AudioBalanceHelpScreen(compact: Boolean, onBack: () -> Unit) {
    ToolPage("音量平衡使用说明", onBack, compact) {
        val sections = listOf(
            "只需要一个开关" to "在设置或播放器中开启“统一播放音量”。应用会自动减小不同歌曲之间忽大忽小的差异，无需逐曲校准，也不用调整增益数值。关闭后按原始响度播放。",
            "第一次播放时发生什么" to "应用在本机分析整首音频。首次分析需要一些时间，播放会先开始，完成后平滑调整，不需要等待分析才能听歌。同一首歌的结果会缓存，后续直接使用。",
            "音量仍由你决定" to "手机或电脑的系统音量决定设备输出大小，播放器音量控制整个应用。自动平衡只是补偿歌曲之间的差异，不会自动修改系统音量，也不再记忆每首歌不同的主音量。",
            "会不会修改音频文件" to "不会。分析和播放增益只在播放时使用，原文件及其标签保持原样。为保护峰值，一些很安静的歌曲不会被强行放大到完全一样响。",
            "为何开启后仍有差异" to "录音动态、乐器和人声的分布不同，感受也会不同。目标是减少切歌时明显的音量跳变，而非压平音乐的所有强弱。静音、损坏或不支持的文件不会写入成功分析缓存。",
            "遇到异常怎么办" to "先关闭音量平衡比较。检查源文件是否完整、是否仍可访问；Windows 需要可用的 FFmpeg。分析失败时保留正常播放，并显示错误信息，不会改写音频。",
        )
        sections.forEach { (title, body) -> SectionHeader(title); Text(body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp)) }
    }
}

@Composable
internal fun AssistantScreen(compact: Boolean, onBack: () -> Unit) {
    val feature = LocalMusicFeatures.current ?: return
    val assistant = feature.assistant ?: return
    val state by assistant.state.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var images by remember { mutableStateOf(emptyList<AiImage>()) }
    var showSessionsDialog by remember { mutableStateOf(false) }

    val activeConnection = feature.settings.connections.firstOrNull { it.provider == feature.settings.assistantProvider }
    val activeModelName = activeConnection?.model?.ifBlank { null } ?: "默认模型"
    val activeProviderName = feature.settings.assistantProvider.label

    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(imeBottom) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize().padding(if (compact) 16.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Column(modifier = Modifier.weight(1f)) {
                Text("音乐助手", style = MaterialTheme.typography.titleLarge)
                Text("$activeProviderName · $activeModelName", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = { feature.launch { assistant.createSession() } }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("新对话")
            }
            TextButton(onClick = { showSessionsDialog = true }) {
                Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("历史")
            }
            TextButton(onClick = { feature.onNavigate(LibraryDestination.Tasks) }) { Text("任务") }
            TextButton(onClick = { feature.onNavigate(LibraryDestination.Settings) }) { Text("设置") }
        }
        Text("本月输入 ${state.usage.inputTokens} · 缓存命中 ${state.usage.cachedTokens} · 输出 ${state.usage.outputTokens} · 语音 ${state.speechSeconds} 秒", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted)
        if (state.task.running) MusicTaskProgress(state.task.progress, assistant::cancel)
        LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.messages.withIndex().toList(), key = { "chat-${it.index}" }) { (_, item) ->
                Surface(
                    color = if (item.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        val senderLabel = if (item.role == "user") {
                            "你"
                        } else {
                            val replyModel = item.model?.takeIf(String::isNotBlank) ?: activeModelName
                            "音乐助手 · 回复自 $replyModel"
                        }
                        Text(
                            senderLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.role == "user") MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else ResonanceColors.Muted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.content,
                            color = if (item.role == "user") MaterialTheme.colorScheme.onPrimaryContainer else ResonanceColors.TextPrimary,
                        )
                    }
                }
            }
            if (feature.message.isNotBlank()) item { Text(feature.message, color = MaterialTheme.colorScheme.primary) }
            if (!state.task.running && state.task.message.isNotBlank()) item { Text(state.task.message, style = MaterialTheme.typography.bodySmall) }
        }
        if (images.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("已选择 ${images.size} 个截图片段", modifier = Modifier.weight(1f))
            TextButton(onClick = { images = emptyList() }) { Text("移除图片") }
        }
        OutlinedTextField(input, { input = it }, placeholder = { Text("例如：把截图中的歌单整理到我的曲库") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { feature.launch { images = feature.services.chooseAssistantImages() } }, enabled = !state.task.running) { Text("歌单截图") }
            Button(onClick = { val text = input; val attachments = images; input = ""; images = emptyList(); feature.launch { assistant.chat(text, attachments); feature.onReload() } }, enabled = !state.task.running && (input.isNotBlank() || images.isNotEmpty())) { Text("发送") }
        }
    }

    if (showSessionsDialog) {
        AlertDialog(
            onDismissRequest = { showSessionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("对话历史", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { feature.launch { assistant.createSession() }; showSessionsDialog = false }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("新对话")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "会话完全独立存储，切换不消耗额外 Token；每次请求仅携带当前会话最近 16 条上下文。",
                        style = MaterialTheme.typography.bodySmall,
                        color = ResonanceColors.Muted,
                    )
                    if (state.sessions.isEmpty()) {
                        Text("暂无历史对话", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        state.sessions.sortedByDescending { it.updatedAt }.forEach { session ->
                            val isCurrent = session.id == state.currentSessionId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        feature.launch { assistant.switchSession(session.id) }
                                        showSessionsDialog = false
                                    },
                                shape = MaterialTheme.shapes.small,
                                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            session.title.ifBlank { "新对话" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "${session.messages.size} 条消息",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ResonanceColors.Muted,
                                        )
                                    }
                                    if (state.sessions.size > 1) {
                                        IconButton(
                                            onClick = { feature.launch { assistant.deleteSession(session.id) } },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "删除会话",
                                                modifier = Modifier.size(18.dp),
                                                tint = ResonanceColors.Muted,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSessionsDialog = false }) { Text("关闭") }
            },
        )
    }
}

@Composable
internal fun MusicFeatureDialogs() {
    val feature = LocalMusicFeatures.current ?: return
    if (feature.showVideoImport) VideoImportDialog(feature)
    if (feature.showOverlayPermissionPrompt) {
        AlertDialog(
            onDismissRequest = { feature.showOverlayPermissionPrompt = false },
            icon = { Icon(Icons.Default.PictureInPicture, contentDescription = null) },
            title = { Text("开启桌面悬浮歌词") },
            text = { Text("在 Android 上显示桌面悬浮歌词需要「显示在其他应用上层」的系统权限。请前往系统设置允许权限。") },
            confirmButton = {
                Button(onClick = {
                    feature.showOverlayPermissionPrompt = false
                    feature.services.requestOverlayPermission()
                }) { Text("前往设置") }
            },
            dismissButton = {
                TextButton(onClick = { feature.showOverlayPermissionPrompt = false }) { Text("取消") }
            },
        )
    }
    if (feature.showGenerateLyrics) {
        val track = feature.player.currentTrack
        AlertDialog(onDismissRequest = { feature.showGenerateLyrics = false }, title = { Text("生成同步歌词") }, text = {
            Text("将把「${track?.title.orEmpty()}」的音频发送给阿里云百炼语音识别，根据人声时间戳生成歌词。服务按音频时长收费，与聊天 Token 预算分开；本月已处理 ${feature.assistant?.state?.value?.speechSeconds ?: 0} 秒，上限 ${feature.settings.monthlySpeechSeconds} 秒。歌曲可能存在识别错误，原歌词会保留，结果缓存后无需重复付费。")
        }, confirmButton = { TextButton(onClick = { feature.showGenerateLyrics = false; feature.generateLyrics() }) { Text("开始生成") } }, dismissButton = { TextButton(onClick = { feature.showGenerateLyrics = false }) { Text("取消") } })
    }
    feature.reasonTrack?.let { track ->
        var reason by remember(track.id) { mutableStateOf("") }
        AlertDialog(onDismissRequest = { feature.reasonTrack = null }, title = { Text("不喜欢的原因（可选）") }, text = {
            Column { Text("已开始删除「${track.title}」。原因只用于改善后续推荐。"); OutlinedTextField(reason, { reason = it.take(1000) }, label = { Text("原因") }, modifier = Modifier.fillMaxWidth()) }
        }, confirmButton = { TextButton(onClick = { feature.reasonTrack = null; feature.launch { feature.assistant?.recordDislikeReason(track, reason) } }) { Text("提交") } }, dismissButton = { TextButton(onClick = { feature.reasonTrack = null }) { Text("跳过") } })
    }
}

@Composable
private fun VideoImportDialog(feature: MusicFeatureController) {
    var link by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var album by rememberSaveable { mutableStateOf("B 站音乐") }
    var cover by rememberSaveable { mutableStateOf("") }
    var inspected by remember { mutableStateOf(false) }
    var playlistId by remember { mutableStateOf<String?>(null) }
    val state = feature.assistant?.state?.collectAsState()?.value ?: MusicAssistantState()
    AlertDialog(onDismissRequest = { if (!state.task.running) feature.showVideoImport = false }, title = { Text("从 B 站导入音频") }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(link, { link = it; inspected = false }, label = { Text("视频链接 / BV 号（支持分 P）") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { feature.launch { feature.assistant?.inspectVideo(link)?.let { title = it.title; cover = it.coverUrl; inspected = true } } }, enabled = link.isNotBlank() && !state.task.running) { Text("读取视频信息") }
            if (inspected) {
                OutlinedTextField(title, { title = it }, label = { Text("歌名") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(artist, { artist = it }, label = { Text("歌手") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(album, { album = it }, label = { Text("专辑") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AlbumArtwork(0, Modifier.size(80.dp), artworkPath = cover.takeIf(String::isNotBlank))
                    TextButton(onClick = { feature.launch { feature.services.chooseCoverImage()?.let { cover = it } } }) { Text("自定义封面") }
                }
                Text("加入歌单", style = MaterialTheme.typography.labelMedium)
                FilterChip(playlistId == null, { playlistId = null }, label = { Text("仅加入曲库") })
                feature.playlists.forEach { list -> FilterChip(playlistId == list.id, { playlistId = list.id }, label = { Text(list.name) }) }
            }
            if (state.task.running) MusicTaskProgress(state.task.progress, feature.assistant?.let { it::cancel })
            if (feature.message.isNotBlank()) Text(feature.message, color = MaterialTheme.colorScheme.primary)
        }
    }, confirmButton = { Button(onClick = { feature.launch { feature.assistant?.importVideo(VideoImportRequest(link, title.trim(), artist.trim(), album.trim(), cover, playlistId)); feature.onReload(); feature.showVideoImport = false } }, enabled = inspected && title.isNotBlank() && artist.isNotBlank() && !state.task.running) { Text("下载并入库") } }, dismissButton = { TextButton(onClick = { feature.assistant?.cancel(); feature.showVideoImport = false }) { Text(if (state.task.running) "取消任务" else "关闭") } })
}

@Composable
internal fun TranslationControls() {
    val feature = LocalMusicFeatures.current ?: return
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { feature.showGenerateLyrics = true }, enabled = feature.player.currentTrack != null && !feature.generatingLyrics) { Text(if (feature.generatingLyrics) "正在生成…" else "生成同步歌词") }
        TextButton(onClick = { feature.translate() }, enabled = !feature.translating && feature.lyrics is LyricsUiState.Ready) { Text(if (feature.translating) "正在翻译…" else "翻译为${feature.settings.translationLanguage}") }
        if (feature.translated?.trackId == feature.player.currentTrack?.id) TextButton(onClick = { feature.translate(true) }, enabled = !feature.translating) { Text("重译") }
        TextButton(onClick = { feature.onNavigate(LibraryDestination.Settings) }) { Text("语言与显示") }
    }
}
