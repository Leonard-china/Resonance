package com.resonance.player.playback

import com.resonance.player.model.PlayerState
import com.resonance.player.platform.ListenTogetherService
import com.resonance.player.platform.TogetherPlayback
import com.resonance.player.platform.TogetherStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.math.abs
import kotlin.time.TimeSource

/** Native playback scope owns synchronization; rendering or navigating a Compose page is optional. */
class TogetherPlaybackDriver(
    private val together: ListenTogetherService,
    private val scope: CoroutineScope,
    private val current: () -> PlayerState,
    private val prepare: (TogetherPlayback) -> Unit,
    private val seek: (Float) -> Unit,
    private val playing: (Boolean) -> Unit,
    private val speed: (Float) -> Unit,
    private val ready: suspend (String) -> Boolean,
    private val smoothSpeedChanges: Boolean = false,
) {
    private val origin = TimeSource.Monotonic.markNow()
    private var latest: TogetherPlayback? = null
    private var latestAt = 0L
    private var preparedId: String? = null
    private var preparing: Job? = null
    private var policy = TogetherSyncPolicy()
    private fun now() = origin.elapsedNow().inWholeMilliseconds
    private val roomJob = scope.launch {
        var wasGuest = false
        var wasPaused = false
        together.state.collect { room ->
            if (room.isGuest && !wasGuest) playing(false)
            if (room.isGuest && room.localAudioPaused && current().isPlaying) playing(false)
            if (room.isGuest && wasPaused && !room.localAudioPaused) {
                // Re-prepare from the next current frame after an explicit resume, never from a stale paused frame.
                preparedId = null
            }
            if (!room.isGuest && wasGuest) {
                preparing?.cancel(); preparing = null; preparedId = null; latest = null
                playing(false)
            }
            wasGuest = room.isGuest
            wasPaused = room.localAudioPaused
        }
    }
    private val frameJob = scope.launch {
        together.remotePlayback.collect { frame ->
            latest = frame; latestAt = now()
            if (frame?.track == null || !together.state.value.isGuest) {
                preparing?.cancel(); preparing = null; preparedId = null
                if (current().currentTrack?.id?.startsWith("together:") == true) playing(false)
                return@collect
            }
            val track = frame.track
            if (preparedId != track.id) {
                preparing?.cancel()
                preparedId = track.id
                policy = TogetherSyncPolicy()
                playing(false)
                prepare(frame.copy(playing = false))
                val pending = scope.launch(start = CoroutineStart.LAZY) {
                    try {
                        if (!ready(track.id)) { together.reportPlaybackFailure(frame.mediaId); return@launch }
                        val fresh = latest?.takeIf { it.track?.id == track.id && together.state.value.isGuest &&
                            together.state.value.status in setOf(TogetherStatus.Preparing, TogetherStatus.Connected) } ?: return@launch
                        val elapsed = if (fresh.playing) (now() - latestAt).coerceAtLeast(0) * fresh.speed else 0f
                        val target = if (fresh.durationMs > 0) (fresh.progress + elapsed / fresh.durationMs).coerceIn(0f, 1f) else fresh.progress
                        val localPosition = current().positionMs ?: (current().progress * fresh.durationMs).toLong()
                        if (abs(localPosition - (target * fresh.durationMs).toLong()) > 250) {
                            seek(target)
                            if (!ready(track.id)) { together.reportPlaybackFailure(fresh.mediaId); return@launch }
                        }
                        if (!together.state.value.isGuest || together.state.value.status == TogetherStatus.Reconnecting || latest?.track?.id != track.id) return@launch
                        if (abs(current().playbackSpeed - fresh.speed) > .001f) speed(fresh.speed)
                        playing(fresh.playing && !together.state.value.localAudioPaused)
                        together.reportPlaybackReady(fresh.mediaId)
                    } finally { if (preparedId == track.id) preparing = null }
                }
                preparing = pending
                pending.start()
            } else if (preparing == null) {
                val local = current()
                if (together.state.value.localAudioPaused) {
                    if (local.isPlaying) playing(false)
                    return@collect
                }
                val position = local.positionMs ?: (local.progress * frame.durationMs).toLong()
                val target = (frame.progress * frame.durationMs).toLong()
                if (frame.durationMs > 0 && policy.shouldSeek(position, target, now(), frame.playing)) seek(frame.progress)
                val targetSpeed = if (smoothSpeedChanges) policy.effectiveSpeed(position, target, frame.speed, frame.playing) else frame.speed
                if (abs(local.playbackSpeed - targetSpeed) > .001f) speed(targetSpeed)
                val shouldPlay = frame.playing && !together.state.value.localAudioPaused
                if (local.isPlaying != shouldPlay) playing(shouldPlay)
            }
        }
    }
    fun close() { roomJob.cancel(); frameJob.cancel(); preparing?.cancel() }
}
