package com.resonance.player.playback

import kotlin.math.min

/** Native-session accounting; UI recreation/pause does not start another listening pass. */
class ListeningSessionTracker(private val record: (String, Int, Boolean) -> Unit) {
    private var id: String? = null
    private var position = 0L
    private var duration = 0L
    private var clock = 0L
    private var speed = 1f
    private var playing = false
    private var remainder = 0.0
    private var heard = 0.0
    private var wallBudget = 0.0
    private var completed = false

    fun start(trackId: String?, positionMs: Long, nowMs: Long) {
        id = trackId?.takeUnless { it.isBlank() || it.startsWith("together:") || it == "source-preview" }
        position = positionMs.coerceAtLeast(0); clock = nowMs
        duration = 0; speed = 1f; playing = false
        remainder = 0.0; heard = 0.0; wallBudget = 0.0; completed = false
    }

    fun sample(trackId: String?, positionMs: Long, durationMs: Long, isPlaying: Boolean,
               playbackSpeed: Float, nowMs: Long, discontinuity: Boolean = false) {
        val eligible = trackId?.takeUnless { it.isBlank() || it.startsWith("together:") || it == "source-preview" }
        if (id != eligible) start(eligible, positionMs, nowMs)
        val elapsed = (nowMs - clock).coerceAtLeast(0)
        val movement = positionMs - position
        // Cap elapsed time by real media advancement; buffering, seek jumps and speed changes
        // must not manufacture listened seconds. A final pause sample can settle the last interval.
        if (id != null && playing && !discontinuity) wallBudget += elapsed
        if (id != null && playing && !discontinuity && movement > 0) {
            val milliseconds = min(wallBudget, movement.toDouble() / speed)
            wallBudget -= milliseconds
            heard += milliseconds; remainder += milliseconds
            val seconds = (remainder / 1000).toInt()
            val complete = !completed && heard >= 1000 && durationMs > 0 && positionMs >= durationMs * .95
            if (complete) completed = true
            if (seconds > 0 || complete) {
                remainder -= seconds * 1000.0
                record(id!!, seconds, complete)
            }
        }
        if (discontinuity || !isPlaying) wallBudget = 0.0
        position = positionMs.coerceAtLeast(0); duration = durationMs.coerceAtLeast(0)
        clock = nowMs; playing = isPlaying
        speed = playbackSpeed.takeIf { it.isFinite() && it > 0 } ?: 1f
    }

    /** Call only for a native natural end/automatic repeat, never for seek, stop or failure. */
    fun finish(nowMs: Long) {
        sample(id, maxOf(position, duration), duration, false, speed, nowMs)
        if (id != null && !completed && heard > 0) {
            completed = true
            record(id!!, 0, true)
        }
        playing = false
    }
}
