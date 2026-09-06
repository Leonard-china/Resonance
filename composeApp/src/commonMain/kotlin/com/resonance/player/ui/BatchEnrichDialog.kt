package com.resonance.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceShapes
import com.resonance.player.model.BatchEnrichOptions
import com.resonance.player.model.BatchEnrichProgress
import com.resonance.player.model.BatchEnrichReport
import com.resonance.player.model.Track

@Composable
fun BatchEnrichDialog(
    selectedTracks: List<Track>,
    isRunning: Boolean,
    progress: BatchEnrichProgress?,
    report: BatchEnrichReport?,
    hasDeepSeekConfigured: Boolean,
    onDismiss: () -> Unit,
    onStartEnrich: (BatchEnrichOptions) -> Unit,
    onCancel: () -> Unit,
) {
    var enrichCover by remember { mutableStateOf(true) }
    var enrichLyrics by remember { mutableStateOf(true) }
    var enrichArtistAndAlbum by remember { mutableStateOf(true) }
    var useAiFallback by remember { mutableStateOf(hasDeepSeekConfigured) }
    var forceRefresh by remember { mutableStateOf(false) }

    val missingCoverCount = remember(selectedTracks) { selectedTracks.count { it.isMissingCover } }
    val missingArtistCount = remember(selectedTracks) { selectedTracks.count { it.isMissingArtist } }
    val missingAlbumCount = remember(selectedTracks) { selectedTracks.count { it.isMissingAlbum } }

    Dialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(18.dp)),
            color = ResonanceColors.CanvasElevated,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                if (report != null) {
                    // 完成状态
                    EnrichCompletedView(report = report, onFinish = onDismiss)
                } else if (isRunning) {
                    // 运行中进度状态
                    EnrichRunningView(
                        progress = progress,
                        totalCount = selectedTracks.size,
                        onCancel = onCancel,
                    )
                } else {
                    // 初始配置状态
                    EnrichConfigView(
                        totalTracks = selectedTracks.size,
                        missingCoverCount = missingCoverCount,
                        missingArtistCount = missingArtistCount,
                        missingAlbumCount = missingAlbumCount,
                        enrichCover = enrichCover,
                        onEnrichCoverChange = { enrichCover = it },
                        enrichLyrics = enrichLyrics,
                        onEnrichLyricsChange = { enrichLyrics = it },
                        enrichArtistAndAlbum = enrichArtistAndAlbum,
                        onEnrichArtistAndAlbumChange = { enrichArtistAndAlbum = it },
                        useAiFallback = useAiFallback,
                        onUseAiFallbackChange = { useAiFallback = it },
                        forceRefresh = forceRefresh,
                        onForceRefreshChange = { forceRefresh = it },
                        hasDeepSeekConfigured = hasDeepSeekConfigured,
                        onDismiss = onDismiss,
                        onStart = {
                            onStartEnrich(
                                BatchEnrichOptions(
                                    enrichCover = enrichCover,
                                    enrichLyrics = enrichLyrics,
                                    enrichArtistAndAlbum = enrichArtistAndAlbum,
                                    useAiFallback = useAiFallback && hasDeepSeekConfigured,
                                    forceRefresh = forceRefresh,
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EnrichConfigView(
    totalTracks: Int,
    missingCoverCount: Int,
    missingArtistCount: Int,
    missingAlbumCount: Int,
    enrichCover: Boolean,
    onEnrichCoverChange: (Boolean) -> Unit,
    enrichLyrics: Boolean,
    onEnrichLyricsChange: (Boolean) -> Unit,
    enrichArtistAndAlbum: Boolean,
    onEnrichArtistAndAlbumChange: (Boolean) -> Unit,
    useAiFallback: Boolean,
    onUseAiFallbackChange: (Boolean) -> Unit,
    forceRefresh: Boolean,
    onForceRefreshChange: (Boolean) -> Unit,
    hasDeepSeekConfigured: Boolean,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ResonanceColors.CoralSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ResonanceColors.Coral, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("智能补全与 AI 检索", style = MaterialTheme.typography.titleLarge, color = ResonanceColors.Ivory, fontWeight = FontWeight.Bold)
            Text("已选 $totalTracks 首歌曲 · 节省 Token 优化设计", style = MaterialTheme.typography.bodySmall, color = ResonanceColors.Dim)
        }
    }

    Spacer(Modifier.height(14.dp))

    // 缺失统计小卡片
    Surface(
        color = ResonanceColors.Soft,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            StatBadge("缺封面", "$missingCoverCount 首", Icons.Default.Image)
            StatBadge("缺歌手/专辑", "${maxOf(missingArtistCount, missingAlbumCount)} 首", Icons.Default.Person)
            StatBadge("歌词库检索", "$totalTracks 首", Icons.Default.GraphicEq)
        }
    }

    Spacer(Modifier.height(14.dp))
    Text("选择补全项", style = MaterialTheme.typography.labelMedium, color = ResonanceColors.Dim)
    Spacer(Modifier.height(6.dp))

    EnrichOptionItem(
        checked = enrichCover,
        onCheckedChange = onEnrichCoverChange,
        title = "补全专辑封面",
        description = "从 iTunes / 酷狗官方高清库下载并保存本地",
    )

    EnrichOptionItem(
        checked = enrichLyrics,
        onCheckedChange = onEnrichLyricsChange,
        title = "匹配 / 检索同步歌词",
        description = "自动扫描本地与 LRCLIB 精准 LRC 同步歌词",
    )

    EnrichOptionItem(
        checked = enrichArtistAndAlbum,
        onCheckedChange = onEnrichArtistAndAlbumChange,
        title = "规范歌曲与歌手名",
        description = "清除文件名冗余后缀，修正未知歌手与专辑名",
    )

    HorizontalDivider(color = ResonanceColors.Divider, thickness = Dp.Hairline, modifier = Modifier.padding(vertical = 8.dp))

    EnrichOptionItem(
        checked = useAiFallback,
        onCheckedChange = onUseAiFallbackChange,
        title = "启用 DeepSeek AI 智能兜底",
        description = if (hasDeepSeekConfigured) "免费源未命中时才调用 AI，最大限度节省 Token；关闭则 100% 纯免费运行" else "未配置 DeepSeek Key，将仅使用免费官方源（0 Token）",
        enabled = hasDeepSeekConfigured,
    )

    EnrichOptionItem(
        checked = forceRefresh,
        onCheckedChange = onForceRefreshChange,
        title = "强制覆盖已有信息",
        description = "默认跳过已完整的歌曲以节省网络与 Token",
    )

    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDismiss) {
            Text("取消", color = ResonanceColors.Muted)
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onStart,
            shape = ResonanceShapes.Button,
            colors = ButtonDefaults.buttonColors(
                containerColor = ResonanceColors.Coral,
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("开始补全", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EnrichRunningView(
    progress: BatchEnrichProgress?,
    totalCount: Int,
    onCancel: () -> Unit,
) {
    val current = progress?.current ?: 0
    val total = if (progress != null && progress.total > 0) progress.total else totalCount
    val fraction = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            progress = { fraction },
            color = ResonanceColors.Coral,
            trackColor = ResonanceColors.Soft,
            modifier = Modifier.size(54.dp),
            strokeWidth = 4.dp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "正在智能处理 ($current / $total)",
            style = MaterialTheme.typography.titleLarge,
            color = ResonanceColors.Ivory,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            progress?.currentTrack?.title ?: "准备中…",
            style = MaterialTheme.typography.bodyMedium,
            color = ResonanceColors.Coral,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            progress?.statusText ?: "正在检索官方元数据与封面…",
            style = MaterialTheme.typography.bodySmall,
            color = ResonanceColors.Dim,
        )
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { fraction },
            color = ResonanceColors.Coral,
            trackColor = ResonanceColors.Soft,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onCancel,
            shape = ResonanceShapes.Button,
        ) {
            Text("停止并保留已处理", color = ResonanceColors.Muted)
        }
    }
}

@Composable
private fun EnrichCompletedView(
    report: BatchEnrichReport,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ResonanceColors.CoralSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = ResonanceColors.Coral, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("补全处理完成", style = MaterialTheme.typography.titleLarge, color = ResonanceColors.Ivory, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(report.message, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Dim, modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.height(16.dp))

        Surface(
            color = ResonanceColors.Soft,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultRow("已处理歌曲", "${report.totalProcessed} 首")
                ResultRow("补全专辑封面", "${report.coversEnriched} 个")
                ResultRow("获取同步歌词", "${report.lyricsEnriched} 份")
                ResultRow("修正歌手与专辑", "${report.metadataEnriched} 条")
                ResultRow("Token 节省模式", "已优先使用免 Token 官方源")
            }
        }

        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onFinish,
            shape = ResonanceShapes.Button,
            colors = ButtonDefaults.buttonColors(
                containerColor = ResonanceColors.Coral,
                contentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("完成", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EnrichOptionItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    description: String,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = ResonanceColors.Coral,
                uncheckedColor = ResonanceColors.Dim,
                checkmarkColor = Color.White,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) ResonanceColors.Ivory else ResonanceColors.Dim,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = ResonanceColors.Dim,
            )
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ResonanceColors.Dim, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = ResonanceColors.Dim)
        }
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = ResonanceColors.Ivory, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Dim)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = ResonanceColors.Ivory, fontWeight = FontWeight.SemiBold)
    }
}
