package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonance.player.design.FrostedAlertDialog
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceShapes
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
                Text(
                    if (exporting) "正在打包音频、封面和歌单。文件较大时需要一些时间，完成后会显示保存位置与分享入口。"
                    else "正在校验歌单文件并导入音乐库，请稍候。"
                )
                Text("可以继续使用应用，请保持应用运行。", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Muted)
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
    onShareExported: (suspend (String) -> Boolean)? = null,
) {
    val report = outcome.report
    val exported = report.exportedPackage.takeIf { report.success && outcome.exporting }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var copied by remember(outcome) { mutableStateOf(false) }
    var opening by remember(outcome) { mutableStateOf(false) }
    var openFailed by remember(outcome) { mutableStateOf(false) }
    var sharing by remember(outcome) { mutableStateOf(false) }
    var shareFailed by remember(outcome) { mutableStateOf(false) }
    FrostedAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    outcome.exporting && report.success -> "歌单导出完成"
                    outcome.exporting -> "歌单未能导出"
                    report.success -> "歌单导入完成"
                    else -> "歌单未能导入"
                }
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (report.success && (report.trackCount > 0 || report.playlistCount > 0)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ResonanceColors.PrimarySoft)
                            .border(1.dp, ResonanceColors.Divider, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            if (outcome.exporting) "已打包 ${report.trackCount} 首歌曲 · ${report.playlistCount} 个歌单"
                            else "已合并 ${report.trackCount} 首歌曲 · ${report.playlistCount} 个歌单",
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            color = ResonanceColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (exported == null) {
                    Text(report.message)
                } else {
                    Text("文件已保存，可直接发送给另一台设备，或在「歌单导入与导出」页一键管理。")
                    Text("保存位置", style = MaterialTheme.typography.labelLarge)
                    SelectionContainer { Text(exported.directory, style = MaterialTheme.typography.bodyMedium) }
                    Text("文件名", style = MaterialTheme.typography.labelLarge)
                    SelectionContainer { Text(exported.fileName, style = MaterialTheme.typography.bodyMedium) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (exported.shareUri != null && onShareExported != null) {
                            FilledTonalButton(
                                enabled = !sharing,
                                shape = ResonanceShapes.Button,
                                onClick = {
                                    sharing = true
                                    shareFailed = false
                                    scope.launch {
                                        try {
                                            shareFailed = !onShareExported(exported.shareUri)
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            shareFailed = true
                                        } finally {
                                            sharing = false
                                        }
                                    }
                                },
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (sharing) "正在调起分享…" else "分享文件")
                            }
                        }
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(exported.directory))
                            copied = true
                        }) { Text(if (copied) "已复制保存位置" else "复制保存位置") }
                    }
                }
                if (shareFailed) Text("无法调起系统分享，请点击打开目录后手动发送。", color = ResonanceColors.Danger, style = MaterialTheme.typography.bodySmall)
                if (openFailed) Text("无法打开文件管理器，请按上方保存位置手动查找。", color = ResonanceColors.Danger, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("完成") } },
        dismissButton = exported?.openLocationLabel?.let { label ->
            {
                TextButton(enabled = !opening, onClick = {
                    opening = true
                    scope.launch {
                        try {
                            openFailed = !onOpenLocation(exported.locationUri ?: exported.directory)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            openFailed = true
                        } finally {
                            opening = false
                        }
                    }
                }) { Text(if (opening) "正在打开…" else label) }
            }
        },
    )
}
