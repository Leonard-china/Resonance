package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resonance.player.design.FrostedAlertDialog
import com.resonance.player.model.AudioDeletionRequest

@Composable
internal fun AssistantAudioDeletionDialog(request: AudioDeletionRequest, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    FrostedAlertDialog(onDismissRequest = onDismiss,
        title = { Text("删除助手选中的 ${request.tracks.size} 首音频？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("以下具体文件将永久删除，并从音乐库和全部歌单移除。此操作不可撤销。")
                LazyColumn(Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(request.tracks, key = { it.id }) { track ->
                        Column {
                            Text(track.title + " · " + track.artist)
                            Text(track.sourceUri?.substringAfterLast('/') ?: "离线曲库记录", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确认删除这些文件") } })
}

@Composable
internal fun AssistantDeletionPrompt(controller: MusicFeatureController) {
    // Observe within this child scope; chat progress need not recompose the whole application.
    val state = controller.assistant?.state?.collectAsState()?.value ?: return
    val request = state.pendingAudioDeletion?.takeIf { !state.task.running } ?: return
    AssistantAudioDeletionDialog(request,
        onDismiss = { controller.launch { controller.assistant?.confirmAudioDeletion(request.id, false) } },
        onConfirm = { controller.launch {
            try { controller.assistant?.confirmAudioDeletion(request.id, true) }
            finally { controller.onReload() }
        } })
}
