package com.resonance.player.playback

import kotlin.math.abs

/**
 * Hysteresis and gentle tempo adaptation prevent small timing noise and clock drift
 * from causing repeated audible seeks that flush the audio decoder.
 */
class TogetherSyncPolicy {
    private var lastSeekMs = Long.MIN_VALUE

    fun shouldSeek(positionMs: Long, targetMs: Long, nowMs: Long, playing: Boolean): Boolean {
        val drift = abs(positionMs - targetMs)
        if (!playing) {
            if (drift <= 80L) return false
            lastSeekMs = nowMs
            return true
        }
        // During active playback, only large gaps (>= 1.2s) trigger a hard seek that flushes the decoder.
        // Smaller drifts are smoothly eliminated via tempo adaptation without buffer interruption.
        if (drift < 1_200L) return false
        if (lastSeekMs != Long.MIN_VALUE && nowMs - lastSeekMs < 3_500L) return false
        lastSeekMs = nowMs
        return true
    }

    /** Gently nudges playback speed by ±2.5% for moderate drift (200-1200ms) to converge seamlessly without seeking. */
    fun effectiveSpeed(positionMs: Long, targetMs: Long, baseSpeed: Float, playing: Boolean): Float {
        if (!playing || !baseSpeed.isFinite() || baseSpeed <= 0f) return baseSpeed
        val delta = targetMs - positionMs // positive: local is behind, speed up; negative: local is ahead, slow down
        return when {
            delta > 200L -> (baseSpeed * 1.025f).coerceIn(0.25f, 4f)
            delta < -200L -> (baseSpeed * 0.975f).coerceIn(0.25f, 4f)
            else -> baseSpeed
        }
    }
}
