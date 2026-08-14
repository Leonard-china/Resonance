package com.resonance.player.platform

import com.resonance.player.model.ImportReport
import com.resonance.player.model.DeleteTrackReport
import com.resonance.player.model.Playlist
import com.resonance.player.model.PlaylistImportReport
import com.resonance.player.model.SyncReport
import com.resonance.player.model.LanShareInfo
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Track
import kotlinx.coroutines.flow.Flow

interface PlatformServices {
    val playbackEnded: Flow<Unit>
    val playbackProgress: Flow<Float>
    val activeTrackChanges: Flow<String>
    val incomingLanLinks: Flow<String>
    suspend fun loadLibrary(): List<Track>
    suspend fun loadPlaylists(): List<Playlist>
    suspend fun savePlaylists(playlists: List<Playlist>)
    suspend fun exportSyncPackage(passphrase: String): SyncReport
    suspend fun importSyncPackage(passphrase: String): SyncReport
    suspend fun startLanShare(): LanShareInfo
    suspend fun importLanShare(link: String): SyncReport
    fun stopLanShare()
    suspend fun importMusic(convertToMp3: Boolean = false): ImportReport
    suspend fun importPlaylistLink(link: String): PlaylistImportReport
    suspend fun deleteLocalTrack(track: Track): DeleteTrackReport
    fun play(track: Track, queue: List<Track>, shuffle: Boolean, repeatMode: RepeatMode)
    fun setPlaying(isPlaying: Boolean)
    fun setPlaybackMode(shuffle: Boolean, repeatMode: RepeatMode)
    fun seekTo(fraction: Float)
    fun close()
}
