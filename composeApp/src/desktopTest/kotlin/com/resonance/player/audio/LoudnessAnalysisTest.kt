package com.resonance.player.audio

import kotlin.test.*

class LoudnessAnalysisTest {
    private fun output(lufs: String, peak: String) = """log {"input_i":"$lufs","input_tp":"$peak"} tail"""
    @Test fun weakTrackUsesTruePeakHeadroom() {
        assertEquals(3f, LoudnessAnalysis.gainFromOutput(output("-28", "-4")))
    }
    @Test fun loudTrackIsAttenuatedAndVeryWeakGainIsBounded() {
        assertEquals(-6f, LoudnessAnalysis.gainFromOutput(output("-8", "0")))
        assertEquals(14f, LoudnessAnalysis.gainFromOutput(output("-40", "-30")))
    }
    @Test fun silenceAndIncompleteOutputAreNotCached() {
        assertNull(LoudnessAnalysis.gainFromOutput(output("-inf", "-inf")))
        assertNull(LoudnessAnalysis.gainFromOutput("mean_volume: -30 dB"))
    }
    @Test fun wholeTrackAnalysisDoesNotSeekOrLimitDuration() {
        val args = LoudnessAnalysis.arguments("music with spaces.flac").toList()
        assertFalse("-ss" in args)
        assertFalse("-t" in args)
        assertTrue(LoudnessAnalysis.FILTER in args)
        assertTrue("music with spaces.flac" in args)
    }
}
