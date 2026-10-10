package com.resonance.player.audio

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class FfmpegLoudnessIntegrationTest {
    @Test fun wholeTrackAnalyzerDistinguishesWeakAndLoudAudio() = runBlocking {
        val directory = Files.createTempDirectory("resonance-loudness-fixture-")
        try {
            suspend fun measure(name: String, volume: String): Float {
                val audio = directory.resolve("$name.wav")
                val process = ProcessBuilder(FfmpegExecutable.locate(), "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                    "-f", "lavfi", "-i", "sine=frequency=440:duration=3", "-af", "volume=$volume", audio.toString())
                    .redirectErrorStream(true).start()
                val error = process.inputStream.bufferedReader().use { it.readText() }
                assertEquals(0, process.waitFor(), error)
                return assertNotNull(DesktopLoudnessAnalyzer().analyze(audio.toString()))
            }
            assertTrue(measure("weak", "0.1") > 0f)
            assertTrue(measure("loud", "6") < 0f)
        } finally { directory.toFile().deleteRecursively() }
    }
}
