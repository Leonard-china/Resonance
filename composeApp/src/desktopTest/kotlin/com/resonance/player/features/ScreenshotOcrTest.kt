package com.resonance.player.features

import com.resonance.player.media.SongIdentityResolver
import com.resonance.player.media.PublicSongMetadataLookup
import com.resonance.player.model.*
import com.resonance.player.ocr.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import kotlin.test.*

class ScreenshotOcrTest {
    private fun line(text: String, y: Int, height: Int = 32, x: Int = 90) = OcrLine(text, x, y, 650, y + height)

    @Test fun parserPairsSongsAndSkipsControlsBadgesAndOverlappingRows() {
        val rows = listOf(line("歌曲", 10), OcrLine("1", 20, 100, 45, 130), line("晴天", 100), line("VIP", 100, x = 700),
            line("周杰伦 - 叶惠美", 140, 25), line("See You Again...", 210), line("Wiz Khalifa - Furious 7", 250, 25), line("下载", 330))
        val lists = ScreenshotPlaylistParser.parse(listOf(OcrPage(900, 900, rows), OcrPage(900, 900, rows)))
        assertEquals(1, lists.size)
        assertEquals(listOf("晴天", "See You Again..."), lists.single().songs.map { it.title })
        assertEquals("周杰伦", lists.single().songs.first().artist)
        assertEquals("叶惠美", lists.single().songs.first().album)
    }

    @Test fun parserDoesNotInventArtistsFromEmptyOrUnpairedText() {
        assertTrue(ScreenshotPlaylistParser.parse(listOf(OcrPage(900, 900, listOf(line("只有一个歌名", 100))))).isEmpty())
    }

    @Test fun numericSongTitlesArePreservedWhileRankingColumnIsIgnored() {
        val rows = listOf(OcrLine("1", 20, 100, 42, 130), OcrLine("7", 100, 100, 125, 135),
            OcrLine("Artist - Album", 100, 145, 500, 171))
        val songs = ScreenshotPlaylistParser.parse(listOf(OcrPage(900, 900, rows))).single().songs
        assertEquals("7", songs.single().title)
        assertEquals("Artist", songs.single().artist)
    }

    @Test fun headerAndCreatorAbovePlaybackToolbarAreNotImportedAsMusic() {
        val rows = listOf(line("我的通勤歌单", 30, 48), line("小明", 90, 24), line("播放全部 20首", 150),
            line("晴天", 220, 36), line("周杰伦 - 叶惠美", 265, 26))
        val list = ScreenshotPlaylistParser.parse(listOf(OcrPage(900, 900, rows))).single()
        assertEquals("我的通勤歌单", list.name)
        assertEquals(listOf("晴天"), list.songs.map { it.title })
    }

    @Test fun recognitionModeDefaultsLocallyAndRoundTripsWithoutCredentials() {
        assertEquals(ScreenshotRecognition.LocalOcr, MusicFeatureCodec.settings(JSONObject()).screenshotRecognition)
        for (mode in ScreenshotRecognition.entries) {
            val settings = MusicFeatureSettings(screenshotRecognition = mode)
            assertEquals(mode, MusicFeatureCodec.settings(MusicFeatureCodec.encode(settings)).screenshotRecognition)
        }
        assertEquals(ScreenshotRecognition.LocalOcr, MusicFeatureCodec.settings(JSONObject().put("screenshotRecognition", "future")).screenshotRecognition)
    }

    @Test fun truncatedSongIsCompletedOnlyFromUnambiguousMatchingEvidence() {
        val request = SongRequest("See You Ag...", "Wiz Khalifa")
        val full = SongRequest("See You Again", "Wiz Khalifa", "Furious 7")
        assertEquals(full, SongIdentityResolver.choose(request, listOf(full, full, full.copy(artist = "Someone Else"), full.copy(title = "See You Again (Live)"))))
        assertFailsWith<IllegalStateException> { SongIdentityResolver.choose(request, listOf(full, full.copy(title = "See You Again and Again"))) }
        assertNull(SongIdentityResolver.choose(request, listOf(full.copy(artist = "Someone Else"))))
        assertFalse(SongIdentityResolver.truncated("Waiting...For You"))
        assertTrue(SongIdentityResolver.truncated("好久不见…"))
        val video = BilibiliVideo("BV1234567890", 1L, "Wiz Khalifa《See You Again》", "Official", "", 180)
        assertTrue(SongIdentityResolver.videoNames(request.copy(artist = "Wiz..."), listOf(video)).isEmpty())
        assertEquals("See You Again", SongIdentityResolver.videoNames(request, listOf(video)).single().title)
    }

    @Test fun publicCatalogParsingAndRelaxedSearchStillRequireTheVisibleTitleAndArtist() {
        val request = SongRequest("See You Ag...", "Wiz Khalifa")
        assertTrue("See You Wiz Khalifa" in PublicSongMetadataLookup.terms(request))
        val english = PublicSongMetadataLookup.parse("""{"results":[{"trackName":"See You Again (feat. Charlie Puth)","artistName":"Wiz Khalifa","collectionName":"Furious 7"}]}""")
        assertEquals("See You Again (feat. Charlie Puth)", SongIdentityResolver.choose(request, english)?.title)
        val chinese = PublicSongMetadataLookup.parse("""{"code":0,"data":{"song":{"list":[{"songname":"说好的幸福呢","singer":[{"name":"周杰伦"}],"albumname":"魔杰座"},{"songname":"说好的幸福呢 (DJ 凌熙版)","singer":[{"name":"周杰伦"}]}]}}}""")
        assertEquals("说好的幸福呢", SongIdentityResolver.choose(SongRequest("说好的幸...", "周杰伦"), chinese)?.title)
        assertFalse(SongIdentityResolver.matches(SongRequest("说好的幸...", "周杰伦"), SongRequest("说好的幸福呢 (节奏布鲁斯版)", "周杰伦")))
        assertFalse(SongIdentityResolver.matches(SongRequest("说好的幸...", "周杰伦"), SongRequest("说好的幸福呢+淘汰+青花瓷", "周杰伦")))
        assertTrue(SongIdentityResolver.matches(SongRequest("Ol...", "Artist"), SongRequest("Olive", "Artist")))
        assertEquals("See You Again", SongIdentityResolver.searchTitle("See You Again (feat. Charlie Puth)"))
        assertEquals("See You Again (Live)", SongIdentityResolver.searchTitle("See You Again (Live)"))
    }

    @Test fun bundledModelsActuallyReadChineseAndEnglishScreenshot() = runBlocking {
        // Fixed synthetic pixels keep CI independent of installed CJK font packs.
        val bytes = requireNotNull(ScreenshotOcrTest::class.java.getResourceAsStream("/ocr/playlist-fixture.png")).use { it.readBytes() }
        val pages = desktopScreenshotOcr().recognize(listOf(AiImage(bytes, "image/png"))) {}
        val text = pages.flatMap { it.lines }.joinToString("\n") { it.text }
        println("Bundled local OCR: $text")
        assertTrue(text.contains("晴天"), text)
        assertTrue(text.contains("周杰伦"), text)
        assertTrue(text.contains("See You Again", ignoreCase = true), text)
        val songs = ScreenshotPlaylistParser.parse(pages).flatMap { it.songs }
        assertTrue(songs.any { it.title == "晴天" && it.artist == "周杰伦" }, songs.toString())
        assertTrue(songs.any { it.title.contains("See You Again") && it.artist.contains("Wiz Khalifa") }, songs.toString())
    }
}
