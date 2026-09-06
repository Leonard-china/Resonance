package com.resonance.player.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.resonance.player.platform.ResonanceBackHandler

@Composable
fun PlaylistLinkDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var link by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val valid = link.contains("http://") || link.contains("https://")
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    ResonanceBackHandler(enabled = true, onBack = onDismiss)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入歌单或单曲") },
        text = {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it.take(800) },
                label = { Text("酷狗歌单 / 单曲链接或分享文本") },
                supportingText = { Text("支持短链（如 t1.kugou.com）、单曲链接或直接粘贴完整的分享口令文本。读取公开曲目元数据并自动匹配本机音乐。") },
                placeholder = { Text("粘贴链接或分享文案（如 https://t1.kugou.com/…）") },
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(link.trim()) }, enabled = valid) { Text("导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
