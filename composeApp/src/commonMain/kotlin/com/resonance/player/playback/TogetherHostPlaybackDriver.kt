package com.resonance.player.playback

import com.resonance.player.model.PlayerState
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Track
import com.resonance.player.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Native scope owns host commands as well as queue edits; a Compose page is not required. */
class TogetherHostPlaybackDriver(
    private val together: ListenTogetherService,
    private val scope: CoroutineScope,
    private val current: () -> PlayerState,
    private val replaceQueue: (List<Track>, String?) -> Unit,
    private val select: (Track, List<Track>, Boolean) -> Unit,
    private val playing: (Boolean) -> Unit,
    private val seek: (Float) -> Unit,
    private val onError: () -> Unit = {},
) {
    private val gate = Mutex()
    private fun queue(): TogetherHostQueue? = together.hostQueue.value?.takeIf {
        together.state.value.role == TogetherRole.Host && it.roomSessionId == together.state.value.roomSessionId
    }
    private fun synchronize(snapshot: TogetherHostQueue) {
        val localId = current().currentTrack?.id
        val target = localId?.takeIf { id -> snapshot.tracks.any { it.id == id } }
            ?: snapshot.currentTrackId?.takeIf { id -> snapshot.tracks.any { it.id == id } }
            ?: snapshot.tracks.firstOrNull()?.id
        replaceQueue(snapshot.tracks, target)
    }
    private suspend fun safely(block: () -> Unit) {
        try { gate.withLock { block() } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { onError() }
    }
    private val queueJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        together.hostQueue.collect { snapshot ->
            if (snapshot != null) safely {
                if (queue()?.roomSessionId == snapshot.roomSessionId && queue()?.revision == snapshot.revision) synchronize(snapshot)
            }
        }
    }
    private val actionJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        together.approvedActions.collect { request -> safely {
            val snapshot = queue() ?: return@safely
            if (request.roomSessionId.isNotEmpty() && request.roomSessionId != snapshot.roomSessionId) return@safely
            synchronize(snapshot)
            when (request.action) {
                TogetherControl.Play -> if (current().currentTrack != null) playing(true)
                TogetherControl.Pause -> playing(false)
                TogetherControl.Seek -> request.trackId.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..1f }?.let(seek)
                TogetherControl.Select -> snapshot.tracks.firstOrNull { it.id == request.trackId }?.let { select(it, snapshot.tracks, true) }
                TogetherControl.Next -> step(snapshot, 1, false)
                TogetherControl.Previous -> step(snapshot, -1, false)
                TogetherControl.Append, TogetherControl.Remove, TogetherControl.Move -> Unit // Queue snapshots already own these mutations.
            }
        } }
    }
    private fun step(snapshot: TogetherHostQueue, direction: Int, automatic: Boolean) {
        val tracks = snapshot.tracks
        if (tracks.isEmpty()) return
        val local = current()
        val index = tracks.indexOfFirst { it.id == local.currentTrack?.id }.coerceAtLeast(0)
        if (automatic && local.repeatMode == RepeatMode.One) { select(tracks[index], tracks, true); return }
        if (automatic && index == tracks.lastIndex && !local.shuffleEnabled && local.repeatMode == RepeatMode.Off) {
            playing(false); return
        }
        val target = if (local.shuffleEnabled && tracks.size > 1) tracks.filterNot { it.id == tracks[index].id }.random()
            else tracks[(index + direction).mod(tracks.size)]
        select(target, tracks, true)
    }
    /** Windows has a single-track decoder; Android already advances its native Media3 timeline. */
    fun trackEnded(): Boolean {
        val snapshot = queue() ?: return false
        val endedId = current().currentTrack?.id
        scope.launch { safely {
            if (queue()?.roomSessionId == snapshot.roomSessionId && current().currentTrack?.id == endedId)
                step(queue() ?: return@safely, 1, true)
        } }
        return true
    }
    fun close() { queueJob.cancel(); actionJob.cancel() }
}
