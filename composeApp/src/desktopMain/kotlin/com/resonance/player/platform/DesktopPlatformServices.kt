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
import kotlinx.coroutines.ensureActive
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

class DesktopPlatformServices : PlatformServices {
    private val closed = java.util.concurrent.atomic.AtomicBoolean(false)
    var fileDialogOwner: java.awt.Component? = null
    override val listenTogether by lazy {
        com.resonance.player.together.JvmListenTogetherService(library.appDirectory.resolve("cache/together-audio"), { track ->
            val source = requireNotNull(track.sourceUri)
            require(!source.contains("://") || source.startsWith("file:")) { "一起听仅共享本机音频" }
            val path = if (source.startsWith("file:")) java.nio.file.Paths.get(java.net.URI(source)) else java.nio.file.Paths.get(source)
            Files.newInputStream(path)
        }, onGuestStopped = { setPlaying(false) }, prefetchNetworkAllowed = { automaticRecommendationAllowed(unmeteredOnly = true) },
            companionStore = com.resonance.player.together.TogetherCompanionStore(library.appDirectory.resolve("together-companions"),
                credentialVault::encrypt,
                credentialVault::decrypt, TogetherDefaults.RelayUrl)).apply { trackPlayerReadiness(true) }
    }

    override fun playSynchronized(track: Track, progress: Float, playing: Boolean, speed: Float) {
        stopAudioPreview()
        playingMusicId = track.id
        player.setPlaybackSpeed(speed)
        player.play(track, autoPlay = playing, initialProgress = progress.coerceIn(0f, 1f))
    }
    override suspend fun awaitSynchronizedReady(trackId: String): Boolean = kotlinx.coroutines.withTimeoutOrNull(15_000) {
        while (!player.isPrepared(trackId)) kotlinx.coroutines.delay(25)
        true
    } ?: false
    override val supportsKgg = true
    private val currentImport = java.util.concurrent.atomic.AtomicReference<com.resonance.player.io.ImportControl?>()
    override val importProgress = MutableStateFlow<ImportProgress?>(null)
    override fun cancelImport() { currentImport.get()?.cancel() }
    private val droppedMusic = kotlinx.coroutines.channels.Channel<List<String>>(1)
    private val dropPending = java.util.concurrent.atomic.AtomicBoolean(false)
    override val externalMusicImports = droppedMusic.receiveAsFlow()

    fun offerDroppedMusic(paths: List<Path>): Boolean {
        if (paths.isEmpty() || currentImport.get() != null || !dropPending.compareAndSet(false, true)) return false
        if (droppedMusic.trySend(paths.map(Path::toString)).isSuccess) return true
        dropPending.set(false)
        return false
    }
    private val loudnessAnalyzer = com.resonance.player.audio.DesktopLoudnessAnalyzer()
    override suspend fun loadPlaybackSnapshot() = withContext(Dispatchers.IO) {
        com.resonance.player.playback.PlaybackSnapshotCodec.decode(library.readUiValue("playback.snapshot"))
    }
    override suspend fun savePlaybackSnapshot(snapshot: com.resonance.player.playback.PlaybackSnapshot) = withContext(Dispatchers.IO) {
        library.writeUiValue("playback.snapshot", com.resonance.player.playback.PlaybackSnapshotCodec.encode(snapshot))
    }
    override fun restorePausedPlayback(track: Track, queue: List<Track>, progress: Float, shuffle: Boolean, repeatMode: RepeatMode) {
        playingMusicId = track.id
        player.play(track, autoPlay = false, initialProgress = progress)
    }
    override val foreground = MutableStateFlow(true)
    private val _vocalSuppressionState = MutableStateFlow(false)
    override val isVocalSuppressionEnabled: StateFlow<Boolean> = _vocalSuppressionState
    override fun setVocalSuppression(enabled: Boolean) {
        _vocalSuppressionState.value = enabled
        player.setVocalSuppression(enabled)
    }
    private val listeningStatsRepository by lazy { com.resonance.player.playback.ListeningStatsRepository.get(library.appDirectory) }
    override val listeningStats get() = listeningStatsRepository.snapshots
    private val _audioEffectPresetState = MutableStateFlow(com.resonance.player.model.AudioEffectPreset.Off)
    override val currentAudioEffectPreset: StateFlow<com.resonance.player.model.AudioEffectPreset> = _audioEffectPresetState

    override fun setAudioEffectPreset(preset: com.resonance.player.model.AudioEffectPreset) {
        _audioEffectPresetState.value = preset
        _vocalSuppressionState.value = (preset == com.resonance.player.model.AudioEffectPreset.Karaoke)
        player.setPreset(preset)
    }

    override suspend fun loadAudioEffectPreset(): com.resonance.player.model.AudioEffectPreset = withContext(Dispatchers.IO) {
        com.resonance.player.model.AudioEffectPreset.fromId(library.readUiValue("audio.effect_preset"))
    }

    override suspend fun saveAudioEffectPreset(preset: com.resonance.player.model.AudioEffectPreset): Unit = withContext(Dispatchers.IO) {
        library.writeUiValue("audio.effect_preset", preset.id)
    }

    override suspend fun recordListeningDuration(trackId: String, deltaSeconds: Int, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        listeningStatsRepository.record(trackId, deltaSeconds, isCompleted)
        listeningStatsRepository.flush()
    }

    override suspend fun flushListeningStats() = withContext(Dispatchers.IO) { listeningStatsRepository.flush() }

    override fun getTrackListeningStats(trackId: String): com.resonance.player.model.TrackListeningStats? {
        return listeningStatsRepository.getTrackStats(trackId)
    }

    override fun getAllListeningStats(): com.resonance.player.model.AllListeningStats {
        return listeningStatsRepository.getAllStats()
    }
    override val systemReduceMotion: Boolean get() = java.awt.Toolkit.getDefaultToolkit()
        .getDesktopProperty("win.clientAreaAnimation") == false
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override suspend fun loadAppearance() = withContext(Dispatchers.IO) {
        AppearanceStorage.preferences(library.readUiValue("app.appearance"))
    }
    override suspend fun saveAppearance(preferences: AppearancePreferences) = withContext(Dispatchers.IO) {
        library.writeUiValue("app.appearance", AppearanceStorage.encode(preferences))
    }
    override suspend fun loadWeatherSnapshot() = withContext(Dispatchers.IO) {
        AppearanceStorage.snapshot(library.readUiValue("app.weather"))
    }
    override suspend fun saveWeatherSnapshot(snapshot: WeatherSnapshot) = withContext(Dispatchers.IO) {
        library.writeUiValue("app.weather", AppearanceStorage.encode(snapshot))
    }

    override suspend fun loadTrackVolume(trackId: String): Float? = withContext(Dispatchers.IO) {
        library.readUiValue("vol.track.$trackId")?.toFloatOrNull()
    }

    override suspend fun saveTrackVolume(trackId: String, volume: Float): Unit = withContext(Dispatchers.IO) {
        library.writeUiValue("vol.track.$trackId", volume.coerceIn(0f, 1f).toString())
    }

    override suspend fun loadPlaylistVolume(playlistId: String): Float? = withContext(Dispatchers.IO) {
        library.readUiValue("vol.playlist.$playlistId")?.toFloatOrNull()
    }

    override suspend fun savePlaylistVolume(playlistId: String, volume: Float): Unit = withContext(Dispatchers.IO) {
        library.writeUiValue("vol.playlist.$playlistId", volume.coerceIn(0f, 1f).toString())
    }

    override suspend fun loadGlobalVolume(): Float = withContext(Dispatchers.IO) {
        library.readUiValue("vol.global")?.toFloatOrNull() ?: 1.0f
    }

    override suspend fun saveGlobalVolume(volume: Float): Unit = withContext(Dispatchers.IO) {
        library.writeUiValue("vol.global", volume.coerceIn(0f, 1f).toString())
    }

    override suspend fun loadTrackGain(trackId: String): Float? = withContext(Dispatchers.IO) {
        library.readUiValue("gain.track.$trackId")?.toFloatOrNull()
    }

    override suspend fun saveTrackGain(trackId: String, gainDb: Float): Unit = withContext(Dispatchers.IO) {
        library.writeUiValue("gain.track.$trackId", gainDb.coerceIn(-12f, 12f).toString())
    }

    override fun setTrackGain(gainDb: Float, userInitiated: Boolean) = player.setTrackGain(gainDb)

    private val _desktopCalibrationStatus = MutableStateFlow(CalibrationStatus())
    override val calibrationStatus: StateFlow<CalibrationStatus> = _desktopCalibrationStatus

    override fun startVolumeCalibration(trackId: String): Boolean {
        _desktopCalibrationStatus.value = CalibrationStatus(
            isActive = true,
            trackId = trackId,
            baseVolume = 10,
            currentVolume = 10,
            maxVolume = 15,
            deltaDb = 0f,
        )
        return true
    }

    override fun finishVolumeCalibration(trackId: String): Float? {
        val cur = _desktopCalibrationStatus.value
        _desktopCalibrationStatus.value = CalibrationStatus(isActive = false)
        return cur.deltaDb
    }

    override fun cancelVolumeCalibration() {
        _desktopCalibrationStatus.value = CalibrationStatus(isActive = false)
    }

    override suspend fun loadTrackCalibratedGain(trackId: String): Float? = withContext(Dispatchers.IO) {
        library.readUiValue("gain.calibrated.$trackId")?.toFloatOrNull()
    }

    override suspend fun saveTrackCalibratedGain(trackId: String, gainDb: Float): Unit = withContext(Dispatchers.IO) {
        library.writeUiValue("gain.calibrated.$trackId", gainDb.coerceIn(-18f, 18f).toString())
    }

    override suspend fun clearTrackCalibratedGain(trackId: String): Unit = withContext(Dispatchers.IO) {
        library.removeUiValue("gain.calibrated.$trackId")
    }

    override suspend fun isAutoLoudnessEnabled(): Boolean = withContext(Dispatchers.IO) {
        library.readUiValue("gain.auto_enabled")?.toBooleanStrictOrNull() ?: true
    }

    override suspend fun setAutoLoudnessEnabled(enabled: Boolean): Unit = withContext(Dispatchers.IO) {
        library.writeUiValue("gain.auto_enabled", enabled.toString())
    }

    override suspend fun loadAutoLoudnessGain(track: Track): Float? = withContext(Dispatchers.IO) {
        library.readUiValue("gain.auto.${com.resonance.player.audio.LoudnessAnalysis.CACHE_VERSION}.${track.id}")?.toFloatOrNull()
    }

    override suspend fun analyzeAndCacheLoudnessGain(track: Track): Float = withContext(Dispatchers.IO) {
        loadAutoLoudnessGain(track)?.let { return@withContext it }
        val source = track.sourceUri ?: return@withContext 0f
        val measured = loudnessAnalyzer.analyze(source) ?: return@withContext 0f
        library.writeUiValue("gain.auto.${com.resonance.player.audio.LoudnessAnalysis.CACHE_VERSION}.${track.id}", measured.toString())
        measured
    }
    override suspend fun searchWeatherCities(query: String) = WeatherClient.cities(query)
    override suspend fun fetchWeather(location: WeatherLocation, previous: WeatherPalette) = WeatherClient.current(location, previous)
    override suspend fun weatherLocation(requestPermission: Boolean): WeatherLocation? = WindowsWeatherLocation.current()
    override suspend fun chooseLyricsFolder(): String? = withContext(Dispatchers.IO) {
        var selected: String? = null
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                dialogTitle = "选择本地歌词目录"
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) selected = chooser.selectedFile.absolutePath
        }
        selected
    }
    private val library = DesktopLibraryStore()
    private val credentialVault by lazy { com.resonance.player.features.DesktopCredentialVault(library.appDirectory) }
    @Volatile private var playingMusicId: String? = null
    private val assistantScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private var recommendationJob: kotlinx.coroutines.Job? = null
    private var recommendationSchedule: Triple<Boolean, Int, Boolean>? = null
    override val supportsKeyboardShortcuts get() = true
    override suspend fun saveReplacementAudio(track: Track): Unit = withContext(Dispatchers.IO) {
        check(audioFileAvailable(track)) { "所选音频已不可读取，未替换来源" }
        library.replaceAudio(track)
    }
    override fun formatDateTime(epochMillis: Long): String = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    override suspend fun chooseReplacementAudio(track: Track): Track? = withContext(Dispatchers.IO) {
        val chosen = AtomicReference<java.io.File?>()
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                dialogTitle = "为「${track.title}」重新定位音频"
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("音频文件", "mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "ape")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chosen.set(chooser.selectedFile)
        }
        chosen.get()?.let { file ->
            require(file.isFile) { "请选择可读取的音频文件" }
            track.copy(sourceUri = file.absolutePath, mimeType = Files.probeContentType(file.toPath()) ?: when (file.extension.lowercase()) {
                "flac" -> "audio/flac"; "m4a" -> "audio/mp4"; "wav" -> "audio/wav"; "ogg", "opus" -> "audio/ogg"; "aac" -> "audio/aac"; else -> "audio/mpeg"
            })
        }
    }
    override suspend fun automaticRecommendationAllowed(unmeteredOnly: Boolean) = AutomaticDownloadNetwork.allowed(unmeteredOnly)
    override val musicAssistant by lazy {
        com.resonance.player.features.JvmMusicAssistant(
            com.resonance.player.features.MusicJsonStore(library.appDirectory.resolve("music-assistant")), library.managedMusicDirectory,
            com.resonance.player.features.DesktopMusicAccess(library, { id -> if (playingMusicId == id) { player.close(); playingMusicId = null; isPlayingEvents.tryEmit(false) } }, { playingMusicId }),
            credentialVault::encrypt, credentialVault::decrypt,
            screenshotOcr = com.resonance.player.features.desktopScreenshotOcr(),
            autoInstallBuiltinSources = true,
        )
    }
    override suspend fun configureBackgroundRecommendations(settings: MusicFeatureSettings) {
        val schedule = Triple(settings.recommendationsEnabled && settings.backgroundRecommendations, settings.intervalHours, settings.recommendationsUnmeteredOnly)
        if (recommendationSchedule == schedule) return
        recommendationSchedule = schedule
        recommendationJob?.cancel()
        if (settings.recommendationsEnabled && settings.backgroundRecommendations) {
            recommendationJob = assistantScope.launch {
                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    try { if (automaticRecommendationAllowed(settings.recommendationsUnmeteredOnly)) musicAssistant.recommend() } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (_: Exception) {}
                    kotlinx.coroutines.delay(60_000)
                }
            }
        }
    }
    override suspend fun chooseAssistantImages() = withContext(Dispatchers.IO) {
        com.resonance.player.features.DesktopAssistantImages.slices(com.resonance.player.features.DesktopAssistantImages.select(true))
    }
    override suspend fun chooseCoverImage(): String? = withContext(Dispatchers.IO) {
        com.resonance.player.features.DesktopAssistantImages.select(false).firstOrNull()?.toString()
    }
    private val lyricsRepository = LrclibLyricsRepository(library.lyricsCacheDirectory)
    private val deepSeekClient = DeepSeekClient { messages, tokens -> musicAssistant.completeText(messages.map { AssistantMessage(it.getValue("role"), it.getValue("content")) }, tokens) }
    private val endedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val progressEvents = MutableSharedFlow<Float>(extraBufferCapacity = 1)
    private val isPlayingEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    @Volatile private var hostShuffle = false
    @Volatile private var hostRepeat = RepeatMode.Off
    private val errorEvents = MutableSharedFlow<PlaybackIssue>(extraBufferCapacity = 8)
    override val playbackErrors: Flow<String> get() = playbackIssues.map { it.message }
    override val playbackIssues: Flow<PlaybackIssue> get() = merge(errorEvents,
        listeningStatsRepository.errors.filterNotNull().map { PlaybackIssue(it, PlaybackIssue.Source.Statistics) })
    override fun isCurrentPlaybackIssue(issue: PlaybackIssue): Boolean =
        issue.matches(player.snapshot().currentTrack?.id, player.currentRequestId)
    private val player: DesktopAudioPlayer = DesktopAudioPlayer(
        onEnded = {
            playingMusicId = null
            isPlayingEvents.tryEmit(false)
            if (!sleep.trackEnded() && !(listenTogether.state.value.role == TogetherRole.Host && togetherHostDriver.trackEnded())) endedEvents.tryEmit(Unit)
        },
        onProgress = { progressEvents.tryEmit(it) },
        onPlayingChanged = { isPlayingEvents.tryEmit(it) },
        onBufferingChanged = { isBufferingEvents.tryEmit(it) },
        onError = { errorEvents.tryEmit(it) },
        stats = listeningStatsRepository.recorder(),
    )
    override var togetherLocalSnapshot: Any? = null
    override val managesTogetherPlayback: Boolean get() = true
    private val togetherPlaybackScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    private val togetherHostDriver by lazy {
        com.resonance.player.playback.TogetherHostPlaybackDriver(listenTogether, togetherPlaybackScope,
            current = { player.snapshot().copy(shuffleEnabled = hostShuffle, repeatMode = hostRepeat) },
            replaceQueue = { tracks, target ->
                val current = player.snapshot()
                if (tracks.isEmpty()) {
                    if (current.currentTrack != null) { player.close(); playingMusicId = null; isPlayingEvents.tryEmit(false) }
                } else if (tracks.none { it.id == current.currentTrack?.id }) {
                    val wanted = player.playWhenReady
                    tracks.firstOrNull { it.id == target }?.let {
                        playingMusicId = it.id
                        player.play(it, autoPlay = wanted)
                    }
                }
            }, select = { track, _, autoPlay ->
                stopAudioPreview(); playingMusicId = track.id
                player.play(track, autoPlay = autoPlay)
            }, playing = ::setPlaying, seek = ::seekTo, onError = listenTogether::reportHostPlaybackFailure)
    }
    private val togetherDriver by lazy {
        com.resonance.player.playback.TogetherPlaybackDriver(listenTogether, togetherPlaybackScope,
            current = { player.snapshot() }, prepare = { frame -> frame.track?.let { playSynchronized(it, frame.progress, false, frame.speed) } },
            seek = ::seekTo, playing = ::setPlaying, speed = ::setPlaybackSpeed, ready = ::awaitSynchronizedReady)
    }
    init {
        togetherPlaybackScope.launch {
            togetherDriver
            togetherHostDriver
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                  val room = listenTogether.state.value
                  val hosting = room.role == TogetherRole.Host
                  if (hosting) listenTogether.publishNativePlayback(player.snapshot(), room.roomSessionId)
                  kotlinx.coroutines.delay(if (hosting) 100 else 500)
            }
        }
    }

    override val playbackEnded: Flow<Unit> = endedEvents
    private val sleep: com.resonance.player.playback.PlaybackSleepTimer = com.resonance.player.playback.PlaybackSleepTimer(assistantScope, onExpire = { player.setPlaying(false); isPlayingEvents.tryEmit(false) })
    override val sleepTimer get() = sleep.state
    override fun setSleepTimer(option: com.resonance.player.model.SleepTimerOption) = sleep.set(option)
    private val preview = com.resonance.player.audio.DesktopPcmPlayer({}, {}, {}, { errorEvents.tryEmit(PlaybackIssue("试听失败：$it", PlaybackIssue.Source.Preview)) })
    override fun previewAudio(path: String) {
        preview.close()
        preview.play(Track("source-preview", "试听", "", "", "0:30", 0, sourceUri = path))
    }
    override fun stopAudioPreview() = preview.close()
    override suspend fun audioFileAvailable(track: Track): Boolean = withContext(Dispatchers.IO) {
        val source = track.sourceUri ?: return@withContext false
        runCatching { Files.isRegularFile(if (source.startsWith("file:")) Path.of(java.net.URI(source)) else Path.of(source)) }.getOrDefault(false)
    }
    override val playbackPositionMs: Long? get() = player.positionMs
    override val playbackProgress: Flow<Float> = progressEvents
    override val isPlayingChanges: Flow<Boolean> = isPlayingEvents
    private val isBufferingEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    override val isBufferingChanges: Flow<Boolean> = isBufferingEvents
    override val activeTrackChanges: Flow<String> = emptyFlow()

    override suspend fun loadLibrary(): List<Track> = withContext(Dispatchers.IO) { library.load() }
    override suspend fun chooseMusicSourceFile(): String? = withContext(Dispatchers.IO) {
        val selection = AtomicReference<Path?>()
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                dialogTitle = "导入 MusicFree 歌源"
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("JavaScript 歌源", "js")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) selection.set(chooser.selectedFile.toPath())
        }
        selection.get()?.let { path ->
            require(Files.size(path) <= 2_000_000) { "歌源文件超过 2 MB" }
            Files.readString(path)
        }
    }

    override suspend fun loadPlaylists(): List<Playlist> = withContext(Dispatchers.IO) { library.loadPlaylists() }

    override suspend fun savePlaylists(playlists: List<Playlist>) = withContext(Dispatchers.IO) {
        library.savePlaylists(playlists)
    }

    override suspend fun loadLastSelectedPlaylistId(): String? = withContext(Dispatchers.IO) {
        library.loadLastSelectedPlaylistId()
    }

    override suspend fun saveLastSelectedPlaylistId(playlistId: String?) = withContext(Dispatchers.IO) {
        library.saveLastSelectedPlaylistId(playlistId)
    }

    override suspend fun loadThemeMode(): com.resonance.player.model.ThemeMode = withContext(Dispatchers.IO) {
        library.loadThemeMode()
    }

    override suspend fun saveThemeMode(mode: com.resonance.player.model.ThemeMode): Unit = withContext(Dispatchers.IO) {
        library.saveThemeMode(mode)
    }
    override suspend fun loadTutorialProgress() = withContext(Dispatchers.IO) {
        com.resonance.player.features.TutorialProgressCodec.decode(library.readUiValue(com.resonance.player.features.TutorialProgressCodec.KEY))
    }
    override suspend fun saveTutorialProgress(progress: com.resonance.player.tutorial.TutorialProgress): Unit = withContext(Dispatchers.IO) {
        library.writeUiValue(com.resonance.player.features.TutorialProgressCodec.KEY, com.resonance.player.features.TutorialProgressCodec.encode(progress))
    }
    override suspend fun loadPlaybackMode(): PlaybackModePreference = withContext(Dispatchers.IO) {
        library.loadPlaybackMode()
    }
    override suspend fun savePlaybackMode(shuffle: Boolean, repeatMode: RepeatMode): Unit = withContext(Dispatchers.IO) {
        library.savePlaybackMode(shuffle, repeatMode)
    }

    override val libraryLocation: String
        get() = library.managedMusicDirectory.toString()

    override suspend fun loadDeepSeekConfig(): DeepSeekConfig = withContext(Dispatchers.IO) {
        val settings = musicAssistant.settings()
        settings.connections.firstOrNull { it.provider == settings.assistantProvider && it.configured }?.let {
            return@withContext DeepSeekConfig(it.apiKey, it.baseUrl, it.model, true)
        }
        library.loadDeepSeekConfig()
    }

    override suspend fun saveDeepSeekConfig(config: DeepSeekConfig): Unit = withContext(Dispatchers.IO) {
        library.saveDeepSeekConfig(config)
    }

    override suspend fun testDeepSeek(config: DeepSeekConfig): DeepSeekTestResult =
        deepSeekClient.testConnection(config)

    override suspend fun loadCustomLyricsFolder(): String? = withContext(Dispatchers.IO) {
        library.loadCustomLyricsFolder()
    }

    override suspend fun saveCustomLyricsFolder(folder: String?): Unit = withContext(Dispatchers.IO) {
        library.saveCustomLyricsFolder(folder)
    }

    override suspend fun setTrackFavorite(trackId: String, favorite: Boolean) = withContext(Dispatchers.IO) {
        library.setFavorite(trackId, favorite)
    }

    override suspend fun loadLyrics(track: Track, forceRefresh: Boolean): LyricsFetchResult =
        lyricsRepository.fetch(
            track = track,
            forceRefresh = forceRefresh,
            customLyricsFolder = library.loadCustomLyricsFolder(),
            aiFallback = null,
            sourceFallback = musicAssistant::findSourceLyrics,
        )

    override suspend fun requestAiLyrics(track: Track): LyricsFetchResult = withContext(Dispatchers.IO) {
        val config = loadDeepSeekConfig()
        val result = deepSeekClient.generateLyrics(config, track)
        if (result is LyricsFetchResult.Found) {
            lyricsRepository.writeCache(track, result)
        }
        result
    }

    override suspend fun exportSyncPackage(playlistId: String?): SyncReport = withContext(Dispatchers.IO) {
        val allPlaylists = library.loadPlaylists()
        val selectedPlaylists = if (playlistId == null) allPlaylists else allPlaylists.filter { it.id == playlistId }
        val preferredName = syncPackageFileName(if (playlistId == null) null else selectedPlaylists.single().name)
        val target = chooseSyncPackageOnEdt(save = true, suggestedName = preferredName) ?: return@withContext SyncReport(false, "已取消导出", cancelled = true)
        val allTracks = tracksWithPlaylistArtwork(library.load(), allPlaylists)
        val tracks = if (playlistId == null) {
            allTracks
        } else {
            val byId = allTracks.associateBy(Track::id)
            selectedPlaylists.flatMap(Playlist::tracks).map { track -> byId[track.id] ?: track }.distinctBy(Track::id)
        }
        require(tracks.isNotEmpty()) { "所选歌单没有可导出的本地音频" }
        PlainSyncPackage().export(
            target = target,
            tracks = tracks,
            playlists = selectedPlaylists,
            openAudio = { track -> track.sourceUri?.let(Path::of)?.takeIf(Files::isRegularFile)?.let(Files::newInputStream) },
            openArtwork = { track -> openArtworkSource(track.artworkPath) },
        )
        SyncReport(true, "已导出到 $target", tracks.size, selectedPlaylists.size,
            exportedPackage = ExportedPackage(target.fileName.toString(), target.toAbsolutePath().parent.toString(), "打开文件夹"))
    }

    override suspend fun openExportLocation(directory: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val folder = Path.of(directory)
            if (!Files.isDirectory(folder) || !java.awt.Desktop.isDesktopSupported()) return@runCatching false
            val desktop = java.awt.Desktop.getDesktop()
            if (!desktop.isSupported(java.awt.Desktop.Action.OPEN)) return@runCatching false
            desktop.open(folder.toFile())
            true
        }.getOrDefault(false)
    }

    override suspend fun importSyncPackage(): SyncReport = withContext(Dispatchers.IO) {
        val source = chooseSyncPackageOnEdt(save = false) ?: return@withContext SyncReport(false, "已取消导入", cancelled = true)
        val importContext = kotlin.coroutines.coroutineContext
        val imported = PlainSyncPackage().import(source, library.managedMusicDirectory,
            onCommit = { library.mergeImported(it.tracks, it.playlists) }, checkCancelled = { importContext.ensureActive() })
        SyncReport(true, "歌单文件导入完成", imported.tracks.size, imported.playlists.size)
    }

    override suspend fun importMusic(convertToMp3: Boolean): ImportReport =
        importSelectedMusic(convertToMp3) { chooseMusicOnEdt() }

    override suspend fun importMusicPaths(paths: List<String>, convertToMp3: Boolean): ImportReport {
        try {
            return importSelectedMusic(convertToMp3) { paths.map(Path::of) }
        } finally { dropPending.set(false) }
    }

    private suspend fun importSelectedMusic(convertToMp3: Boolean, selection: () -> List<Path>): ImportReport = withContext(Dispatchers.IO) {
        val control = com.resonance.player.io.ImportControl()
        check(currentImport.compareAndSet(null, control)) { "已有导入任务正在运行" }
        try {
            val paths = selection()
            if (paths.isEmpty()) return@withContext ImportReport(emptyList())
            importProgress.value = ImportProgress("扫描文件", detail = "正在查找音频和歌单包…")
            val report = DesktopMusicScanner(library.artworkDirectory, library.managedMusicDirectory) { library.mergeImported(it.tracks, it.playlists) }
                .scan(paths, convertToMp3, control) { importProgress.value = it }
            library.mergeImported(report.tracks, report.playlists)
            report
        } finally { currentImport.set(null); importProgress.value = null }
    }

    private val enricher = com.resonance.player.enrich.MetadataEnricher(
        artworkDirectory = library.artworkDirectory,
        lyricsRepository = lyricsRepository,
        deepSeekClient = deepSeekClient,
    )

    override suspend fun updateTracks(tracks: List<Track>): Unit = withContext(Dispatchers.IO) {
        library.updateTracks(tracks)
    }

    override suspend fun batchEnrichTracks(
        options: com.resonance.player.model.BatchEnrichOptions,
        tracks: List<Track>,
        onProgress: (com.resonance.player.model.BatchEnrichProgress) -> Unit,
    ): com.resonance.player.model.BatchEnrichReport = withContext(Dispatchers.IO) {
        val config = loadDeepSeekConfig()
        val customFolder = library.loadCustomLyricsFolder()
        val report = enricher.batchEnrich(options, tracks, config, customFolder, onProgress) { updated -> library.updateTracks(listOf(updated)) }
        if (report.updatedTracks.isNotEmpty()) {
            library.updateTracks(report.updatedTracks)
        }
        report
    }

    override suspend fun deleteLocalTrack(track: Track): DeleteTrackReport = withContext(Dispatchers.IO) {
        if (playingMusicId == track.id) { player.close(); playingMusicId = null; isPlayingEvents.tryEmit(false) }
        val deleted = try {
            track.sourceUri?.takeIf(String::isNotBlank)?.let { raw ->
                val source = if (raw.startsWith("file:")) Path.of(java.net.URI(raw)) else Path.of(raw)
                Files.deleteIfExists(source)
            } ?: false
        } catch (error: Exception) {
            return@withContext DeleteTrackReport(false, false, "未能删除「${track.title}」，曲库记录已保留：${error.message.orEmpty()}")
        }
        try { library.remove(track.id) }
        catch (error: Exception) {
            return@withContext DeleteTrackReport(false, deleted, "音频处理后曲库更新失败：${error.message.orEmpty()}")
        }
        // Artwork may be shared or user-owned. Audio deletion never implies artwork ownership.
        DeleteTrackReport(true, deleted,
            if (deleted) "已删除「${track.title}」的本地音频文件" else "音频已不存在，已移除「${track.title}」的曲库记录",
            setOf(track.id))
    }

    override suspend fun batchDeleteTracks(tracks: List<Track>): DeleteTrackReport = withContext(Dispatchers.IO) {
        val reports = tracks.distinctBy(Track::id).map { deleteLocalTrack(it) }
        val removed = reports.flatMap { it.removedTrackIds }.toSet()
        val failed = reports.filterNot { it.success }
        DeleteTrackReport(failed.isEmpty(), reports.any { it.fileDeleted },
            "已移除 ${removed.size} 首" + if (failed.isEmpty()) "" else "；${failed.size} 首未完成：" + failed.take(5).joinToString("；") { it.message },
            removed)
    }

    override fun play(track: Track, queue: List<Track>, shuffle: Boolean, repeatMode: RepeatMode) {
        stopAudioPreview()
        hostShuffle = shuffle; hostRepeat = repeatMode
        playingMusicId = track.id
        player.play(track)
    }

    override fun setPlaying(isPlaying: Boolean) {
        if (isPlaying) stopAudioPreview()
        player.setPlaying(isPlaying)
    }

    override fun setPlaybackMode(shuffle: Boolean, repeatMode: RepeatMode) {
        hostShuffle = shuffle; hostRepeat = repeatMode
    }

    override fun setVolume(volume: Float) = player.setVolume(volume)

    override fun setPlaybackSpeed(speed: Float) = player.setPlaybackSpeed(speed)

    override fun seekTo(fraction: Float) = player.seekTo(fraction)

    override suspend fun saveLyricsOffset(track: Track, offsetMs: Long): com.resonance.player.model.Lyrics? = withContext(Dispatchers.IO) {
        lyricsRepository.updateOffset(track, offsetMs)
    }

    override suspend fun embedLyricsToAudioFile(track: Track, lyrics: com.resonance.player.model.Lyrics): Boolean = withContext(Dispatchers.IO) {
        val rawSource = track.sourceUri ?: return@withContext false
        val path = Path.of(rawSource).toAbsolutePath().normalize()
        if (!Files.isRegularFile(path)) return@withContext false
        runCatching {
            val audioFile = AudioFileIO.read(path.toFile())
            val tag = audioFile.tag ?: audioFile.createDefaultTag().also { audioFile.tag = it }
            val lrcText = if (lyrics.synchronized) {
                lyrics.lines.joinToString("\n") { line ->
                    val timestamp = (line.timestampMs ?: 0L) + lyrics.offsetMs
                    "[%02d:%02d.%02d]%s".format(
                        timestamp / 60_000,
                        timestamp / 1_000 % 60,
                        timestamp / 10 % 100,
                        line.text,
                    )
                }
            } else {
                lyrics.lines.joinToString("\n") { it.text }
            }
            tag.setField(FieldKey.LYRICS, lrcText)
            audioFile.commit()
            true
        }.getOrDefault(false)
    }

    override suspend fun editTrackMetadata(track: Track, newTitle: String, newArtist: String, newAlbum: String): Track = withContext(Dispatchers.IO) {
        val cleanTitle = newTitle.trim().ifBlank { track.title }
        val cleanArtist = newArtist.trim().ifBlank { track.artist }
        val cleanAlbum = newAlbum.trim().ifBlank { track.album }
        val rawSource = track.sourceUri
        if (!rawSource.isNullOrBlank()) {
            val path = Path.of(rawSource).toAbsolutePath().normalize()
            if (Files.isRegularFile(path)) {
                runCatching {
                    val audioFile = AudioFileIO.read(path.toFile())
                    val tag = audioFile.tag ?: audioFile.createDefaultTag().also { audioFile.tag = it }
                    tag.setField(FieldKey.TITLE, cleanTitle)
                    tag.setField(FieldKey.ARTIST, cleanArtist)
                    tag.setField(FieldKey.ALBUM, cleanAlbum)
                    audioFile.commit()
                }
            }
        }
        val updated = track.copy(
            title = cleanTitle,
            artist = cleanArtist,
            album = cleanAlbum,
        )
        library.updateTracks(listOf(updated))
        updated
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        cancelImport()
        togetherDriver.close()
        togetherHostDriver.close()
        togetherPlaybackScope.cancel()
        listenTogether.close()
        sleep.close()
        stopAudioPreview()
        assistantScope.cancel()
        player.close()
        kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withTimeoutOrNull(3_000) { runCatching { listeningStatsRepository.flush() } }
        }
    }

    private fun openArtworkSource(value: String?): java.io.InputStream? {
        value ?: return null
        if (value.startsWith("https://")) {
            return java.net.URL(value).openConnection().apply {
                connectTimeout = 8_000
                readTimeout = 12_000
            }.getInputStream().buffered()
        }
        return runCatching { Path.of(value) }.getOrNull()?.takeIf(Files::isRegularFile)?.let(Files::newInputStream)
    }

    private fun chooseMusicOnEdt(): List<Path> {
        fun choose(): List<Path> {
            val chooser = JFileChooser().apply {
                dialogTitle = "选择音乐、歌单包或文件夹（可多选）"
                fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
                isMultiSelectionEnabled = true
                fileFilter = desktopMusicImportFilter()
            }
            return if (chooser.showOpenDialog(fileDialogOwner) == JFileChooser.APPROVE_OPTION)
                chooser.selectedFiles.ifEmpty { listOfNotNull(chooser.selectedFile).toTypedArray() }.map(File::toPath)
            else emptyList()
        }
        if (SwingUtilities.isEventDispatchThread()) return choose()
        val selection = AtomicReference<List<Path>>(emptyList())
        SwingUtilities.invokeAndWait { selection.set(choose()) }
        return selection.get()
    }

    private fun chooseSyncPackageOnEdt(save: Boolean, suggestedName: String = "整个音乐库.resonance"): Path? {
        val selection = AtomicReference<Path?>()
        fun choose(): Path? {
            val chooser = JFileChooser().apply {
                dialogTitle = if (save) "导出歌单文件" else "导入歌单文件"
                fileSelectionMode = JFileChooser.FILES_ONLY
                isAcceptAllFileFilterUsed = false
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Resonance 歌单文件 (*.resonance)", "resonance")
                currentDirectory = library.managedMusicDirectory.parent?.toFile()
                if (save) {
                    val parent = library.managedMusicDirectory.parent
                    val name = uniqueSyncPackageFileName(suggestedName) { Files.exists(parent.resolve(it)) }
                    selectedFile = parent.resolve(name).toFile()
                }
            }
            val result = if (save) chooser.showSaveDialog(fileDialogOwner) else chooser.showOpenDialog(fileDialogOwner)
            if (result == JFileChooser.APPROVE_OPTION) {
                var chosen = chooser.selectedFile.toPath()
                if (save && !chosen.fileName.toString().endsWith(".resonance", ignoreCase = true)) {
                    chosen = chosen.resolveSibling("${chosen.fileName}.resonance")
                }
                if (save) chosen = chosen.resolveSibling(uniqueSyncPackageFileName(chosen.fileName.toString()) { Files.exists(chosen.resolveSibling(it)) })
                return chosen
            }
            return null
        }
        if (SwingUtilities.isEventDispatchThread()) return choose()
        SwingUtilities.invokeAndWait { selection.set(choose()) }
        return selection.get()
    }

}

internal val DESKTOP_AUDIO_EXTENSIONS = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "ape", "kgma", "kgm", "kgg")

internal fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(StandardCharsets.UTF_8))
    .joinToString("") { "%02x".format(it) }

internal fun sha256File(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).buffered().use { input ->
        val buffer = ByteArray(256 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
