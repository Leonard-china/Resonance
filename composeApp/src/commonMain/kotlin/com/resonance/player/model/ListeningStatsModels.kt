package com.resonance.player.model

/**
 * 单曲听歌统计数据
 */
data class TrackListeningStats(
    val trackId: String,
    val totalSeconds: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
) {
    val formattedDuration: String
        get() = formatListeningSeconds(totalSeconds)
}

/**
 * 全库与全量听歌统计快照
 */
data class AllListeningStats(
    val totalSeconds: Long = 0L,
    val trackStats: Map<String, TrackListeningStats> = emptyMap(),
    val dailySeconds: Map<String, Long> = emptyMap(), // "2026-10-27" -> 秒数
) {
    val formattedTotalDuration: String
        get() = formatListeningSeconds(totalSeconds)

    fun getTodaySeconds(dateKey: String): Long = dailySeconds[dateKey] ?: 0L

    fun getTodayFormatted(dateKey: String): String = formatListeningSeconds(getTodaySeconds(dateKey))
}

fun formatListeningSeconds(seconds: Long): String {
    if (seconds <= 0) return "未听过"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}小时${if (minutes > 0) "${minutes}分钟" else ""}"
        minutes > 0 -> "${minutes}分钟"
        else -> "${secs}秒"
    }
}
