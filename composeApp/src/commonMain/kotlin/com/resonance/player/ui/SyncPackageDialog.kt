package com.resonance.player.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun SyncPackageDialog(exporting: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var passphrase by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val valid = passphrase.length >= 8
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exporting) "导出加密同步包" else "导入加密同步包") },
        text = {
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("同步口令（至少 8 个字符）") },
                supportingText = { Text("口令不会保存；另一台设备必须输入同一口令") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = androidx.compose.ui.Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(passphrase) }, enabled = valid) {
                Text(if (exporting) "选择保存位置" else "选择同步包")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
