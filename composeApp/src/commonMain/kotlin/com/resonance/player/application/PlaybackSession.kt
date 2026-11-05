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
    fun startTrack(track: Track, newQueue: List<Track>? = null, shuffle: Boolean = state.player.shuffleEnabled) {
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
                writes.run {
                    services.savePlaybackSnapshot(PlaybackSnapshot(track.id, queue.map(Track::id), playlistId = playlist))
                }
            }) { services.play(track, queue, shuffle, state.player.repeatMode) }
        }
    }
    fun startShufflePlayback(tracks: List<Track>) {
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
        if (state.queue.isEmpty()) return
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
        state.customQueue = buildReorderedQueue(state.queue, from, to)
    }
}
