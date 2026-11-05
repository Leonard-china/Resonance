package com.resonance.player.audio

import com.resonance.player.model.Track
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/** Fixed PCM contract; conversion and device ownership stay outside playback state. */
internal interface DesktopAudioBackend {
    fun decode(track: Track, offset: Double, speed: Double): DecodedPcm
    fun output(): PcmOutput
}
internal interface DecodedPcm : AutoCloseable {
    val input: InputStream
    /** Must not wait for the process or take a device lock. */
    fun cancel()
    fun verifyCompleted()
}
internal interface PcmOutput : AutoCloseable {
    val available: Int
    val framePosition: Long
    fun start()
    fun stop()
    fun write(bytes: ByteArray, offset: Int, length: Int): Int
}

internal class FfmpegJavaSoundBackend : DesktopAudioBackend {
    override fun decode(track: Track, offset: Double, speed: Double): DecodedPcm {
        val log = Files.createTempFile("resonance-playback-", ".log")
        try {
            val raw = requireNotNull(track.sourceUri) { "音频来源不可用，请重新定位文件" }
            val source = if (raw.startsWith("file:")) Path.of(java.net.URI(raw)).toString() else raw
            val args = mutableListOf(FfmpegExecutable.locate(), "-hide_banner", "-nostdin", "-loglevel", "error",
                "-ss", offset.toString(), "-i", source, "-map", "0:a:0", "-vn", "-sn", "-dn")
            if (speed != 1.0) args += listOf("-af", DesktopPcmPlayer.tempoFilter(speed))
            args += listOf("-ac", "2", "-ar", "48000", "-f", "s16le", "pipe:1")
            val process = ProcessBuilder(args).redirectError(log.toFile()).start()
            return object : DecodedPcm {
                override val input: InputStream = process.inputStream
                override fun cancel() { process.toHandle().destroyForcibly() }
                override fun verifyCompleted() {
                    check(process.waitFor(3, TimeUnit.SECONDS)) { "音频解码器未能正常结束" }
                    check(process.exitValue() == 0) { errorTail(log).ifBlank { "FFmpeg 音频解码失败" } }
                }
                override fun close() {
                    cancel()
                    runCatching { input.close() }
                    runCatching { process.waitFor(300, TimeUnit.MILLISECONDS) }
                    Files.deleteIfExists(log)
                }
            }
        } catch (error: Throwable) {
            Files.deleteIfExists(log)
            throw error
        }
    }
    override fun output(): PcmOutput {
        val format = AudioFormat(48_000f, 16, 2, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        try { line.open(format, 48_000 / 5 * 4) }
        catch (error: Throwable) { runCatching { line.close() }; throw error }
        return JavaSoundOutput(line)
    }
    private class JavaSoundOutput(private val line: SourceDataLine) : PcmOutput {
        override val available: Int get() = line.available()
        override val framePosition: Long get() = line.longFramePosition
        override fun start() = line.start()
        override fun stop() = line.stop()
        override fun write(bytes: ByteArray, offset: Int, length: Int): Int = line.write(bytes, offset, length)
        override fun close() { try { line.stop() } finally { line.close() } }
    }
    private fun errorTail(path: Path): String = Files.newByteChannel(path).use { channel ->
        val size = channel.size()
        channel.position((size - 2_048).coerceAtLeast(0))
        val buffer = java.nio.ByteBuffer.allocate(minOf(size, 2_048).toInt())
        while (buffer.hasRemaining() && channel.read(buffer) > 0) { }
        String(buffer.array(), 0, buffer.position(), Charsets.UTF_8).takeLast(500)
    }
}
