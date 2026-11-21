package com.resonance.player.enrich

import com.resonance.player.lyrics.LrclibLyricsRepository
import com.resonance.player.model.BatchEnrichOptions
import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.Track
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetadataEnricherTest {
    @Test fun publicCatalogMatchingRejectsWrongSongsAndAllowsUnknownArtist() {
        val root = Files.createTempDirectory("catalog-matching-")
        try {
            val enricher = MetadataEnricher(root.resolve("art"), LrclibLyricsRepository(root.resolve("lyrics")))
            assertTrue(enricher.candidateMatches("晴天", "未知歌手", "晴天", "周杰伦"))
            assertTrue(enricher.candidateMatches("周杰伦 - 晴天.mp3", "周杰伦", "晴天", "周杰伦"))
            assertFalse(enricher.candidateMatches("晴天", "周杰伦", "夜曲", "周杰伦"))
            assertFalse(enricher.candidateMatches("晴天", "周杰伦", "晴天", "其他歌手"))
            assertFalse(enricher.candidateMatches("晴天", "周杰伦", "晴天", ""))
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }

    @Test
    fun cleansTrackTitleProperly() {
        val tempDir = Files.createTempDirectory("resonance-test-artwork")
        val lyricsDir = Files.createTempDirectory("resonance-test-lyrics")
        val enricher = MetadataEnricher(tempDir, LrclibLyricsRepository(lyricsDir))

        assertEquals("晴天", enricher.cleanTrackTitle("周杰伦 - 晴天.mp3"))
        assertEquals("夜曲", enricher.cleanTrackTitle("夜曲 [320k].flac"))
        assertEquals("七里香", enricher.cleanTrackTitle("周杰伦 - 七里香 (SQ无损).wav"))
        assertEquals("稻香", enricher.cleanTrackTitle("稻香.m4a"))
        assertEquals("Simple Track", enricher.cleanTrackTitle("Simple Track"))
    }

    @Test
    fun skipsAlreadyCompleteTrackWhenNotForceRefresh() = runBlocking {
        val tempDir = Files.createTempDirectory("resonance-test-artwork")
        val lyricsDir = Files.createTempDirectory("resonance-test-lyrics")
        val enricher = MetadataEnricher(tempDir, LrclibLyricsRepository(lyricsDir))

        val completeTrack = Track(
            id = "track-1",
            title = "Mojito",
            artist = "周杰伦",
            album = "Mojito",
            durationText = "03:05",
            artworkSeed = 1,
            artworkPath = "/path/to/existing.jpg",
        )

        val options = BatchEnrichOptions(
            enrichCover = true,
            enrichLyrics = false,
            enrichArtistAndAlbum = true,
            useAiFallback = false,
            forceRefresh = false,
        )

        var statusMessage = ""
        val (result, modified) = enricher.enrichTrack(
            track = completeTrack,
            options = options,
            config = DeepSeekConfig(),
            onStatus = { statusMessage = it },
        )

        assertFalse(modified)
        assertEquals(completeTrack, result)
        assertEquals("元数据完整，跳过", statusMessage)
    }

    @Test
    fun detectMissingMetadataHelpers() {
        val completeTrack = Track(
            id = "t1",
            title = "Song",
            artist = "Artist",
            album = "Album",
            durationText = "03:00",
            artworkSeed = 1,
            artworkPath = "/covers/1.cover",
        )
        assertFalse(completeTrack.isMissingCover)
        assertFalse(completeTrack.isMissingArtist)
        assertFalse(completeTrack.isMissingAlbum)
        assertFalse(completeTrack.isMissingAnyMetadata)

        val missingCover = completeTrack.copy(artworkPath = null)
        assertTrue(missingCover.isMissingCover)
        assertTrue(missingCover.isMissingAnyMetadata)

        val missingArtist = completeTrack.copy(artist = "未知歌手")
        assertTrue(missingArtist.isMissingArtist)
        assertTrue(missingArtist.isMissingAnyMetadata)

        val missingAlbum = completeTrack.copy(album = "未知专辑")
        assertTrue(missingAlbum.isMissingAlbum)
        assertTrue(missingAlbum.isMissingAnyMetadata)
    }

    @Test
    fun parsesQqCatalogAndTreatsUnreadableMediaStoreCoverAsMissing() = runBlocking {
        val root = Files.createTempDirectory("resonance-enrich-qq-")
        try {
            var checkedCover = false
            val enricher = MetadataEnricher(
                artworkDirectory = root.resolve("art"),
                lyricsRepository = LrclibLyricsRepository(root.resolve("lyrics")),
                hasReadableCover = { track ->
                    checkedCover = true
                    track.artworkPath?.startsWith("content://media/external/audio/albumart/") != true
                },
            )
            val qqJson = """
                {
                  "code": 0,
                  "data": {
                    "song": {
                      "list": [
                        {
                          "songname": "晴天",
                          "albumname": "叶惠美",
                          "albummid": "000MkMni19ClKG",
                          "singer": [{"name": "周杰伦"}]
                        }
                      ]
                    }
                  }
                }
            """.trimIndent()
            val parsed = enricher.parseQqSearch(qqJson, "晴天", "周杰伦", "叶惠美")
            assertEquals("叶惠美", parsed?.album)
            assertEquals("周杰伦", parsed?.artist)
            assertEquals("https://y.gtimg.cn/music/photo_new/T002R800x800M000000MkMni19ClKG.jpg", parsed?.artworkUrl)

            val mediaStoreTrack = Track(
                id = "ms-1",
                title = "不存在的冷门测试曲xyz987654",
                artist = "测试歌手xyz",
                album = "测试专辑xyz",
                durationText = "3:00",
                artworkSeed = 1,
                artworkPath = "content://media/external/audio/albumart/999999",
            )
            var lastStatus = ""
            val report = enricher.batchEnrich(
                options = BatchEnrichOptions(enrichCover = true, enrichLyrics = false, enrichArtistAndAlbum = false, useAiFallback = false, forceRefresh = false),
                tracks = listOf(mediaStoreTrack),
                config = DeepSeekConfig(),
                onProgress = { lastStatus = it.statusText },
            )
            assertTrue(checkedCover, "hasReadableCover probe must run for tracks with non-blank artworkPath")
            assertEquals(0, report.coversEnriched)
            assertTrue(report.message.contains("未匹配到新的"), "Must honestly report zero updates instead of false success: ${report.message}")
            assertTrue(lastStatus != "元数据完整，跳过", "Unreadable MediaStore albumart must not be skipped as complete")
        } finally {
            Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }
}
