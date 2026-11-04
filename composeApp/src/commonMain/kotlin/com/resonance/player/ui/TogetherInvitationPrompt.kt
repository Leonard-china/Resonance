package com.resonance.player.ui

import com.resonance.player.design.FrostedAlertDialog as AlertDialog

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.resonance.player.platform.PlatformServices
import com.resonance.player.platform.TogetherRole
import kotlinx.coroutines.launch

/** External links may offer an invitation, but cannot start network/audio by themselves. */
@Composable
internal fun TogetherInvitationPrompt(services: PlatformServices, onOpenRoom: () -> Unit) {
    val pending by services.togetherInvitations.collectAsState()
    val together = services.listenTogether ?: return
    val scope = rememberCoroutineScope()
    val companion by together.companion.collectAsState()
    val displayedPartner = companion.incomingPartnerId
    if (companion.incomingPartnerId.isNotEmpty()) {
        AlertDialog(onDismissRequest = { together.respondPartnerInvitation(false, displayedPartner) },
            title = { Text("${companion.incomingPartnerName}邀你一起听") },
            text = { Text("已核验为配对过的伙伴设备。确认后加入对方的加密房间；若你正在等待朋友，将关闭当前等待房间。") },
            confirmButton = { TextButton(onClick = { together.respondPartnerInvitation(true, displayedPartner); onOpenRoom() }) { Text("接受邀请") } },
            dismissButton = { TextButton(onClick = { together.respondPartnerInvitation(false, displayedPartner) }) { Text("暂不") } })
        return
    }
    val value = pending ?: return
    val preview = remember(value) { together.inspectInvitation(value) }
    if (preview == null) {
        LaunchedEffect(value) { services.dismissTogetherInvitation() }
        return
    }
    val hasRoom = together.state.value.role != TogetherRole.None
    AlertDialog(
        onDismissRequest = services::dismissTogetherInvitation,
        title = { Text("加入朋友的一起听？") },
        text = { Text("连接地址：${preview.host}:${preview.port}\n请确认邀请来自你认识的朋友。${if (hasRoom) "加入后将离开当前房间。" else ""}") },
        confirmButton = { TextButton(onClick = {
            services.dismissTogetherInvitation()
            onOpenRoom()
            scope.launch { together.join(value) }
        }) { Text(if (hasRoom) "离开并加入" else "确认加入") } },
        dismissButton = { TextButton(onClick = services::dismissTogetherInvitation) { Text("取消") } },
    )
}
