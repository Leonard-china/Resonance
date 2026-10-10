package com.resonance.player.audio

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
    @Test fun playbackTempoRemainsWithinFfmpegFilterRange() {
        assertEquals("atempo=0.5,atempo=0.5", DesktopPcmPlayer.tempoFilter(0.25))
        assertEquals("atempo=2.0,atempo=1.5", DesktopPcmPlayer.tempoFilter(3.0))
    }
}
