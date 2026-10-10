package com.resonance.player

import com.resonance.player.model.Track
import com.resonance.player.util.LibrarySearchIndex
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.TimeSource

class LibrarySearchIndexTest {
    @Test fun indexedSearchPreservesChineseInitialsAndPunctuationMatching() = runTest {
        val track = Track("1", "夜曲", "周杰伦", "November's Chopin", "3:00", 1)
        val index = LibrarySearchIndex.build(listOf(track))
        listOf("夜曲", "zjl", "NOVEMBER", "Novembers Chopin").forEach {
            assertEquals(listOf(track), index.search(it))
        }
        assertTrue(index.search("missing").isEmpty())
        assertTrue(index.search("  ").isEmpty())
    }

    @Test fun searchesTenThousandTracksWithAReusableIndex() = runTest {
        val tracks = (0 until 10_000).map { Track("$it", "夜曲 $it", "周杰伦", "album $it", "3:00", it) }
        val buildStart = TimeSource.Monotonic.markNow()
        val index = LibrarySearchIndex.build(tracks)
        val buildTime = buildStart.elapsedNow()
        val searchStart = TimeSource.Monotonic.markNow()
        repeat(20) { assertEquals(10_000, index.search("zjl").size) }
        println("10000-track index: $buildTime; 20 searches: ${searchStart.elapsedNow()}")
    }
}
