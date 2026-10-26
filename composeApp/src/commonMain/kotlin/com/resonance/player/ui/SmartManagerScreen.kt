package com.resonance.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.*
import com.resonance.player.platform.ResonanceBackHandler

@Composable
fun SmartManagerScreen(
    tracks: List<Track>,
    onBack: () -> Unit,
    onApplyTagClean: (List<CleanTagDraft>) -> Unit,
    onCleanDuplicates: (List<Track>) -> Unit,
    compact: Boolean = false,
) {
    ResonanceBackHandler(onBack = onBack)
    var selectedTab by remember { mutableStateOf(0) }
    var report by remember { mutableStateOf<SmartLibraryAuditReport?>(null) }
    var isAuditing by remember { mutableStateOf(false) }

    val pagePadding = if (compact) 16.dp else 24.dp

    fun runAudit() {
        isAuditing = true
        report = SmartLibraryAuditEngine.audit(tracks)
        isAuditing = false
    }

    LaunchedEffect(tracks) {
        if (report == null && tracks.isNotEmpty()) {
            runAudit()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ResonanceColors.Canvas)
    ) {
        // Top Bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = pagePadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ResonanceColors.TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "曲库智能管家",
                style = MaterialTheme.typography.titleLarge,
                color = ResonanceColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            FilledTonalButton(
                onClick = ::runAudit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = ResonanceColors.Raised,
                    contentColor = ResonanceColors.Primary,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("重新体检", fontSize = 13.sp)
            }
        }

        val currentReport = report
        if (currentReport == null || isAuditing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ResonanceColors.Primary)
            }
            return
        }

        // Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pagePadding, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ResonanceColors.Raised),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                SummaryStat("已体检曲目", "${currentReport.totalScanned}")
                SummaryStat("待清洗标签", "${currentReport.tagIssueCount}", isHighlight = currentReport.tagIssueCount > 0)
                SummaryStat("重复/多版本", "${currentReport.duplicateGroupCount}", isHighlight = currentReport.duplicateGroupCount > 0)
            }
        }

        // Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = ResonanceColors.Canvas,
            contentColor = ResonanceColors.Primary,
            divider = { HorizontalDivider(color = ResonanceColors.Divider) },
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("标签清洗建议 (${currentReport.tagIssueCount})") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("版本与去重 (${currentReport.duplicateGroupCount})") },
            )
        }

        if (selectedTab == 0) {
            TagCleaningView(
                drafts = currentReport.tagDrafts,
                onApply = { draftsToApply ->
                    onApplyTagClean(draftsToApply)
                    runAudit()
                },
                modifier = Modifier.weight(1f).padding(horizontal = pagePadding),
            )
        } else {
            DuplicateCleaningView(
                groups = currentReport.duplicateGroups,
                onClean = { tracksToClean ->
                    onCleanDuplicates(tracksToClean)
                    runAudit()
                },
                modifier = Modifier.weight(1f).padding(horizontal = pagePadding),
            )
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, isHighlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isHighlight) ResonanceColors.Primary else ResonanceColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = ResonanceColors.Dim)
    }
}

@Composable
private fun TagCleaningView(
    drafts: List<CleanTagDraft>,
    onApply: (List<CleanTagDraft>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (drafts.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ResonanceColors.Positive, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("所有歌曲标签均规范整洁，无需清洗！", color = ResonanceColors.Muted, fontSize = 14.sp)
            }
        }
        return
    }

    var selectionMap by remember(drafts) { mutableStateOf(drafts.associate { it.trackId to it.selected }) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("勾选以应用规范化修正", fontSize = 12.sp, color = ResonanceColors.Dim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { selectionMap = drafts.associate { it.trackId to true } }) { Text("全选") }
                TextButton(onClick = { selectionMap = drafts.associate { it.trackId to false } }) { Text("全不选") }
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(drafts, key = { it.trackId }) { draft ->
                val checked = selectionMap[draft.trackId] ?: false
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ResonanceColors.Raised),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                selectionMap = selectionMap + (draft.trackId to isChecked)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = ResonanceColors.Primary),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(draft.suggestedTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ResonanceColors.TextPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text(draft.reason, fontSize = 10.sp, color = ResonanceColors.Primary, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ResonanceColors.Primary.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("歌手：${draft.suggestedArtist} · 专辑：${draft.suggestedAlbum}", fontSize = 12.sp, color = ResonanceColors.Muted)
                            Spacer(Modifier.height(4.dp))
                            Text("原标题：${draft.originalTitle} (${draft.originalArtist})", fontSize = 11.sp, color = ResonanceColors.Dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        val selectedCount = selectionMap.count { it.value }
        Button(
            onClick = {
                val toApply = drafts.filter { selectionMap[it.trackId] == true }
                onApply(toApply)
            },
            enabled = selectedCount > 0,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ResonanceColors.Primary,
                contentColor = ResonanceColors.Canvas,
            ),
        ) {
            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("应用规范化修正 ($selectedCount 首)")
        }
    }
}

@Composable
private fun DuplicateCleaningView(
    groups: List<DuplicateGroup>,
    onClean: (List<Track>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ResonanceColors.Positive, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("曲库中未发现重复音频或冗余文件！", color = ResonanceColors.Muted, fontSize = 14.sp)
            }
        }
        return
    }

    var selectedTrackIds by remember(groups) {
        mutableStateOf(groups.flatMap { it.selectedToCleanIds }.toSet())
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("已自动为您推荐保留最佳版本", fontSize = 12.sp, color = ResonanceColors.Dim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { selectedTrackIds = groups.flatMap { it.selectedToCleanIds }.toSet() }) { Text("推荐选择") }
                TextButton(onClick = { selectedTrackIds = emptySet() }) { Text("全不选") }
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(groups, key = { it.key }) { group ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ResonanceColors.Raised),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        val first = group.tracks.first()
                        Text("${first.title} - ${first.artist}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ResonanceColors.TextPrimary)
                        Spacer(Modifier.height(8.dp))

                        for (track in group.tracks) {
                            val isRecommendedKeep = track.id == group.recommendedToKeepId
                            val isChecked = track.id in selectedTrackIds
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (isRecommendedKeep) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "推荐保留", tint = ResonanceColors.Positive, modifier = Modifier.size(20.dp))
                                } else {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedTrackIds = if (checked) selectedTrackIds + track.id else selectedTrackIds - track.id
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = ResonanceColors.Primary),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(track.durationText, fontSize = 12.sp, color = ResonanceColors.TextPrimary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(track.mimeType.substringAfter("/").uppercase(), fontSize = 10.sp, color = ResonanceColors.Dim, modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(ResonanceColors.Divider).padding(horizontal = 4.dp, vertical = 1.dp))
                                        if (isRecommendedKeep) {
                                            Spacer(Modifier.width(6.dp))
                                            Text("推荐保留", fontSize = 10.sp, color = ResonanceColors.Positive, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (!track.sourceUri.isNullOrBlank()) {
                                        Text(track.sourceUri.substringAfterLast("/"), fontSize = 10.sp, color = ResonanceColors.Dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val allTracksById = remember(groups) { groups.flatMap { it.tracks }.associateBy { it.id } }
        val toCleanTracks = selectedTrackIds.mapNotNull { allTracksById[it] }

        Button(
            onClick = { onClean(toCleanTracks) },
            enabled = toCleanTracks.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ResonanceColors.Notice,
                contentColor = ResonanceColors.Canvas,
            ),
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("安全释放所选冗余音频 (${toCleanTracks.size} 首)")
        }
    }
}
