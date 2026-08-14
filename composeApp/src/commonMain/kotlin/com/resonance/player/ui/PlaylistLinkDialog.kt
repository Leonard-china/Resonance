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

@Composable
fun PlaylistLinkDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var link by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val valid = link.trim().startsWith("https://") || link.trim().startsWith("http://")
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入公开歌单") },
        text = {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it.take(500) },
                label = { Text("酷狗歌单链接") },
                supportingText = { Text("读取公开曲目目录并匹配本机音乐。酷狗没有向本应用开放会员 KGMA 下载接口，因此不会绕过账号或版权限制下载音频。") },
                placeholder = { Text("https://t1.kugou.com/…") },
                singleLine = false,
                minLines = 2,
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(link.trim()) }, enabled = valid) { Text("读取歌单") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
