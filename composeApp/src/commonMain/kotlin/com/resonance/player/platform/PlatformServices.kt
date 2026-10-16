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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface PlatformServices {
    val supportsKgg: Boolean get() = false
    val supportsWebLogin: Boolean get() = false
    val musicAssistant: MusicAssistantServices? get() = null
    val importProgress: StateFlow<com.resonance.player.model.ImportProgress?> get() = MutableStateFlow(null)
    fun cancelImport() {}
    suspend fun chooseAssistantImages(): List<com.resonance.player.model.AiImage> = emptyList()
    suspend fun chooseCoverImage(): String? = null
    suspend fun configureBackgroundRecommendations(settings: com.resonance.player.model.MusicFeatureSettings) {}
    fun updateFloatingLyrics(player: com.resonance.player.model.PlayerState, lyrics: com.resonance.player.model.LyricsUiState, settings: com.resonance.player.model.MusicFeatureSettings) {}
    fun canDrawOverlays(): Boolean = true
    fun requestOverlayPermission(): Boolean = false
    val playbackErrors: Flow<String> get() = kotlinx.coroutines.flow.emptyFlow()
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
    val activeTrackChanges: Flow<String>

    suspend fun loadLibrary(): List<Track>
    suspend fun loadPlaylists(): List<Playlist>
    suspend fun savePlaylists(playlists: List<Playlist>)
    suspend fun loadLastSelectedPlaylistId(): String?
    suspend fun saveLastSelectedPlaylistId(playlistId: String?)
    suspend fun loadThemeMode(): com.resonance.player.model.ThemeMode
    suspend fun saveThemeMode(mode: com.resonance.player.model.ThemeMode)
    suspend fun loadTutorialProgress(): com.resonance.player.tutorial.TutorialProgress = com.resonance.player.tutorial.TutorialProgress()
    suspend fun saveTutorialProgress(progress: com.resonance.player.tutorial.TutorialProgress) {}
    suspend fun loadPlaybackMode(): com.resonance.player.model.PlaybackModePreference = com.resonance.player.model.PlaybackModePreference()
    suspend fun savePlaybackMode(shuffle: Boolean, repeatMode: RepeatMode) {}
    val foreground: kotlinx.coroutines.flow.StateFlow<Boolean>
    val systemReduceMotion: Boolean get() = false
    fun updateSystemBars(isDark: Boolean) {}
    fun currentTimeMillis(): Long
    suspend fun loadAppearance(): com.resonance.player.model.AppearancePreferences
    suspend fun saveAppearance(preferences: com.resonance.player.model.AppearancePreferences)
    suspend fun loadWeatherSnapshot(): com.resonance.player.model.WeatherSnapshot?
    suspend fun saveWeatherSnapshot(snapshot: com.resonance.player.model.WeatherSnapshot)
    suspend fun weatherLocation(requestPermission: Boolean = false): com.resonance.player.model.WeatherLocation?
    suspend fun searchWeatherCities(query: String): List<com.resonance.player.model.WeatherLocation>
    suspend fun fetchWeather(location: com.resonance.player.model.WeatherLocation, previous: com.resonance.player.model.WeatherPalette): com.resonance.player.model.WeatherSnapshot
    suspend fun chooseLyricsFolder(): String?
    val libraryLocation: String

    suspend fun loadDeepSeekConfig(): DeepSeekConfig
    suspend fun saveDeepSeekConfig(config: DeepSeekConfig)
    suspend fun testDeepSeek(config: DeepSeekConfig): DeepSeekTestResult
    suspend fun loadCustomLyricsFolder(): String?
    suspend fun saveCustomLyricsFolder(folder: String?)

    suspend fun setTrackFavorite(trackId: String, favorite: Boolean)
    suspend fun loadLyrics(track: Track, forceRefresh: Boolean = false): LyricsFetchResult
    suspend fun requestAiLyrics(track: Track): LyricsFetchResult
    suspend fun exportSyncPackage(playlistId: String?): SyncReport
    suspend fun importSyncPackage(): SyncReport
    suspend fun importMusic(convertToMp3: Boolean = false): ImportReport
    suspend fun deleteLocalTrack(track: Track): DeleteTrackReport
    suspend fun batchDeleteTracks(tracks: List<Track>): DeleteTrackReport
    suspend fun updateTracks(tracks: List<Track>)
    suspend fun batchEnrichTracks(
        options: com.resonance.player.model.BatchEnrichOptions,
        tracks: List<Track>,
        onProgress: (com.resonance.player.model.BatchEnrichProgress) -> Unit = {},
    ): com.resonance.player.model.BatchEnrichReport
    fun play(track: Track, queue: List<Track>, shuffle: Boolean, repeatMode: RepeatMode)
    fun setPlaying(isPlaying: Boolean)
    fun setPlaybackMode(shuffle: Boolean, repeatMode: RepeatMode)
    fun setVolume(volume: Float)
    fun setPlaybackSpeed(speed: Float)
    fun seekTo(fraction: Float)
    suspend fun saveLyricsOffset(track: Track, offsetMs: Long): com.resonance.player.model.Lyrics?
    suspend fun embedLyricsToAudioFile(track: Track, lyrics: com.resonance.player.model.Lyrics): Boolean
    suspend fun editTrackMetadata(track: Track, newTitle: String, newArtist: String, newAlbum: String): Track
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
    fun close()
}
