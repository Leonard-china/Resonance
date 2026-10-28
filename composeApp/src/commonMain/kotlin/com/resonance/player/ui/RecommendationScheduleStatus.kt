package com.resonance.player.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

@Composable
internal fun RecommendationScheduleStatus(feature: MusicFeatureController) {
    val state = feature.assistant?.state?.collectAsState()?.value ?: return
    var now by remember { mutableStateOf(feature.services.currentTimeMillis()) }
    var networkAllowed by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(feature.settings.recommendationsUnmeteredOnly) {
        while (true) {
            now = feature.services.currentTimeMillis()
            networkAllowed = feature.services.automaticRecommendationAllowed(feature.settings.recommendationsUnmeteredOnly)
            delay(30_000)
        }
    }
    val due = state.recommendations.createdAt + feature.settings.intervalHours * 3_600_000L
    val hasUnlistened = remember(state.recommendations.trackIds, now) {
        val stats = feature.services
        state.recommendations.trackIds.any { id ->
            val s = stats.getTrackListeningStats(id)
            (s == null || (s.totalSeconds <= 0 && s.playCount <= 0))
        }
    }
    val next = when {
        networkAllowed == false -> "等待符合设置的网络"
        state.task.running -> "音乐任务处理中，完成后再检查"
        state.recommendations.createdAt > 0L && state.recommendations.trackIds.isNotEmpty() && hasUnlistened -> "本批推荐尚有歌曲未听过，保留不换"
        state.recommendations.createdAt == 0L || now >= due -> "已到期，条件满足时检查"
        else -> "预计下次到期：" + feature.services.formatDateTime(due)
    }
    Text(next + if (feature.settings.backgroundRecommendations) "；后台受系统调度影响，可能延迟。" else "；下次打开应用时检查。", style = MaterialTheme.typography.bodySmall)
}
