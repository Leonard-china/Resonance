package com.resonance.player.audio

import java.io.InputStream
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class AudioFeatures(
    val trackId: String,
    val bpm: Int,
    val energy: Int,
    val calculatedAt: Long = System.currentTimeMillis(),
)

object AudioFeatureAnalyzer {

    /**
     * Analyze raw PCM 16-bit little-endian samples and extract BPM and Energy (0-100).
     * @param bytes PCM 16-bit little-endian data
     * @param sampleRate sample rate (e.g. 48000, 44100, or 22050)
     * @param channels number of channels (1 for mono, 2 for stereo)
     */
    fun analyzePcm(bytes: ByteArray, sampleRate: Int = 48_000, channels: Int = 2): Pair<Int, Int> {
        val totalSamples = bytes.size / (channels * 2)
        if (totalSamples < sampleRate * 2) {
            // Need at least 2 seconds
            return 120 to 50
        }

        // Downsample to ~12 kHz mono for ultra fast and robust rhythm detection
        val downsampleRatio = (sampleRate / 12_000).coerceAtLeast(1)
        val targetRate = sampleRate / downsampleRatio
        val monoLength = totalSamples / downsampleRatio
        val monoSamples = FloatArray(monoLength)

        var monoIdx = 0
        var i = 0
        while (i < totalSamples && monoIdx < monoLength) {
            val offset = i * channels * 2
            var sum = 0f
            for (c in 0 until channels) {
                val byteOffset = offset + c * 2
                val s = ((bytes[byteOffset].toInt() and 0xff) or (bytes[byteOffset + 1].toInt() shl 8)).toShort()
                sum += s / 32768.0f
            }
            monoSamples[monoIdx++] = sum / channels
            i += downsampleRatio
        }

        // Compute short-time energy envelope (frame size ~ 20ms, hop size ~ 10ms -> 100 fps)
        val frameSize = (targetRate * 0.020).roundToInt().coerceAtLeast(16)
        val hopSize = (targetRate * 0.010).roundToInt().coerceAtLeast(8)
        val frameRate = targetRate.toFloat() / hopSize

        val numFrames = (monoIdx - frameSize) / hopSize
        if (numFrames < 100) return 120 to 50

        val energyEnv = FloatArray(numFrames)
        var totalEnergy = 0.0
        var peakEnergy = 0.0f

        for (f in 0 until numFrames) {
            var sumSq = 0.0
            val start = f * hopSize
            for (j in 0 until frameSize) {
                val s = monoSamples[start + j]
                sumSq += s * s
            }
            val rms = sqrt(sumSq / frameSize).toFloat()
            energyEnv[f] = rms
            totalEnergy += rms
            if (rms > peakEnergy) peakEnergy = rms
        }

        val avgRms = (totalEnergy / numFrames).toFloat()
        // Compute energy score normalized between 10 and 99
        val normalizedEnergy = ((avgRms * 180f) + (peakEnergy * 40f)).roundToInt().coerceIn(10, 99)

        // Compute Onset Detection Function (first order spectral/energy flux)
        val onsets = FloatArray(numFrames)
        for (f in 1 until numFrames) {
            val diff = energyEnv[f] - energyEnv[f - 1]
            if (diff > 0f) onsets[f] = diff
        }

        // Autocorrelation over BPM range [55, 205]
        val minBpm = 55
        val maxBpm = 205
        val minLag = (frameRate * 60f / maxBpm).roundToInt().coerceAtLeast(2)
        val maxLag = (frameRate * 60f / minBpm).roundToInt().coerceAtMost(numFrames / 2)

        var bestLag = 0
        var bestCorrelation = -1.0f

        val correlations = FloatArray(maxLag + 1)
        for (lag in minLag..maxLag) {
            var sum = 0f
            val count = numFrames - lag
            for (k in 0 until count) {
                sum += onsets[k] * onsets[k + lag]
            }
            correlations[lag] = sum / count
            if (correlations[lag] > bestCorrelation) {
                bestCorrelation = correlations[lag]
                bestLag = lag
            }
        }

        if (bestLag <= 0) return 120 to normalizedEnergy

        // Check half lag (octave / double-beat check)
        val halfLag = bestLag / 2
        if (halfLag >= minLag && correlations[halfLag] >= bestCorrelation * 0.72f) {
            val potentialBpm = (frameRate * 60f / halfLag).roundToInt()
            if (potentialBpm in 85..165) {
                bestLag = halfLag
            }
        }

        val calculatedBpm = (frameRate * 60f / bestLag).roundToInt().coerceIn(55, 210)
        return calculatedBpm to normalizedEnergy
    }
}
