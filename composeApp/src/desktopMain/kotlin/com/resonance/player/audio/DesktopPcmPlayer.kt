package com.resonance.player.audio

import com.resonance.player.model.Track
import com.resonance.player.model.durationTextToSeconds
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/** FFmpeg decoding + Java Sound output provides real digital preamp gain on Windows. */
class DesktopPcmPlayer(
    private val onEnded: () -> Unit,
    private val onProgress: (Float) -> Unit,
    private val onPlayingChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
) {
    private class Session(val track: Track, val offset: Double, val speed: Double) {
        val closed = AtomicBoolean(false)
        @Volatile var process: Process? = null
        @Volatile var line: SourceDataLine? = null
        fun position(): Double = offset + (line?.longFramePosition ?: 0L) / 48_000.0 * speed
        fun close() {
            closed.set(true)
            process?.destroyForcibly()
            runCatching { line?.stop(); line?.close() }
        }
    }
    @Volatile private var session: Session? = null
    @Volatile private var paused = false
    @Volatile private var volume = 1.0
    @Volatile private var gain = 1.0
    @Volatile private var speed = 1.0
    @Volatile private var vocalSuppression = 0.0
    private val pauseMonitor = java.lang.Object()

    @Synchronized
    fun play(track: Track, autoPlay: Boolean = true, initialProgress: Float = 0f) {
        session?.close()
        paused = !autoPlay
        val next = Session(track, durationTextToSeconds(track.durationText) * initialProgress.toDouble(), speed)
        session = next
        synchronized(pauseMonitor) { pauseMonitor.notifyAll() }
        Thread({ decode(next) }, "resonance-audio-decoder").apply { isDaemon = true; start() }
    }

    private fun active(target: Session) = session === target && !target.closed.get()
    private fun decode(target: Session) {
        var log: Path? = null
        try {
            log = Files.createTempFile("resonance-playback-", ".log")
            val raw = target.track.sourceUri ?: return
            val input = if (raw.startsWith("file:")) Path.of(java.net.URI(raw)).toString() else raw
            val args = mutableListOf(FfmpegExecutable.locate(), "-hide_banner", "-nostdin", "-loglevel", "error",
                "-ss", target.offset.toString(), "-i", input, "-map", "0:a:0", "-vn", "-sn", "-dn")
            if (target.speed != 1.0) args += listOf("-af", tempoFilter(target.speed))
            args += listOf("-ac", "2", "-ar", "48000", "-f", "s16le", "pipe:1")
            val process = ProcessBuilder(args).redirectError(log.toFile()).start()
            target.process = process
            if (!active(target)) return
            val line = AudioSystem.getSourceDataLine(AudioFormat(48_000f, 16, 2, true, false))
            target.line = line
            line.open(AudioFormat(48_000f, 16, 2, true, false), 48_000 / 5 * 4)
            if (!active(target)) return
            if (!paused) line.start()
            onPlayingChanged(!paused)
            val buffer = ByteArray(4096)
            val processor = PcmGainProcessor(volume * gain)
            var retained = 0
            var lastProgress = System.nanoTime()
            process.inputStream.use { inputStream ->
                while (active(target)) {
                    synchronized(pauseMonitor) {
                        while (paused && active(target)) pauseMonitor.wait(100)
                    }
                    if (!active(target)) return@use
                    val read = inputStream.read(buffer, retained, buffer.size - retained)
                    if (read < 0) break
                    val size = retained + read
                    val frames = size - size % 4
                    processor.process(buffer, frames, volume * gain, vocalSuppression)
                    var written = 0
                    while (written < frames && active(target)) written += line.write(buffer, written, frames - written)
                    retained = size - frames
                    if (retained > 0) buffer.copyInto(buffer, 0, frames, size)
                    val now = System.nanoTime()
                    if (now - lastProgress >= 250_000_000L && active(target)) {
                        val duration = durationTextToSeconds(target.track.durationText)
                        onProgress(if (duration > 0) (target.position() / duration).toFloat().coerceIn(0f, 1f) else 0f)
                        lastProgress = now
                    }
                }
            }
            if (active(target)) {
                line.drain()
                check(process.waitFor() == 0) { Files.readString(log).takeLast(500).ifBlank { "FFmpeg 音频解码失败" } }
                if (active(target)) { onPlayingChanged(false); onProgress(1f); onEnded() }
            }
        } catch (error: Exception) {
            if (active(target)) { onPlayingChanged(false); onError(error.message ?: "音频播放失败") }
        } finally {
            target.close()
            target.process?.let { runCatching { it.waitFor(3, java.util.concurrent.TimeUnit.SECONDS) } }
            log?.let { runCatching { Files.deleteIfExists(it) } }
        }
    }

    val positionMs: Long? get() = session?.takeUnless { it.closed.get() }?.let { (it.position() * 1000).toLong().coerceAtLeast(0L) }

    fun setPlaying(playing: Boolean) {
        session?.takeIf { playing && it.closed.get() }?.let { play(it.track); return }
        paused = !playing
        session?.line?.let { if (playing) it.start() else it.stop() }
        synchronized(pauseMonitor) { pauseMonitor.notifyAll() }
        onPlayingChanged(playing && session != null)
    }
    fun setVolume(value: Float) { volume = value.toDouble().coerceIn(0.0, 1.0) }
    fun setVocalSuppression(enabled: Boolean) { vocalSuppression = if (enabled) 1.0 else 0.0 }
    fun setTrackGain(db: Float) { gain = Math.pow(10.0, db.toDouble().coerceIn(-60.0, 18.0) / 20.0) }
    @Synchronized fun setPlaybackSpeed(value: Float) {
        val current = session
        val duration = current?.track?.durationText?.let(::durationTextToSeconds) ?: 0
        val progress = if (duration > 0) (current!!.position() / duration).toFloat().coerceIn(0f, 1f) else 0f
        speed = value.toDouble().coerceIn(0.25, 3.0)
        current?.let { play(it.track, !paused, progress) }
    }
    @Synchronized fun seekTo(progress: Float) { session?.let { play(it.track, !paused, progress.coerceIn(0f, 1f)) } }
    @Synchronized fun close() {
        val previous = session; session = null
        previous?.close()
        synchronized(pauseMonitor) { pauseMonitor.notifyAll() }
        previous?.process?.let { runCatching { it.waitFor(3, java.util.concurrent.TimeUnit.SECONDS) } }
    }

    companion object {
        fun tempoFilter(speed: Double): String {
            var remaining = speed
            val filters = mutableListOf<String>()
            while (remaining < 0.5) { filters += "atempo=0.5"; remaining /= 0.5 }
            while (remaining > 2.0) { filters += "atempo=2.0"; remaining /= 2.0 }
            filters += "atempo=$remaining"
            return filters.joinToString(",")
        }
    }
}
