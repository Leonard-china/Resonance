package com.resonance.player.playlist

import com.resonance.player.model.Track
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KugouPlaylistImporterTest {

    @Test
    fun testParseEmbeddedPlaylistJson() {
        val jsonStr = """
            {
                "info": {
                    "listinfo": {
                        "name": "Leonard",
                        "count": 23,
                        "pic": "http://c1.kgimg.com/stdmusic/400/cover.jpg"
                    },
                    "songs": [
                        {
                            "name": "Guns N' Roses - Welcome To The Jungle",
                            "hash": "E3E0ADDF0807C052CE7F27AC3B7B7E6D",
                            "timelen": 271908,
                            "albuminfo": { "name": "Greatest Hits" },
                            "singerinfo": [{ "name": "Guns N' Roses" }]
                        },
                        {
                            "name": "Eagles - Hotel California",
                            "hash": "546894D4439DA40BA0AB484E6EDEFE3D",
                            "timelen": 391000,
                            "albuminfo": { "name": "Hotel California" },
                            "singerinfo": [{ "name": "Eagles" }]
                        }
                    ]
                }
            }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val songs = json.getJSONObject("info").getJSONArray("songs")
        val name = json.getJSONObject("info").getJSONObject("listinfo").getString("name")

        val localTracks = listOf(
            Track(
                id = "local-1",
                title = "Hotel California",
                artist = "Eagles",
                album = "Hotel California",
                durationText = "6:31",
                artworkSeed = 1,
                sourceUri = "file:///music/hotel.mp3",
                mimeType = "audio/mpeg",
            )
        )

        val importer = KugouPlaylistImporter()
        val report = importer.parseEmbeddedPlaylistResponse(
            name = name,
            id = "gcid_3z12o39pwz4z025",
            songs = songs,
            localTracks = localTracks,
        )

        assertEquals("Leonard", report.playlist.name)
        assertEquals(2, report.playlist.tracks.size)
        assertEquals(1, report.matchedTrackCount)
        assertEquals(2, report.catalogTrackCount)
        assertEquals("Welcome To The Jungle", report.playlist.tracks[0].title)
        assertEquals("Hotel California", report.playlist.tracks[1].title)
        assertEquals("file:///music/hotel.mp3", report.playlist.tracks[1].sourceUri)
    }

    @Test
    fun testResolveEmbeddedTarget() {
        val url = "https://m.kugou.com/songlist/gcid_3z12o39pwz4z025/?src_cid=3z12o39pwz4z025&uid=1979464932&chl=message&iszlist=1"
        val importer = KugouPlaylistImporter()
        val target = importer.resolveTarget(url)
        assertNotNull(target)
        assertTrue(target is KugouPlaylistImporter.ImportTarget.EmbeddedPlaylist)
        assertEquals("gcid_3z12o39pwz4z025", target.id)
        assertEquals("Leonard", target.name)
        assertTrue(target.songs.length() > 0)
    }
}
