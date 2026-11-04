package com.resonance.player.ui

import androidx.compose.runtime.*
import com.resonance.player.model.*
import com.resonance.player.platform.*
import com.resonance.player.playback.PlaybackStateStore
import kotlin.math.abs
import com.resonance.player.playback.TogetherSyncPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Uses the same generation-guarded playback owner as local music, without persisting peer media. */
@Composable
internal fun TogetherPlaybackBinding(services: PlatformServices, playback: PlaybackStateStore, player: PlayerState, onGuestTrack: (Track?) -> Unit) {
    val together = services.listenTogether ?: return
    val room by together.state.collectAsState()
    val updateQueue by rememberUpdatedState(onGuestTrack)
    SideEffect { together.publishPlayback(player) }
    LaunchedEffect(together, room.isGuest) {
        if (room.isGuest && !services.managesTogetherPlayback) services.setPlaying(false)
    }
    LaunchedEffect(together) {
        var latest: TogetherPlayback? = null
        var pending: String? = null
        var readyJob: Job? = null
        var sync = TogetherSyncPolicy()
        together.remotePlayback.collect { incoming ->
            val frame = incoming?.let { if (together.state.value.localAudioPaused) it.copy(playing = false) else it }
            latest = frame
            val track = frame?.track
            if (track == null || !together.state.value.isGuest) {
                pending = null
                readyJob?.cancel()
                if (playback.state.value.currentTrack?.isTogetherTrack() == true) {
                    playback.clearCurrentTrack()
                    updateQueue(null)
                }
                return@collect
            }
            if (playback.state.value.currentTrack?.id != track.id) {
                readyJob?.cancel()
                sync = TogetherSyncPolicy()
                pending = track.id
                if (!services.managesTogetherPlayback) services.setPlaying(false)
                updateQueue(track)
                if (services.managesTogetherPlayback) {
                    playback.activate(track, null, expectNativeTrack = true, analyzeLoudness = false) {
                        val current = latest?.takeIf { it.track?.id == track.id && together.state.value.isGuest } ?: return@activate
                        playback.state.value = playback.state.value.copy(isPlaying = current.playing, progress = current.progress,
                            positionMs = (current.progress * current.durationMs).toLong(), playbackSpeed = current.speed,
                            shuffleEnabled = false, repeatMode = RepeatMode.Off)
                        pending = null
                    }
                    return@collect
                }
                playback.activate(track, null, expectNativeTrack = true, analyzeLoudness = false) {
                    val current = latest?.takeIf { it.track?.id == track.id && together.state.value.isGuest } ?: return@activate
                    services.playSynchronized(track, current.progress, current.playing, current.speed)
                    playback.state.value = playback.state.value.copy(isPlaying = current.playing, progress = current.progress,
                        positionMs = (current.progress * current.durationMs).toLong(), playbackSpeed = current.speed,
                        shuffleEnabled = false, repeatMode = RepeatMode.Off)
                    pending = null
                    readyJob = launch {
                        if (services.awaitSynchronizedReady(track.id) && latest?.mediaId == frame.mediaId && together.state.value.isGuest) {
                            together.reportPlaybackReady(frame.mediaId)
                        }
                    }
                }
            } else if (pending == null) {
                val current = playback.state.value
                if (services.managesTogetherPlayback) {
                    playback.state.value = current.copy(isPlaying = frame.playing, playbackSpeed = frame.speed)
                    return@collect
                }
                val position = current.positionMs ?: (current.progress * frame.durationMs).toLong()
                val target = (frame.progress * frame.durationMs).toLong()
                if (frame.durationMs > 0 && sync.shouldSeek(position, target, services.currentTimeMillis(), frame.playing)) services.seekTo(frame.progress)
                if (current.isPlaying != frame.playing) services.setPlaying(frame.playing)
                if (abs(current.playbackSpeed - frame.speed) > .001f) services.setPlaybackSpeed(frame.speed)
                playback.state.value = current.copy(isPlaying = frame.playing, playbackSpeed = frame.speed)
            }
        }
    }
}
