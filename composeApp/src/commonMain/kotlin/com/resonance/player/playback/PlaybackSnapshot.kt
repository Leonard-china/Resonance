package com.resonance.player.playback

data class PlaybackSnapshot(
    val trackId: String,
    val queueIds: List<String>,
    val progress: Float = 0f,
    val playlistId: String? = null,
)

data class PlaybackResume(val snapshot: PlaybackSnapshot, val isPlaying: Boolean = false, val attached: Boolean = false)
