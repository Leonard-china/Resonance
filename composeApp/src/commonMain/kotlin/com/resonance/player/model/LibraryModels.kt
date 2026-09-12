package com.resonance.player.model

const val APP_VERSION = "0.2.2"

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationText: String,
    val artworkSeed: Int,
    val isFavorite: Boolean = false,
    val sourceUri: String? = null,
    val artworkPath: String? = null,
    val mimeType: String = "audio/mpeg",
) {
    val isMissingCover: Boolean
        get() = artworkPath.isNullOrBlank()

    val isMissingArtist: Boolean
        get() = artist.isBlank() || artist.trim() in setOf("未知歌手", "未知艺术家", "未知", "Unknown", "Unknown Artist")

    val isMissingAlbum: Boolean
        get() = album.isBlank() || album.trim() in setOf("未知专辑", "本地音乐", "未知", "Unknown", "Unknown Album")

    val isMissingAnyMetadata: Boolean
        get() = isMissingCover || isMissingArtist || isMissingAlbum
}

data class ImportReport(
    val tracks: List<Track>,
    val skippedCount: Int = 0,
    val warnings: List<String> = emptyList(),
)

data class PlaylistImportReport(
    val playlist: Playlist,
    val catalogTrackCount: Int,
    val matchedTrackCount: Int,
    val message: String,
)

data class DeleteTrackReport(
    val success: Boolean,
    val fileDeleted: Boolean,
    val message: String,
)

data class SyncReport(
    val success: Boolean,
    val message: String,
    val trackCount: Int = 0,
    val playlistCount: Int = 0,
)

data class LanShareInfo(
    val active: Boolean,
    val message: String,
    val qrPath: String? = null,
    val expiresText: String? = null,
)

data class Playlist(
    val id: String,
    val name: String,
    val subtitle: String,
    val tracks: List<Track>,
    val artworkSeed: Int,
)

enum class LibraryDestination {
    Library,
    Discover,
    Playing,
    Profile,
    Import,
    Sync,
    Settings,
}

enum class RepeatMode {
    Off,
    All,
    One,
}

enum class ThemeMode {
    Dark,
    Light,
    System,
}

enum class TrackSortOption(val label: String) {
    Default("默认排序"),
    Title("歌名 (A-Z)"),
    Artist("歌手 (A-Z)"),
    Album("专辑 (A-Z)"),
    DurationAsc("时长 (短到长)"),
    DurationDesc("时长 (长到短)"),
}

enum class TrackFilterOption(val label: String) {
    All("全部"),
    Lossless("无损 FLAC"),
    HighQuality("高品质 320K"),
    MissingCover("缺少封面"),
    MissingLyrics("缺少歌词"),
    MissingMetadata("缺少歌手/专辑"),
}

enum class SleepTimerOption(val label: String, val minutes: Int?) {
    Off("关闭定时", null),
    Min15("15 分钟", 15),
    Min30("30 分钟", 30),
    Min45("45 分钟", 45),
    Min60("60 分钟", 60),
    EndOfTrack("播完当前歌曲停止", null),
}

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val progress: Float = 0f,
    val volume: Float = 1.0f,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.Off,
    val sleepTimerRemainingSeconds: Int? = null,
)

data class LyricLine(
    val timestampMs: Long?,
    val text: String,
)

data class Lyrics(
    val trackId: String,
    val lines: List<LyricLine>,
    val synchronized: Boolean,
    val instrumental: Boolean,
    val source: String,
    val fromCache: Boolean = false,
    val offsetMs: Long = 0L,
)

sealed interface LyricsFetchResult {
    data class Found(val lyrics: Lyrics) : LyricsFetchResult
    data class Unavailable(val message: String, val retryable: Boolean) : LyricsFetchResult
}

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data class Ready(val lyrics: Lyrics) : LyricsUiState
    data class Unavailable(val message: String, val retryable: Boolean) : LyricsUiState
}

fun durationTextToSeconds(text: String): Int {
    val parts = text.trim().split(':').map { it.toIntOrNull() ?: return 0 }
    return when (parts.size) {
        3 -> parts[0] * 3_600 + parts[1] * 60 + parts[2]
        2 -> parts[0] * 60 + parts[1]
        1 -> parts[0]
        else -> 0
    }.coerceAtLeast(0)
}

data class BatchEnrichOptions(
    val enrichCover: Boolean = true,
    val enrichLyrics: Boolean = true,
    val enrichArtistAndAlbum: Boolean = true,
    val useAiFallback: Boolean = true,
    val forceRefresh: Boolean = false,
)

data class BatchEnrichProgress(
    val current: Int,
    val total: Int,
    val currentTrack: Track,
    val statusText: String,
)

data class BatchEnrichReport(
    val totalProcessed: Int,
    val coversEnriched: Int,
    val lyricsEnriched: Int,
    val metadataEnriched: Int,
    val aiCallsCount: Int,
    val updatedTracks: List<Track>,
    val message: String,
)

