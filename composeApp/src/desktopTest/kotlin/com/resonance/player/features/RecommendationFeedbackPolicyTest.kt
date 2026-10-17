package com.resonance.player.features

import com.resonance.player.model.*
import org.json.JSONObject
import kotlin.test.*

class RecommendationFeedbackPolicyTest {
    @Test fun feedbackUsesMusicalIdentityAndRemovingVoteKeepsComment() {
        val root = JSONObject()
        val original = RecommendationFeedback("Song Ａ", "Artist", vote = RecommendationVote.Like, comment = "喜欢轻快的旋律")
        RecommendationFeedbackCodec.write(root, original)
        val key = RecommendationPolicy.identity("Song A", "Artist")
        assertEquals(original, RecommendationFeedbackCodec.read(root)[key])
        RecommendationFeedbackCodec.write(root, original.copy(vote = RecommendationVote.Neutral))
        assertEquals(original.comment, RecommendationFeedbackCodec.read(root).getValue(key).comment)
        RecommendationFeedbackCodec.write(root, original.copy(vote = RecommendationVote.Neutral, comment = ""))
        assertTrue(RecommendationFeedbackCodec.read(root).isEmpty())
    }

    @Test fun likesAndNegativeCommentsChangeCandidateRankingWithoutBanningAnArtist() {
        val feedback = listOf(RecommendationFeedback("Old", "Artist", "Album", RecommendationVote.Like, "喜欢轻快，少推摇滚"))
        val similar = SongRequest("轻快新歌", "Artist", "Album")
        val loud = SongRequest("摇滚新歌", "Other")
        assertTrue(RecommendationFeedbackPolicy.score(similar, feedback) > RecommendationFeedbackPolicy.score(loud, feedback))
        assertEquals(-1, RecommendationFeedbackPolicy.commentSignals("不要太吵的摇滚")["摇滚"])
        assertTrue(RecommendationFeedbackPolicy.seeds(emptyList(), feedback).contains("Artist"))
        assertTrue(RecommendationFeedbackPolicy.seeds(emptyList(), feedback).contains("轻快"))
    }

    @Test fun favoritesHaveMoreSeedWeightThanLikesAndAiSummaryIsBounded() {
        val favorite = Track("saved", "Saved", "Favorite Artist", "Album", "3:00", 0, isFavorite = true)
        val feedback = (0..29).map { RecommendationFeedback("Song$it", "Artist$it", vote = RecommendationVote.Like, comment = "a".repeat(500), updatedAt = it.toLong()) }
        assertEquals("Favorite Artist", RecommendationFeedbackPolicy.seeds(listOf(favorite), feedback).first())
        val summary = org.json.JSONArray(RecommendationFeedbackPolicy.summary(feedback))
        assertEquals(24, summary.length())
        assertEquals(160, summary.getJSONObject(0).getString("comment").length)
    }
}
