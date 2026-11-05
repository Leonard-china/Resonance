package com.resonance.player

import com.resonance.player.model.ImportReport
import com.resonance.player.model.Track
import com.resonance.player.model.Playlist
import kotlin.test.Test
import kotlin.test.assertContains

class ImportReportMessageTest {
    @Test
    fun packageImportReportsPlaylistsAndDoesNotClaimAudioWasConverted() {
        val report = ImportReport(tracks(1), playlists = listOf(Playlist("p", "歌单", "1 首", tracks(1), 0)), importedPackageCount = 1)
        val message = importReportMessage(report, conversionRequested = true).orEmpty()
        assertContains(message, "已导入 1 首音乐、1 个歌单")
        assertContains(message, "歌单包内音频保留原格式")
    }

    @Test
    fun emptyPlaylistPackageHasVisibleSuccessMessage() {
        val report = ImportReport(emptyList(), playlists = listOf(Playlist("p", "空歌单", "0 首", emptyList(), 0)), importedPackageCount = 1)
        assertContains(importReportMessage(report, false).orEmpty(), "已导入 0 首音乐、1 个歌单")
    }

    @Test
    fun fullSuccessReportsConvertedCount() {
        val message = importReportMessage(
            ImportReport(tracks = tracks(21)),
            conversionRequested = true,
        ).orEmpty()

        assertContains(message, "已转换 21 首音乐")
    }

    @Test
    fun realConversionProblemIsReportedAsNotProcessed() {
        val message = importReportMessage(
            ImportReport(
                tracks = tracks(18),
                skippedCount = 3,
                warnings = listOf("转换失败 1/21：文件损坏"),
            ),
            conversionRequested = true,
        ).orEmpty()

        assertContains(message, "已转换 18 首")
        assertContains(message, "3 首未处理")
        assertContains(message, "已存在的 MP3 不受影响")
    }

    private fun tracks(count: Int) = List(count) { index ->
        Track(
            id = "track-$index",
            title = "Track $index",
            artist = "Artist",
            album = "Album",
            durationText = "0:01",
            artworkSeed = index,
        )
    }
}
