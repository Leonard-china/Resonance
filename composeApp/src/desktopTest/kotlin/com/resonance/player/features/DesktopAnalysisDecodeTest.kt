package com.resonance.player.features

import com.resonance.player.model.Track
import com.resonance.player.platform.DesktopLibraryStore
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlin.test.*

class DesktopAnalysisDecodeTest {
    @Test fun nativeAnalysisDecoderIsLocalAndCapsPcmAtNinetySeconds() = runBlocking {
        val root = Files.createTempDirectory("resonance-analysis-test-")
        try {
            val count = 16000 * 91
            val wave = ByteBuffer.allocate(44 + count * 2).order(ByteOrder.LITTLE_ENDIAN)
            wave.put("RIFF".toByteArray()).putInt(36 + count * 2).put("WAVEfmt ".toByteArray()).putInt(16)
                .putShort(1).putShort(1).putInt(16000).putInt(32000).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(count * 2)
            repeat(count) { i -> wave.putShort(if (i % 8000 < 800) (16000 * kotlin.math.sin(i * 2 * Math.PI * 440 / 16000)).toInt().toShort() else 0) }
            val source = root.resolve("generated-beats.wav"); Files.write(source, wave.array())
            val track = Track("fixture", "Generated", "", "", "1:31", 0, sourceUri = source.toString())
            val access = DesktopMusicAccess(DesktopLibraryStore(root.resolve("data")), {}, { null })
            val before = access.audioRevision(track)
            val output = root.resolve("analysis.pcm")
            access.prepareAnalysisAudio(track, output)
            assertEquals(90L * 16000 * 2, Files.size(output))
            val bpm = com.resonance.player.audio.AudioFeatureAnalyzer.analyzePcm(Files.readAllBytes(output), 16000, 1).first
            assertTrue(bpm in 115..125, "Expected generated 120 BPM, found $bpm")
            assertEquals(before, access.audioRevision(track))
            assertTrue(Files.exists(source))
        } finally { root.toFile().deleteRecursively() }
    }
}
