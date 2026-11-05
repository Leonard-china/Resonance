package com.resonance.player.platform

import com.resonance.player.ai.DeepSeekClient
import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.DeepSeekTestResult
import com.resonance.player.model.ImportReport
import com.resonance.player.model.DeleteTrackReport
import com.resonance.player.model.Playlist
import com.resonance.player.model.SyncReport
import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Track
import com.resonance.player.lyrics.LrclibLyricsRepository
import com.resonance.player.conversion.DesktopManagedMp3Converter
import com.resonance.player.sync.PlainSyncPackage
import kotlinx.coroutines.Dispatchers
import com.resonance.player.model.CalibrationStatus
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancel
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Properties
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import com.resonance.player.model.*
import com.resonance.player.weather.AppearanceStorage
import com.resonance.player.weather.WeatherClient

internal class DesktopAudioPlayer(
    private val onEnded: () -> Unit,
    private val onProgress: (Float) -> Unit,
    private val onPlayingChanged: (Boolean) -> Unit,
    private val onBufferingChanged: (Boolean) -> Unit,
    private val onError: (PlaybackIssue) -> Unit,
    private val stats: com.resonance.player.playback.ListeningStatsRecorder,
) {
    private val backend: com.resonance.player.audio.DesktopPcmPlayer =
        com.resonance.player.audio.DesktopPcmPlayer({}, {}, {}, {}, ::event)
    val currentRequestId: Long? get() = backend.currentRequestId
    @Synchronized private fun event(event: com.resonance.player.audio.DesktopPlaybackEvent) {
        if (event.requestId != backend.currentRequestId) return
        sample(event.snapshot)
        when (event.kind) {
            com.resonance.player.audio.DesktopPlaybackEvent.Kind.Buffering -> onBufferingChanged(true)
            com.resonance.player.audio.DesktopPlaybackEvent.Kind.Playing -> {
                onBufferingChanged(event.snapshot.isBuffering)
                onPlayingChanged(event.snapshot.isPlaying)
            }
            com.resonance.player.audio.DesktopPlaybackEvent.Kind.Progress -> onProgress(event.snapshot.progress)
            com.resonance.player.audio.DesktopPlaybackEvent.Kind.Ended -> { onBufferingChanged(false); stats.finish(); onProgress(1f); onEnded() }
            com.resonance.player.audio.DesktopPlaybackEvent.Kind.Error -> {
                onBufferingChanged(false)
                onPlayingChanged(false)
                onError(PlaybackIssue(event.message,
                    trackId = event.snapshot.currentTrack?.id, requestId = event.requestId))
            }
        }
    }
    private fun sample(state: PlayerState = backend.snapshot(), discontinuity: Boolean = false) {
        stats.sample(state.currentTrack?.id, state.positionMs ?: 0L,
            state.currentTrack?.durationText?.let { durationTextToSeconds(it) * 1000L } ?: 0L,
            state.isPlaying, state.playbackSpeed, discontinuity)
    }
    val positionMs: Long? get() = backend.positionMs
    fun isPrepared(trackId: String): Boolean = backend.isPrepared(trackId)
    fun snapshot(): PlayerState = backend.snapshot()
    val playWhenReady: Boolean get() = backend.playWhenReady
    @Synchronized fun play(track: Track, autoPlay: Boolean = true, initialProgress: Float = 0f) {
        sample(); stats.start(track.id, (durationTextToSeconds(track.durationText) * 1000L * initialProgress).toLong())
        backend.play(track, autoPlay, initialProgress)
    }
    @Synchronized fun setPlaying(isPlaying: Boolean) {
        sample()
        if (!isPlaying) onBufferingChanged(false)
        if (isPlaying && backend.sessionClosed) stats.start(backend.snapshot().currentTrack?.id)
        backend.setPlaying(isPlaying)
    }
    fun setVolume(volume: Float) = backend.setVolume(volume)
    fun setTrackGain(gainDb: Float) = backend.setTrackGain(gainDb)
    @Synchronized fun setPlaybackSpeed(speed: Float) { sample(); backend.setPlaybackSpeed(speed); sample(discontinuity = true) }
    fun setVocalSuppression(enabled: Boolean) = backend.setVocalSuppression(enabled)
    fun setPreset(preset: com.resonance.player.model.AudioEffectPreset) = backend.setPreset(preset)
    @Synchronized fun seekTo(fraction: Float) { sample(); backend.seekTo(fraction); sample(discontinuity = true) }
    @Synchronized fun close() { sample(); onBufferingChanged(false); backend.close(); stats.flushSoon() }
}

