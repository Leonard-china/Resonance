package com.resonance.player.platform

import com.resonance.player.model.Playlist
import com.resonance.player.model.Track

internal fun tracksWithPlaylistArtwork(tracks: List<Track>, playlists: List<Playlist>): List<Track> {
    val playlistArtwork = playlists.asSequence()
        .flatMap(Playlist::tracks)
        .filter { !it.artworkPath.isNullOrBlank() }
        .associateBy(Track::id)
    return tracks.map { track ->
        val decorated = playlistArtwork[track.id]
        if (track.artworkPath != null || decorated == null) track else track.copy(
            artworkPath = decorated.artworkPath,
            artworkSeed = decorated.artworkSeed,
        )
    }
}
