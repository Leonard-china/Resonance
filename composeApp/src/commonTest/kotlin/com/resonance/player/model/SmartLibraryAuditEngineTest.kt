package com.resonance.player.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartLibraryAuditEngineTest {

    @Test
    fun auditCleansGarbagePrefixAndParsesArtistFromTitle() {
        val tracks = listOf(
            Track(
                id = "t1",
                title = "[320k] 周杰伦 - 晴天.mp3",
                artist = "未知歌手",
                album = "本地音乐",
                durationText = "4:29",
                artworkSeed = 1,
            )
        )
        val report = SmartLibraryAuditEngine.audit(tracks)
        assertEquals(1, report.tagIssueCount)
        val draft = report.tagDrafts.first()
        assertEquals("晴天", draft.suggestedTitle)
        assertEquals("周杰伦", draft.suggestedArtist)
        assertTrue(draft.reason.contains("垃圾前缀") || draft.reason.contains("分离标题"))
    }

    @Test
    fun auditSuggestsReviewWithoutPreselectingDestructiveActions() {
        val tracks = listOf(
            Track("1", "七里香", "周杰伦", "七里香", "4:59", 1, mimeType = "audio/mpeg"),
            Track("2", "七里香", "周杰伦", "七里香", "4:59", 2, mimeType = "audio/flac", isFavorite = true),
        )
        val report = SmartLibraryAuditEngine.audit(tracks)
        assertEquals(1, report.duplicateGroupCount)
        val group = report.duplicateGroups.first()
        // ID 2 is flac and favorite, should be kept
        assertEquals("2", group.recommendedToKeepId)
        assertTrue(group.selectedToCleanIds.isEmpty())
    }
    @Test fun recordingVersionsDurationsAndUnknownArtistsAreNotInterchangeable() {
        val base = Track("studio", "Song", "Artist", "Album", "3:00", 0)
        val distinct = listOf(base, base.copy(id = "live", title = "Song (Live)", durationText = "8:00"),
            base.copy(id = "remix", title = "Song (Remix)"),
            base.copy(id = "bracket-live", title = "[Live] Song"), base.copy(id = "karaoke", title = "【伴奏】Song"),
            base.copy(id = "cover", title = "Song (翻唱)"), base.copy(id = "long", durationText = "5:00"),
            base.copy(id = "unknown-a", artist = "未知歌手"), base.copy(id = "unknown-b", artist = "未知歌手"))
        val report = SmartLibraryAuditEngine.audit(distinct)
        assertTrue(report.duplicateGroups.isEmpty())
        assertTrue(report.tagDrafts.none { it.suggestedTitle == "Song" && it.trackId in setOf("bracket-live", "karaoke") })
    }

}
