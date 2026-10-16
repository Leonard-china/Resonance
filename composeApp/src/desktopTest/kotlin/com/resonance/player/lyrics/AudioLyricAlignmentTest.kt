package com.resonance.player.lyrics

import com.resonance.player.model.*
import org.json.JSONObject
import kotlin.test.*

class AudioLyricAlignmentTest {
    @Test fun alignsRepeatedChorusesToTheirActualAudioOccurrences() {
        val original = Lyrics("t", listOf("你好世界", "再见朋友", "你好世界").map { LyricLine(null, it) }, false, false, "plain")
        val words = listOf(SpeechWord("你好世界", 9000, 14000), SpeechWord("再见朋友", 21000, 25000), SpeechWord("你好世界", 40000, 45000))
        val recognized = SpeechRecognition(words.map { LyricLine(it.beginMs, it.text) }, words, 50)
        val aligned = AudioLyricAlignment.align(original, recognized)
        assertEquals(listOf(9000L, 21000L, 40000L), aligned.map { it.timestampMs })
        assertEquals(original.lines.map { it.text }, aligned.map { it.text })
    }
    @Test fun unrelatedLyricsNeverReceiveInventedTimestamps() {
        val original = Lyrics("t", listOf(LyricLine(null, "完全不一样的另一首歌曲")), false, false, "plain")
        val result = SpeechRecognition(listOf(LyricLine(3000, "hello world")), listOf(SpeechWord("hello world", 3000, 7000)), 10)
        assertEquals(result.lines, AudioLyricAlignment.align(original, result))
    }
    @Test fun partialSpeechEventsAreIgnoredAndFinalWordTimesArePreserved() {
        fun event(final: Boolean) = JSONObject("""{"payload":{"output":{"sentence":{"sentence_end":$final,"begin_time":1200,"end_time":2400,"text":"Hello","words":[{"text":"Hello","begin_time":1234,"end_time":2300}]}}}}""")
        assertNull(ParaformerLyricsClient.parseSentence(event(false)))
        val result = assertNotNull(ParaformerLyricsClient.parseSentence(event(true)))
        assertEquals(1200L, result.first.timestampMs)
        assertEquals(1234L, result.second.single().beginMs)
    }
    @Test fun zeroOnlyAndOutOfDurationLrcAreNotSynchronized() {
        assertFalse(usableTimeline(parseLrc("[00:00]first\n[00:00]second"), 180))
        assertFalse(usableTimeline(parseLrc("[00:10]first\n[05:00]second"), 180))
        assertTrue(usableTimeline(parseLrc("[00:10]first\n[00:30]second"), 180))
    }
}
