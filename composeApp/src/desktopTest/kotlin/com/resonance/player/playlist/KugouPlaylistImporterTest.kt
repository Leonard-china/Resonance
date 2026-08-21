package com.resonance.player.playlist

import com.resonance.player.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class KugouPlaylistImporterTest {
    @Test
    fun parsesOfficialPlaylistAndMatchesLocalTrack() {
        val fixture = """
            {
              "error_code": 0,
              "data": {
                "list_info": { "name": "Leonard" },
                "songs": [
                  {
                    "name": "Eagles - Hotel California",
                    "hash": "HOTEL_HASH",
                    "timelen": 391000,
                    "cover": "https://img.example/{size}/hotel.jpg",
                    "singerinfo": [{ "name": "Eagles" }],
                    "albuminfo": { "name": "Hotel California" }
                  },
                  {
                    "name": "温岚 - 夏天的风",
                    "hash": "SUMMER_HASH",
                    "timelen": 241000,
                    "cover": "https://img.example/{size}/summer.jpg",
                    "singerinfo": [{ "name": "温岚" }],
                    "albuminfo": { "name": "温式效应" }
                  }
                ]
              }
            }
        """.trimIndent()
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
        assertEquals(2, result.catalogTrackCount)
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
