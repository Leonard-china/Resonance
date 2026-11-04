package com.resonance.player.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.resonanceGlass
import com.resonance.player.design.resonanceSpring
import com.resonance.player.model.Track
import com.resonance.player.platform.*

/** Invitation actions live outside the scroll container, regardless of queue size. */
@Composable
internal fun TogetherRoomPage(
    compact: Boolean,
    scroll: ScrollState,
    onBack: () -> Unit,
    roomAction: @Composable () -> Unit,
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = if (compact) 20.dp else 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 1100.dp).fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccessibleIconButton("返回音乐库", onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Text("一起听", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                roomAction()
            }
            header()
        }
        Column(
            Modifier.widthIn(max = 1100.dp).fillMaxWidth().weight(1f).verticalScroll(scroll)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
internal fun TogetherRoomHero(room: TogetherState, track: Track?, playing: Boolean) {
    val active = room.role != TogetherRole.None
    val title = if (room.role == TogetherRole.Host) track?.title.orEmpty() else room.trackTitle.ifBlank { track?.title.orEmpty() }
    val artist = room.sharedQueue.firstOrNull { it.current }?.artist ?: track?.artist.orEmpty()
    val status = when (room.status) {
        TogetherStatus.Idle -> "把音乐分享给此刻想见的人"
        TogetherStatus.Error -> "连接未完成，可以重新尝试"
        TogetherStatus.Listening -> "房间已开启，等朋友加入"
        TogetherStatus.Connecting -> "正在连接朋友的房间…"
        TogetherStatus.AwaitingApproval -> if (room.role == TogetherRole.Host) "朋友想加入，等你确认" else "等待房主确认加入…"
        TogetherStatus.Preparing -> "正在准备播放器…"
        TogetherStatus.Reconnecting -> "正在重连（${room.reconnectAttempt}/3）"
        TogetherStatus.Receiving -> "正在接收歌曲 · ${room.receivedBytes / 1024} KB"
        TogetherStatus.Connected -> when {
            room.localAudioPaused -> "本机声音已暂停"
            room.role == TogetherRole.Host && !room.peerReady -> "朋友已连接，正在准备歌曲"
            playing -> "正在一起听"
            else -> "已连接，播放已暂停"
        }
    }
    Column(
        Modifier.fillMaxWidth().resonanceGlass(shape = RoundedCornerShape(24.dp), shadowElevation = 3.dp)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (active && track != null) {
                AlbumArtwork(track.artworkSeed, Modifier.size(80.dp), cornerRadius = 16.dp,
                    artworkPath = track.artworkPath, audioSource = track.sourceUri)
            } else {
                Box(Modifier.size(72.dp).background(ResonanceColors.PrimarySoft, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Headphones, null, Modifier.size(34.dp), tint = ResonanceColors.Primary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (active) title.ifBlank { "房间已准备好" } else "同一首歌，一起听",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (active) artist.ifBlank { "选首喜欢的歌，等朋友一起听" } else "创建房间，或加入朋友的邀请。",
                    style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Muted,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(7.dp).background(
                if (room.status == TogetherStatus.Error) MaterialTheme.colorScheme.error else ResonanceColors.Primary, CircleShape))
            Text(status, style = MaterialTheme.typography.bodyMedium)
        }
        if (room.message.isNotBlank()) Text(room.message, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
        if (active) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.People, null, Modifier.size(16.dp), tint = ResonanceColors.Muted)
                    Text("${room.memberCount} 人在房间", style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Muted)
                }
                if (room.verificationCode.isNotBlank()) Text("核验码 ${room.verificationCode}",
                    style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Muted)
            }
        }
    }
}

@Composable
internal fun TogetherSection(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = ResonanceColors.Glass) {
        Column(Modifier.fillMaxWidth().animateContentSize(resonanceSpring()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                }
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
internal fun TogetherQueueSummary(room: TogetherState, onOpen: () -> Unit, onAdd: () -> Unit) {
    val queue = room.sharedQueue
    val start = queue.indexOfFirst { it.current }.coerceAtLeast(0)
    TogetherSection(
        title = "共享队列",
        subtitle = if (queue.isEmpty()) "还没有歌曲，选一首开始吧" else "${queue.size} 首歌 · 和朋友接着听",
        action = { TextButton(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) { Text("查看全部") } },
    ) {
        // Only the current song and its next two neighbours are composed on the room page.
        queue.subList(start, minOf(start + 3, queue.size)).forEachIndexed { offset, item ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (item.current) ResonanceColors.PrimarySoft else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item.current) Icon(Icons.Default.GraphicEq, "当前歌曲", Modifier.size(20.dp), tint = ResonanceColors.Primary)
                else Text("${start + offset + 1}", Modifier.width(20.dp), style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Muted)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text(if (item.playable) item.artist else "歌曲传输中…", maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                }
            }
        }
        if (room.status == TogetherStatus.Connected && room.isMultiplayer) {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("贡献歌曲")
            }
        }
    }
}

@Composable
internal fun TogetherQueueDialog(room: TogetherState, service: ListenTogetherService, onDismiss: () -> Unit) {
    val list = rememberLazyListState(initialFirstVisibleItemIndex = room.sharedQueue.indexOfFirst { it.current }.coerceAtLeast(0))
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(16.dp).widthIn(max = 680.dp).fillMaxWidth().fillMaxHeight(.85f).resonanceGlass(shape = RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp), color = androidx.compose.ui.graphics.Color.Transparent) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("共享队列", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text("${room.sharedQueue.size} 首歌", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
                    }
                    AccessibleIconButton("关闭共享队列", onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }
                if (room.isGuest && !room.equalControls) Text("对方使用旧版，点歌需要对方确认。", style = MaterialTheme.typography.bodySmall)
                if (room.sharedQueue.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("队列还没有歌曲") }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(room.sharedQueue, key = { _, item -> item.id }) { index, item ->
                            TogetherQueueRow(item, index, room, service)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("返回房间") }
            }
        }
    }
}

@Composable
private fun TogetherQueueRow(item: TogetherQueueItem, index: Int, room: TogetherState, service: ListenTogetherService) {
    var menu by remember(item.id) { mutableStateOf(false) }
    val connected = room.status == TogetherStatus.Connected
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(if (item.current) ResonanceColors.PrimarySoft else Color.Transparent).padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (item.current) Icon(Icons.Default.GraphicEq, "当前歌曲", Modifier.size(20.dp), tint = ResonanceColors.Primary)
        else Text("${index + 1}", Modifier.widthIn(min = 20.dp), style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Muted)
        Column(Modifier.weight(1f).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOf(item.artist, item.sourceMemberName.takeIf { it.isNotBlank() }?.let { "由 $it 添加" },
                if (item.playable) null else "歌曲传输中").filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (!item.current) {
            IconButton(onClick = { service.requestControl(TogetherControl.Select, item.id) }, enabled = connected && item.playable) {
                Icon(Icons.Default.PlayArrow, "点歌 ${item.title}")
            }
        }
        if (room.multiplayer) Box {
            IconButton(onClick = { menu = true }, enabled = connected) { Icon(Icons.Default.MoreVert, "管理 ${item.title}") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("上移") }, enabled = connected && index > 0,
                    onClick = { menu = false; service.moveSharedTrack(item.id, index - 1) })
                DropdownMenuItem(text = { Text("下移") }, enabled = connected && index < room.sharedQueue.lastIndex,
                    onClick = { menu = false; service.moveSharedTrack(item.id, index + 1) })
                DropdownMenuItem(text = { Text("从房间队列移除") }, enabled = connected,
                    onClick = { menu = false; service.removeSharedTrack(item.id) })
            }
        }
    }
}
