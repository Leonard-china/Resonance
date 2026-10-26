package com.resonance.player.audio

import kotlin.math.abs
import kotlin.math.tanh

/** Stereo little-endian PCM16 gain with a 192 ms ramp, soft peak knee, and center-channel vocal suppression (karaoke mode). */
class PcmGainProcessor(
    initialGain: Double = 1.0,
    private val sampleRate: Int = 48_000,
    private val channels: Int = 2,
    initialVocalSuppression: Double = 0.0,
) {
    private var currentGain = initialGain
    private var previousTarget = initialGain
    private var step = 0.0
    private var remaining = 0
    private var currentVocalSuppression = initialVocalSuppression.coerceIn(0.0, 1.0)

    // 1-pole low-pass filter state for retaining bass frequencies (< ~160 Hz) during vocal suppression
    private var lowPassState = 0.0
    private val lowPassAlpha = (2.0 * Math.PI * 160.0 / sampleRate).coerceIn(0.001, 0.2)

    fun process(bytes: ByteArray, count: Int, targetGain: Double, vocalSuppression: Double = currentVocalSuppression) {
        val target = targetGain.takeIf(Double::isFinite)?.coerceIn(0.0, 8.0) ?: 0.0
        val targetVocal = vocalSuppression.coerceIn(0.0, 1.0)
        currentVocalSuppression = targetVocal
        if (target != previousTarget) {
            remaining = (sampleRate * 0.192).toInt().coerceAtLeast(1)
            step = (target - currentGain) / remaining
            previousTarget = target
        }
        val frames = count / (channels * 2)
        val isStereo = channels == 2
        for (frame in 0 until frames) {
            if (remaining > 0) {
                currentGain += step
                if (--remaining == 0) currentGain = previousTarget
            }
            if (isStereo && currentVocalSuppression > 0.0) {
                val offsetL = frame * 4
                val offsetR = offsetL + 2
                val sampleL = ((bytes[offsetL].toInt() and 0xff) or (bytes[offsetL + 1].toInt() shl 8)).toShort().toInt()
                val sampleR = ((bytes[offsetR].toInt() and 0xff) or (bytes[offsetR + 1].toInt() shl 8)).toShort().toInt()

                val normL = sampleL / 32768.0
                val normR = sampleR / 32768.0

                // Center channel (Mid = (L + R) / 2)
                val mid = (normL + normR) * 0.5
                // Extract low bass from center channel
                lowPassState += lowPassAlpha * (mid - lowPassState)
                val midVocal = mid - lowPassState

                // Subtract center mid vocal portion from both channels
                val processedL = normL - currentVocalSuppression * midVocal
                val processedR = normR - currentVocalSuppression * midVocal

                applySample(bytes, offsetL, processedL * currentGain)
                applySample(bytes, offsetR, processedR * currentGain)
            } else {
                for (channel in 0 until channels) {
                    val offset = (frame * channels + channel) * 2
                    val sample = ((bytes[offset].toInt() and 0xff) or (bytes[offset + 1].toInt() shl 8)).toShort().toInt()
                    applySample(bytes, offset, (sample / 32768.0) * currentGain)
                }
            }
        }
    }

    private fun applySample(bytes: ByteArray, offset: Int, amplified: Double) {
        val magnitude = abs(amplified)
        val limited = if (magnitude <= 0.98) amplified else
            (0.98 + 0.02 * tanh((magnitude - 0.98) / 0.02)) * if (amplified < 0) -1 else 1
        val value = (limited * 32768).toInt().coerceIn(-32768, 32767)
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value shr 8).toByte()
    }
}
