package com.resonance.player.playlist

import com.resonance.player.model.Track
import java.nio.charset.StandardCharsets
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class KugouPlaylistImporterTest {
    @Test
    fun parsesOfficialPlaylistAndMatchesLocalTrack() {
        val fixture = Path("../build/kugou-official-tracks.json").readText(StandardCharsets.UTF_8)
        val local = Track(
            id = "local-hotel-california",
            title = "Hotel California",
            artist = "Eagles",
            album = "Local Album",
            durationText = "6:31",
            artworkSeed = 1,
            sourceUri = "D:/Music/Hotel California.mp3",
        )

        val result = KugouPlaylistImporter().parseOfficialResponse(
            fixture,
            "collection_3_2257531406_3_0",
            listOf(local),
        )

        assertEquals("Leonard", result.playlist.name)
        assertEquals(21, result.catalogTrackCount)
        assertEquals(1, result.matchedTrackCount)
        assertEquals(local.sourceUri, result.playlist.tracks.first().sourceUri)
        assertEquals(local.title, result.playlist.tracks.first().title)
        kotlin.test.assertNotNull(result.playlist.tracks.first().artworkPath)
        assertEquals("夏天的风", result.playlist.tracks[1].title)
        assertEquals("温岚", result.playlist.tracks[1].artist)
        assertNull(result.playlist.tracks[1].sourceUri)
    }

    @Test
    fun rejectsNonKugouLinksBeforeAnyNetworkRequest() {
        assertFailsWith<IllegalArgumentException> {
            KugouPlaylistImporter().resolveGlobalCollectionId("https://example.com/playlist?global_collection_id=collection_3_1_1_0")
        }
    }
}
