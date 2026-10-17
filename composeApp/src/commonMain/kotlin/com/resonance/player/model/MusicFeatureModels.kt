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
    val speechApiKey: String = "",
    val speechUrl: String = "wss://dashscope.aliyuncs.com/api-ws/v1/inference",
    val monthlySpeechSeconds: Int = 3600,
    val screenshotRecognition: ScreenshotRecognition = ScreenshotRecognition.LocalOcr,
    val recommendationMode: RecommendationMode = RecommendationMode.Source,
    val recommendationCooldownDays: Int = 7,
    val temporaryStorageMb: Int = 512,
    val preserveOriginalAudio: Boolean = true,
    val bilibiliFallback: Boolean = true,
    val preferredBilibiliUids: String = "",
    val recommendationsUnmeteredOnly: Boolean = false,
)

enum class RecommendationMode(val label: String) { Source("歌源推荐 · 不调用模型"), Ai("AI 精选") }
enum class ScreenshotRecognition(val label: String) {
    LocalOcr("本地 OCR"),
    CloudApi("云端 API"),
}

enum class LyricDisplay { Original, Translation, Bilingual }
data class AiImage(val bytes: ByteArray, val mimeType: String = "image/jpeg")
data class AssistantMessage(val role: String, val content: String, val model: String? = null)
data class AssistantSession(
    val id: String,
    val title: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val messages: List<AssistantMessage> = emptyList(),
)
data class AssistantUsage(val inputTokens: Long = 0, val cachedTokens: Long = 0, val outputTokens: Long = 0) {
    val total get() = inputTokens + outputTokens
}
data class MusicTaskStatus(val running: Boolean = false, val progress: ImportProgress = ImportProgress(), val message: String = "")
enum class MusicTaskOutcome { Running, Completed, Failed, Cancelled, Interrupted }
data class MusicTaskEntry(
    val id: String, val title: String, val kind: String = "", val trackId: String = "",
    val outcome: MusicTaskOutcome = MusicTaskOutcome.Running,
    val progress: ImportProgress = ImportProgress(), val message: String = "", val createdAt: Long = 0,
    val retryable: Boolean = false,
    val parentId: String = "",
)
data class ScreenshotPlaylistDraft(val name: String, val tracks: List<SongRequest>)
data class ScreenshotImportDraft(val id: String, val playlists: List<ScreenshotPlaylistDraft>, val recognition: String)
data class BilibiliVideo(val bvid: String, val cid: Long, val title: String, val uploader: String, val coverUrl: String, val durationSeconds: Int, val page: Int = 1, val uploaderId: Long = 0)
data class VideoImportRequest(val link: String, val title: String, val artist: String, val album: String, val cover: String = "", val playlistId: String? = null, val preserveAudio: Boolean = false)
data class SongRequest(val title: String, val artist: String, val album: String = "", val reason: String = "")
data class RecommendationBatch(val createdAt: Long = 0, val trackIds: List<String> = emptyList(), val reasons: Map<String, String> = emptyMap(), val createdDate: String = "")
data class MusicAssistantState(
    val messages: List<AssistantMessage> = emptyList(),
    val task: MusicTaskStatus = MusicTaskStatus(),
    val usage: AssistantUsage = AssistantUsage(),
    val recommendations: RecommendationBatch = RecommendationBatch(),
    val tasks: List<MusicTaskEntry> = emptyList(),
    val speechSeconds: Int = 0,
    val sessions: List<AssistantSession> = emptyList(),
    val currentSessionId: String = "",
    val sources: List<MusicSourceInfo> = emptyList(),
    val origins: Map<String, MediaOrigin> = emptyMap(),
    val actions: List<AssistantAction> = emptyList(),
    val recommendationFeedback: Map<String, RecommendationFeedback> = emptyMap(),
    val screenshotDraft: ScreenshotImportDraft? = null,
    val recommendationHistory: List<RecommendationHistoryItem> = emptyList(),
    val releasedDownloadIds: Set<String> = emptySet(),
)
data class RecommendationHistoryItem(val title: String, val artist: String, val album: String = "", val recommendedAt: Long = 0)

const val DAILY_PLAYLIST_ID = "resonance-daily-recommendations"

fun Lyrics.withTranslation(lines: List<String>, language: String): Lyrics = copy(
    lines = this.lines.mapIndexed { i, line -> line.copy(translation = lines.getOrNull(i)) },
    translationLanguage = language,
)

data class BiliQrSession(
    val qrcodeKey: String,
    val loginUrl: String,
    val matrixSize: Int,
    val bits: BooleanArray,
)

enum class BiliQrPollStatus {
    WAITING,
    SCANNED,
    EXPIRED,
    SUCCESS,
    ERROR
}

data class BiliQrPollResult(
    val status: BiliQrPollStatus,
    val cookie: String? = null,
    val message: String = "",
)
