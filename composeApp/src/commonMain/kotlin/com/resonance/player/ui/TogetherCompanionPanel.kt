package com.resonance.player.ui

import com.resonance.player.design.FrostedAlertDialog as AlertDialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.resonance.player.platform.ListenTogetherService
import com.resonance.player.platform.TogetherDefaults
import com.resonance.player.design.ResonanceColors
import kotlinx.coroutines.launch

@Composable
internal fun TogetherCompanionPanel(service: ListenTogetherService, notificationsBlocked: Boolean = false, requestNotifications: () -> Unit = {}) {
    val state by service.companion.collectAsState()
    val displayedTarget = state.conversationId.ifEmpty { state.peerId }
    // Private draft content is intentionally not placed in Android SavedState.
    var draft by remember(displayedTarget) { mutableStateOf("") }
    var clearingPeer by remember { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }
    var showNetwork by remember { mutableStateOf(false) }
    var showRendezvous by remember { mutableStateOf(false) }
    var rendezvousDraft by remember(state.rendezvousUrl) { mutableStateOf(state.rendezvousUrl) }
    var showRelay by remember { mutableStateOf(false) }
    var relayDraft by remember(state.relayUrl) { mutableStateOf(state.relayUrl) }
    // The relay secret is deliberately transient and is never placed in saved UI state.
    var relaySecret by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    val following by remember { derivedStateOf { !list.canScrollForward } }
    val latestVisible = state.messages.isNotEmpty() && list.layoutInfo.visibleItemsInfo.lastOrNull()?.index == state.messages.lastIndex
    var previousCount by remember(displayedTarget) { mutableIntStateOf(0) }
    LaunchedEffect(displayedTarget, state.messages.lastOrNull(), following, latestVisible) {
        if (state.messages.isNotEmpty() && (following || previousCount == 0)) list.scrollToItem(state.messages.lastIndex)
        if (latestVisible) service.markChatRead(displayedTarget)
        previousCount = state.messages.size
    }
    if (!state.available) {
        if (state.message.isNotEmpty()) Text(state.message, color = MaterialTheme.colorScheme.error)
        return
    }
    if (displayedTarget.isNotEmpty()) {
        Surface(shape = RoundedCornerShape(24.dp), color = ResonanceColors.Glass) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (state.members.size > 2) "房间聊天 · ${state.members.size} 人" else "和${state.peerName}聊聊", style = MaterialTheme.typography.titleMedium)
                if (state.unreadCount > 0 && !following) TextButton(onClick = {
                    scope.launch {
                        if (state.messages.isNotEmpty()) list.scrollToItem(state.messages.lastIndex)
                        service.markChatRead(displayedTarget)
                    }
                }) { Text("${state.unreadCount} 条新消息 · 跳到最新") }
                Text(if (state.connected) "房间连接加密 · 消息由协调设备转发，记录保存在各自设备" else "连接已断开 · 记录保存在本机，群聊消息可能需要重新发送",
                    style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                if (notificationsBlocked) {
                    Text("系统通知未开启，离开此页面后可能错过消息提醒。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                    TextButton(onClick = requestNotifications, modifier = Modifier.heightIn(min = 48.dp)) { Text("开启消息提醒") }
                }
                if (state.messages.isEmpty()) {
                    Text("一边听歌，一边说说今天。", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp), state = list,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.messages, key = { "${it.outgoing}:${it.sequence}:${it.senderId}" }) { message ->
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.outgoing) Alignment.End else Alignment.Start) {
                                if (!message.outgoing && message.senderName.isNotEmpty() && state.members.size > 2) {
                                    Text(message.senderName, style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                                }
                                Surface(shape = RoundedCornerShape(16.dp), color = if (message.outgoing) MaterialTheme.colorScheme.primaryContainer else ResonanceColors.GlassLight) {
                                    Text(message.text, Modifier.padding(12.dp).widthIn(max = 480.dp),
                                        color = if (message.outgoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                }
                                if (message.outgoing) Text(if (message.recipientCount > 0 && message.delivered) "发送时的成员均已接收"
                                    else if (message.recipientCount > 0) "等待 ${message.pendingRecipients.size} 位朋友接收"
                                    else if (message.delivered && state.conversationId.length == 32) "协调设备已接收" else if (message.delivered) "已送达" else "待发送 / 等待送达确认",
                                    style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Muted)
                            }
                        }
                    }
                }
                OutlinedTextField(value = draft, onValueChange = { if (it.length <= 2000) draft = it },
                    label = { Text("发给朋友") }, modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 4,
                    supportingText = { Text("${draft.length} / 2000") })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val submitted = draft
                        sending = true
                        scope.launch { try { if (service.sendChat(submitted, displayedTarget) && draft == submitted) draft = "" } finally { sending = false } }
                    }, enabled = draft.isNotBlank() && !sending, modifier = Modifier.heightIn(min = 48.dp)) { Text("发送") }
                    TextButton(onClick = { clearingPeer = displayedTarget }, modifier = Modifier.heightIn(min = 48.dp)) { Text("清空本机记录") }
                    if (!state.paired && state.connected && state.members.size <= 2) OutlinedButton(onClick = { service.requestPairing(state.peerId) },
                        enabled = !state.pairingRequested, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (state.pairingRequested) "等待朋友确认" else "记为常听伙伴")
                    }
                    if (state.paired && state.members.size <= 2) Text("已记为常听伙伴", Modifier.padding(vertical = 14.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    if (state.partners.isNotEmpty()) {
        Text("常听伙伴", style = MaterialTheme.typography.titleMedium)
        state.partners.forEach { partner ->
            ListItem(headlineContent = { Text(partner.name) },
                supportingContent = { Text("已记住这台设备 · ${partner.id.take(8)}") },
                trailingContent = {
                    FlowRow {
                        TextButton(onClick = { service.invitePartner(partner.id) }, enabled = !state.connected,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("邀请一起听") }
                        TextButton(onClick = { removing = partner.id }, modifier = Modifier.heightIn(min = 48.dp)) { Text("移除") }
                    }
                })
        }
        Text("双方需打开 Resonance 且网络可直达。连接地址变化时可发送新链接重新连接，已配对身份会保留。",
            style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
    }
    if (state.message.isNotEmpty()) Text(state.message, style = MaterialTheme.typography.bodySmall)
    Text(if (state.relayUrl == TogetherDefaults.RelayUrl) "默认连接：Resonance 服务器" else if (state.relayConfigured) "已使用自定义连接服务" else "当前仅使用设备直连",
        style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
    TextButton(onClick = { showNetwork = !showNetwork; if (!showNetwork) relaySecret = "" }) {
        Text(if (showNetwork) "收起连接设置" else "连接设置")
    }
    if (showNetwork) {
    TextButton(onClick = { service.configureRelay(TogetherDefaults.RelayUrl, ""); relayDraft = TogetherDefaults.RelayUrl; relaySecret = "" }) { Text("使用 Resonance 服务器") }
    TextButton(onClick = { showRendezvous = !showRendezvous }, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(if (showRendezvous) "收起伙伴寻址设置" else "可选：自动寻找伙伴的新地址")
    }
    if (showRendezvous) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("双方配置同一个自托管 HTTPS 寻址服务，可在网络地址变化后寻找伙伴。它只登记地址与设备身份；内容始终端到端加密，若另启用中继可能经过中继，不能保证任意网络接通。", style = MaterialTheme.typography.bodySmall)
            Text("服务运营者可看到设备标识、来源 IP 和登记的伙伴关系。默认关闭；关闭应用后无法接收直邀。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
            OutlinedTextField(value = rendezvousDraft, onValueChange = { if (it.length <= 512) rendezvousDraft = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("寻址服务 HTTPS 地址") }, singleLine = true)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { service.configureRendezvous(rendezvousDraft) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("保存寻址设置") }
                TextButton(onClick = { service.configureRendezvous(""); rendezvousDraft = "" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("关闭寻址服务") }
            }
            if (state.rendezvousStatus.isNotEmpty()) Text(state.rendezvousStatus, style = MaterialTheme.typography.bodySmall)
        }
    }
    TextButton(onClick = { showRelay = !showRelay; if (!showRelay) relaySecret = "" }, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(if (showRelay) "收起跨网中继设置" else "自定义服务器")
    }
    if (showRelay) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("配置服务器后，房间连接、音频和聊天统一经服务器转发，不再尝试设备直连。内容保持加密；服务方可看到地址、连接时长和流量。", style = MaterialTheme.typography.bodySmall)
            Text("默认使用 Resonance 服务器。仅在需要自托管时修改，双方需使用支持此连接的版本。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
            OutlinedTextField(value = relayDraft, onValueChange = { if (it.length <= 512) relayDraft = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("服务器地址（WSS / HTTPS）") }, singleLine = true)
            if (!relayDraft.startsWith("wss://", ignoreCase = true)) OutlinedTextField(value = relaySecret, onValueChange = { if (it.length <= 43) relaySecret = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("中继访问密钥") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation())
            Text(if (state.relayConfigured) "已保存连接服务，下次创建房间生效。" else "HTTPS 私有中继需要访问密钥；WSS 服务无需填写密钥。", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { service.configureRelay(relayDraft, relaySecret); relaySecret = "" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("保存中继设置") }
                TextButton(onClick = { service.configureRelay("", ""); relayDraft = ""; relaySecret = "" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("关闭中继") }
            }
        }
    }
    }
    if (state.pairingIncoming) AlertDialog(onDismissRequest = { service.respondPairing(false) },
        title = { Text("成为常听伙伴？") }, text = { Text("记住对方的设备身份，方便下次一起听。更换设备需要重新配对；不会自动加入房间。") },
        confirmButton = { TextButton(onClick = { service.respondPairing(true) }) { Text("同意并记住") } },
        dismissButton = { TextButton(onClick = { service.respondPairing(false) }) { Text("暂不") } })
    if (clearingPeer != null) AlertDialog(onDismissRequest = { clearingPeer = null }, title = { Text("清空本机聊天记录？") },
        text = { Text("也会取消本机尚未发送的消息。朋友设备上的记录不受影响；此操作无法撤销。") },
        confirmButton = { TextButton(onClick = { clearingPeer?.let(service::clearChat); clearingPeer = null }) { Text("清空本机记录") } },
        dismissButton = { TextButton(onClick = { clearingPeer = null }) { Text("取消") } })
    if (removing != null) AlertDialog(onDismissRequest = { removing = null }, title = { Text("移除常听伙伴？") },
        text = { Text("只从本机移除，保留聊天记录。以后可重新发邀请配对。") },
        confirmButton = { TextButton(onClick = { removing?.let(service::removePartner); removing = null }) { Text("移除") } },
        dismissButton = { TextButton(onClick = { removing = null }) { Text("取消") } })
}
