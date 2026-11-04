package com.resonance.player.data

import com.resonance.player.model.PlaybackModePreference
import com.resonance.player.model.RepeatMode

/** Capture both values at the user action, then serialize them with other preference writes. */
class PlaybackModeWriter(private val queue: PersistenceQueue, private val persist: suspend (PlaybackModePreference) -> Unit) {
    fun save(shuffle: Boolean, repeat: RepeatMode) {
        val snapshot = PlaybackModePreference(shuffle, repeat)
        queue.submitLatest("playback.mode") { persist(snapshot) }
    }
}
