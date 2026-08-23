package com.resonance.player.model

const val APP_VERSION = "0.2.0"

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
)

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
    Explore,
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

data class PlayerState(

    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val progress: Float = 0f,
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
