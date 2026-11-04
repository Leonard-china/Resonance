package com.resonance.player.platform

data class PlaybackIssue(
    val message: String,
    val source: Source = Source.Playback,
    val trackId: String? = null,
    val requestId: Long? = null,
) {
    enum class Source { Playback, Preview, Statistics, Preferences }
    val interruptsPlayback: Boolean get() = source == Source.Playback
    fun matches(currentTrack: String?, currentRequest: Long?): Boolean = !interruptsPlayback ||
        ((trackId == null || trackId == currentTrack) && (requestId == null || requestId == currentRequest))
}
