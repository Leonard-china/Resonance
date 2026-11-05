package com.resonance.player

import com.resonance.player.model.*
import kotlin.test.*

class OfflineLibraryImprovementsTest {
    private fun song(id: String, source: String = "/old/$id.flac") = Track(id, id, "Artist", "Album", "4:00", 0, isFavorite = true, sourceUri = source)
    @Test fun relinkingOnlySuggestsUniqueFileNamesAndNeverSharesOneFileAcrossIds() {
        val one = AudioRelinkCandidate("/new/a.flac", "a.flac", "a.flac")
        assertEquals("a", matchAudioRelinks(listOf(song("a")), listOf(one)).single().track.id)
        assertEquals("a", matchAudioRelinks(listOf(song("a", """C:\Old\Music\a.flac""")), listOf(one)).single().track.id)
        assertEquals("a", matchAudioRelinks(listOf(song("a", "content://documents/document/primary%3AMusic%2Fa.flac")), listOf(one)).single().track.id)
        assertTrue(matchAudioRelinks(listOf(song("a")), listOf(one, one.copy(sourceUri = "/other/a.flac"))).isEmpty())
        assertTrue(matchAudioRelinks(listOf(song("a"), song("b", "/also/a.flac")), listOf(one)).isEmpty())
        assertTrue(matchAudioRelinks(listOf(song("live", "/old/a-live.flac")), listOf(one)).isEmpty())
    }
    @Test fun realBpmRangeExcludesUnknownAndOutOfRangeSongs() {
        val intent = SemanticSearchIntentParser.parse("120-140 BPM")
        assertEquals(120..140, intent.bpmRange)
        val tracks = listOf(song("a"), song("b"), song("c"))
        val result = matchLocalSemanticTracks(intent, tracks, mapOf("a" to 128, "b" to 90))
        assertEquals(listOf("a"), result.map { it.track!!.id })
        assertTrue(result.single().reason.contains("本地估算"))
    }
    @Test fun timestampIndexPreservesDuplicateNullAndUnorderedBehavior() {
        for (lines in listOf(listOf(LyricLine(0, "a"), LyricLine(100, "b"), LyricLine(100, "c"), LyricLine(null, "d")),
            listOf(LyricLine(200, "a"), LyricLine(100, "b"), LyricLine(null, "c")))) {
            val timeline = LyricTimeline(lines)
            for (position in listOf(-1L, 0L, 99L, 100L, 200L, 1000L)) {
                assertEquals(lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= position }, timeline.activeIndex(position))
            }
        }
        assertEquals(-1, LyricTimeline(emptyList()).activeIndex(100))
    }
}
