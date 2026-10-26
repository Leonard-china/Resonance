package com.resonance.player.audio

import com.resonance.player.audio.AudioFeatureAnalyzer
import kotlin.math.sin
import kotlin.test.*

class AudioFeatureAnalyzerTest {

    @Test
    fun syntheticBeatsAt120BpmAreAccuratelyDetected() {
        val sampleRate = 22_050
        val seconds = 6
        val totalSamples = sampleRate * seconds
        val bpm = 120
        val beatInterval = (sampleRate * 60.0 / bpm).toInt() // samples between beats

        val bytes = ByteArray(totalSamples * 4) // stereo PCM16
        for (i in 0 until totalSamples) {
            val isBeat = (i % beatInterval) < (sampleRate * 0.05) // 50ms burst
            val sampleVal = if (isBeat) {
                // 440 Hz tone burst
                (16000 * sin(2.0 * Math.PI * 440.0 * i / sampleRate)).toInt()
            } else {
                0
            }
            val offset = i * 4
            // Left
            bytes[offset] = (sampleVal and 0xff).toByte()
            bytes[offset + 1] = ((sampleVal shr 8) and 0xff).toByte()
            // Right
            bytes[offset + 2] = (sampleVal and 0xff).toByte()
            bytes[offset + 3] = ((sampleVal shr 8) and 0xff).toByte()
        }

        val result = AudioFeatureAnalyzer.analyzePcm(bytes, sampleRate = sampleRate, channels = 2)
        val detectedBpm = result.first
        val energy = result.second
        assertTrue(detectedBpm in 115..125, "Expected BPM around 120, got $detectedBpm")
        assertTrue(energy > 10, "Expected positive energy, got $energy")
    }

    @Test
    fun shortOrSilenceAudioReturnsDefaults() {
        val bytes = ByteArray(100)
        val result = AudioFeatureAnalyzer.analyzePcm(bytes, 48000, 2)
        assertEquals(120, result.first)
        assertEquals(50, result.second)
    }
}
