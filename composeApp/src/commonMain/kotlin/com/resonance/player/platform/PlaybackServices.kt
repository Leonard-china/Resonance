package com.resonance.player.platform

import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.DeepSeekTestResult
import com.resonance.player.model.DeleteTrackReport
import com.resonance.player.model.ImportReport
import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.Playlist
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.SyncReport
import com.resonance.player.model.Track
import com.resonance.player.model.CalibrationStatus
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Focused platform contract; implemented by the existing native adapters. */
interface PlaybackServices {
    val listeningStats: StateFlow<com.resonance.player.model.ListeningStatsSnapshot>? get() = null
    suspend fun awaitSynchronizedReady(trackId: String): Boolean = true
    fun playSynchronized(track: Track, progress: Float, playing: Boolean, speed: Float) {
        restorePausedPlayback(track, listOf(track), progress, false, RepeatMode.Off)
        setPlaybackSpeed(speed)
        setPlaying(playing)
    }
    val playbackErrors: Flow<String> get() = kotlinx.coroutines.flow.emptyFlow()
    val playbackIssues: Flow<PlaybackIssue> get() = playbackErrors.map { PlaybackIssue(it) }
    fun isCurrentPlaybackIssue(issue: PlaybackIssue): Boolean = true
    val sleepTimer: StateFlow<com.resonance.player.model.SleepTimerState> get() = MutableStateFlow(com.resonance.player.model.SleepTimerState())
    fun setSleepTimer(option: com.resonance.player.model.SleepTimerOption) {}
    fun setVocalSuppression(enabled: Boolean) {}
    val isVocalSuppressionEnabled: StateFlow<Boolean> get() = MutableStateFlow(false)
    fun setAudioEffectPreset(preset: com.resonance.player.model.AudioEffectPreset) {
        setVocalSuppression(preset == com.resonance.player.model.AudioEffectPreset.Karaoke)
    }
    val currentAudioEffectPreset: StateFlow<com.resonance.player.model.AudioEffectPreset> get() = MutableStateFlow(com.resonance.player.model.AudioEffectPreset.Off)
    suspend fun loadAudioEffectPreset(): com.resonance.player.model.AudioEffectPreset = com.resonance.player.model.AudioEffectPreset.Off
    suspend fun saveAudioEffectPreset(preset: com.resonance.player.model.AudioEffectPreset) {}
    suspend fun recordListeningDuration(trackId: String, deltaSeconds: Int, isCompleted: Boolean = false) {}
    suspend fun flushListeningStats() {}
    fun getTrackListeningStats(trackId: String): com.resonance.player.model.TrackListeningStats? = null
    fun getAllListeningStats(): com.resonance.player.model.AllListeningStats = com.resonance.player.model.AllListeningStats()
    fun previewAudio(path: String) {}
    fun stopAudioPreview() {}
    suspend fun loadPlaybackSnapshot(): com.resonance.player.playback.PlaybackSnapshot? = null
    suspend fun savePlaybackSnapshot(snapshot: com.resonance.player.playback.PlaybackSnapshot) {}
    suspend fun resumePlayback(queue: List<Track>): com.resonance.player.playback.PlaybackResume? =
        loadPlaybackSnapshot()?.let { com.resonance.player.playback.PlaybackResume(it) }
    fun restorePausedPlayback(track: Track, queue: List<Track>, progress: Float, shuffle: Boolean, repeatMode: RepeatMode) {
        play(track, queue, shuffle, repeatMode)
        setPlaying(false)
        seekTo(progress)
    }
    val playbackEnded: Flow<Unit>
    val playbackPositionMs: Long? get() = null
    val playbackProgress: Flow<Float>
    val isPlayingChanges: Flow<Boolean>
    val isBufferingChanges: Flow<Boolean> get() = kotlinx.coroutines.flow.emptyFlow()
    val activeTrackChanges: Flow<String>

    suspend fun loadPlaybackMode(): com.resonance.player.model.PlaybackModePreference = com.resonance.player.model.PlaybackModePreference()
    suspend fun savePlaybackMode(shuffle: Boolean, repeatMode: RepeatMode) {}
    fun play(track: Track, queue: List<Track>, shuffle: Boolean, repeatMode: RepeatMode)
    fun setPlaying(isPlaying: Boolean)
    /** Apply to the native session only; persist the captured pair separately through the ordered writer. */
    fun setPlaybackMode(shuffle: Boolean, repeatMode: RepeatMode)
    fun setVolume(volume: Float)
    fun setPlaybackSpeed(speed: Float)
    fun seekTo(fraction: Float)
    suspend fun loadTrackVolume(trackId: String): Float? = null
    suspend fun saveTrackVolume(trackId: String, volume: Float) {}
    suspend fun loadPlaylistVolume(playlistId: String): Float? = null
    suspend fun savePlaylistVolume(playlistId: String, volume: Float) {}
    suspend fun loadGlobalVolume(): Float = 1.0f
    suspend fun saveGlobalVolume(volume: Float) {}
    suspend fun loadTrackGain(trackId: String): Float? = null
    suspend fun saveTrackGain(trackId: String, gainDb: Float) {}
    fun setTrackGain(gainDb: Float, userInitiated: Boolean = false) {}
    val calibrationStatus: StateFlow<CalibrationStatus> get() = MutableStateFlow(CalibrationStatus())
    fun startVolumeCalibration(trackId: String): Boolean = false
    fun finishVolumeCalibration(trackId: String): Float? = null
    fun cancelVolumeCalibration() {}
    suspend fun loadTrackCalibratedGain(trackId: String): Float? = null
    suspend fun saveTrackCalibratedGain(trackId: String, gainDb: Float) {}
    suspend fun clearTrackCalibratedGain(trackId: String) {}
    suspend fun loadAutoLoudnessGain(track: Track): Float? = null
    suspend fun analyzeAndCacheLoudnessGain(track: Track): Float = 0.0f
    suspend fun isAutoLoudnessEnabled(): Boolean = true
    suspend fun setAutoLoudnessEnabled(enabled: Boolean) {}
}
