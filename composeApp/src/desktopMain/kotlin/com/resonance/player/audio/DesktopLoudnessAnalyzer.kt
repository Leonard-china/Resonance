package com.resonance.player.audio

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class DesktopLoudnessAnalyzer {
    private val mutex = Mutex()
    suspend fun analyze(source: String): Float? = mutex.withLock {
        val input = if (source.startsWith("file:")) Path.of(java.net.URI(source)).toString() else source
        withTimeout(180_000) {
            runInterruptible(Dispatchers.IO) {
                val log = Files.createTempFile("resonance-loudness-", ".log")
                val process = ProcessBuilder(listOf(FfmpegExecutable.locate()) + LoudnessAnalysis.arguments(input))
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start()
                try {
                    if (process.waitFor() != 0) null else LoudnessAnalysis.gainFromOutput(Files.readString(log))
                } finally {
                    if (process.isAlive) {
                        process.destroyForcibly()
                        try { process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS) }
                        catch (_: InterruptedException) { Thread.currentThread().interrupt() }
                    }
                    Files.deleteIfExists(log)
                }
            }
        }
    }

}
