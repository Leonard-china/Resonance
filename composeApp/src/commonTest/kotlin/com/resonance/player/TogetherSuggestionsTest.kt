package com.resonance.player

import com.resonance.player.model.togetherSuggestionIndices
import kotlin.test.*

class TogetherSuggestionsTest {
    @Test fun indicesReferOnlyToTheCapturedCandidateList() {
        assertEquals(listOf(2, 0, 4), togetherSuggestionIndices(" 3, 1,5 ", 5))
    }
    @Test fun malformedInventedAndDuplicateCandidatesAreRejected() {
        listOf("0", "6", "1,1", "1,2,3,4,5,6", "1;delete", "歌曲1", "2147483648", "").forEach {
            assertFailsWith<IllegalArgumentException>(it) { togetherSuggestionIndices(it, 5) }
        }
    }
}
