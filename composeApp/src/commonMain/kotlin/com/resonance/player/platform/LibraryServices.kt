package com.resonance.player.platform

import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.DeepSeekTestResult
import com.resonance.player.model.DeleteTrackReport
import com.resonance.player.model.ImportReport
import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.Playlist
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.SyncReport
import com.resonance.player.model.Track
import com.resonance.player.model.CalibrationStatus
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Focused platform contract; implemented by the existing native adapters. */
interface LibraryServices {
    val importProgress: StateFlow<com.resonance.player.model.ImportProgress?> get() = MutableStateFlow(null)
    fun cancelImport() {}
    /** Desktop file-manager drops; Android keeps using its permission-aware picker. */
    val externalMusicImports: Flow<List<String>> get() = kotlinx.coroutines.flow.emptyFlow()
    suspend fun importMusicPaths(paths: List<String>, convertToMp3: Boolean = false): ImportReport =
        error("当前平台不支持文件拖入")
    suspend fun audioFileAvailable(track: Track): Boolean = track.sourceUri != null
    suspend fun chooseRelinkDirectory(): List<com.resonance.player.model.AudioRelinkCandidate>? = null
    suspend fun chooseReplacementAudio(track: Track): Track? = null
    suspend fun saveReplacementAudio(track: Track) { updateTracks(listOf(track)) }
    /** Recheck sources under the store lock and atomically persist all valid replacements once. */
    suspend fun saveReplacementAudioBatch(matches: List<com.resonance.player.model.AudioRelinkMatch>): Pair<Int, List<String>> =
        error("当前平台不支持批量音频重新定位")
    suspend fun loadLibrary(): List<Track>
    suspend fun loadPlaylists(): List<Playlist>
    suspend fun savePlaylists(playlists: List<Playlist>)
    suspend fun loadLastSelectedPlaylistId(): String?
    suspend fun saveLastSelectedPlaylistId(playlistId: String?)
    val libraryLocation: String

    suspend fun exportSyncPackage(playlistId: String?): SyncReport
    /** Opens the report's platform path/URI; false leaves its display path available for copying. */
    suspend fun openExportLocation(directory: String): Boolean = false
    suspend fun importSyncPackage(): SyncReport
    suspend fun importMusic(convertToMp3: Boolean = false): ImportReport
    suspend fun deleteLocalTrack(track: Track): DeleteTrackReport
    suspend fun batchDeleteTracks(tracks: List<Track>): DeleteTrackReport
    suspend fun updateTracks(tracks: List<Track>)
    suspend fun batchEnrichTracks(
        options: com.resonance.player.model.BatchEnrichOptions,
        tracks: List<Track>,
        onProgress: (com.resonance.player.model.BatchEnrichProgress) -> Unit = {},
    ): com.resonance.player.model.BatchEnrichReport
}
