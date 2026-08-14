package com.resonance.player.model

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
    val cleanupWarnings: List<String> = emptyList(),
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

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val progress: Float = 0f,
)
