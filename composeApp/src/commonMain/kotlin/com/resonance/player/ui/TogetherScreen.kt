package com.resonance.player.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.platform.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun TogetherScreen(compact: Boolean, onBack: () -> Unit) {
    val feature = LocalMusicFeatures.current
    val together = feature?.services?.listenTogether
    if (together == null) {
        ToolPage("与朋友一起听", onBack, compact) { Text("当前环境暂不支持设备直连") }
        return
    }
    val room by together.state.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var joining by rememberSaveable { mutableStateOf(false) }
    // Invitations are capabilities: do not save them into SavedState or preferences.
    var invitation by remember { mutableStateOf("") }
    var address by remember(room.addresses) { mutableStateOf(room.addresses.firstOrNull().orEmpty()) }
    var localPort by rememberSaveable { mutableStateOf("45873") }
    var externalPort by remember(room.port) { mutableStateOf(if (room.port > 0) room.port.toString() else "45873") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showAdvancedPort by rememberSaveable { mutableStateOf(false) }
    var showAdvancedHost by rememberSaveable { mutableStateOf(false) }

    // Smart clipboard detection for invitations
    var clipboardInvite by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(room.role) {
        if (room.role == TogetherRole.None) {
            val clipText = clipboard.getText()?.text?.trim()
            if (!clipText.isNullOrBlank() && clipText.contains("resonance-listen://")) {
                clipboardInvite = clipText
            }
        }
    }

    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        message = ""
        scope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "操作未完成，请检查网络连接、地址或邀请" }
            finally { busy = false }
        }
    }

    ToolPage("与朋友一起听", onBack, compact) {
        // Main status banner
        Surface(shape = RoundedCornerShape(24.dp), color = ResonanceColors.CanvasElevated) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(52.dp).background(ResonanceColors.PrimarySoft, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Headphones, null, tint = ResonanceColors.Primary, modifier = Modifier.size(28.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("隔着距离，听同一首歌", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("双人直连 · 端到端加密 · 无中转服务器", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Lock, null, Modifier.size(16.dp), tint = ResonanceColors.Primary)
                    Text("音频与播放状态全程加密传输，充分保障隐私", style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Primary)
                }

                Text(when (room.status) {
                    TogetherStatus.Idle, TogetherStatus.Error -> "创建房间邀请一位朋友，或粘贴朋友发来的邀请加入。"
                    TogetherStatus.Listening -> "房间已开启，等待朋友加入中…"
                    TogetherStatus.Connecting -> "正在建立端到端加密连接…"
                    TogetherStatus.Receiving -> "正在接收歌曲 · ${room.receivedBytes / 1024} KB"
                    TogetherStatus.Connected -> if (room.role == TogetherRole.Host) "朋友已连接，正在一起听" else "跟听中 · 由房主控制播放"
                }, style = MaterialTheme.typography.bodyLarge)

                if (room.message.isNotBlank()) {
                    Text(room.message, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                }

                if (room.verificationCode.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = ResonanceColors.PrimarySoft) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("房间核验码", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted)
                            Text(room.verificationCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ResonanceColors.Primary)
                        }
                    }
                    Text("双方显示的核验码一致即可确认连接的是同一房间，未被篡改。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                }

                val title = if (room.role == TogetherRole.Host) feature.player.currentTrack?.title else room.trackTitle
                if (!title.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MusicNote, null, tint = ResonanceColors.Primary, modifier = Modifier.size(18.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium, color = ResonanceColors.TextPrimary)
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            Text(message, color = ResonanceColors.Primary, style = MaterialTheme.typography.bodyMedium)
        }

        val active = room.role != TogetherRole.None

        // Not in room: choose role (Host vs Guest)
        if (!active) {
            // Smart clipboard banner if invitation detected
            if (clipboardInvite != null && invitation.isBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, ResonanceColors.Primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, null, tint = ResonanceColors.Primary, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text("检测到朋友发来的一起听邀请", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("点击即可一键加入跟听", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                        }
                        Button(
                            onClick = {
                                val clip = clipboardInvite.orEmpty()
                                invitation = clip
                                joining = true
                                run {
                                    together.join(clip)
                                    if (together.state.value.isGuest) {
                                        invitation = ""
                                        clipboardInvite = null
                                    }
                                }
                            },
                            enabled = !busy,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("一键加入")
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(!joining, { joining = false }, label = { Text("我来创建") })
                FilterChip(joining, { joining = true }, label = { Text("加入朋友") })
            }

            if (joining) {
                // Quick paste button
                OutlinedButton(
                    onClick = {
                        val clip = clipboard.getText()?.text?.trim().orEmpty()
                        if (clip.isNotBlank()) {
                            invitation = clip
                            run {
                                together.join(clip)
                                if (together.state.value.isGuest) invitation = ""
                            }
                        } else {
                            message = "剪贴板为空，请先复制朋友发来的完整邀请"
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("从剪贴板粘贴并加入")
                }

                OutlinedTextField(
                    value = invitation,
                    onValueChange = { invitation = it.take(2048) },
                    label = { Text("朋友发来的完整邀请") },
                    placeholder = { Text("支持直接粘贴整条微信/QQ邀请消息或 resonance-listen://…") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    enabled = !busy
                )
                Text("复制朋友发来的消息并粘贴在此，系统会自动提取其中的直连凭据。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)

                Button(
                    onClick = {
                        run {
                            together.join(invitation.trim())
                            if (together.state.value.isGuest) invitation = ""
                        }
                    },
                    enabled = !busy && invitation.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Link, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("加密连接并跟听")
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ResonanceColors.CanvasElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(36.dp).background(ResonanceColors.PrimarySoft, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, tint = ResonanceColors.Primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("我是房主", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("由你控制播放进度与切歌，当前歌曲直接加密传输给朋友", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                            }
                        }
                        Text("💡 最简连接：两人连接同一个 Wi-Fi（或一人开手机热点让另一人连接），点击下方即可一键创建房间。",
                            style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                    }
                }

                Button(
                    onClick = {
                        run {
                            together.host(localPort.toIntOrNull()?.takeIf { it in 1..65535 } ?: 45873)
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("创建双人房间", style = MaterialTheme.typography.titleSmall)
                }

                // Collapsible port setting
                TextButton(onClick = { showAdvancedPort = !showAdvancedPort }) {
                    Icon(if (showAdvancedPort) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showAdvancedPort) "收起端口设置" else "高级：自定义本地端口 (默认 45873)")
                }
                if (showAdvancedPort) {
                    OutlinedTextField(
                        value = localPort,
                        onValueChange = { localPort = it.filter(Char::isDigit).take(5) },
                        label = { Text("房间端口") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !busy
                    )
                    Text("房主监听的本地 TCP 端口。普通局域网直连无需修改，保持默认 45873 即可。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                }
            }
        }

        // Host in-room sharing controls
        if (room.role == TogetherRole.Host) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ResonanceColors.CanvasElevated,
                border = BorderStroke(1.dp, ResonanceColors.Primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Share, null, tint = ResonanceColors.Primary, modifier = Modifier.size(20.dp))
                        Text("邀请朋友", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Text("复制邀请发给朋友，朋友在应用中点击加入即可同步听歌。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)

                    if (room.addresses.size > 1) {
                        Text("选择网络连接入口：", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            room.addresses.forEach { candidate ->
                                FilterChip(
                                    selected = (address == candidate),
                                    onClick = { address = candidate },
                                    label = { Text(candidate) }
                                )
                            }
                        }
                    } else if (address.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("连接地址：$address : ${room.port}", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Button(
                        onClick = {
                            runCatching {
                                val inviteUrl = together.invitation(address, externalPort.toIntOrNull() ?: room.port)
                                val friendlyInvite = "🎵【Resonance 一起听】我发起了一起听，来跟我听同一首歌吧！打开 Resonance 即可自动加入：\n$inviteUrl"
                                clipboard.setText(AnnotatedString(friendlyInvite))
                                message = "邀请已复制！直接发送给微信/QQ好友即可。"
                            }.onFailure { message = "请检查连接地址和端口是否有效" }
                        },
                        enabled = address.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("复制加密邀请")
                    }

                    // Collapsible advanced host configuration
                    TextButton(onClick = { showAdvancedHost = !showAdvancedHost }) {
                        Icon(if (showAdvancedHost) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (showAdvancedHost) "收起异地/端口设置" else "异地跨网 / 端口映射设置")
                    }
                    if (showAdvancedHost) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it.take(253) },
                            label = { Text("邀请中的连接地址") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = externalPort,
                            onValueChange = { externalPort = it.filter(Char::isDigit).take(5) },
                            label = { Text("连接端口") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("同网使用内网地址；异地填写可直达的 IPv6 或已在路由器映射的外部端口（本机监听端口为 ${room.port}）。",
                            style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                    }
                }
            }
        }

        // Active session controls (both Host and Guest)
        if (active) {
            if (room.isGuest) {
                Text("歌曲接收完整后自动对齐进度。音频只作临时播放，不加入本机曲库。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
            }
            OutlinedButton(
                onClick = { together.leave(); invitation = ""; busy = false },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(if (room.role == TogetherRole.Host) "结束房间" else "退出一起听")
            }
        }

        // Clear Help & FAQ Guide
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ResonanceColors.CanvasElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable { showHelp = !showHelp },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = ResonanceColors.Muted, modifier = Modifier.size(20.dp))
                        Text("常见问题与连接指南", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    }
                    Icon(if (showHelp) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = ResonanceColors.Muted)
                }

                if (showHelp) {
                    Spacer(Modifier.height(4.dp))
                    TogetherFaqItem("💡 怎样最快和朋友连上？",
                        "两人连在同一个 Wi-Fi 下，或者一人开启手机热点、另一人连上热点。房主点「创建双人房间」，点「复制加密邀请」发到微信/QQ，朋友复制后打开本软件即可一键加入！无需任何网络配置。")
                    TogetherFaqItem("🌐 异地不在同一 Wi-Fi 能连吗？",
                        "可以。异地需要双方网络具备直达能力，例如移动数据网络下的公网 IPv6，或房主路由器配置了 TCP 端口映射。房主在高级设置中将连接地址换成公网 IP/域名即可。")
                    TogetherFaqItem("🔒 为什么不需要登录账号？",
                        "Resonance 采用点对点端到端加密（TLS）直连传输，音乐直接从房主手机/电脑发往朋友设备，不经过任何第三方服务器中转，充分保障两人的私密听歌体验。")
                    TogetherFaqItem("📦 朋友听完会占用存储吗？",
                        "不会。传输给朋友的音频仅保存在临时播放缓存中，单曲上限 256 MB，不会写入本地永久曲库，退出后自动释放。")
                }
            }
        }
    }
}

@Composable
private fun TogetherFaqItem(title: String, answer: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = ResonanceColors.TextPrimary)
        Text(answer, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
    }
}
