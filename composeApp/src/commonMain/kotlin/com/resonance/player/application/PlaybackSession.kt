package com.resonance.player.application

import com.resonance.player.data.PersistenceQueue
import com.resonance.player.data.PlaybackModeWriter
import com.resonance.player.model.*
import com.resonance.player.platform.*
import com.resonance.player.playback.*
import com.resonance.player.resolveTrackPlaybackQueue
import kotlinx.coroutines.*
import kotlin.random.Random

/** Queue commands and file preparation are independent of the screen that issued them. */
class PlaybackSession(
    private val services: PlatformServices,
    private val scope: CoroutineScope,
    private val playback: PlaybackStateStore,
    private val writes: PersistenceQueue,
    private val modeWriter: PlaybackModeWriter,
    private val state: State,
    private val missing: (Track, List<Track>?, Boolean) -> Unit,
    private val ready: () -> Unit,
    private val message: (String, Boolean) -> Unit,
) {
    interface State {
        var player: PlayerState
        var customQueue: List<Track>?
        var activePlaylistId: String?
        val selectedPlaylistId: String?
        val fullLibraryQueue: List<Track>
        val queue: List<Track>
    }
    private var request: Job? = null
    private var generation = 0L
    fun close() { ++generation; request?.cancel() }
    fun allowLocalPlayback(): Boolean {
        if (services.listenTogether?.state?.value?.isGuest == true) {
            message("一起听时请使用共享队列；退出房间后可独立播放本地音乐。", false)
            return false
        }
        return true
    }
    fun requestTogetherToggle(): Boolean {
        val together = services.listenTogether ?: return false
        if (!together.state.value.isGuest) return false
        if (together.state.value.localAudioPaused) {
            together.resumeGuestAudio()
            if (together.remotePlayback.value?.playing != true) together.requestControl(TogetherControl.Play)
        } else together.requestControl(if (state.player.isPlaying) TogetherControl.Pause else TogetherControl.Play)
        return true
    }
    fun startTrack(track: Track, newQueue: List<Track>? = null, shuffle: Boolean = state.player.shuffleEnabled) {
        if (!allowLocalPlayback()) return
        val selected = ++generation
        request?.cancel()
        request = scope.launch {
            val available = services.audioFileAvailable(track)
            ensureActive()
            if (selected != generation) return@launch
            if (!available) { missing(track, newQueue ?: state.customQueue, shuffle); return@launch }
            ready()
            val target = when {
                newQueue != null -> newQueue.filter { it.sourceUri != null }.takeIf { it.isNotEmpty() }
                state.customQueue?.any { it.id == track.id } == true -> state.customQueue
                else -> null
            }
            val queue = resolveTrackPlaybackQueue(track, target ?: state.fullLibraryQueue)
            state.customQueue = if (target != null || queue != state.fullLibraryQueue) queue else null
            val playlist = state.selectedPlaylistId
            state.activePlaylistId = playlist
            services.setPlaying(false)
            state.player = state.player.copy(isPlaying = false, shuffleEnabled = shuffle)
            playback.activate(track, playlist, expectNativeTrack = true, prepare = {
                if (!track.isTogetherTrack()) writes.run {
                    services.savePlaybackSnapshot(PlaybackSnapshot(track.id, queue.filterNot { it.isTogetherTrack() }.map(Track::id), playlistId = playlist))
                }
            }) { services.play(track, queue, shuffle, state.player.repeatMode) }
        }
    }
    fun startShufflePlayback(tracks: List<Track>) {
        if (!allowLocalPlayback()) return
        val playable = tracks.filter { it.sourceUri != null }
        val pick = playable.takeIf { it.isNotEmpty() }?.random() ?: return
        if (!state.player.shuffleEnabled) {
            state.player = state.player.copy(shuffleEnabled = true)
            services.setPlaybackMode(true, state.player.repeatMode)
            modeWriter.save(true, state.player.repeatMode)
        }
        startTrack(pick, playable, true)
    }
    fun moveInQueue(direction: Int, automatic: Boolean = false) {
        val together = services.listenTogether
        if (together?.state?.value?.isGuest == true) {
            if (!automatic) {
                if (direction > 0) together.requestControl(TogetherControl.Next)
                else if (together.state.value.equalControls) together.requestControl(TogetherControl.Previous)
                else message("对方使用旧版，暂不支持共同切换上一首。", false)
            }
            return
        }
        if (!allowLocalPlayback() || state.queue.isEmpty()) return
        val queue = state.queue
        val player = state.player
        val current = player.currentTrack ?: queue.first()
        if (automatic && player.repeatMode == RepeatMode.One) { startTrack(current, queue); return }
        val index = queue.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        if (automatic && direction > 0 && index == queue.lastIndex && player.repeatMode == RepeatMode.Off && !player.shuffleEnabled) {
            services.setPlaying(false)
            state.player = player.copy(isPlaying = false, progress = 1f)
            return
        }
        val target = if (player.shuffleEnabled && queue.size > 1) queue.filterNot { it.id == current.id }.random(Random.Default)
            else queue[(index + direction).mod(queue.size)]
        startTrack(target, queue)
    }
    fun playNext(track: Track) {
        if (track.sourceUri == null) return
        if (state.player.currentTrack?.id == track.id) { message("「${track.title}」正在播放中", true); return }
        state.customQueue = buildPlayNextQueue(state.queue, state.player.currentTrack, track)
        message("已将「${track.title}」设为下一首播放", true)
    }
    fun removeFromQueue(track: Track) {
        val together = services.listenTogether
        if (together?.state?.value?.role == TogetherRole.Host) {
            together.hostQueue.value?.publicIds?.get(track.id)?.let(together::removeSharedTrack)
            return
        }
        val queue = state.queue
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val remaining = buildRemoveFromQueue(queue, track)
        state.customQueue = remaining
        if (state.player.currentTrack?.id == track.id) {
            if (remaining.isEmpty()) { close(); services.setPlaying(false); playback.clearCurrentTrack() }
            else {
                val next = if (state.player.shuffleEnabled) remaining.random() else remaining[index.mod(remaining.size)]
                // The new request owns the reduced queue; an old captured queue cannot restore the removed song.
                startTrack(next, remaining)
            }
        }
    }
    fun clearQueue() {
        val current = state.player.currentTrack
        state.customQueue = current?.let(::listOf) ?: emptyList()
        if (current == null) { close(); services.setPlaying(false); playback.clearCurrentTrack() }
    }
    fun moveTrackInQueue(from: Int, to: Int) {
        val together = services.listenTogether
        if (together?.state?.value?.role == TogetherRole.Host) {
            val ids = together.hostQueue.value?.publicIds.orEmpty()
            val id = state.queue.getOrNull(from)?.id?.let(ids::get) ?: return
            val target = state.queue.getOrNull(to)?.id?.let(ids::get) ?: return
            val destination = together.state.value.sharedQueue.indexOfFirst { it.id == target }
            if (destination >= 0) together.moveSharedTrack(id, destination)
            return
        }
        state.customQueue = buildReorderedQueue(state.queue, from, to)
    }
}
