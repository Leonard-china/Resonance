package com.resonance.player.features

import com.resonance.player.model.*
import java.nio.file.Path

/** The same durable library operations are supplied to foreground and background runtimes. */
interface MusicLibraryAccess {
    suspend fun tracks(): List<Track>
    suspend fun playlists(): List<Playlist>
    suspend fun savePlaylists(playlists: List<Playlist>)
    suspend fun update(track: Track)
    suspend fun delete(track: Track): DeleteTrackReport
    suspend fun favorite(track: Track, value: Boolean)
    suspend fun importAudio(path: Path, request: VideoImportRequest, cover: Path?): Track
    suspend fun transcode(input: Path, output: Path, request: VideoImportRequest, cover: Path?)
    suspend fun adoptRestoredAudio(previous: Track, imported: Track): Track {
        val restored = imported.copy(id = previous.id, isFavorite = previous.isFavorite)
        update(restored)
        savePlaylists(playlists().map { playlist ->
            playlist.copy(tracks = playlist.tracks.map { if (it.id == previous.id) restored else it })
        })
        return restored
    }
    suspend fun audioInfo(track: Track): AudioFileInfo = AudioFileInfo()
    suspend fun prepareSpeechAudio(track: Track, output: Path) { error("此平台未提供音频识别准备能力") }
    fun playingTrackId(): String? = null
}
