package com.resonance.player.playback

/** A coherent view of the current native media item, read on the UI dispatcher. */
data class NativePlaybackState(
    val trackId: String,
    val isPlaying: Boolean,
    val playWhenReady: Boolean,
    val isBuffering: Boolean,
    val progress: Float,
    val positionMs: Long?,
)
