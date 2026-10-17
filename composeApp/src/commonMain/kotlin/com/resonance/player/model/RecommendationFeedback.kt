package com.resonance.player.model

enum class RecommendationVote { Neutral, Like, Dislike }

/** A preference is independent of favorites, playlist membership and downloaded files. */
data class RecommendationFeedback(
    val title: String,
    val artist: String,
    val album: String = "",
    val vote: RecommendationVote = RecommendationVote.Neutral,
    val comment: String = "",
    val updatedAt: Long = 0,
)
