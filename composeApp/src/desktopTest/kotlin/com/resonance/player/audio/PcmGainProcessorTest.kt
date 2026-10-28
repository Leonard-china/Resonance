package com.resonance.player.audio

import com.resonance.player.model.AudioEffectPreset
import kotlin.math.abs
import kotlin.test.*

class PcmGainProcessorTest {
    private fun samples(count: Int, value: Int): ByteArray = ByteArray(count * 4).also { bytes ->
        for (offset in bytes.indices step 2) { bytes[offset] = value.toByte(); bytes[offset + 1] = (value shr 8).toByte() }
    }
    private fun read(bytes: ByteArray, offset: Int = 0) =
        ((bytes[offset].toInt() and 0xff) or (bytes[offset + 1].toInt() shl 8)).toShort().toInt()

    @Test fun positiveDigitalPreampActuallyAmplifiesPcm() {
        val bytes = samples(10, 8192)
        PcmGainProcessor(initialGain = 2.0).process(bytes, bytes.size, 2.0)
        assertEquals(16384, read(bytes))
    }
    @Test fun largeGainNeverWrapsOrClipsAbruptly() {
        for (sample in listOf(20_000, -20_000)) {
            val bytes = samples(10, sample)
            PcmGainProcessor(initialGain = 8.0).process(bytes, bytes.size, 8.0)
            assertTrue(abs(read(bytes)) >= 32_000)
            assertTrue((read(bytes) < 0) == (sample < 0))
        }
    }
    @Test fun newGainRampsInsteadOfJumping() {
        val bytes = samples(10_000, 8192)
        PcmGainProcessor().process(bytes, bytes.size, 2.0)
        assertTrue(read(bytes) in 8192..8200)
        assertEquals(16384, read(bytes, bytes.size - 4))
    }
    @Test fun masterMuteProducesSilence() {
        val bytes = samples(10, 8192)
        PcmGainProcessor(initialGain = 0.0).process(bytes, bytes.size, 0.0)
        assertTrue(bytes.all { it == 0.toByte() })
    }
    @Test fun vocalSuppressionSuppressesCenterChannelWhileRetainingStereo() {
        // Create pure center audio (L == R == 10000)
        val centerBytes = ByteArray(40).also { bytes ->
            for (i in 0 until 10) {
                val offsetL = i * 4
                val offsetR = offsetL + 2
                // L
                bytes[offsetL] = (10000 and 0xff).toByte()
                bytes[offsetL + 1] = ((10000 shr 8) and 0xff).toByte()
                // R
                bytes[offsetR] = (10000 and 0xff).toByte()
                bytes[offsetR + 1] = ((10000 shr 8) and 0xff).toByte()
            }
        }
        val processor = PcmGainProcessor(initialGain = 1.0, initialVocalSuppression = 1.0)
        processor.process(centerBytes, centerBytes.size, 1.0, vocalSuppression = 1.0)
        val suppressedVal = read(centerBytes, 0)
        // Mid center channel should be significantly reduced compared to original 10000
        assertTrue(abs(suppressedVal) < 2000, "Expected center channel to be suppressed, but got $suppressedVal")
    }
    @Test fun playbackTempoRemainsWithinFfmpegFilterRange() {
        assertEquals("atempo=0.5,atempo=0.5", DesktopPcmPlayer.tempoFilter(0.25))
        assertEquals("atempo=2.0,atempo=1.5", DesktopPcmPlayer.tempoFilter(3.0))
    }

    @Test fun allAudioEffectPresetsProcessPcmSafely() {
        for (preset in AudioEffectPreset.entries) {
            val bytes = samples(100, 8000)
            val processor = PcmGainProcessor(initialGain = 1.0, initialPreset = preset)
            processor.process(bytes, bytes.size, 1.0, preset = preset)
            val output = read(bytes, 20)
            assertTrue(output != 0, "Preset $preset should produce non-zero output")
            assertTrue(abs(output) <= 32767, "Preset $preset output should not exceed 16-bit range")
        }
    }

    @Test fun surroundPresetWidensStereoSpread() {
        // Create stereo with L != R (L = 6000, R = 2000)
        val bytes = ByteArray(80).also { b ->
            for (i in 0 until 20) {
                val offsetL = i * 4
                val offsetR = offsetL + 2
                b[offsetL] = (6000 and 0xff).toByte()
                b[offsetL + 1] = ((6000 shr 8) and 0xff).toByte()
                b[offsetR] = (2000 and 0xff).toByte()
                b[offsetR + 1] = ((2000 shr 8) and 0xff).toByte()
            }
        }
        val processor = PcmGainProcessor(initialGain = 1.0, initialPreset = AudioEffectPreset.Surround)
        processor.process(bytes, bytes.size, 1.0, preset = AudioEffectPreset.Surround)
        val outL = read(bytes, 40)
        val outR = read(bytes, 42)
        // Stereo difference (outL - outR) should be amplified compared to original (6000 - 2000 = 4000)
        assertTrue(abs(outL - outR) > 4000, "Surround effect should widen stereo difference, got ${outL - outR}")
    }
}
