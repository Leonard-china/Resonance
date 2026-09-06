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
}
