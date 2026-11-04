package com.resonance.player.ui

import com.resonance.player.design.FrostedAlertDialog as AlertDialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    var confirmFriend by rememberSaveable { mutableStateOf(true) }
    var prefetchNext by rememberSaveable { mutableStateOf(false) }
    var showAddSongDialog by remember { mutableStateOf(false) }
    var pendingInvitation by remember { mutableStateOf<String?>(null) }
    val pageScroll = rememberScrollState()
    val remotePlayback by together.remotePlayback.collectAsState()
    var showQueue by remember { mutableStateOf(false) }
    var showAiQueue by remember { mutableStateOf(false) }
    var showMembers by remember { mutableStateOf(false) }
    var invitationMenu by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(2_000); copied = false }
    }
    var observedRole by remember { mutableStateOf(room.role) }
    var observedSession by remember { mutableStateOf(room.roomSessionId) }
    LaunchedEffect(room.role, room.roomSessionId) {
        if (observedRole != room.role || observedSession != room.roomSessionId) pageScroll.scrollTo(0)
        observedRole = room.role
        observedSession = room.roomSessionId
        showQueue = false
        showAddSongDialog = false
        invitationMenu = false
        showAiQueue = false
        showMembers = false
        showAdvancedHost = false
        copied = false
        message = ""
    }
    var qr by remember { mutableStateOf<TogetherQr?>(null) }
    var now by remember { mutableStateOf(feature.services.currentTimeMillis()) }
    LaunchedEffect(room.role, room.invitationExpiresAtMs) {
        qr = null
        while (room.role == TogetherRole.Host) {
            now = feature.services.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
    }
    fun offerJoin(value: String) {
        if (together.inspectInvitation(value) == null) message = "邀请格式无效，请复制完整邀请后重试"
        else pendingInvitation = value
    }
    fun friendlyInvitation(): String = "🎵【Resonance 一起听】来听同一首歌吧！打开邀请后确认加入：\n" +
        together.invitation(address, externalPort.toIntOrNull() ?: room.port, room.addresses)

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

    TogetherRoomPage(compact, pageScroll, onBack, roomAction = {
        if (room.role != TogetherRole.None) {
            TextButton(onClick = { together.leave(); invitation = ""; busy = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (room.role == TogetherRole.Host) "结束房间" else "退出一起听")
            }
        }
    }, header = {
        if (room.role == TogetherRole.Host) {
            val remaining = ((room.invitationExpiresAtMs - now) / 1_000).coerceAtLeast(0)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (remaining == 0L) {
                            together.renewInvitation()
                            message = "邀请已刷新，可以重新复制"
                        } else {
                            runCatching { clipboard.setText(AnnotatedString(friendlyInvitation())) }
                                .onSuccess { copied = true; message = "" }
                                .onFailure { message = "邀请未复制，请检查连接地址和端口" }
                        }
                    },
                    enabled = address.isNotBlank(),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Icon(if (copied) Icons.Default.Check else if (remaining == 0L) Icons.Default.Refresh else Icons.Default.ContentCopy,
                        null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (copied) "已复制邀请" else if (remaining == 0L) "刷新邀请" else "复制邀请链接")
                }
                IconButton(enabled = address.isNotBlank() && remaining > 0, onClick = {
                    runCatching { feature.services.shareTogetherInvitation(friendlyInvitation()) }.onSuccess { shared ->
                        if (!shared) {
                            runCatching { clipboard.setText(AnnotatedString(friendlyInvitation())) }
                                .onSuccess { copied = true; message = "邀请已复制，可粘贴发给朋友" }
                                .onFailure { message = "邀请未复制，请重试" }
                        }
                    }.onFailure { message = "无法分享邀请，请检查连接地址和端口" }
                }) { Icon(Icons.Default.Share, "分享邀请") }
                Box {
                    IconButton(onClick = { invitationMenu = true }) { Icon(Icons.Default.MoreHoriz, "邀请选项") }
                    DropdownMenu(expanded = invitationMenu, onDismissRequest = { invitationMenu = false }) {
                        DropdownMenuItem(text = { Text("邀请二维码") }, enabled = address.isNotBlank() && remaining > 0, onClick = {
                            invitationMenu = false
                            runCatching { together.invitationQr(friendlyInvitation()) }
                                .onSuccess { qr = it; if (it == null) message = "当前环境暂不支持邀请二维码" }
                                .onFailure { message = "无法生成二维码，请检查邀请地址" }
                        })
                        DropdownMenuItem(text = { Text("刷新邀请") }, onClick = {
                            invitationMenu = false; together.renewInvitation(); copied = false
                            message = "已刷新邀请，旧链接已失效，请重新分享"
                        })
                        DropdownMenuItem(text = { Text("连接详情") }, onClick = { invitationMenu = false; showAdvancedHost = true })
                    }
                }
            }
            Text(if (address.isBlank()) "正在准备邀请地址…" else if (remaining > 0)
                "邀请 ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')} 后过期 · 仅分享给朋友"
                else "邀请已过期，刷新后即可继续分享",
                style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp))
        }
        if (message.isNotBlank()) {
            Row(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
                Text(message, Modifier.weight(1f).padding(start = 4.dp), style = MaterialTheme.typography.bodySmall,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                AccessibleIconButton("关闭提示", onClick = { message = "" }) { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) }
            }
        }
    }) {
        TogetherRoomHero(room,
            track = if (room.role == TogetherRole.Host) feature.player.currentTrack else remotePlayback?.track,
            playing = if (room.role == TogetherRole.Host) feature.player.isPlaying else remotePlayback?.playing == true)

        if (room.role == TogetherRole.Host && room.pendingJoin) {
            val applicant = room.pendingJoinMemberName.ifBlank { "朋友" }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${applicant} 请求加入，确认是你邀请的朋友后，再允许加入。", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { together.approveJoin(true) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("同意加入") }
                    OutlinedButton(onClick = { together.approveJoin(false) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("拒绝") }
                }
            }
        }

        if (room.members.isNotEmpty() && room.memberCount > 1) {
            TogetherSection("房间成员", "${room.memberCount} 人一起听", action = {
                TextButton(onClick = { showMembers = !showMembers }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (showMembers) "收起" else "查看成员")
                }
            }) {
                if (showMembers) room.members.forEach { member ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(36.dp).background(ResonanceColors.PrimarySoft, CircleShape), contentAlignment = Alignment.Center) {
                            Text(member.name.take(1).ifBlank { "友" }, style = MaterialTheme.typography.labelLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.bodyMedium)
                            if (member.isCoordinator) Text("房间创建者", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted)
                        }
                        Text(if (member.ready) "已就绪" else "准备中…", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted)
                    }
                } else Text(room.members.joinToString("、") { it.name }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted)
            }
        }

        if (room.isGuest && room.localAudioPaused) {
            Text(room.audioPauseReason.ifBlank { "本机声音已暂停" }, style = MaterialTheme.typography.bodyMedium)
            Text("房间与聊天保持连接；恢复后对齐朋友当前进度。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
            Button(onClick = together::resumeGuestAudio, modifier = Modifier.heightIn(min = 48.dp)) { Text("恢复本机跟听") }
        }

        if (room.status == TogetherStatus.Connected && (room.isGuest || room.isMultiplayer)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { together.requestControl(TogetherControl.Pause) }) { Text(if (room.equalControls) "暂停" else "申请暂停") }
                OutlinedButton(onClick = { together.requestControl(TogetherControl.Play) }) { Text(if (room.equalControls) "继续" else "申请继续") }
                OutlinedButton(onClick = { together.requestControl(TogetherControl.Next) }) { Text(if (room.equalControls) "下一首" else "申请下一首") }
                if (room.equalControls) OutlinedButton(onClick = { together.requestControl(TogetherControl.Previous) }) { Text("上一首") }
            }
        }

        if (showAddSongDialog) {
            AlertDialog(
                onDismissRequest = { showAddSongDialog = false },
                title = { Text("贡献歌曲到共享队列") },
                text = {
                    val tracks = feature.library.take(30)
                    if (tracks.isEmpty()) {
                        Text("本地曲库暂无可贡献的歌曲")
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(tracks) { t ->
                                ListItem(
                                    headlineContent = { Text(t.title) },
                                    supportingContent = { Text(t.artist) },
                                    modifier = Modifier.clickable {
                                        together.appendSharedTrack(t)
                                        showAddSongDialog = false
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddSongDialog = false }) { Text("取消") }
                }
            )
        }

        room.requests.forEach { request ->
            val label = when (request.action) {
                TogetherControl.Play -> "继续播放"
                TogetherControl.Pause -> "暂停播放"
                TogetherControl.Next -> "播放下一首"
                TogetherControl.Select -> feature.library.firstOrNull { it.id == request.trackId }?.title ?: "播放队列歌曲"
                TogetherControl.Previous -> "播放上一首"
                TogetherControl.Seek -> "调整播放进度"
                TogetherControl.Append -> "添加歌曲"
                TogetherControl.Remove -> "移除歌曲"
                TogetherControl.Move -> "调整队列顺序"
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("朋友请求：$label", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { together.resolveRequest(request.id, true) }) { Text("接受") }
                    TextButton(onClick = { together.resolveRequest(request.id, false) }) { Text("暂不接受") }
                }
            }
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
                            Text("查看地址并确认加入", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                        }
                        Button(
                            onClick = {
                                val clip = clipboardInvite.orEmpty()
                                invitation = clip
                                joining = true
                                offerJoin(clip)
                            },
                            enabled = !busy,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("查看邀请")
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
                            offerJoin(clip)
                        } else {
                            message = "剪贴板为空，请先复制朋友发来的完整邀请"
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("从剪贴板查看邀请")
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
                Text("复制朋友发来的消息并粘贴在此，即可查看并确认加入。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)

                Button(
                    onClick = {
                        offerJoin(invitation.trim())
                    },
                    enabled = !busy && invitation.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Link, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("加密连接并跟听")
                }
            } else {
                Button(
                    onClick = {
                        run {
                            together.requireJoinApproval(confirmFriend)
                            together.enablePrefetch(prefetchNext && feature.services.automaticRecommendationAllowed(true))
                            together.host(localPort.toIntOrNull()?.takeIf { it in 1..65535 } ?: 45873)
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(if (busy) "正在创建…" else "创建房间", style = MaterialTheme.typography.titleSmall)
                }
                TextButton(onClick = { showAdvancedPort = !showAdvancedPort }) {
                    Text(if (showAdvancedPort) "收起房间选项" else "房间选项")
                }
                if (showAdvancedPort) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("加入前由我确认", style = MaterialTheme.typography.bodyLarge)
                        Text("邀请仅分享给朋友", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                    }
                    Switch(checked = confirmFriend, onCheckedChange = { confirmFriend = it }, modifier = Modifier.semantics { contentDescription = "加入前由我确认" })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("提前预备下一首", style = MaterialTheme.typography.bodyLarge)
                        Text("仅非计费网络，减少换歌等待", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                    }
                    Switch(checked = prefetchNext, onCheckedChange = { prefetchNext = it }, modifier = Modifier.semantics { contentDescription = "提前预备下一首" })
                }

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

        if (room.sharedQueue.isNotEmpty() || room.status == TogetherStatus.Connected || room.role == TogetherRole.Host) {
            TogetherQueueSummary(room, onOpen = { showQueue = true }, onAdd = { showAddSongDialog = true })
        }
        if (room.role == TogetherRole.Host) {
            TogetherSection("一起选下一首", "让 AI 从你的曲库里挑选，确认后加入队列", action = {
                TextButton(onClick = { showAiQueue = !showAiQueue }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (showAiQueue) "收起" else "选歌")
                }
            }) {
                if (showAiQueue) TogetherAiQueue(feature, room)
            }
        }

        val notificationsBlocked = feature.services.togetherNotificationsBlocked?.collectAsState()?.value ?: false
        TogetherCompanionPanel(together, notificationsBlocked, feature.services::requestTogetherNotifications)

        // Active session controls (both Host and Guest)
        if (active) {
            if (room.isGuest) {
                Text("歌曲接收完整后自动对齐进度。音频只作临时播放，不加入本机曲库。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
            }
        }

        // Clear Help & FAQ Guide
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ResonanceColors.Glass,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { showHelp = !showHelp },
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
                    TogetherFaqItem("怎样最快和朋友连上？",
                        "建议两人连接同一个 Wi-Fi，或一人开热点让另一人连接。房主创建房间后分享加密邀请，朋友打开软件粘贴并确认加入。若网络隔离设备或防火墙阻止连接，仍需调整网络。")
                    TogetherFaqItem("不在同一 Wi-Fi，也能一起听吗？",
                        "默认通过 Resonance 服务器连接，音频和聊天由服务器加密转发，无需配置 IPv6 或路由器端口。服务繁忙或网络受限时请稍后重试。")
                    TogetherFaqItem("为什么不需要登录账号？",
                        "邀请包含临时房间凭据，音频和聊天通过双方设备间的加密连接传输。可选中继只转发密文，但服务运营者能看到连接元数据；请只向信任的朋友分享邀请。")
                    TogetherFaqItem("听完会占用存储吗？",
                        "播放时会使用临时存储。传输给朋友的音频仅保存在临时播放缓存中，单曲上限 256 MB，不会写入本地永久曲库，退出后自动释放。")
                }
            }
        }
    }
    if (showQueue && room.role != TogetherRole.None) {
        TogetherQueueDialog(room, together, onDismiss = { showQueue = false })
    }
    if (showAdvancedHost && room.role == TogetherRole.Host) {
        AlertDialog(onDismissRequest = { showAdvancedHost = false }, title = { Text("连接详情") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (room.viaRelay) "当前通过服务器连接，传输内容加密。" else "配置服务器的房间统一通过服务器连接；旧的本地邀请仍可使用直连。",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("邀请已包含可用的连接地址。只有需要端口映射等特殊配置时，才需要修改以下设置。",
                        style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                        if (room.addresses.size > 1) {
                            Text("指定默认主地址（可选，已默认自动包含全部可用通道）：", style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                room.addresses.forEach { candidate ->
                                    val label = when {
                                        candidate.contains(":") -> "公网 IPv6"
                                        candidate.startsWith("192.168.43.") -> "热点"
                                        else -> "局域网 IPv4"
                                    }
                                    FilterChip(
                                        selected = (address == candidate),
                                        onClick = { address = candidate },
                                        label = { Text("$label: $candidate") }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it.take(253) },
                            label = { Text("邀请中的主连接地址") },
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
            },
            confirmButton = { TextButton(onClick = { showAdvancedHost = false }) { Text("完成") } })
    }
    pendingInvitation?.let { value ->
        val preview = together.inspectInvitation(value)
        val candidateCount = preview?.candidates?.size ?: 0
        val networkHint = if (preview?.relayHost.isNullOrEmpty() && candidateCount > 0) "已包含 $candidateCount 个候选直连地址，将并行尝试并选择首先可用的连接。\n" else ""
        val relayHint = preview?.relayHost?.takeIf { it.isNotEmpty() }?.let { "将通过服务器 $it 连接，不尝试设备直连；服务方可看到连接元数据。\n" }.orEmpty()
        AlertDialog(onDismissRequest = { pendingInvitation = null }, title = { Text("确认加入朋友？") },
            text = { Text((if (preview?.relayHost.isNullOrEmpty()) "连接地址：${preview?.host}:${preview?.port}\n" else "") + "${networkHint}${relayHint}请确认邀请来自你认识的朋友。加入后会暂停本地播放。") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                pendingInvitation = null
                run { together.join(value); invitation = ""; clipboardInvite = null }
            }) { Text("确认加入") } },
            dismissButton = { TextButton(onClick = { pendingInvitation = null }) { Text("取消") } })
    }
    qr?.let { code ->
        AlertDialog(onDismissRequest = { qr = null }, title = { Text("邀请朋友一起听") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Canvas(Modifier.fillMaxWidth().aspectRatio(1f).semantics { contentDescription = "一起听邀请二维码" }) {
                    drawRect(Color.White)
                    val cell = size.width / code.size
                    code.bits.forEachIndexed { index, black -> if (black) drawRect(Color.Black,
                        Offset((index % code.size) * cell, (index / code.size) * cell), Size(cell + .1f, cell + .1f)) }
                }
                Text("二维码包含临时房间凭据，仅向朋友展示。可用支持链接的扫码工具打开；也可复制邀请在 Resonance 内粘贴。", style = MaterialTheme.typography.bodySmall)
            } }, confirmButton = { TextButton(onClick = { qr = null }) { Text("完成") } })
    }
}

@Composable
private fun TogetherFaqItem(title: String, answer: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = ResonanceColors.TextPrimary)
        Text(answer, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
    }
}
