package com.resonance.player.platform

import com.resonance.player.model.Playlist
import com.resonance.player.model.Track

internal fun tracksWithPlaylistArtwork(tracks: List<Track>, playlists: List<Playlist>): List<Track> {
    val candidates = tracks + playlists.flatMap(Playlist::tracks)
    val playlistArtwork = candidates.asSequence()
        .filter { !it.artworkPath.isNullOrBlank() }
        .associateBy(Track::id)
    fun normalized(value: String) = value.lowercase().filter(Char::isLetterOrDigit)
    fun albumKey(track: Track): String? {
        if (track.isMissingAlbum || track.album in setOf("单曲", "B 站音乐", "本地音乐") || track.artist.isBlank()) return null
        return normalized(track.artist) + ":" + normalized(track.album)
    }
    val albumArtwork = candidates.filter { track ->
        val path = track.artworkPath.orEmpty()
        albumKey(track) != null && (track.mimeType == "application/x-resonance-catalog" ||
            path.replace('\\', '/').contains("/album-artwork/") || path.startsWith("https://y.gtimg.cn/music/"))
    }.groupBy(::albumKey).mapValues { (_, group) ->
        group.distinctBy(Track::artworkPath).singleOrNull()
    }
    return tracks.map { track ->
        val decorated = playlistArtwork[track.id] ?: albumKey(track)?.let(albumArtwork::get)
        if (!track.artworkPath.isNullOrBlank() || decorated == null) track else track.copy(
            artworkPath = decorated.artworkPath,
            artworkSeed = decorated.artworkSeed,
        )
    }
}
