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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
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

class DesktopPlatformServices : PlatformServices {
    override val listenTogether by lazy {
        com.resonance.player.together.JvmListenTogetherService(library.appDirectory.resolve("cache/together-audio"), { track ->
            val source = requireNotNull(track.sourceUri)
            require(!source.contains("://") || source.startsWith("file:")) { "一起听仅共享本机音频" }
            val path = if (source.startsWith("file:")) java.nio.file.Paths.get(java.net.URI(source)) else java.nio.file.Paths.get(source)
            Files.newInputStream(path)
        }, onGuestStopped = { setPlaying(false) })
    }

    override fun playSynchronized(track: Track, progress: Float, playing: Boolean, speed: Float) {
        stopAudioPreview()
        playingMusicId = track.id
        player.setPlaybackSpeed(speed)
        player.play(track, autoPlay = playing, initialProgress = progress.coerceIn(0f, 1f))
        isPlayingEvents.tryEmit(playing)
    }
    override val supportsKgg = true
    private val currentImport = java.util.concurrent.atomic.AtomicReference<com.resonance.player.io.ImportControl?>()
    override val importProgress = MutableStateFlow<ImportProgress?>(null)
    override fun cancelImport() { currentImport.get()?.cancel() }
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
    private val listeningStatsStore by lazy { com.resonance.player.playback.ListeningStatsStore(library.appDirectory) }
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

    override fun recordListeningDuration(trackId: String, deltaSeconds: Int, isCompleted: Boolean) {
        listeningStatsStore.recordPlayback(trackId, deltaSeconds, isCompleted)
    }

    override fun getTrackListeningStats(trackId: String): com.resonance.player.model.TrackListeningStats? {
        return listeningStatsStore.getTrackStats(trackId)
    }

    override fun getAllListeningStats(): com.resonance.player.model.AllListeningStats {
        return listeningStatsStore.getAllStats()
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
            com.resonance.player.features.DesktopCredentialVault::encrypt, com.resonance.player.features.DesktopCredentialVault::decrypt,
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
    private val errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val playbackErrors: Flow<String> = errorEvents
    private val player: DesktopAudioPlayer = DesktopAudioPlayer(
        onEnded = {
            playingMusicId = null
            isPlayingEvents.tryEmit(false)
            if (!sleep.trackEnded()) endedEvents.tryEmit(Unit)
        },
        onProgress = { progressEvents.tryEmit(it) },
        onPlayingChanged = { isPlayingEvents.tryEmit(it) },
        onError = { errorEvents.tryEmit(it) },
    )

    override val playbackEnded: Flow<Unit> = endedEvents
    private val sleep: com.resonance.player.playback.PlaybackSleepTimer = com.resonance.player.playback.PlaybackSleepTimer(assistantScope, onExpire = { player.setPlaying(false); isPlayingEvents.tryEmit(false) })
    override val sleepTimer get() = sleep.state
    override fun setSleepTimer(option: com.resonance.player.model.SleepTimerOption) = sleep.set(option)
    private val preview = com.resonance.player.audio.DesktopPcmPlayer({}, {}, {}, { errorEvents.tryEmit("试听失败：$it") })
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
        val target = chooseSyncPackageOnEdt(save = true) ?: return@withContext SyncReport(false, "已取消导出")
        val allTracks = tracksWithPlaylistArtwork(library.load(), allPlaylists)
        val selectedPlaylists = if (playlistId == null) allPlaylists else allPlaylists.filter { it.id == playlistId }
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
        SyncReport(true, "已导出到 $target", tracks.size, selectedPlaylists.size)
    }

    override suspend fun importSyncPackage(): SyncReport = withContext(Dispatchers.IO) {
        val source = chooseSyncPackageOnEdt(save = false) ?: return@withContext SyncReport(false, "已取消导入")
        val imported = PlainSyncPackage().import(source, library.managedMusicDirectory)
        library.merge(imported.tracks)
        val mergedPlaylists = (library.loadPlaylists() + imported.playlists).associateBy(Playlist::id).values.toList()
        library.savePlaylists(mergedPlaylists)
        SyncReport(true, "歌单文件导入完成", imported.tracks.size, imported.playlists.size)
    }

    override suspend fun importMusic(convertToMp3: Boolean): ImportReport = withContext(Dispatchers.IO) {
        val control = com.resonance.player.io.ImportControl()
        check(currentImport.compareAndSet(null, control)) { "已有导入任务正在运行" }
        try {
            val folder = chooseFolderOnEdt() ?: return@withContext ImportReport(emptyList())
            val report = DesktopMusicScanner(library.artworkDirectory, library.managedMusicDirectory)
                .scan(folder, convertToMp3, control) { importProgress.value = it }
            if (report.tracks.isNotEmpty()) library.merge(report.tracks)
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
        val rawSource = track.sourceUri ?: return@withContext DeleteTrackReport(false, false, "这首歌曲没有本地音频文件")
        val source = Path.of(rawSource).toAbsolutePath().normalize()
        val deleted = runCatching { Files.deleteIfExists(source) }.getOrDefault(false)
        library.remove(track.id)
        track.artworkPath?.let { runCatching { Files.deleteIfExists(Path.of(it)) } }
        DeleteTrackReport(true, deleted, if (deleted) "已删除「${track.title}」的本地音频文件" else "已从音乐库移除「${track.title}」")
    }

    override suspend fun batchDeleteTracks(tracks: List<Track>): DeleteTrackReport = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext DeleteTrackReport(true, false, "未选择任何歌曲")
        var fileDeletedCount = 0
        tracks.forEach { track ->
            val rawSource = track.sourceUri
            if (!rawSource.isNullOrBlank()) {
                val source = Path.of(rawSource).toAbsolutePath().normalize()
                if (runCatching { Files.deleteIfExists(source) }.getOrDefault(false)) {
                    fileDeletedCount++
                }
            }
            track.artworkPath?.let { runCatching { Files.deleteIfExists(Path.of(it)) } }
        }
        library.removeMultiple(tracks.map(Track::id).toSet())
        DeleteTrackReport(
            success = true,
            fileDeleted = fileDeletedCount > 0,
            message = "已批量删除 ${tracks.size} 首歌曲${if (fileDeletedCount > 0) "（含 $fileDeletedCount 首本地文件）" else ""}",
        )
    }

    override fun play(track: Track, queue: List<Track>, shuffle: Boolean, repeatMode: RepeatMode) {
        stopAudioPreview()
        playingMusicId = track.id
        isPlayingEvents.tryEmit(true)
        player.play(track)
    }

    override fun setPlaying(isPlaying: Boolean) {
        if (isPlaying) stopAudioPreview()
        isPlayingEvents.tryEmit(isPlaying)
        player.setPlaying(isPlaying)
    }

    override fun setPlaybackMode(shuffle: Boolean, repeatMode: RepeatMode) {
        library.savePlaybackMode(shuffle, repeatMode)
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
        listenTogether.close()
        sleep.close()
        stopAudioPreview()
        assistantScope.cancel()
        player.close()
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

    private fun chooseFolderOnEdt(): Path? {
        if (SwingUtilities.isEventDispatchThread()) return chooseFolder()
        val selection = AtomicReference<Path?>()
        SwingUtilities.invokeAndWait { selection.set(chooseFolder()) }
        return selection.get()
    }

    private fun chooseSyncPackageOnEdt(save: Boolean): Path? {
        val selection = AtomicReference<Path?>()
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                dialogTitle = if (save) "导出歌单文件" else "导入歌单文件"
                fileSelectionMode = JFileChooser.FILES_ONLY
                isAcceptAllFileFilterUsed = false
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Resonance 歌单文件 (*.resonance)", "resonance")
                currentDirectory = library.managedMusicDirectory.parent?.toFile()
                if (save) selectedFile = library.managedMusicDirectory.parent.resolve("Resonance-Sync.resonance").toFile()
            }
            val result = if (save) chooser.showSaveDialog(null) else chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                var chosen = chooser.selectedFile.toPath()
                if (save && !chosen.fileName.toString().endsWith(".resonance", ignoreCase = true)) {
                    chosen = chosen.resolveSibling("${chosen.fileName}.resonance")
                }
                selection.set(chosen)
            }
        }
        return selection.get()
    }

    private fun chooseFolder(): Path? {
        val chooser = JFileChooser().apply {
            dialogTitle = "选择要扫描的音乐文件夹"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.toPath() else null
    }
}

internal class DesktopMusicScanner(
    private val artworkDirectory: Path,
    managedMusicDirectory: Path,
) {
    private val audioExtensions = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "ape", "kgma", "kgm", "kgg")
    private val converter = DesktopManagedMp3Converter(managedMusicDirectory)

    fun scan(root: Path, convertToMp3: Boolean,
        control: com.resonance.player.io.ImportControl = com.resonance.player.io.ImportControl(),
        progress: (ImportProgress) -> Unit = {},
    ): ImportReport {
        val tracks = mutableListOf<Track>()
        val warnings = mutableListOf<String>()
        var skipped = 0
        try {
            val candidates = mutableListOf<Path>()
            Files.walk(root).use { paths ->
                val iterator = paths.iterator()
                while (iterator.hasNext()) {
                    control.check()
                    val path = iterator.next()
                    if (Files.isRegularFile(path) && path.fileName.toString().substringAfterLast('.', "").lowercase() in audioExtensions) candidates += listOf(path)
                    if (candidates.size % 100 == 0) progress(ImportProgress("扫描目录", completed = candidates.size, detail = "已发现 ${candidates.size} 个音频文件"))
                }
            }
            candidates.forEachIndexed { index, path ->
                control.check()
                val name = path.fileName.toString()
                progress(ImportProgress("处理音频", name, index, candidates.size, index.toFloat() / candidates.size.coerceAtLeast(1)))
                try {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val source = if (convertToMp3 || ext in listOf("kgma", "kgm", "kgg")) converter.importAsMp3(path, control) { done, total ->
                        progress(ImportProgress("解密音频", name, index, candidates.size,
                            (index + (if (total > 0) done.toFloat() / total * 0.7f else 0f)) / candidates.size.coerceAtLeast(1), "${done / 1024} / ${total / 1024} KB"))
                    } else path
                    control.check()
                    tracks += readAudioTrack(source)
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { skipped++; if (warnings.size < 10) warnings += "$name：${error.message.orEmpty()}" }
                progress(ImportProgress("处理音频", name, index + 1, candidates.size, (index + 1f) / candidates.size.coerceAtLeast(1)))
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { warnings += "已取消；已完成 ${tracks.size} 首，源文件保留" }
        return ImportReport(tracks.sortedWith(compareBy(Track::artist, Track::title)), skipped, warnings, control.isCancelled)
    }

    fun readAudioTrack(path: Path): Track {
        val file = AudioFileIO.read(path.toFile())
        val tag = file.tag
        val rawTitle = tag?.getFirst(FieldKey.TITLE)?.takeIf(String::isNotBlank)
        val rawArtist = tag?.getFirst(FieldKey.ARTIST)?.takeIf(String::isNotBlank)
        val rawAlbum = tag?.getFirst(FieldKey.ALBUM)?.takeIf(String::isNotBlank)

        var title = rawTitle
        var artist = rawArtist
        val fileName = path.fileName.toString()
        val cleanBaseName = fileName.substringBeforeLast('.').replace(Regex("""\s*\[[0-9a-fA-F]{10}\]$"""), "")
        if (title == null || artist == null) {
            if (cleanBaseName.contains(" - ")) {
                val parsedArtist = cleanBaseName.substringBefore(" - ").trim()
                val parsedTitle = cleanBaseName.substringAfter(" - ").trim()
                if (artist == null && parsedArtist.isNotBlank()) artist = parsedArtist
                if (title == null && parsedTitle.isNotBlank()) title = parsedTitle
            } else if (title == null && cleanBaseName.isNotBlank()) {
                title = cleanBaseName
            }
        }
        val finalTitle = title ?: "未知歌曲"
        val finalArtist = artist ?: "未知歌手"
        val finalAlbum = rawAlbum ?: "本地音乐"

        val durationSeconds = file.audioHeader.trackLength.coerceAtLeast(0)
        val id = sha256File(path)
        val artworkPath = tag?.firstArtwork?.binaryData?.takeIf { it.isNotEmpty() }?.let { bytes ->
            Files.createDirectories(artworkDirectory)
            val target = artworkDirectory.resolve("$id.cover")
            Files.write(target, bytes)
            target.toString()
        }
        val extension = path.fileName.toString().substringAfterLast('.', "").lowercase()
        val mimeType = when (extension) {
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "m4a", "aac" -> "audio/mp4"
            "ogg", "opus" -> "audio/ogg"
            else -> "audio/mpeg"
        }
        return Track(
            id = id,
            title = finalTitle,
            artist = finalArtist,
            album = finalAlbum,
            durationText = "%d:%02d".format(durationSeconds / 60, durationSeconds % 60),
            artworkSeed = (id.take(8).toLongOrNull(16)?.toInt() ?: id.hashCode()),
            sourceUri = path.toAbsolutePath().normalize().toString(),
            artworkPath = artworkPath,
            mimeType = mimeType,
        )
    }
}

internal class DesktopLibraryStore(
    val appDirectory: Path = (System.getProperty("RESONANCE_DATA") ?: System.getenv("RESONANCE_DATA"))?.takeIf(String::isNotBlank)?.let(Path::of)
        ?: Path.of(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "Resonance"),
) {
    private val libraryFile = appDirectory.resolve("library.properties")
    private val playlistFile = appDirectory.resolve("playlists.properties")
    private val uiStateFile = appDirectory.resolve("ui-state.properties")
    @Synchronized
    fun readUiValue(key: String): String? {
        return com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile).getProperty(key)
    }
    @Synchronized
    fun writeUiValue(key: String, value: String) {
        Files.createDirectories(appDirectory)
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
        properties.setProperty(key, value)
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }
    @Synchronized
    fun removeUiValue(key: String) {
        Files.createDirectories(appDirectory)
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
        properties.remove(key)
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }
    val artworkDirectory: Path = appDirectory.resolve("artwork")
    val lyricsCacheDirectory: Path = appDirectory.resolve("lyrics-cache")
    val managedMusicDirectory: Path = preferredManagedMusicDirectory()

    @Synchronized
    fun installationId(): String {
        Files.createDirectories(appDirectory)
        val target = appDirectory.resolve("installation.id")
        if (Files.isRegularFile(target)) {
            Files.readString(target, StandardCharsets.UTF_8).trim().takeIf(String::isNotBlank)?.let { return it }
        }
        val created = java.util.UUID.randomUUID().toString()
        Files.writeString(target, created, StandardCharsets.UTF_8)
        return created
    }

    @Synchronized
    fun load(): List<Track> {
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(libraryFile) {
            com.resonance.player.io.AtomicPropertiesFile.validIndexed(it, "track", "source")
        }
        val count = properties.getProperty("count")?.toIntOrNull() ?: return emptyList()
        val usesContentIds = properties.getProperty("id.format") == "sha256-audio-v1"
        return (0 until count).mapNotNull { index ->
            val prefix = "track.$index."
            val source = properties.getProperty(prefix + "source") ?: return@mapNotNull null
            if (source.isBlank() && properties.getProperty(prefix + "id").isNullOrBlank()) return@mapNotNull null
            if (source.isNotBlank() && !Files.isRegularFile(Path.of(source))) return@mapNotNull null
            Track(
                id = if (usesContentIds || source.isBlank()) {
                    properties.getProperty(prefix + "id") ?: sha256File(Path.of(source))
                } else {
                    sha256File(Path.of(source))
                },
                title = properties.getProperty(prefix + "title") ?: File(source).nameWithoutExtension,
                artist = properties.getProperty(prefix + "artist") ?: "未知歌手",
                album = properties.getProperty(prefix + "album") ?: "未知专辑",
                durationText = properties.getProperty(prefix + "duration") ?: "0:00",
                artworkSeed = properties.getProperty(prefix + "seed")?.toIntOrNull() ?: source.hashCode(),
                isFavorite = properties.getProperty(prefix + "favorite").toBoolean(),
                sourceUri = source.takeIf(String::isNotBlank),
                artworkPath = properties.getProperty(prefix + "artwork")?.takeIf(String::isNotBlank),
                mimeType = properties.getProperty(prefix + "mime") ?: "audio/mpeg",
            )
        }.associateBy(Track::id).values.toList()
    }

    @Synchronized
    fun save(tracks: List<Track>) {
        Files.createDirectories(appDirectory)
        val properties = Properties().apply {
            setProperty("count", tracks.size.toString())
            setProperty("id.format", "sha256-audio-v1")
        }
        tracks.forEachIndexed { index, track ->
            val prefix = "track.$index."
            properties.setProperty(prefix + "id", track.id)
            properties.setProperty(prefix + "title", track.title)
            properties.setProperty(prefix + "artist", track.artist)
            properties.setProperty(prefix + "album", track.album)
            properties.setProperty(prefix + "duration", track.durationText)
            properties.setProperty(prefix + "seed", track.artworkSeed.toString())
            properties.setProperty(prefix + "favorite", track.isFavorite.toString())
            properties.setProperty(prefix + "source", track.sourceUri.orEmpty())
            properties.setProperty(prefix + "artwork", track.artworkPath.orEmpty())
            properties.setProperty(prefix + "mime", track.mimeType)
        }
        writePropertiesAtomically(libraryFile, properties, "Resonance music library")
    }

    @Synchronized
    fun merge(imported: List<Track>) {
        val merged = (load() + imported).associateBy(Track::id).values.toList()
        save(merged)
    }

    /** Relinking must reinsert the old identity even when load() excludes its missing file. */
    @Synchronized
    fun replaceAudio(track: Track) {
        save(load().filterNot { it.id == track.id } + track)
        savePlaylists(loadPlaylists().map { playlist -> playlist.copy(tracks = playlist.tracks.map { if (it.id == track.id) track else it }) })
    }

    @Synchronized
    fun updateTracks(updated: List<Track>) {
        val updatedById = updated.associateBy(Track::id)
        val currentLibrary = load().map { current -> updatedById[current.id] ?: current }
        save(currentLibrary)
        val currentPlaylists = loadPlaylists().map { playlist ->
            playlist.copy(
                tracks = playlist.tracks.map { track ->
                    updatedById[track.id]?.let { u ->
                        track.copy(
                            title = u.title,
                            artist = u.artist,
                            album = u.album,
                            sourceUri = u.sourceUri,
                            artworkPath = u.artworkPath ?: track.artworkPath,
                            artworkSeed = if (u.artworkPath != null) u.artworkSeed else track.artworkSeed,
                        )
                    } ?: track
                }
            )
        }
        savePlaylists(currentPlaylists)
    }

    @Synchronized
    fun removeMultiple(trackIds: Set<String>) {
        save(load().filterNot { it.id in trackIds })
        savePlaylists(loadPlaylists().map { playlist -> playlist.copy(tracks = playlist.tracks.filterNot { it.id in trackIds }) })
    }

    @Synchronized
    fun remove(trackId: String) {
        removeMultiple(setOf(trackId))
    }

    @Synchronized
    fun setFavorite(trackId: String, favorite: Boolean) {
        save(load().map { track -> if (track.id == trackId) track.copy(isFavorite = favorite) else track })
        savePlaylists(loadPlaylists().map { playlist ->
            playlist.copy(tracks = playlist.tracks.map { track ->
                if (track.id == trackId) track.copy(isFavorite = favorite) else track
            })
        })
    }

    @Synchronized
    fun loadPlaylists(): List<Playlist> {
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(playlistFile) {
            com.resonance.player.io.AtomicPropertiesFile.validIndexed(it, "playlist", "id")
        }
        val tracksById = load().associateBy(Track::id)
        val count = properties.getProperty("count")?.toIntOrNull() ?: return emptyList()
        return (0 until count).mapNotNull { index ->
            val prefix = "playlist.$index."
            val id = properties.getProperty(prefix + "id") ?: return@mapNotNull null
            val trackIds = properties.getProperty(prefix + "tracks").orEmpty().split(',').filter(String::isNotBlank)
            Playlist(
                id = id,
                name = properties.getProperty(prefix + "name") ?: "未命名歌单",
                subtitle = properties.getProperty(prefix + "subtitle") ?: "${trackIds.size} 首",
                tracks = trackIds.mapIndexedNotNull { trackIndex, trackId ->
                    val snapshot = readCatalogTrack(properties, prefix, trackIndex, trackId)
                    tracksById[trackId]?.let { local ->
                        local.copy(
                            artworkPath = local.artworkPath ?: snapshot?.artworkPath,
                            artworkSeed = if (local.artworkPath == null && snapshot != null) snapshot.artworkSeed else local.artworkSeed,
                        )
                    } ?: snapshot
                },
                artworkSeed = properties.getProperty(prefix + "seed")?.toIntOrNull() ?: id.hashCode(),
            )
        }
    }

    @Synchronized
    fun savePlaylists(playlists: List<Playlist>) {
        Files.createDirectories(appDirectory)
        val properties = Properties().apply { setProperty("count", playlists.size.toString()) }
        playlists.forEachIndexed { index, playlist ->
            val prefix = "playlist.$index."
            properties.setProperty(prefix + "id", playlist.id)
            properties.setProperty(prefix + "name", playlist.name)
            properties.setProperty(prefix + "subtitle", playlist.subtitle)
            properties.setProperty(prefix + "seed", playlist.artworkSeed.toString())
            properties.setProperty(prefix + "tracks", playlist.tracks.joinToString(",", transform = Track::id))
            playlist.tracks.forEachIndexed { trackIndex, track -> writeCatalogTrack(properties, prefix, trackIndex, track) }
        }
        writePropertiesAtomically(playlistFile, properties, "Resonance playlists")
    }

    @Synchronized
    fun loadLastSelectedPlaylistId(): String? {
        if (!Files.isRegularFile(uiStateFile)) return null
        val properties = Properties()
        Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(properties::load)
        return properties.getProperty("library.selectedPlaylistId")?.takeIf(String::isNotBlank)
    }

    @Synchronized
    fun saveLastSelectedPlaylistId(playlistId: String?) {
        Files.createDirectories(appDirectory)
        val properties = if (Files.isRegularFile(uiStateFile)) {
            Properties().apply { Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(::load) }
        } else {
            Properties()
        }
        if (playlistId.isNullOrBlank()) {
            properties.remove("library.selectedPlaylistId")
        } else {
            properties.setProperty("library.selectedPlaylistId", playlistId)
        }
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }

    @Synchronized
    fun loadThemeMode(): com.resonance.player.model.ThemeMode {
        if (!Files.isRegularFile(uiStateFile)) return com.resonance.player.model.ThemeMode.Light
        val properties = Properties()
        Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(properties::load)
        return when (properties.getProperty("app.themeMode")) {
            "Dark" -> com.resonance.player.model.ThemeMode.Dark
            "Light" -> com.resonance.player.model.ThemeMode.Light
            "System" -> com.resonance.player.model.ThemeMode.System
            else -> com.resonance.player.model.ThemeMode.Light
        }
    }

    @Synchronized
    fun saveThemeMode(mode: com.resonance.player.model.ThemeMode) {
        Files.createDirectories(appDirectory)
        val properties = if (Files.isRegularFile(uiStateFile)) {
            Properties().apply { Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(::load) }
        } else {
            Properties()
        }
        properties.setProperty("app.themeMode", mode.name)
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }

    @Synchronized
    fun loadPlaybackMode(): PlaybackModePreference {
        if (!Files.isRegularFile(uiStateFile)) return PlaybackModePreference()
        val properties = Properties()
        Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(properties::load)
        val shuffle = properties.getProperty("playback.shuffleEnabled")?.toBooleanStrictOrNull() ?: false
        val repeatMode = when (properties.getProperty("playback.repeatMode")) {
            "All" -> RepeatMode.All
            "One" -> RepeatMode.One
            else -> RepeatMode.Off
        }
        return PlaybackModePreference(shuffleEnabled = shuffle, repeatMode = repeatMode)
    }

    @Synchronized
    fun savePlaybackMode(shuffle: Boolean, repeatMode: RepeatMode) {
        Files.createDirectories(appDirectory)
        val properties = if (Files.isRegularFile(uiStateFile)) {
            Properties().apply { Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(::load) }
        } else {
            Properties()
        }
        properties.setProperty("playback.shuffleEnabled", shuffle.toString())
        properties.setProperty("playback.repeatMode", repeatMode.name)
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }

    @Synchronized
    fun loadDeepSeekConfig(): DeepSeekConfig {
        if (!Files.isRegularFile(uiStateFile)) return DeepSeekConfig()
        val properties = Properties()
        Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(properties::load)
        return DeepSeekConfig(
            apiKey = properties.getProperty("deepseek.apiKey", ""),
            baseUrl = properties.getProperty("deepseek.baseUrl", "https://api.deepseek.com/v1"),
            model = properties.getProperty("deepseek.model", "deepseek-chat"),
            enabled = properties.getProperty("deepseek.enabled", "true").toBoolean(),
        )
    }

    @Synchronized
    fun saveDeepSeekConfig(config: DeepSeekConfig) {
        Files.createDirectories(appDirectory)
        val properties = if (Files.isRegularFile(uiStateFile)) {
            Properties().apply { Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(::load) }
        } else {
            Properties()
        }
        properties.setProperty("deepseek.apiKey", config.apiKey)
        properties.setProperty("deepseek.baseUrl", config.baseUrl)
        properties.setProperty("deepseek.model", config.model)
        properties.setProperty("deepseek.enabled", config.enabled.toString())
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }

    @Synchronized
    fun loadCustomLyricsFolder(): String? {
        if (!Files.isRegularFile(uiStateFile)) return null
        val properties = Properties()
        Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(properties::load)
        return properties.getProperty("lyrics.customFolder")?.takeIf(String::isNotBlank)
    }

    @Synchronized
    fun saveCustomLyricsFolder(folder: String?) {
        Files.createDirectories(appDirectory)
        val properties = if (Files.isRegularFile(uiStateFile)) {
            Properties().apply { Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(::load) }
        } else {
            Properties()
        }
        if (folder.isNullOrBlank()) {
            properties.remove("lyrics.customFolder")
        } else {
            properties.setProperty("lyrics.customFolder", folder)
        }
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }

    private fun readCatalogTrack(properties: Properties, playlistPrefix: String, trackIndex: Int, id: String): Track? {
        val prefix = "${playlistPrefix}catalog.$trackIndex."
        val title = properties.getProperty(prefix + "title") ?: return null
        return Track(
            id = id,
            title = title,
            artist = properties.getProperty(prefix + "artist") ?: "未知歌手",
            album = properties.getProperty(prefix + "album") ?: "未知专辑",
            durationText = properties.getProperty(prefix + "duration") ?: "0:00",
            artworkSeed = properties.getProperty(prefix + "seed")?.toIntOrNull() ?: id.hashCode(),
            isFavorite = properties.getProperty(prefix + "favorite").toBoolean(),
            artworkPath = properties.getProperty(prefix + "artwork")?.takeIf(String::isNotBlank),
            mimeType = properties.getProperty(prefix + "mime") ?: "application/vnd.resonance.catalog+audio",
        )
    }

    private fun writeCatalogTrack(properties: Properties, playlistPrefix: String, trackIndex: Int, track: Track) {
        val prefix = "${playlistPrefix}catalog.$trackIndex."
        properties.setProperty(prefix + "title", track.title)
        properties.setProperty(prefix + "artist", track.artist)
        properties.setProperty(prefix + "album", track.album)
        properties.setProperty(prefix + "duration", track.durationText)
        properties.setProperty(prefix + "seed", track.artworkSeed.toString())
        properties.setProperty(prefix + "favorite", track.isFavorite.toString())
        properties.setProperty(prefix + "artwork", track.artworkPath.orEmpty())
        properties.setProperty(prefix + "mime", track.mimeType)
    }

    private fun writePropertiesAtomically(target: Path, properties: Properties, comment: String) {
        com.resonance.player.io.AtomicPropertiesFile.write(target, properties, comment)
    }

    private fun preferredManagedMusicDirectory(): Path {
        System.getenv("RESONANCE_LIBRARY")?.takeIf(String::isNotBlank)?.let { return Path.of(it) }
        val dDrive = Path.of("D:\\")
        return if (Files.isDirectory(dDrive)) dDrive.resolve("Music").resolve("Resonance")
        else Path.of(System.getProperty("user.home"), "Music", "Resonance")
    }
}

private class DesktopAudioPlayer(
    onEnded: () -> Unit,
    onProgress: (Float) -> Unit,
    onPlayingChanged: (Boolean) -> Unit,
    onError: (String) -> Unit,
) {
    private val backend = com.resonance.player.audio.DesktopPcmPlayer(onEnded, onProgress, onPlayingChanged, onError)
    val positionMs: Long? get() = backend.positionMs
    fun play(track: Track, autoPlay: Boolean = true, initialProgress: Float = 0f) = backend.play(track, autoPlay, initialProgress)
    fun setPlaying(isPlaying: Boolean) = backend.setPlaying(isPlaying)
    fun setVolume(volume: Float) = backend.setVolume(volume)
    fun setTrackGain(gainDb: Float) = backend.setTrackGain(gainDb)
    fun setPlaybackSpeed(speed: Float) = backend.setPlaybackSpeed(speed)
    fun setVocalSuppression(enabled: Boolean) = backend.setVocalSuppression(enabled)
    fun setPreset(preset: com.resonance.player.model.AudioEffectPreset) = backend.setPreset(preset)
    fun seekTo(fraction: Float) = backend.seekTo(fraction)
    fun close() = backend.close()
}

private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(StandardCharsets.UTF_8))
    .joinToString("") { "%02x".format(it) }

private fun sha256File(path: Path): String {
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
