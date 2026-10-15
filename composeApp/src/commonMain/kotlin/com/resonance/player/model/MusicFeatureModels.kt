package com.resonance.player.model

data class ImportProgress(
    val stage: String = "准备中",
    val fileName: String = "",
    val completed: Int = 0,
    val total: Int = 0,
    val fraction: Float? = null,
    val detail: String = "",
)

enum class AiProvider(val label: String, val url: String, val defaultModel: String) {
    Qwen("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen3-vl-flash"),
    DeepSeek("DeepSeek", "https://api.deepseek.com/v1", "deepseek-flash"),
    Doubao("豆包 · 火山方舟", "https://ark.cn-beijing.volces.com/api/v3", ""),
    Custom("自定义兼容接口", "", ""),
}

data class AiConnection(
    val provider: AiProvider = AiProvider.Qwen,
    val baseUrl: String = provider.url,
    val model: String = provider.defaultModel,
    val apiKey: String = "",
    val vision: Boolean = provider != AiProvider.DeepSeek,
) {
    val configured get() = apiKey.isNotBlank() && model.isNotBlank() && baseUrl.startsWith("https://")
}

data class MusicFeatureSettings(
    val connections: List<AiConnection> = listOf(AiConnection(), AiConnection(AiProvider.DeepSeek), AiConnection(AiProvider.Doubao)),
    val assistantProvider: AiProvider = AiProvider.Qwen,
    val visionProvider: AiProvider = AiProvider.Qwen,
    val recommendationsEnabled: Boolean = false,
    val backgroundRecommendations: Boolean = false,
    val intervalHours: Int = 24,
    val recommendationCount: Int = 5,
    val monthlyTokenLimit: Long = 1_000_000,
    val floatingLyrics: Boolean = false,
    val floatingLyricsLocked: Boolean = false,
    val floatingLyricsSize: Int = 24,
    val translationLanguage: String = "中文",
    val lyricDisplay: LyricDisplay = LyricDisplay.Bilingual,
    val bilibiliCookie: String = "",
)

enum class LyricDisplay { Original, Translation, Bilingual }
data class AiImage(val bytes: ByteArray, val mimeType: String = "image/jpeg")
data class AssistantMessage(val role: String, val content: String)
data class AssistantUsage(val inputTokens: Long = 0, val cachedTokens: Long = 0, val outputTokens: Long = 0) {
    val total get() = inputTokens + outputTokens
}
data class MusicTaskStatus(val running: Boolean = false, val progress: ImportProgress = ImportProgress(), val message: String = "")
data class BilibiliVideo(val bvid: String, val cid: Long, val title: String, val uploader: String, val coverUrl: String, val durationSeconds: Int, val page: Int = 1)
data class VideoImportRequest(val link: String, val title: String, val artist: String, val album: String, val cover: String = "", val playlistId: String? = null)
data class SongRequest(val title: String, val artist: String, val album: String = "", val reason: String = "")
data class RecommendationBatch(val createdAt: Long = 0, val trackIds: List<String> = emptyList(), val reasons: Map<String, String> = emptyMap())
data class MusicAssistantState(
    val messages: List<AssistantMessage> = emptyList(),
    val task: MusicTaskStatus = MusicTaskStatus(),
    val usage: AssistantUsage = AssistantUsage(),
    val recommendations: RecommendationBatch = RecommendationBatch(),
)

fun Lyrics.withTranslation(lines: List<String>, language: String): Lyrics = copy(
    lines = this.lines.mapIndexed { i, line -> line.copy(translation = lines.getOrNull(i)) },
    translationLanguage = language,
)
