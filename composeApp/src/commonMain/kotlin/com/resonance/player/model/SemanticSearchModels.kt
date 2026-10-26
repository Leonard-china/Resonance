package com.resonance.player.model

data class SemanticSearchQueryIntent(
    val rawQuery: String,
    val artists: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val scenes: List<String> = emptyList(),
    val bpmRange: IntRange? = null,
    val keywords: List<String> = emptyList(),
)

data class SemanticTrackMatch(
    val track: Track? = null,
    val sourceSong: SourceSong? = null,
    val title: String,
    val artist: String,
    val album: String,
    val reason: String,
    val confidence: Float,
    val isLocal: Boolean,
)
