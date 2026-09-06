package com.resonance.player.platform

import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.DeepSeekTestResult
import com.resonance.player.model.DeleteTrackReport
import com.resonance.player.model.ImportReport
import com.resonance.player.model.LanShareInfo
import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.Playlist
import com.resonance.player.model.PlaylistImportReport
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.SyncReport
import com.resonance.player.model.Track
import kotlinx.coroutines.flow.Flow

interface PlatformServices {
    val playbackEnded: Flow<Unit>
    val playbackProgress: Flow<Float>
    val isPlayingChanges: Flow<Boolean>
    val activeTrackChanges: Flow<String>
    val incomingLanLinks: Flow<String>

    suspend fun loadLibrary(): List<Track>
    suspend fun loadPlaylists(): List<Playlist>
    suspend fun savePlaylists(playlists: List<Playlist>)
    suspend fun loadLastSelectedPlaylistId(): String?
    suspend fun saveLastSelectedPlaylistId(playlistId: String?)
    suspend fun loadThemeMode(): com.resonance.player.model.ThemeMode
    suspend fun saveThemeMode(mode: com.resonance.player.model.ThemeMode)
    val libraryLocation: String

    suspend fun loadDeepSeekConfig(): DeepSeekConfig
    suspend fun saveDeepSeekConfig(config: DeepSeekConfig)
    suspend fun testDeepSeek(config: DeepSeekConfig): DeepSeekTestResult
    suspend fun loadCustomLyricsFolder(): String?
    suspend fun saveCustomLyricsFolder(folder: String?)

    suspend fun setTrackFavorite(trackId: String, favorite: Boolean)
    suspend fun loadLyrics(track: Track, forceRefresh: Boolean = false): LyricsFetchResult
    suspend fun requestAiLyrics(track: Track): LyricsFetchResult
    suspend fun exportSyncPackage(playlistId: String?): SyncReport
    suspend fun importSyncPackage(): SyncReport
    suspend fun startLanShare(): LanShareInfo
    suspend fun importLanShare(link: String): SyncReport
    fun stopLanShare()
    suspend fun importMusic(convertToMp3: Boolean = false): ImportReport
    suspend fun importPlaylistLink(link: String): PlaylistImportReport
    suspend fun deleteLocalTrack(track: Track): DeleteTrackReport
    suspend fun batchDeleteTracks(tracks: List<Track>): DeleteTrackReport
    suspend fun updateTracks(tracks: List<Track>)
    suspend fun batchEnrichTracks(
        options: com.resonance.player.model.BatchEnrichOptions,
        tracks: List<Track>,
        onProgress: (com.resonance.player.model.BatchEnrichProgress) -> Unit = {},
    ): com.resonance.player.model.BatchEnrichReport
    fun play(track: Track, queue: List<Track>, shuffle: Boolean, repeatMode: RepeatMode)
    fun setPlaying(isPlaying: Boolean)
    fun setPlaybackMode(shuffle: Boolean, repeatMode: RepeatMode)
    fun setVolume(volume: Float)
    fun setPlaybackSpeed(speed: Float)
    fun seekTo(fraction: Float)
    suspend fun saveLyricsOffset(track: Track, offsetMs: Long): com.resonance.player.model.Lyrics?
    suspend fun embedLyricsToAudioFile(track: Track, lyrics: com.resonance.player.model.Lyrics): Boolean
    suspend fun editTrackMetadata(track: Track, newTitle: String, newArtist: String, newAlbum: String): Track
    fun close()
}
