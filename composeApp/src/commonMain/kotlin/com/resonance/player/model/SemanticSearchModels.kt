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

fun matchLocalSemanticTracks(intent: SemanticSearchQueryIntent, tracks: List<Track>, bpms: Map<String, Int>): List<SemanticTrackMatch> =
    tracks.mapNotNull { track ->
        val text = "${track.title} ${track.artist} ${track.album}".lowercase()
        val bpm = bpms[track.id]
        if (intent.bpmRange != null && (bpm == null || bpm !in intent.bpmRange)) return@mapNotNull null
        // Explicit names and genres constrain results even when tempo also matches.
        // Scene/mood hints can still rank BPM-based suggestions without matching song titles.
        val requiredText = (intent.keywords + intent.artists + intent.genres).distinct()
        if (requiredText.any { !text.contains(it.lowercase()) }) return@mapNotNull null
        val hits = (requiredText + intent.moods + intent.scenes).distinct().filter { text.contains(it.lowercase()) }
        if (hits.isEmpty() && intent.bpmRange == null) return@mapNotNull null
        val reason = (hits.map { "匹配：$it" } + if (intent.bpmRange != null) listOf("约 $bpm BPM · 本地估算") else emptyList()).joinToString(" · ")
        SemanticTrackMatch(track = track, title = track.title, artist = track.artist, album = track.album,
            reason = reason, confidence = hits.size * 25f + if (intent.bpmRange != null) 30f else 0f, isLocal = true)
    }.sortedByDescending { it.confidence }
