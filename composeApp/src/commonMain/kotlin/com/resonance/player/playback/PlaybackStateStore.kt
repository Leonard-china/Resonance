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
    private var generation = 0L
    private var volumeRevision = 0L
    private var gainRevision = 0L
    private var audioJob: Job? = null
    private data class StartAction(val prepare: suspend () -> Unit, val ready: () -> Unit)
    private var pendingStart: StartAction? = null
    private var closed = false
    private var expectedNativeTrack: String? = null

    fun activate(track: Track, playlistId: String?, resetGain: Boolean = true, expectNativeTrack: Boolean = false,
        prepare: suspend () -> Unit = {}, onReady: () -> Unit = {}) {
        if (closed) return
        if (expectNativeTrack) expectedNativeTrack = track.id
        else if (expectedNativeTrack != track.id) expectedNativeTrack = null
        services.cancelVolumeCalibration()
        val request = ++generation
        val volumeVersion = volumeRevision
        val gainVersion = gainRevision
        audioJob?.cancel()
        val start = StartAction(prepare, onReady)
        pendingStart = start
        state.value = state.value.copy(currentTrack = track, progress = 0f, positionMs = null,
            trackGainDb = 0f, isCurrentTrackCalibrated = false)
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
                onReady()
                started = true
                pendingStart = null
                if (calibrated == null && automatic && cached == null) {
                    val analyzed = services.analyzeAndCacheLoudnessGain(track)
                    coroutineContext.ensureActive()
                    if (request == generation && gainVersion == gainRevision) applyGain(analyzed, false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (request == generation) {
                    if (!started) {
                        // Persistence/preparation failures must not start an obsolete queue.
                        pendingStart = null
                        expectedNativeTrack = null
                    }
                    onError(error)
                }
            }
        }
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
        val current = state.value
        current.currentTrack?.let {
            val pending = pendingStart
            activate(it, playlistId, resetGain = false, prepare = pending?.prepare ?: {}, onReady = pending?.ready ?: {})
            state.value = state.value.copy(progress = current.progress, positionMs = current.positionMs, isPlaying = current.isPlaying)
        }
    }

    /** Ignore old controller events until the latest user-requested track is acknowledged. */
    fun acceptNativeTrack(trackId: String): Boolean {
        if (closed) return false
        expectedNativeTrack?.let { expected ->
            if (trackId != expected) return false
            expectedNativeTrack = null
        }
        return pendingStart == null && trackId != state.value.currentTrack?.id
    }

    fun playbackFailed() {
        expectedNativeTrack = null
        state.value = state.value.copy(isPlaying = false)
    }

    fun close() { closed = true; ++generation; pendingStart = null; expectedNativeTrack = null; audioJob?.cancel() }
}
