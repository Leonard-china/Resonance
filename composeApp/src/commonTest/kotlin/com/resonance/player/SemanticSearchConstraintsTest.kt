package com.resonance.player

import com.resonance.player.model.*
import kotlin.test.*

class SemanticSearchConstraintsTest {
    private val tracks = listOf(
        Track("jay", "晴天", "周杰伦", "流行", "3:00", 0),
        Track("other", "另一个晴天", "其他歌手", "摇滚", "3:00", 0),
        Track("unknown", "晴天", "周杰伦", "流行", "3:00", 0),
    )
    private val bpms = mapOf("jay" to 120, "other" to 120)
    @Test fun bpmCannotReplaceExplicitArtistOrTitleConstraints() {
        assertEquals(listOf("jay"), matchLocalSemanticTracks(SemanticSearchIntentParser.parse("周杰伦 120 BPM"), tracks, bpms).map { it.track!!.id })
        assertTrue(matchLocalSemanticTracks(SemanticSearchIntentParser.parse("周杰伦 七里香 120 BPM"), tracks, bpms).isEmpty())
        assertTrue(matchLocalSemanticTracks(SemanticSearchIntentParser.parse("周杰伦 150-170 BPM"), tracks, bpms).isEmpty())
    }
    @Test fun explicitGenreStillFiltersAndUnknownBpmIsExcluded() {
        assertEquals(listOf("other"), matchLocalSemanticTracks(SemanticSearchIntentParser.parse("摇滚 120 BPM"), tracks, bpms).map { it.track!!.id })
        assertEquals(listOf("jay"), matchLocalSemanticTracks(SemanticSearchIntentParser.parse("流行 120 BPM"), tracks, bpms).map { it.track!!.id })
    }
    @Test fun tempoOnlyAndSceneSuggestionsDoNotRequireSceneWordsInSongNames() {
        for (query in listOf("120 BPM", "跑步", "适合跑步的音乐 120 BPM")) {
            assertEquals(setOf("jay", "other"), matchLocalSemanticTracks(SemanticSearchIntentParser.parse(query), tracks, bpms).map { it.track!!.id }.toSet())
        }
    }
    @Test fun textSearchAndExplicitArtistFieldStillWorkWithoutBpm() {
        assertEquals(listOf("jay", "unknown"), matchLocalSemanticTracks(SemanticSearchIntentParser.parse("周杰伦 晴天"), tracks, emptyMap()).map { it.track!!.id })
        assertEquals(listOf("other"), matchLocalSemanticTracks(SemanticSearchQueryIntent("", artists = listOf("其他歌手")), tracks, bpms).map { it.track!!.id })
    }
}
