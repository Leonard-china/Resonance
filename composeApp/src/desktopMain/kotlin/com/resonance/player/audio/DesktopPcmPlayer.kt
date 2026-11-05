package com.resonance.player.audio

import com.resonance.player.model.AudioEffectPreset
import com.resonance.player.model.PlayerState
import com.resonance.player.model.Track
import com.resonance.player.model.durationTextToSeconds
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Commands only change session intent. Decoder/device calls never execute on their caller. */
class DesktopPcmPlayer private constructor(
    private val onEnded: () -> Unit,
    private val onProgress: (Float) -> Unit,
    private val onPlayingChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
    private val onEvent: ((DesktopPlaybackEvent) -> Unit)?,
    private val backend: DesktopAudioBackend,
    private val stallMillis: Long,
) {
    constructor(onEnded: () -> Unit, onProgress: (Float) -> Unit, onPlayingChanged: (Boolean) -> Unit,
        onError: (String) -> Unit, onEvent: ((DesktopPlaybackEvent) -> Unit)? = null) :
        this(onEnded, onProgress, onPlayingChanged, onError, onEvent, FfmpegJavaSoundBackend(), 5_000)
    internal constructor(backend: DesktopAudioBackend, stallMillis: Long = 5_000, onEvent: (DesktopPlaybackEvent) -> Unit) :
        this({}, {}, {}, {}, onEvent, backend, stallMillis)

    private class Session(val track: Track, val offset: Double, val speed: Double, val requestId: Long, playing: Boolean) {
        val closed = AtomicBoolean(false)
        @Volatile var decoder: DecodedPcm? = null
        @Volatile var output: PcmOutput? = null
        @Volatile var prepared = false
        @Volatile var playing = playing
        @Volatile var buffering = true
        @Volatile var frames = 0L
        fun position(): Long = ((offset + frames / 48_000.0 * speed) * 1_000).toLong().coerceAtLeast(0)
    }
    private val requests = AtomicLong()
    @Volatile private var session: Session? = null
    @Volatile private var volume = 1.0
    @Volatile private var gain = 1.0
    @Volatile private var speed = 1.0
    @Volatile private var vocalSuppression = 0.0
    @Volatile private var preset = AudioEffectPreset.Off
    private val wake = Object()
    // A stuck native device cannot create an unbounded number of decoders or sound lines.
    private val deviceSlots = Semaphore(2)
    private val workers = ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS, ArrayBlockingQueue(1),
        { task -> Thread(task, "resonance-audio-decoder").apply { isDaemon = true } }, ThreadPoolExecutor.DiscardOldestPolicy()
    ).apply { allowCoreThreadTimeOut(true) }
    val currentRequestId: Long? get() = session?.requestId
    val sessionClosed: Boolean get() = session?.closed?.get() == true
    private fun active(target: Session) = session === target && !target.closed.get()
    private fun signal() = synchronized(wake) { wake.notifyAll() }
    private fun cancel(target: Session?) {
        target ?: return
        target.closed.set(true)
        // Process cancellation is nonblocking; device cleanup belongs to the decoder worker.
        runCatching { target.decoder?.cancel() }
        signal()
    }

    @Synchronized fun play(track: Track, autoPlay: Boolean = true, initialProgress: Float = 0f) {
        cancel(session)
        val fraction = initialProgress.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f
        val next = Session(track, durationTextToSeconds(track.durationText) * fraction.toDouble(), speed,
            requests.incrementAndGet(), autoPlay)
        session = next
        publish(next, DesktopPlaybackEvent.Kind.Buffering)
        workers.execute { if (active(next)) decode(next) }
    }

    private fun awaitPlaying(target: Session): Boolean {
        if (!target.playing && active(target)) {
            // Never hold the command wakeup monitor while calling a native device.
            target.output?.stop()
            target.frames = target.output?.framePosition ?: target.frames
            synchronized(wake) { while (active(target) && !target.playing) wake.wait(50) }
            if (active(target)) target.output?.start()
        }
        return active(target)
    }
    private fun waitForDevice() = synchronized(wake) { wake.wait(10) }
    private fun decode(target: Session) {
        var slot = false
        var decoder: DecodedPcm? = null
        var output: PcmOutput? = null
        try {
            val waitingSince = System.nanoTime()
            while (active(target) && !slot) {
                slot = deviceSlots.tryAcquire(20, TimeUnit.MILLISECONDS)
                check(System.nanoTime() - waitingSince < stallMillis * 1_000_000) { "音频设备未响应，请重试播放" }
            }
            if (!active(target)) return
            decoder = backend.decode(target.track, target.offset, target.speed)
            target.decoder = decoder
            if (!active(target)) return
            output = backend.output()
            target.output = output
            if (!active(target)) return
            if (target.playing) output.start()
            val buffer = ByteArray(4096)
            val processor = PcmGainProcessor(volume * gain, 48_000, 2, vocalSuppression, preset)
            var retained = 0
            var queuedFrames = 0L
            var lastEvent = System.nanoTime()
            decoder.input.use { input ->
                while (active(target)) {
                    val read = input.read(buffer, retained, buffer.size - retained)
                    if (read < 0) break
                    if (read == 0) { waitForDevice(); continue }
                    val size = retained + read
                    val frames = size - size % 4
                    if (frames > 0) {
                        target.prepared = true
                        if (!target.playing && target.buffering) {
                            target.buffering = false
                            publish(target, DesktopPlaybackEvent.Kind.Playing)
                        }
                    }
                    if (!awaitPlaying(target)) return@use
                    processor.process(buffer, frames, volume * gain, vocalSuppression, preset)
                    var written = 0
                    var lastAdvance = System.nanoTime()
                    while (written < frames && active(target)) {
                        if (!target.playing) {
                            if (!awaitPlaying(target)) return@use
                            lastAdvance = System.nanoTime()
                        }
                        val capacity = minOf(output.available.coerceAtLeast(0), frames - written, 2048).let { it - it % 4 }
                        val count = if (capacity > 0) output.write(buffer, written, capacity) else 0
                        check(count in 0..capacity && count % 4 == 0) { "音频设备返回无效写入长度" }
                        if (count == 0) {
                            check(System.nanoTime() - lastAdvance < stallMillis * 1_000_000) { "音频设备持续无响应，请检查输出设备后重试" }
                            waitForDevice()
                        } else {
                            written += count
                            queuedFrames += count / 4
                            lastAdvance = System.nanoTime()
                            target.frames = output.framePosition
                            if (target.buffering) {
                                target.buffering = false
                                publish(target, DesktopPlaybackEvent.Kind.Playing)
                            }
                        }
                        val now = System.nanoTime()
                        if (now - lastEvent >= 250_000_000) {
                            publish(target, DesktopPlaybackEvent.Kind.Progress)
                            lastEvent = now
                        }
                    }
                    retained = size - frames
                    if (retained > 0) buffer.copyInto(buffer, 0, frames, size)
                }
            }
            if (active(target)) {
                // SourceDataLine.drain() can wait forever. Observe consumption with the same budget.
                var lastFrame = output.framePosition
                var lastAdvance = System.nanoTime()
                while (active(target) && output.framePosition < queuedFrames) {
                    val wasPaused = !target.playing
                    if (!awaitPlaying(target)) return
                    if (wasPaused) lastAdvance = System.nanoTime()
                    target.frames = output.framePosition
                    val now = System.nanoTime()
                    if (target.frames != lastFrame) { lastFrame = target.frames; lastAdvance = now }
                    check(now - lastAdvance < stallMillis * 1_000_000) { "音频设备未能播放完缓冲数据" }
                    waitForDevice()
                }
                if (!active(target)) return
                decoder.verifyCompleted()
                target.frames = output.framePosition
                target.playing = false
                target.buffering = false
                publish(target, DesktopPlaybackEvent.Kind.Playing)
                publish(target, DesktopPlaybackEvent.Kind.Ended)
            }
        } catch (error: Exception) {
            if (active(target)) {
                target.playing = false
                target.buffering = false
                publish(target, DesktopPlaybackEvent.Kind.Error, error.message ?: "音频播放失败")
            }
        } finally {
            target.closed.set(true)
            runCatching { decoder?.close() }
            runCatching { output?.close() }
            if (slot) deviceSlots.release()
        }
    }

    private fun snapshot(target: Session) = PlayerState(currentTrack = target.track, positionMs = target.position(),
        progress = (target.position().toFloat() / (durationTextToSeconds(target.track.durationText).coerceAtLeast(1) * 1000L)).coerceIn(0f, 1f),
        isPlaying = target.playing && !target.closed.get() && target.prepared && !target.buffering,
        playbackSpeed = target.speed.toFloat(), isBuffering = target.buffering && target.playing && !target.closed.get())
    private fun publish(target: Session, kind: DesktopPlaybackEvent.Kind, message: String = "") {
        if (!active(target)) return
        val state = snapshot(target)
        if (onEvent != null) onEvent.invoke(DesktopPlaybackEvent(target.requestId, kind, state, message))
        else when (kind) {
            DesktopPlaybackEvent.Kind.Buffering -> onPlayingChanged(false)
            DesktopPlaybackEvent.Kind.Playing -> onPlayingChanged(state.isPlaying)
            DesktopPlaybackEvent.Kind.Progress -> onProgress(state.progress)
            DesktopPlaybackEvent.Kind.Ended -> { onProgress(1f); onEnded() }
            DesktopPlaybackEvent.Kind.Error -> { onPlayingChanged(false); onError(message) }
        }
    }
    val positionMs: Long? get() = session?.takeUnless { it.closed.get() }?.position()
    fun isPrepared(trackId: String): Boolean = session?.let { it.track.id == trackId && !it.closed.get() && it.prepared } == true
    val playWhenReady: Boolean get() = session?.let { it.playing && !it.closed.get() } == true
    fun snapshot(): PlayerState = session?.let(::snapshot) ?: PlayerState()
    @Synchronized fun setPlaying(playing: Boolean) {
        val target = session ?: run { onPlayingChanged(false); return }
        if (playing && target.closed.get()) { play(target.track); return }
        target.playing = playing
        signal()
        publish(target, DesktopPlaybackEvent.Kind.Playing)
    }
    fun setVolume(value: Float) { volume = value.takeIf(Float::isFinite)?.coerceIn(0f, 1f)?.toDouble() ?: volume }
    fun setVocalSuppression(enabled: Boolean) { vocalSuppression = if (enabled) 1.0 else 0.0 }
    fun setPreset(value: AudioEffectPreset) { preset = value; setVocalSuppression(value == AudioEffectPreset.Karaoke) }
    fun setTrackGain(db: Float) { if (db.isFinite()) gain = Math.pow(10.0, db.toDouble().coerceIn(-60.0, 18.0) / 20.0) }
    @Synchronized fun setPlaybackSpeed(value: Float) {
        if (!value.isFinite()) return
        val normalized = value.toDouble().coerceIn(0.25, 3.0)
        if (kotlin.math.abs(speed - normalized) < .001) return
        val current = session
        speed = normalized
        current?.let { play(it.track, it.playing, snapshot(it).progress) }
    }
    @Synchronized fun seekTo(progress: Float) { if (progress.isFinite()) session?.let { play(it.track, it.playing, progress) } }
    @Synchronized fun close() { val previous = session; session = null; cancel(previous) }
    companion object {
        fun tempoFilter(speed: Double): String {
            require(speed.isFinite() && speed > 0)
            var remaining = speed
            val filters = mutableListOf<String>()
            while (remaining < 0.5) { filters += "atempo=0.5"; remaining /= 0.5 }
            while (remaining > 2.0) { filters += "atempo=2.0"; remaining /= 2.0 }
            filters += "atempo=$remaining"
            return filters.joinToString(",")
        }
    }
}

/** Decoder generation remains present even when track A is selected twice. */
data class DesktopPlaybackEvent(val requestId: Long, val kind: Kind, val snapshot: PlayerState, val message: String = "") {
    enum class Kind { Playing, Progress, Ended, Error, Buffering }
}
