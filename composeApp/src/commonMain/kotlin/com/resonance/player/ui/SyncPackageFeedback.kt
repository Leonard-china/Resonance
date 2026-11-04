package com.resonance.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.resonance.player.design.FrostedAlertDialog
import com.resonance.player.model.SyncReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal data class SyncPackageOutcome(val exporting: Boolean, val report: SyncReport)

@Composable
internal fun SyncPackageProgressDialog(exporting: Boolean, onDismiss: () -> Unit) {
    FrostedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exporting) "正在导出歌单" else "正在导入歌单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(if (exporting) "正在打包音频、封面和歌单。文件较大时需要一些时间，完成后会显示保存位置。"
                    else "正在校验歌单文件并导入音乐库，请稍候。")
                Text("可以继续使用应用，请保持应用运行。", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("继续使用应用") } },
    )
}

@Composable
internal fun SyncPackageResultDialog(
    outcome: SyncPackageOutcome,
    onDismiss: () -> Unit,
    onOpenLocation: suspend (String) -> Boolean,
) {
    val report = outcome.report
    val exported = report.exportedPackage.takeIf { report.success && outcome.exporting }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var copied by remember(outcome) { mutableStateOf(false) }
    var opening by remember(outcome) { mutableStateOf(false) }
    var openFailed by remember(outcome) { mutableStateOf(false) }
    FrostedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when {
            outcome.exporting && report.success -> "歌单导出完成"
            outcome.exporting -> "歌单未能导出"
            report.success -> "歌单导入完成"
            else -> "歌单未能导入"
        }) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (exported == null) {
                    Text(report.message)
                } else {
                    Text("文件已保存，可以在文件管理中找到或分享给其他设备。")
                    Text("保存位置", style = MaterialTheme.typography.labelLarge)
                    SelectionContainer { Text(exported.directory) }
                    Text("文件名", style = MaterialTheme.typography.labelLarge)
                    SelectionContainer { Text(exported.fileName) }
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(exported.directory))
                        copied = true
                    }) { Text(if (copied) "已复制保存位置" else "复制保存位置") }
                }
                if (openFailed) Text("无法打开文件管理器，请按上方保存位置手动查找。", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("完成") } },
        dismissButton = exported?.openLocationLabel?.let { label -> {
            TextButton(enabled = !opening, onClick = {
                opening = true
                scope.launch {
                    try {
                        openFailed = !onOpenLocation(exported.locationUri ?: exported.directory)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        openFailed = true
                    } finally { opening = false }
                }
            }) { Text(if (opening) "正在打开…" else label) }
        } },
    )
}
