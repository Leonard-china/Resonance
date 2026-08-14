package com.resonance.player

import com.resonance.player.model.ImportReport
import com.resonance.player.model.Track
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class ImportReportMessageTest {
    @Test
    fun cleanupFailureDoesNotClaimConversionFailed() {
        val message = importReportMessage(
            ImportReport(
                tracks = tracks(21),
                cleanupWarnings = listOf("源文件没有删除权限", "另一个源文件没有删除权限"),
            ),
            conversionRequested = true,
        ).orEmpty()

        assertContains(message, "21 首 MP3 已保存")
        assertContains(message, "2 个源 KGMA")
        assertFalse("失败" in message)
    }

    @Test
    fun realConversionProblemIsReportedAsNotProcessed() {
        val message = importReportMessage(
            ImportReport(
                tracks = tracks(18),
                skippedCount = 3,
                warnings = listOf("转换失败 1/21：文件损坏"),
                cleanupWarnings = listOf("源文件没有删除权限"),
            ),
            conversionRequested = true,
        ).orEmpty()

        assertContains(message, "已转换 18 首")
        assertContains(message, "3 首未处理")
        assertContains(message, "1 个源 KGMA 未删除")
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
