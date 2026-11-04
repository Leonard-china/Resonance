package com.resonance.player.ui

import com.resonance.player.design.FrostedAlertDialog as AlertDialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Visible above the full player and every destination; no success state replaces a failure. */
@Composable
internal fun PlaybackErrorDialog(detail: String, onDismiss: () -> Unit, onRetry: (() -> Unit)?, title: String = "播放未能继续") {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(detail.ifBlank { "音频暂时无法读取，请检查文件或网络后重试。" })
        } },
        confirmButton = { if (onRetry != null) TextButton(onClick = onRetry) { Text("重试播放") }
            else TextButton(onClick = onDismiss) { Text("知道了") } },
        dismissButton = { if (onRetry != null) TextButton(onClick = onDismiss) { Text("关闭") } })
}
