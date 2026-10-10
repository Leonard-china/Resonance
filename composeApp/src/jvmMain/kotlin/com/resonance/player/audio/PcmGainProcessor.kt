package com.resonance.player.audio

import kotlin.math.abs
import kotlin.math.tanh

/** Stereo little-endian PCM16 gain with a 192 ms ramp and a soft peak knee. */
class PcmGainProcessor(initialGain: Double = 1.0, private val sampleRate: Int = 48_000, private val channels: Int = 2) {
    private var currentGain = initialGain
    private var previousTarget = initialGain
    private var step = 0.0
    private var remaining = 0

    fun process(bytes: ByteArray, count: Int, targetGain: Double) {
        val target = targetGain.takeIf(Double::isFinite)?.coerceIn(0.0, 8.0) ?: 0.0
        if (target != previousTarget) {
            remaining = (sampleRate * 0.192).toInt().coerceAtLeast(1)
            step = (target - currentGain) / remaining
            previousTarget = target
        }
        for (frame in 0 until count / (channels * 2)) {
            if (remaining > 0) {
                currentGain += step
                if (--remaining == 0) currentGain = previousTarget
            }
            for (channel in 0 until channels) {
                val offset = (frame * channels + channel) * 2
                val sample = ((bytes[offset].toInt() and 0xff) or (bytes[offset + 1].toInt() shl 8)).toShort().toInt()
                val amplified = sample / 32768.0 * currentGain
                val magnitude = abs(amplified)
                val limited = if (magnitude <= 0.98) amplified else
                    (0.98 + 0.02 * tanh((magnitude - 0.98) / 0.02)) * if (amplified < 0) -1 else 1
                val value = (limited * 32768).toInt().coerceIn(-32768, 32767)
                bytes[offset] = value.toByte()
                bytes[offset + 1] = (value shr 8).toByte()
            }
        }
    }
}
