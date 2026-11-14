package com.resonance.player.playback

import androidx.compose.runtime.mutableStateOf
import com.resonance.player.model.PlayerState
import com.resonance.player.model.Track
import com.resonance.player.platform.PlatformServices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/** Owns track audio work independently of composition. All calls run on the UI dispatcher. */
class PlaybackStateStore(
    private val services: PlatformServices,
    private val scope: CoroutineScope,
    private val onError: (Throwable) -> Unit = {},
) {
    val state = mutableStateOf(PlayerState())
    val currentGeneration: Long get() = generation
    private var generation = 0L
    private var userPlayIntent = true
    private var volumeRevision = 0L
    private var gainRevision = 0L
    private var audioJob: Job? = null
    private data class StartAction(
        val prepare: suspend () -> Unit,
        val ready: () -> Unit,
        val startsNativePlayback: Boolean,
        val controlsPlayback: Boolean,
        val retry: () -> Unit,
        var prepared: Boolean = false,
    )
    private var pendingStart: StartAction? = null
    private var retryStart: (() -> Unit)? = null
    private var closed = false
    private var expectedNativeTrack: String? = null

    fun activate(
        track: Track,
        playlistId: String?,
        resetGain: Boolean = true,
        expectNativeTrack: Boolean = false,
        autoPlay: Boolean = true,
        prepare: suspend () -> Unit = {},
        analyzeLoudness: Boolean = true,
        nativePlayback: NativePlaybackState? = null,
        onReady: () -> Unit = {},
    ) {
        if (closed) return
        retryStart = null
        userPlayIntent = autoPlay
        if (expectNativeTrack) expectedNativeTrack = track.id
        else if (expectedNativeTrack != track.id) expectedNativeTrack = null
        services.cancelVolumeCalibration()
        val request = ++generation
        val volumeVersion = volumeRevision
        val gainVersion = gainRevision
        audioJob?.cancel()
        val start = StartAction(prepare, onReady, startsNativePlayback = expectNativeTrack, controlsPlayback = nativePlayback == null,
            retry = { activate(track, playlistId, resetGain, expectNativeTrack, true, prepare, analyzeLoudness, nativePlayback, onReady) })
        pendingStart = start
        state.value = state.value.copy(
            currentTrack = track,
            isPreparing = nativePlayback == null,
            isBuffering = nativePlayback?.isBuffering ?: false,
            isPlaying = nativePlayback?.isPlaying ?: false,
            progress = nativePlayback?.progress ?: 0f,
            positionMs = nativePlayback?.positionMs,
            trackGainDb = 0f,
            isCurrentTrackCalibrated = false,
        )
        if (resetGain) services.setTrackGain(0f)
        audioJob = scope.launch {
            var started = false
            try {
                val volume = services.loadGlobalVolume()
                val calibrated: Float? = null
                val automatic = services.isAutoLoudnessEnabled()
                val cached = if (automatic) services.loadAutoLoudnessGain(track) else null
                val initialGain = if (automatic) cached ?: 0f else 0f
                coroutineContext.ensureActive()
                if (request != generation) return@launch
                if (volumeVersion == volumeRevision) {
                    services.setVolume(volume)
                    state.value = state.value.copy(volume = volume)
                }
                if (gainVersion == gainRevision) applyGain(initialGain, calibrated != null)
                state.value = state.value.copy(autoLoudnessEnabled = automatic)
                start.prepare()
                coroutineContext.ensureActive()
                if (request != generation) return@launch
                state.value = state.value.copy(isPreparing = false)
                started = true
                start.prepared = true
                if (start.controlsPlayback) {
                    if (userPlayIntent) {
                        if (!startPreparedPlayback(start)) return@launch
                    } else {
                        // User requested pause or lost focus while preparing; do not resume playback on ready.
                        services.setPlaying(false)
                    }
                } else pendingStart = null
                if (analyzeLoudness && !track.id.startsWith("together:") && calibrated == null && automatic && cached == null) {
                    val analyzed = services.analyzeAndCacheLoudnessGain(track)
                    coroutineContext.ensureActive()
                    if (request == generation && gainVersion == gainRevision) applyGain(analyzed, false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (request == generation) {
                    val failedBeforeStart = start.controlsPlayback && !started
                    state.value = state.value.copy(
                        isPreparing = false,
                        isBuffering = if (failedBeforeStart) false else state.value.isBuffering,
                        isPlaying = if (failedBeforeStart) false else state.value.isPlaying,
                    )
                    if (!started) {
                        // Persistence/preparation failures must not start an obsolete queue.
                        pendingStart = null
                        retryStart = start.retry
                        userPlayIntent = false
                    }
                    onError(error)
                }
            }
        }
    }

    /** Adopt an autonomous native transition without restarting or pausing its media item. */
    fun adoptNativeTrack(track: Track, playlistId: String?, native: NativePlaybackState) {
        if (native.trackId != track.id || !acceptNativeTrack(track.id)) return
        activate(track, playlistId, resetGain = false, autoPlay = native.playWhenReady, nativePlayback = native)
    }

    fun setUserPlayIntent(playing: Boolean) {
        if (closed) return
        userPlayIntent = playing
        if (!playing) {
            state.value = state.value.copy(isPlaying = false)
        }
    }

    /** A resume must submit the selected media before it can resume the native player. */
    fun requestPlaying(playing: Boolean) {
        if (closed) return
        setUserPlayIntent(playing)
        if (playing) retryStart?.let { retry -> retryStart = null; retry(); return }
        val pending = pendingStart
        if (playing && pending?.controlsPlayback == true) {
            if (pending.prepared) startPreparedPlayback(pending)
            // While preparing, remember the intent without resuming the previous native item.
        } else services.setPlaying(playing)
    }

    private fun startPreparedPlayback(start: StartAction): Boolean {
        if (pendingStart !== start || !start.prepared) return false
        pendingStart = null // Native callbacks may be synchronous.
        try { start.ready(); return true }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            retryStart = start.retry
            userPlayIntent = false
            state.value = state.value.copy(isPlaying = false, isPreparing = false, isBuffering = false)
            onError(error)
            return false
        }
    }

    fun setPlaying(playing: Boolean) {
        if (closed) return
        // Until onReady submits the new track, native status still belongs to the previous media.
        // Explicit user actions go through setUserPlayIntent and must remain authoritative.
        if (pendingStart?.startsNativePlayback == true || retryStart != null) return
        userPlayIntent = playing
        state.value = state.value.copy(
            isPlaying = playing,
            isBuffering = if (playing) false else state.value.isBuffering,
        )
    }

    fun setBuffering(buffering: Boolean) {
        if (closed) return
        state.value = state.value.copy(isBuffering = buffering)
    }

    /** Cancels pending starts before releasing a temporary peer-owned playback item. */
    fun clearCurrentTrack() {
        ++generation
        userPlayIntent = false
        audioJob?.cancel()
        audioJob = null
        pendingStart = null
        retryStart = null
        expectedNativeTrack = null
        services.setPlaying(false)
        state.value = state.value.copy(
            currentTrack = null,
            progress = 0f,
            positionMs = null,
            isPlaying = false,
            isBuffering = false,
            isPreparing = false,
        )
    }

    private fun applyGain(gain: Float, calibrated: Boolean) {
        services.setTrackGain(gain)
        state.value = state.value.copy(trackGainDb = gain, isCurrentTrackCalibrated = calibrated)
    }

    fun setVolume(volume: Float) {
        ++volumeRevision
        services.setVolume(volume)
        state.value = state.value.copy(volume = volume)
    }

    fun setGain(gain: Float, calibrated: Boolean = false) {
        ++gainRevision
        services.setTrackGain(gain, userInitiated = true)
        state.value = state.value.copy(trackGainDb = gain, isCurrentTrackCalibrated = calibrated)
    }

    fun setAudioEffectPreset(preset: com.resonance.player.model.AudioEffectPreset) {
        services.setAudioEffectPreset(preset)
        state.value = state.value.copy(audioEffectPreset = preset)
    }

    fun refreshAudio(playlistId: String?) {
        // Retry must still prepare the selected media, even if its audio preferences were changed.
        if (retryStart != null) return
        val current = state.value
        current.currentTrack?.let {
            val pending = pendingStart
            activate(
                it,
                playlistId,
                resetGain = false,
                expectNativeTrack = pending?.startsNativePlayback == true,
                autoPlay = userPlayIntent,
                prepare = pending?.prepare ?: {},
                nativePlayback = if (pending?.controlsPlayback != true) NativePlaybackState(
                    it.id, current.isPlaying, userPlayIntent, current.isBuffering, current.progress, current.positionMs,
                ) else null,
                onReady = pending?.ready ?: {},
            )
            state.value = state.value.copy(progress = current.progress, positionMs = current.positionMs, isPlaying = current.isPlaying)
        }
    }

    /** Ignore old controller events until the latest user-requested track is acknowledged. */
    fun acceptNativeTrack(trackId: String): Boolean {
        if (closed) return false
        if (retryStart != null) return false
        expectedNativeTrack?.let { expected ->
            if (trackId != expected) return false
            expectedNativeTrack = null
        }
        return pendingStart?.controlsPlayback != true && trackId != state.value.currentTrack?.id
    }

    fun playbackFailed() {
        ++generation
        audioJob?.cancel()
        pendingStart = null
        retryStart = null
        expectedNativeTrack = null
        userPlayIntent = false
        state.value = state.value.copy(isPlaying = false, isBuffering = false, isPreparing = false)
    }

    fun close() {
        closed = true
        ++generation
        userPlayIntent = false
        pendingStart = null
        retryStart = null
        expectedNativeTrack = null
        audioJob?.cancel()
        state.value = state.value.copy(isPreparing = false, isBuffering = false, isPlaying = false)
    }
}
