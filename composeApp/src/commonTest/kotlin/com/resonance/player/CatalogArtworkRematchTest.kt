package com.resonance.player

import com.resonance.player.model.Playlist
import com.resonance.player.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogArtworkRematchTest {
    @Test
    fun matchedLocalTrackKeepsCatalogArtworkWhenLocalArtworkIsMissing() {
        val catalog = Track(
            id = "catalog",
            title = "Song",
            artist = "Artist",
            album = "Album",
            durationText = "3:00",
            artworkSeed = 99,
            artworkPath = "https://cover.example/song.jpg",
            mimeType = "application/x-resonance-catalog",
        )
        val local = catalog.copy(
            id = "local",
            artworkSeed = 1,
            sourceUri = "file:///song.mp3",
            artworkPath = null,
            mimeType = "audio/mpeg",
        )

        val rematched = rematchCatalogTracks(
            listOf(Playlist("kugou-test", "List", "1", listOf(catalog), 1)),
            listOf(local),
        ).single().tracks.single()

        assertEquals("file:///song.mp3", rematched.sourceUri)
        assertEquals("https://cover.example/song.jpg", rematched.artworkPath)
        assertEquals(99, rematched.artworkSeed)
    }
}
