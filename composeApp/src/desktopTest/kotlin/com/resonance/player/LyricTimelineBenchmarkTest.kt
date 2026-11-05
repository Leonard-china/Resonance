package com.resonance.player

import com.resonance.player.model.*
import kotlin.test.*
import kotlin.system.measureNanoTime

/** Bounded CPU comparison, not a rendering FPS or physical-device benchmark. */
class LyricTimelineBenchmarkTest {
    @Test fun indexedLookupMatchesLinearReferenceForLongLyrics() {
        val lines = (0 until 2000).map { LyricLine(it * 1000L, "Synthetic lyric $it") }
        val timeline = LyricTimeline(lines)
        val positions = LongArray(20000) { ((it.toLong() * 9973) % 2000000) }
        repeat(5) { for (position in positions) timeline.activeIndex(position) }
        val linearTimes = mutableListOf<Long>(); val indexedTimes = mutableListOf<Long>()
        repeat(5) {
            var reference = 0L; var actual = 0L
            linearTimes += measureNanoTime { for (position in positions) reference += lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= position } }
            indexedTimes += measureNanoTime { for (position in positions) actual += timeline.activeIndex(position) }
            assertEquals(reference, actual)
        }
        println("Lyric lookup, 2000 lines / 20000 positions: linearMedianNs=${linearTimes.sorted()[2]}, indexedMedianNs=${indexedTimes.sorted()[2]}")
    }
}
