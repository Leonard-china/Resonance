package com.resonance.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import com.resonance.player.application.ApplicationEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import com.resonance.player.model.SleepTimerOption
import com.resonance.player.data.PreviewLibrary
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.design.WeatherAppearanceTheme
import com.resonance.player.model.LibraryDestination
import com.resonance.player.model.ImportReport
import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.LyricsUiState
import com.resonance.player.model.PlayerState
import com.resonance.player.model.Playlist
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.ThemeMode
import com.resonance.player.model.Track
import com.resonance.player.platform.PlatformServices
import com.resonance.player.platform.tracksWithPlaylistArtwork
import com.resonance.player.ui.LibraryShell
import com.resonance.player.ui.MusicFeatureController
import com.resonance.player.ui.LocalMusicFeatures
import com.resonance.player.ui.MusicFeatureDialogs
import com.resonance.player.ui.LocalTutorial
import com.resonance.player.ui.TutorialController
import com.resonance.player.ui.TutorialDialogs
import com.resonance.player.tutorial.TutorialCompletion
import com.resonance.player.platform.FloatingLyricsHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import com.resonance.player.playback.PlaybackStateStore
import com.resonance.player.platform.isTogetherTrack
import com.resonance.player.playback.PlaybackSnapshot
import com.resonance.player.data.PersistenceQueue
import kotlin.random.Random

private enum class SyncAction { Export, Import }

/** Application state outlives individual desktop windows; Android uses the inline host. */
@Composable
fun App(
    services: PlatformServices,
    mainWindow: @Composable (@Composable () -> Unit) -> Unit = { it() },
    lifetime: com.resonance.player.application.ApplicationLifetime? = null,
) {
    var themeMode by remember { mutableStateOf(ThemeMode.Light) }
    val compositionScope = rememberCoroutineScope()
    val tasks = remember(services) { com.resonance.player.application.ApplicationTasks(compositionScope.coroutineContext) }
    val scope = tasks.scope
    val applicationLifetime = remember(services, lifetime) { lifetime ?: com.resonance.player.application.ApplicationLifetime(services::close) }

    ApplicationEffect(scope, services) {
        try {
            themeMode = services.loadThemeMode()
        } catch (_: Throwable) {}
    }

    val stopping by applicationLifetime.stopping.collectAsState()
    WeatherAppearanceTheme(services, themeMode, applicationLifetime, scope) {
        CompositionLocalProvider(com.resonance.player.design.LocalGlassState provides dev.chrisbanes.haze.rememberHazeState()) {
        var destination by rememberSaveable { mutableStateOf(LibraryDestination.Library) }
        var playlists by remember { mutableStateOf(emptyList<Playlist>()) }
        var importedTracks by remember { mutableStateOf(emptyList<com.resonance.player.model.Track>()) }
        var showCreateDialog by remember { mutableStateOf(false) }
        var trackPendingDeletion by remember { mutableStateOf<Track?>(null) }
        var trackPendingRestore by remember { mutableStateOf<PendingTrackRestore?>(null) }
        var showSleepTimer by remember { mutableStateOf(false) }
        var operationInProgress by remember { mutableStateOf(false) }
        var initialLoadInProgress by remember { mutableStateOf(true) }
        var previewMode by remember { mutableStateOf(false) }
        val selectedPlaylistIdState = remember { mutableStateOf<String?>(null) }
        var selectedPlaylistId by selectedPlaylistIdState
        var importMessage by remember { mutableStateOf<String?>(null) }
        ApplicationEffect(scope, tasks) {
            tasks.failures.collect { failure ->
                if (failure != null) importMessage = "后台任务未完成：${failure.message ?: failure::class.simpleName.orEmpty()}"
            }
        }
        var playbackIssue by remember { mutableStateOf<com.resonance.player.platform.PlaybackIssue?>(null) }
        val playback = remember(services) { PlaybackStateStore(services, scope) {
            importMessage = "音频设置未能完成：${it.message.orEmpty()}"
        } }
        var player by playback.state
        val sleepTimer by services.sleepTimer.collectAsState()
        ApplicationEffect(scope, sleepTimer) { player = player.copy(sleepTimerOption = sleepTimer.option, sleepTimerRemainingSeconds = sleepTimer.remainingSeconds) }
        val customQueueState = remember { mutableStateOf<List<Track>?>(null) }
        var customQueue by customQueueState
        val activePlaylistIdState = remember { mutableStateOf<String?>(null) }
        var activePlaylistId by activePlaylistIdState
        val together = services.listenTogether
        val togetherRoom = together?.state?.collectAsState()?.value
        val sharedLyrics = together?.remoteLyrics?.collectAsState()?.value
        var wasHostingTogether by remember(services) { mutableStateOf(false) }
        ApplicationEffect(scope, togetherRoom?.role) {
            if (togetherRoom?.role == com.resonance.player.platform.TogetherRole.Host) {
                wasHostingTogether = true
            } else if (wasHostingTogether) {
                wasHostingTogether = false
                customQueue = customQueue?.filterNot { it.isTogetherTrack() }?.takeIf { it.isNotEmpty() }
                if (player.currentTrack?.isTogetherTrack() == true) {
                    services.setPlaying(false)
                    player = player.copy(currentTrack = null, isPlaying = false, progress = 0f, positionMs = null)
                }
            }
        }
        ApplicationEffect(scope, togetherRoom?.isGuest) {
            if (togetherRoom?.isGuest == true && services.togetherLocalSnapshot == null && player.currentTrack?.isTogetherTrack() != true) {
                services.togetherLocalSnapshot = TogetherLocalSnapshot(player, customQueue, activePlaylistId)
            } else if (togetherRoom?.isGuest != true) {
                val saved = services.togetherLocalSnapshot as? TogetherLocalSnapshot
                services.togetherLocalSnapshot = null
                if (saved != null) {
                    customQueue = saved.queue
                    activePlaylistId = saved.playlistId
                    val track = saved.player.currentTrack
                    if (track != null) {
                        playback.activate(track, saved.playlistId, expectNativeTrack = true,
                            prepare = { check(services.audioFileAvailable(track)) { "原来的歌曲暂时不可用" } }) {
                            services.restorePausedPlayback(track, saved.queue ?: listOf(track), saved.player.progress,
                                saved.player.shuffleEnabled, saved.player.repeatMode)
                            player = saved.player.copy(isPlaying = false)
                        }
                    }
                }
            }
        }
        ApplicationEffect(scope, together) {
            together?.hostQueue?.collect { queue ->
                if (queue != null && together.state.value.role == com.resonance.player.platform.TogetherRole.Host &&
                    together.state.value.roomSessionId == queue.roomSessionId) customQueue = queue.tracks
            }
        }
        com.resonance.player.ui.TogetherPlaybackBinding(services, playback, player) { track ->
            customQueue = track?.let(::listOf)
            activePlaylistId = null
        }
        val persistence = remember(services) { PersistenceQueue(scope) {
            importMessage = "保存失败：${it.message.orEmpty()}"
            playbackIssue = com.resonance.player.platform.PlaybackIssue(importMessage.orEmpty(),
                com.resonance.player.platform.PlaybackIssue.Source.Preferences)
        } }
        val preferences = remember(persistence, services) {
            com.resonance.player.application.ApplicationPreferences(persistence, services, services)
        }
        val musicImport = remember(persistence, services) {
            com.resonance.player.application.LibraryImportUseCase(services, persistence)
        }
        val playbackModeWriter = remember(persistence, services) {
            com.resonance.player.data.PlaybackModeWriter(persistence) { value ->
                services.savePlaybackMode(value.shuffleEnabled, value.repeatMode)
            }
        }
        val tutorial = remember(services) { TutorialController { value ->
            persistence.submitLatest("tutorial.progress") { services.saveTutorialProgress(value) }
        } }
        tutorial.onNavigate = { destination = it }
        tutorial.currentDestination = destination
        tutorial.hasTrack = player.currentTrack != null
        ApplicationEffect(scope, services) {
            try { tutorial.restore(services.loadTutorialProgress()) }
            catch (error: Exception) { importMessage = "教程进度读取失败：${error.message.orEmpty()}" }
        }
        var syncAction by remember { mutableStateOf<SyncAction?>(null) }
        var syncInProgress by remember { mutableStateOf<SyncAction?>(null) }
        var showSyncProgress by remember { mutableStateOf(false) }
        var syncResult by remember { mutableStateOf<com.resonance.player.ui.SyncPackageOutcome?>(null) }
        var lyricsState by remember { mutableStateOf<LyricsUiState>(LyricsUiState.Idle) }
        val lyricSession = remember(services) { com.resonance.player.application.LyricsSession(services) { lyricsState = it } }
        var deepSeekConfig by remember { mutableStateOf(com.resonance.player.model.DeepSeekConfig()) }
        var customLyricsFolder by remember { mutableStateOf<String?>(null) }
        var batchEnrichCandidateTracks by remember { mutableStateOf<List<Track>?>(null) }
        var batchEnrichRunning by remember { mutableStateOf(false) }
        var batchEnrichProgress by remember { mutableStateOf<com.resonance.player.model.BatchEnrichProgress?>(null) }
        var batchEnrichReport by remember { mutableStateOf<com.resonance.player.model.BatchEnrichReport?>(null) }
        var batchEnrichJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

        val features = remember(services) { MusicFeatureController(services, scope) }
        com.resonance.player.ui.TogetherInvitationPrompt(services) { destination = LibraryDestination.Together }
        ApplicationEffect(scope, tutorial.activeChapterId, tutorial.step?.id, importedTracks, playlists, player.currentTrack, features.settings) {
            val completion = tutorial.step?.completion
            val satisfied = when (completion) {
                TutorialCompletion.MusicAvailable -> importedTracks.any { it.sourceUri != null }
                TutorialCompletion.TrackSelected -> player.currentTrack != null
                TutorialCompletion.TrackFavorite -> player.currentTrack?.isFavorite == true
                TutorialCompletion.PlaylistCreated -> playlists.any { it.id != com.resonance.player.model.DAILY_PLAYLIST_ID }
                TutorialCompletion.TrackAdded -> player.currentTrack?.id?.let { id -> playlists.any { it.id != com.resonance.player.model.DAILY_PLAYLIST_ID && it.tracks.any { track -> track.id == id } } } == true
                TutorialCompletion.CookieAvailable -> features.settings.bilibiliCookie.isNotBlank()
                TutorialCompletion.AiConfigured -> features.settings.connections.any { it.provider == features.settings.assistantProvider && it.configured }
                TutorialCompletion.RecommendationsEnabled -> features.settings.recommendationsEnabled
                TutorialCompletion.FloatingEnabled -> features.settings.floatingLyrics
                else -> false
            }
            if (satisfied && completion != null) tutorial.event(completion)
        }
        features.library = importedTracks
        features.playlists = playlists
        features.player = player
        features.onNavigate = { destination = it }
        features.beforeTask = { persistence.run {}; services.musicAssistant?.updateUiContext(player, destination) }
        features.onToggleBalance = {
            val enabled = !player.autoLoudnessEnabled
            player = player.copy(autoLoudnessEnabled = enabled)
            persistence.submit { services.setAutoLoudnessEnabled(enabled); playback.refreshAudio(activePlaylistId) }
        }
        features.onReload = {
            val loaded = services.loadLibrary()
            val lists = services.loadPlaylists()
            importedTracks = tracksWithPlaylistArtwork(loaded, lists)
            playlists = lists
            customQueue = customQueue?.mapNotNull { queued ->
                if (queued.isTogetherTrack() && together?.state?.value?.role == com.resonance.player.platform.TogetherRole.Host) queued
                else loaded.firstOrNull { it.id == queued.id }
            }
            player.currentTrack?.takeUnless { it.isTogetherTrack() }?.let { current ->
                val updated = loaded.firstOrNull { it.id == current.id }
                player = if (updated != null) player.copy(currentTrack = updated) else { services.setPlaying(false); PlayerState(autoLoudnessEnabled = player.autoLoudnessEnabled, volume = player.volume) }
            }
            services.musicAssistant?.updateLibrarySnapshot(importedTracks, playlists)
        }
        ApplicationEffect(scope, services) {
            try {
                features.load()
                // Migrate the user's existing DeepSeek connection without changing its credentials.
                val legacy = services.loadDeepSeekConfig()
                if (legacy.isConfigured && features.settings.connections.none { it.configured }) {
                    val connection = com.resonance.player.model.AiConnection(com.resonance.player.model.AiProvider.DeepSeek, legacy.baseUrl, legacy.model, legacy.apiKey, false)
                    val next = features.settings.copy(connections = features.settings.connections.filterNot { it.provider == connection.provider } + connection, assistantProvider = connection.provider)
                    features.settings = next; services.musicAssistant?.saveSettings(next)
                }
            } catch (error: Exception) { features.message = error.message.orEmpty() }
        }
        ApplicationEffect(scope, importedTracks, playlists) { if (!initialLoadInProgress) services.musicAssistant?.updateLibrarySnapshot(importedTracks, playlists) }
        val foreground by services.foreground.collectAsState()
        ApplicationEffect(scope, foreground, features.loaded, features.settings.recommendationsEnabled, features.settings.intervalHours, features.settings.recommendationsUnmeteredOnly, initialLoadInProgress) {
            if (foreground && features.loaded && !initialLoadInProgress && features.settings.recommendationsEnabled) {
                while (true) {
                    try { if (services.automaticRecommendationAllowed(features.settings.recommendationsUnmeteredOnly)) { persistence.run {}; services.musicAssistant?.recommend() } }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (error: Exception) { features.message = error.message.orEmpty() }
                    delay(60_000)
                }
            }
        }
        val featureTaskState = services.musicAssistant?.state?.collectAsState()?.value
        ApplicationEffect(scope, featureTaskState?.task?.running) {
            if (featureTaskState?.task?.running == false && !initialLoadInProgress) {
                try { features.onReload() }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { features.message = "曲库刷新失败：${error.message}" }
            }
        }
        val originalLyrics = features.generatedLyrics?.takeIf { it.trackId == player.currentTrack?.id }?.let { LyricsUiState.Ready(it) } ?: lyricsState
        ApplicationEffect(scope, player.currentTrack?.id) {
            features.cancelTextLyrics()
            features.generatedLyrics = null
            features.reloadGeneratedLyrics()
        }
        features.lyrics = originalLyrics
        val translatedLyrics = (originalLyrics as? LyricsUiState.Ready)?.lyrics?.let(features::translatedLyrics)
        val displayLyrics = translatedLyrics?.let { LyricsUiState.Ready(it) } ?: originalLyrics
        SideEffect {
            if (togetherRoom?.role == com.resonance.player.platform.TogetherRole.Host) {
                together?.publishLyrics(player.currentTrack?.id.orEmpty(), (displayLyrics as? LyricsUiState.Ready)?.lyrics)
            }
        }
        ApplicationEffect(scope, player.currentTrack?.id, (originalLyrics as? LyricsUiState.Ready)?.lyrics?.copy(offsetMs = 0),
            features.settings.translationLanguage) {
            features.reloadTranslation()
        }
        ApplicationEffect(scope, player, displayLyrics, features.settings) { services.updateFloatingLyrics(player, displayLyrics, features.settings) }

        fun startBatchEnrich(tracks: List<Track>, options: com.resonance.player.model.BatchEnrichOptions) {
            batchEnrichRunning = true
            batchEnrichProgress = null
            batchEnrichReport = null
            batchEnrichJob = scope.launch {
                try {
                    val report = services.batchEnrichTracks(options, tracks) { p ->
                        batchEnrichProgress = p
                    }
                    batchEnrichReport = report
                    com.resonance.player.platform.ArtworkMemoryCache.clear()
                    val loadedTracks = services.loadLibrary()
                    val loadedPlaylists = rematchCatalogTracks(services.loadPlaylists(), loadedTracks)
                    importedTracks = tracksWithPlaylistArtwork(loadedTracks, loadedPlaylists)
                    playlists = loadedPlaylists
                    services.savePlaylists(loadedPlaylists)
                    player.currentTrack?.let { curr ->
                        report.updatedTracks.firstOrNull { it.id == curr.id }?.let { updatedCurr ->
                            player = player.copy(currentTrack = updatedCurr)
                        }
                    }
                } catch (error: Throwable) {
                    importMessage = "智能补全已停止或遇到问题：${error.message ?: error::class.simpleName.orEmpty()}"
                } finally {
                    batchEnrichRunning = false
                }
            }
        }

        fun selectPlaylist(playlistId: String?, persist: Boolean = true) {
            selectedPlaylistId = playlistId
            if (persist && !previewMode) {
                preferences.selectedPlaylist(playlistId)
            }
        }


        ApplicationEffect(scope, services) {
            try {
                val playbackMode = services.loadPlaybackMode()
                val globalVol = services.loadGlobalVolume()
                val autoEnabled = services.isAutoLoudnessEnabled()
                player = player.copy(
                    shuffleEnabled = playbackMode.shuffleEnabled,
                    repeatMode = playbackMode.repeatMode,
                    volume = globalVol,
                    autoLoudnessEnabled = autoEnabled,
                )
                services.setPlaybackMode(playbackMode.shuffleEnabled, playbackMode.repeatMode)
            } catch (_: Throwable) {}
            launch {
                try {
                    services.calibrationStatus.collect { status ->
                        player = player.copy(calibrationStatus = status)
                    }
                } catch (_: Throwable) {}
            }
            try {
                val loadedTracks = services.loadLibrary()
                val loadedPlaylists = rematchCatalogTracks(services.loadPlaylists(), loadedTracks)
                val restoredPlaylistId = resolveSelectedPlaylistId(
                    requestedId = services.loadLastSelectedPlaylistId(),
                    playlists = loadedPlaylists,
                )
                importedTracks = tracksWithPlaylistArtwork(loadedTracks, loadedPlaylists)
                playlists = loadedPlaylists
                selectedPlaylistId = restoredPlaylistId
                deepSeekConfig = services.loadDeepSeekConfig()
                customLyricsFolder = services.loadCustomLyricsFolder()
                val known = (importedTracks + loadedPlaylists.flatMap(Playlist::tracks)).distinctBy(Track::id)
                services.resumePlayback(known)?.let { resume ->
                    val snapshot = resume.snapshot
                    val byId = known.associateBy(Track::id)
                    val queue = snapshot.queueIds.mapNotNull(byId::get).filter { it.sourceUri != null }
                    byId[snapshot.trackId]?.takeIf { it.sourceUri != null }?.let { track ->
                        val restoredQueue = (queue + track).distinctBy(Track::id)
                        customQueue = restoredQueue
                        selectedPlaylistId = resolveSelectedPlaylistId(snapshot.playlistId, loadedPlaylists)
                        activePlaylistId = snapshot.playlistId
                        playback.activate(track, selectedPlaylistId, resetGain = !resume.attached) {
                            if (!resume.attached) services.restorePausedPlayback(track, restoredQueue,
                                snapshot.progress, player.shuffleEnabled, player.repeatMode)
                            player = player.copy(progress = snapshot.progress, isPlaying = resume.isPlaying)
                        }
                    }
                }
                val initialPreset = services.loadAudioEffectPreset()
                playback.setAudioEffectPreset(initialPreset)
                player = player.copy(audioEffectPreset = initialPreset)
                persistence.submit { services.savePlaylists(loadedPlaylists) }
                preferences.selectedPlaylist(selectedPlaylistId)
            } catch (error: Throwable) {
                importMessage = "音乐库加载失败：${error.message ?: error::class.simpleName.orEmpty()}"
            } finally {
                initialLoadInProgress = false
            }
        }
        DisposableEffect(applicationLifetime, persistence, features) {
            applicationLifetime.registerStop("native.import", services::cancelImport)
            applicationLifetime.registerStop("application.tasks", tasks::close)
            applicationLifetime.registerCompletion("application.tasks") { tasks.join() }
            applicationLifetime.register("library.preferences") { persistence.closeAndJoin() }
            applicationLifetime.register("assistant.settings") { features.finishPersistence() }
            onDispose { playback.close(); applicationLifetime.close() }
        }

        val visiblePlaylists = if (previewMode) PreviewLibrary.playlists else playlists
        val visibleLibraryTracks = if (previewMode) {
            PreviewLibrary.playlists.flatMap(Playlist::tracks).distinctBy(Track::id)
        } else {
            importedTracks
        }
        val fullLibraryQueue = remember(importedTracks, playlists) {
            (importedTracks + playlists.flatMap(Playlist::tracks)).filter { it.sourceUri != null }.distinctBy(Track::id)
        }
        val playbackQueue = remember(customQueue, fullLibraryQueue) {
            resolvePlaybackQueue(customQueue, fullLibraryQueue)
        }
        SideEffect { together?.publishQueue(customQueue ?: playbackQueue) }

        fun persistPlaylists(updated: List<Playlist>) {
            playlists = updated
            persistence.submit { services.savePlaylists(updated) }
        }

        suspend fun runMusicImport(convertToMp3: Boolean, paths: List<String>? = null) {
            if (operationInProgress || syncInProgress != null) return
            operationInProgress = true
            try {
                val result = musicImport.execute(convertToMp3, paths) { playlists }
                val report = result.report
                if (result.tracks != null && result.playlists != null) {
                    importedTracks = tracksWithPlaylistArtwork(result.tracks, result.playlists)
                    persistPlaylists(result.playlists)
                    previewMode = false
                    destination = LibraryDestination.Library
                }
                importMessage = importReportMessage(report, convertToMp3)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                importMessage = "导入失败：${error.message ?: error::class.simpleName.orEmpty()}"
            } finally { operationInProgress = false }
        }

        ApplicationEffect(scope, services) {
            services.externalMusicImports.collect { paths ->
                androidx.compose.runtime.snapshotFlow { operationInProgress || initialLoadInProgress || syncInProgress != null }
                    .first { !it }
                runMusicImport(false, paths)
            }
        }

        fun toggleFavorite(track: Track) {
            if (track.isTogetherTrack()) { importMessage = "跟听音频只保存在临时缓存中，不能加入本机收藏"; return }
            val next = !track.isFavorite
            fun mark(list: List<Track>): List<Track> =
                list.map { if (it.id == track.id) it.copy(isFavorite = next) else it }
            importedTracks = mark(importedTracks)
            persistPlaylists(playlists.map { playlist -> playlist.copy(tracks = mark(playlist.tracks)) })
            customQueue = customQueue?.let(::mark)
            player.currentTrack?.takeIf { it.id == track.id }?.let {
                player = player.copy(currentTrack = it.copy(isFavorite = next))
            }
            persistence.submit { services.setTrackFavorite(track.id, next); if (next) services.musicAssistant?.retain(track.id) }
        }

        val libraryQueueReference = androidx.compose.runtime.rememberUpdatedState(fullLibraryQueue)
        val queueSession = remember(services, playback, persistence, scope) {
            com.resonance.player.application.PlaybackSession(services, scope, playback, persistence, playbackModeWriter,
                object : com.resonance.player.application.PlaybackSession.State {
                    override var player: PlayerState
                        get() = playback.state.value
                        set(value) { playback.state.value = value }
                    override var customQueue: List<Track>?
                        get() = customQueueState.value
                        set(value) { customQueueState.value = value }
                    override var activePlaylistId: String?
                        get() = activePlaylistIdState.value
                        set(value) { activePlaylistIdState.value = value }
                    override val selectedPlaylistId: String? get() = selectedPlaylistIdState.value
                    override val fullLibraryQueue: List<Track> get() = libraryQueueReference.value
                    override val queue: List<Track> get() = resolvePlaybackQueue(customQueueState.value, libraryQueueReference.value)
                },
                missing = { track, queue, shuffle -> trackPendingRestore = PendingTrackRestore(track, queue, shuffle) },
                ready = { trackPendingRestore = null },
                message = { text, queueFeedback -> if (queueFeedback) features.message = text else importMessage = text })
        }
        DisposableEffect(applicationLifetime, queueSession) {
            applicationLifetime.registerStop("playback.requests", queueSession::close)
            onDispose { queueSession.close() }
        }
        fun requestTogetherToggle() = queueSession.requestTogetherToggle()
        fun allowLocalPlayback() = queueSession.allowLocalPlayback()
        fun startTrack(track: Track, newQueue: List<Track>? = null, shuffle: Boolean = player.shuffleEnabled) = queueSession.startTrack(track, newQueue, shuffle)
        fun startShufflePlayback(tracks: List<Track>) = queueSession.startShufflePlayback(tracks)
        fun moveInQueue(direction: Int, automatic: Boolean = false) = queueSession.moveInQueue(direction, automatic)
        fun playNext(track: Track) = queueSession.playNext(track)
        fun removeFromQueue(track: Track) = queueSession.removeFromQueue(track)
        fun clearQueue() = queueSession.clearQueue()
        fun moveTrackInQueue(fromIndex: Int, toIndex: Int) = queueSession.moveTrackInQueue(fromIndex, toIndex)

        val togetherAction by androidx.compose.runtime.rememberUpdatedState<(com.resonance.player.platform.TogetherRequest) -> Unit>({ request ->
            when (request.action) {
                com.resonance.player.platform.TogetherControl.Play, com.resonance.player.platform.TogetherControl.Pause -> {
                    val playing = request.action == com.resonance.player.platform.TogetherControl.Play
                    services.setPlaying(playing)
                    player = player.copy(isPlaying = playing)
                }
                com.resonance.player.platform.TogetherControl.Next -> moveInQueue(1)
                com.resonance.player.platform.TogetherControl.Previous -> moveInQueue(-1)
                com.resonance.player.platform.TogetherControl.Seek -> request.trackId.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..1f }?.let {
                    services.seekTo(it); player = player.copy(progress = it)
                }
                com.resonance.player.platform.TogetherControl.Select -> playbackQueue.firstOrNull { it.id == request.trackId }?.let { startTrack(it, playbackQueue) }
                com.resonance.player.platform.TogetherControl.Append -> {
                    (request.track ?: playbackQueue.firstOrNull { it.id == request.trackId })?.let {
                        if (playbackQueue.none { t -> t.id == it.id }) customQueue = playbackQueue + it
                    }
                }
                com.resonance.player.platform.TogetherControl.Remove -> {
                    playbackQueue.firstOrNull { it.id == request.trackId }?.let { removeFromQueue(it) }
                }
                com.resonance.player.platform.TogetherControl.Move -> {
                    val parts = request.trackId.split(":")
                    if (parts.size == 2) {
                        val from = parts[0].toIntOrNull()
                        val to = parts[1].toIntOrNull()
                        if (from != null && to != null && from in playbackQueue.indices && to in playbackQueue.indices) {
                            moveTrackInQueue(from, to)
                        }
                    }
                }
            }
        })
        ApplicationEffect(scope, together) {
            if (services.managesTogetherPlayback) return@ApplicationEffect
            together?.approvedActions?.collect { request ->
                val room = together.state.value
                if (room.role == com.resonance.player.platform.TogetherRole.Host &&
                    (request.roomSessionId.isEmpty() || request.roomSessionId == room.roomSessionId)) togetherAction(request)
            }
        }

        ApplicationEffect(scope, services, playbackQueue, player.currentTrack, player.repeatMode, player.shuffleEnabled) {
            services.playbackEnded.collect {
                if (!(services.managesTogetherPlayback && together?.state?.value?.role == com.resonance.player.platform.TogetherRole.Host))
                    moveInQueue(direction = 1, automatic = true)
            }
        }
        ApplicationEffect(scope, together) {
            if (!services.managesTogetherPlayback) return@ApplicationEffect
            together?.hostPlayback?.collect { snapshot ->
                if (snapshot == null || together.state.value.role != com.resonance.player.platform.TogetherRole.Host) return@collect
                val track = snapshot.currentTrack
                if (track != null) {
                    if (playback.acceptNativeTrack(track.id)) playback.activate(track, activePlaylistId)
                    if (player.currentTrack?.id != track.id) return@collect
                    player = player.copy(isPlaying = snapshot.isPlaying, progress = snapshot.progress, positionMs = snapshot.positionMs,
                        playbackSpeed = snapshot.playbackSpeed)
                } else if (together.hostQueue.value?.tracks?.isEmpty() == true) playback.clearCurrentTrack()
            }
        }
        ApplicationEffect(scope, services) {
            services.playbackProgress.collect { progress -> player = player.copy(progress = progress, positionMs = services.playbackPositionMs) }
        }
        ApplicationEffect(scope, services) {
            services.isBufferingChanges.collect { isBuffering -> playback.setBuffering(isBuffering) }
        }
        ApplicationEffect(scope, services) {
            services.isPlayingChanges.collect { isPlaying -> playback.setPlaying(isPlaying) }
        }
        ApplicationEffect(scope, services) {
            services.playbackIssues.collect { issue ->
                if (services.isCurrentPlaybackIssue(issue) && (!issue.interruptsPlayback || issue.trackId == null || issue.trackId == player.currentTrack?.id)) {
                    playbackIssue = issue
                    if (issue.interruptsPlayback) playback.playbackFailed()
                }
            }
        }
        ApplicationEffect(scope, player.currentTrack?.id, player.isPlaying, playbackIssue) {
            playbackIssue?.takeIf { it.interruptsPlayback }?.let { issue ->
                if (!services.isCurrentPlaybackIssue(issue) || (issue.trackId != null && issue.trackId != player.currentTrack?.id)) playbackIssue = null
            }
        }
        ApplicationEffect(scope, services) {
            services.currentAudioEffectPreset.collect { preset -> player = player.copy(audioEffectPreset = preset) }
        }

        ApplicationEffect(scope, services, playbackQueue, player.currentTrack?.id) {
            while (true) {
                delay(2_000)
                player.currentTrack?.let { track ->
                    if (track.isTogetherTrack()) return@let
                    val snapshot = PlaybackSnapshot(track.id, playbackQueue.filterNot { it.isTogetherTrack() }.map(Track::id), player.progress, activePlaylistId)
                    persistence.submitLatest("playback.snapshot") { services.savePlaybackSnapshot(snapshot) }
                }
            }
        }
        ApplicationEffect(scope, services, player.currentTrack?.id, sharedLyrics) {
            lyricSession.load(player.currentTrack, sharedLyrics)
        }

        fun requestAiLyrics() {
            features.generateTextLyrics()
        }
        ApplicationEffect(scope, services, playbackQueue, selectedPlaylistId) {
            services.activeTrackChanges.collect { trackId ->
                if (!playback.acceptNativeTrack(trackId)) return@collect
                playbackQueue.firstOrNull { it.id == trackId }?.let { track ->
                    if (!track.isTogetherTrack()) playback.activate(track, activePlaylistId, resetGain = false)
                }
            }
        }
        features.onPlay = { track, queue -> startTrack(track, queue) }
        features.onQueueNext = ::playNext
        features.onAppendQueue = { tracks ->
            if (together?.state?.value?.role?.let { it != com.resonance.player.platform.TogetherRole.None } == true) {
                features.message = "一起听中请通过房间队列添加歌曲"
            } else {
                val base = if (player.currentTrack == null && customQueue == null) emptyList() else playbackQueue
                val updated = com.resonance.player.model.appendAssistantQueue(base, tracks)
                customQueue = updated
                features.message = if (updated.size > base.size) "已加入 ${updated.size - base.size} 首歌曲" else "歌曲已在播放队列中"
            }
        }
        features.onAction = { action ->
            when (action.kind) {
                "navigate", "suggest_page" -> {
                    destination = LibraryDestination.entries.firstOrNull { it.name == action.target } ?: error("页面已不可用")
                }
                "suggest_tutorial" -> {
                    val chapter = com.resonance.player.tutorial.TutorialCatalog.chapter(action.target) ?: error("教程章节已不可用")
                    tutorial.start(chapter)
                }
                "play" -> {
                    features.onReload()
                    val ids = action.target.split(',')
                    var queue = ids.mapNotNull { id -> features.library.firstOrNull { it.id == id } }
                    val first = queue.firstOrNull() ?: error("歌曲已不在曲库中")
                    if (first.sourceUri.isNullOrBlank()) {
                        val restored = services.musicAssistant?.restoreDownload(first.id) ?: error("无法重新下载")
                        features.onReload()
                        queue = queue.map { if (it.id == first.id) restored else it }
                    }
                    startTrack(queue.first(), queue)
                }
                "pause", "resume" -> {
                    check(player.currentTrack != null) { "请先选择歌曲" }
                    val playing = action.kind == "resume"
                    val together = services.listenTogether
                    if (together?.state?.value?.isGuest == true) {
                        together.requestControl(if (playing) com.resonance.player.platform.TogetherControl.Play else com.resonance.player.platform.TogetherControl.Pause)
                    } else {
                        playback.setUserPlayIntent(playing)
                        services.setPlaying(playing)
                        player = player.copy(isPlaying = playing)
                    }
                }
                "next" -> moveInQueue(direction = 1)
                "previous" -> moveInQueue(direction = -1)
                "seek" -> {
                    val seconds = action.target.toFloatOrNull() ?: error("播放位置无效")
                    val duration = player.currentTrack?.let { com.resonance.player.model.durationTextToSeconds(it.durationText) } ?: 0
                    check(duration > 0) { "当前歌曲时长尚不可用" }
                    val fraction = (seconds / duration).coerceIn(0f, 1f)
                    val together = services.listenTogether
                    if (together?.state?.value?.isGuest == true) {
                        check(together.state.value.equalControls) { "对方使用旧版，暂不支持共同调整进度" }
                        together.requestControl(com.resonance.player.platform.TogetherControl.Seek, fraction.toString())
                    } else { services.seekTo(fraction); player = player.copy(progress = fraction, positionMs = (fraction * duration * 1000).toLong()) }
                }
                else -> error("操作不支持")
            }
        }

        val automaticAction = featureTaskState?.actions?.firstOrNull { !it.kind.startsWith("suggest_") }
        ApplicationEffect(scope, automaticAction?.id) {
            val action = automaticAction ?: return@ApplicationEffect
            try { features.onAction(action) }
            catch (error: Exception) { features.message = "操作未完成：" + error.message }
            finally { withContext(kotlinx.coroutines.NonCancellable) { services.musicAssistant?.acknowledgeAction(action.id) } }
        }
        if (!stopping) FloatingLyricsHost(player, displayLyrics, features.settings, features::save,
            onTogglePlay = {
                if (!requestTogetherToggle() && allowLocalPlayback()) {
                    val nextPlaying = !player.isPlaying
                    playback.setUserPlayIntent(nextPlaying)
                    services.setPlaying(nextPlaying)
                }
            }, onPrevious = { moveInQueue(-1) }, onNext = { moveInQueue(1) })
        mainWindow {
        CompositionLocalProvider(LocalMusicFeatures provides features, LocalTutorial provides tutorial) {
        val keyboardFocus = remember { FocusRequester() }
        val pressedShortcuts = remember { mutableSetOf<Key>() }
        ApplicationEffect(scope, services) { if (services.supportsKeyboardShortcuts) keyboardFocus.requestFocus() }
        ApplicationEffect(scope, foreground) { pressedShortcuts.clear() }
        ApplicationEffect(scope, features.instantNavigation) { if (features.instantNavigation) { delay(400); features.instantNavigation = false } }
        Surface(
            modifier = Modifier.fillMaxSize().focusRequester(keyboardFocus)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp) pressedShortcuts.remove(event.key)
                    if (!services.supportsKeyboardShortcuts || event.type != KeyEventType.KeyDown) false
                    else when {
                        event.isCtrlPressed && event.key == Key.F -> { features.instantNavigation = true; destination = LibraryDestination.Discover; features.searchFocusRequest++; true }
                        else -> false
                    }
                }.onKeyEvent { event ->
                    if (!services.supportsKeyboardShortcuts || event.type != KeyEventType.KeyDown) false
                    else when {
                        event.isCtrlPressed && event.key == Key.DirectionRight -> { moveInQueue(1); true }
                        event.isCtrlPressed && event.key == Key.DirectionLeft -> { moveInQueue(-1); true }
                        event.key == Key.Escape -> { features.instantNavigation = true; com.resonance.player.platform.KeyboardBackNavigation.handle() }
                        event.key == Key.Spacebar && !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed && !event.isShiftPressed && player.currentTrack != null -> {
                            if (pressedShortcuts.add(event.key)) { val playing = !player.isPlaying; playback.setUserPlayIntent(playing); services.setPlaying(playing); player = player.copy(isPlaying = playing) }; true
                        }
                        else -> false
                    }
                }.focusable(),
            color = com.resonance.player.design.ResonanceColors.Canvas,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                LibraryShell(
                        destination = destination,
                        onDestinationChange = { destination = it },
                        playlists = visiblePlaylists,
                        libraryTracks = visibleLibraryTracks,
                        playerState = player,
                        playbackQueue = playbackQueue,
                        lyricsState = displayLyrics,
                        onTogglePlay = toggle@{
                            if (requestTogetherToggle()) return@toggle
                            if (!allowLocalPlayback()) return@toggle
                            val nextPlaying = !player.isPlaying
                            playback.setUserPlayIntent(nextPlaying)
                            services.setPlaying(nextPlaying)
                            player = player.copy(isPlaying = nextPlaying)
                        },
                        onTrackSelected = { track, queue ->
                            startTrack(track, queue)
                        },
                        onToggleShufflePlay = ::startShufflePlayback,
                        selectedPlaylistId = selectedPlaylistId,
                        userPlaylists = playlists,
                        onPlaylistSelected = { playlistId -> selectPlaylist(playlistId) },
                        onPlaylistMembershipChange = { track, playlistId, include ->
                            val updated = playlists.map { playlist ->
                                if (playlist.id != playlistId) {
                                    playlist
                                } else {
                                    val tracks = if (include) {
                                        (playlist.tracks + track).distinctBy(Track::id)
                                    } else {
                                        playlist.tracks.filterNot { it.id == track.id }
                                    }
                                    playlist.copy(
                                        tracks = tracks,
                                        subtitle = "${tracks.size} 首 · 自建歌单",
                                    )
                                }
                            }
                            persistPlaylists(updated)
                            if (include) scope.launch { services.musicAssistant?.retain(track.id) }
                            if (!include) {
                                customQueue = customQueue?.filterNot { it.id == track.id }
                            }
                        },
                        onRenamePlaylist = { playlistId, name ->
                            val cleanedName = name.trim().take(60)
                            if (cleanedName.isNotEmpty()) {
                                persistPlaylists(playlists.map { playlist ->
                                    if (playlist.id == playlistId) playlist.copy(name = cleanedName) else playlist
                                })
                            }
                        },
                        onDeletePlaylist = { playlistId ->
                            val updated = playlists.filterNot { it.id == playlistId }
                            persistPlaylists(updated)
                            if (selectedPlaylistId == playlistId) {
                                selectPlaylist(updated.firstOrNull()?.id)
                            }
                        },
                        onDeleteLocalTrack = { track -> trackPendingDeletion = track },
                        onToggleFavorite = { track -> toggleFavorite(track) },
                        onRefreshLyrics = {
                            val track = player.currentTrack
                            if (track != null) {
                                features.cancelTextLyrics()
                                features.generatedLyrics = null
                                scope.launch { lyricSession.load(track, forceRefresh = true) }
                            }
                        },
                        onRequestAiLyrics = { requestAiLyrics() },
                        onAdjustLyricsOffset = { delta ->
                            val track = player.currentTrack
                            val current = (displayLyrics as? LyricsUiState.Ready)?.lyrics
                            if (track != null && current != null) scope.launch {
                                try {
                                    val updated = services.musicAssistant?.saveLyricOffset(track, current, delta)
                                    if (updated != null && player.currentTrack?.id == track.id) {
                                        lyricSession.accept(track.id, updated)
                                        features.generatedLyrics = features.generatedLyrics?.copy(offsetMs = updated.offsetMs)
                                        features.translated = features.translated?.copy(offsetMs = updated.offsetMs)
                                    }
                                } catch (error: Exception) { features.message = "歌词校准保存失败：" + error.message }
                            }
                        },
                        libraryLocation = services.libraryLocation,
                        onPrevious = { moveInQueue(direction = -1) },
                        onNext = { moveInQueue(direction = 1) },
                        onToggleShuffle = shuffle@{
                            if (!allowLocalPlayback()) return@shuffle
                            val next = !player.shuffleEnabled
                            services.setPlaybackMode(next, player.repeatMode)
                            player = player.copy(shuffleEnabled = next)
                            playbackModeWriter.save(next, player.repeatMode)
                        },
                        onCycleRepeat = repeat@{
                            if (!allowLocalPlayback()) return@repeat
                            val nextRepeat = when (player.repeatMode) {
                                    RepeatMode.Off -> RepeatMode.All
                                    RepeatMode.All -> RepeatMode.One
                                    RepeatMode.One -> RepeatMode.Off
                            }
                            services.setPlaybackMode(player.shuffleEnabled, nextRepeat)
                            player = player.copy(repeatMode = nextRepeat)
                            playbackModeWriter.save(player.shuffleEnabled, nextRepeat)
                        },
                        onSeek = seek@{ progress ->
                            val together = services.listenTogether
                            if (together?.state?.value?.isGuest == true) {
                                if (together.state.value.equalControls && progress.isFinite())
                                    together.requestControl(com.resonance.player.platform.TogetherControl.Seek, progress.coerceIn(0f, 1f).toString())
                                else importMessage = "对方使用旧版，暂不支持共同调整进度。"
                                return@seek
                            }
                            if (!allowLocalPlayback()) return@seek
                            services.seekTo(progress)
                            player = player.copy(progress = progress)
                        },
                        onVolumeChange = { vol ->
                            playback.setVolume(vol)
                            val trackId = player.currentTrack?.id
                            val playlistId = activePlaylistId
                            persistence.submitLatest("volume.global") { services.saveGlobalVolume(vol) }
                        },
                        onSpeedChange = speed@{ speed ->
                            if (!allowLocalPlayback()) return@speed
                            services.setPlaybackSpeed(speed)
                            player = player.copy(playbackSpeed = speed)
                        },
                        onTrackGainChange = { gain ->
                            playback.setGain(gain)
                            val trackId = player.currentTrack?.id
                            persistence.submit { trackId?.let { services.saveTrackGain(it, gain) } }
                        },
                        onAudioEffectPresetChange = { preset ->
                            playback.setAudioEffectPreset(preset)
                            player = player.copy(audioEffectPreset = preset)
                            persistence.submitLatest("audio.effect_preset") { services.saveAudioEffectPreset(preset) }
                        },
                        onPlayNext = ::playNext,
                        onRemoveFromQueue = ::removeFromQueue,
                        onClearQueue = ::clearQueue,
                        onMoveInQueueOrder = ::moveTrackInQueue,
                        onToggleFloatingLyrics = features::toggleFloatingLyrics,
                        onOpenSleepTimer = { showSleepTimer = true },
                        floatingLyricsEnabled = features.settings.floatingLyrics,
                        onEditTrack = { track, title, artist, album, embed -> features.launch { services.editTrackMetadata(track, title, artist, album); features.onReload() } },
                        onToggleAutoLoudness = {
                            val next = !player.autoLoudnessEnabled
                            player = player.copy(autoLoudnessEnabled = next)
                            persistence.submit {
                                services.setAutoLoudnessEnabled(next)
                                playback.refreshAudio(activePlaylistId)
                            }
                        },
                        onStartCalibration = {
                            player.currentTrack?.let { track ->
                                services.startVolumeCalibration(track.id)
                            }
                        },
                        onFinishCalibration = {
                            player.currentTrack?.let { track ->
                                val gain = services.finishVolumeCalibration(track.id)
                                if (gain != null) {
                                    playback.setGain(gain, calibrated = true)
                                }
                            }
                        },
                        onCancelCalibration = {
                            services.cancelVolumeCalibration()
                        },
                        onResetCalibration = {
                            player.currentTrack?.let { track ->
                                persistence.submit {
                                    services.clearTrackCalibratedGain(track.id)
                                    if (player.currentTrack?.id == track.id) playback.refreshAudio(activePlaylistId)
                                }
                            }
                        },
                        onCreatePlaylist = { showCreateDialog = true },
                        onImport = { convertToMp3 -> scope.launch { runMusicImport(convertToMp3) } },
                        onExportSync = { if (syncInProgress == null) syncAction = SyncAction.Export },
                        onImportSync = { if (syncInProgress == null) syncAction = SyncAction.Import },
                        onExitPreview = {
                            services.setPlaying(false)
                            previewMode = false
                            customQueue = null
                            player = PlayerState(shuffleEnabled = player.shuffleEnabled, repeatMode = player.repeatMode)
                        },
                        previewMode = previewMode,
                        themeMode = themeMode,
                        onThemeModeChange = { newMode ->
                            themeMode = newMode
                            preferences.theme(newMode)
                        },
                        deepSeekConfig = deepSeekConfig,
                        onSaveDeepSeekConfig = { cfg ->
                            deepSeekConfig = cfg
                            persistence.submit { services.saveDeepSeekConfig(cfg) }
                        },
                        onTestDeepSeek = { cfg -> services.testDeepSeek(cfg) },
                        customLyricsFolder = customLyricsFolder,
                        onSaveCustomLyricsFolder = { fld ->
                            customLyricsFolder = fld
                            preferences.lyricsFolder(fld)
                        },
                        onOpenBatchEnrich = { candidateTracks ->
                            batchEnrichCandidateTracks = candidateTracks
                            batchEnrichProgress = null
                            batchEnrichReport = null
                        },
                        onBatchDelete = { candidateTracks ->
                            scope.launch {
                                operationInProgress = true
                                try {
                                    if (player.currentTrack?.let { curr -> candidateTracks.any { it.id == curr.id } } == true) {
                                        services.setPlaying(false)
                                        player = PlayerState(shuffleEnabled = player.shuffleEnabled, repeatMode = player.repeatMode)
                                    }
                                    val report = services.batchDeleteTracks(candidateTracks)
                                    importMessage = report.message
                                    val removed = report.removedTrackIds.ifEmpty { if (report.success) candidateTracks.map(Track::id).toSet() else emptySet() }
                                    customQueue = customQueue?.filterNot { it.id in removed }
                                    if (removed.isNotEmpty()) {
                                        val loadedTracks = services.loadLibrary()
                                        val updatedPlaylists = playlists.map { playlist ->
                                            val remaining = playlist.tracks.filterNot { it.id in removed }
                                            playlist.copy(
                                                tracks = remaining,
                                                subtitle = if (playlist.id.startsWith("kugou-")) {
                                                    "${remaining.size} 首 · 已匹配 ${remaining.count { it.sourceUri != null }} 首本地音乐"
                                                } else {
                                                    "${remaining.size} 首 · 自建歌单"
                                                },
                                            )
                                        }
                                        importedTracks = tracksWithPlaylistArtwork(loadedTracks, updatedPlaylists)
                                        persistPlaylists(updatedPlaylists)
                                    }
                                } catch (error: Throwable) {
                                    importMessage = "批量删除失败：${error.message ?: error::class.simpleName.orEmpty()}"
                                } finally {
                                    operationInProgress = false
                                }
                            }
                        },
                        onBatchAddToPlaylist = { candidateTracks, playlistId ->
                            val updated = playlists.map { playlist ->
                                if (playlist.id != playlistId) playlist
                                else {
                                    val combined = (playlist.tracks + candidateTracks).distinctBy(Track::id)
                                    playlist.copy(
                                        tracks = combined,
                                        subtitle = "${combined.size} 首 · 自建歌单",
                                    )
                                }
                            }
                            persistPlaylists(updated)
                            importMessage = "已将 ${candidateTracks.size} 首歌曲添加到歌单"
                        },
                        message = importMessage,
                        operationInProgress = operationInProgress || initialLoadInProgress || syncInProgress != null,
                    )
                TutorialDialogs(tutorial, ready = !initialLoadInProgress)

                AnimatedVisibility(
                    visible = showCreateDialog,
                    enter = fadeIn(tween(160)),
                    exit = fadeOut(tween(120)),
                ) {
                    com.resonance.player.ui.CreatePlaylistDialog(
                        onDismiss = { showCreateDialog = false },
                        onConfirm = { name ->
                            val playlistId = "playlist-${Random.nextLong().toULong().toString(16)}"
                            val updated = playlists + Playlist(
                                id = playlistId,
                                name = name,
                                subtitle = "0 首 · 自建歌单",
                                tracks = emptyList(),
                                artworkSeed = playlists.size + 20,
                            )
                            persistPlaylists(updated)
                            selectPlaylist(playlistId)
                            destination = LibraryDestination.Library
                            showCreateDialog = false
                        },
                    )
                }

                val activeSyncAction = syncAction
                if (activeSyncAction != null) {
                    com.resonance.player.ui.SyncPackageDialog(
                        exporting = activeSyncAction == SyncAction.Export,
                        playlists = playlists,
                        onDismiss = { syncAction = null },
                        onConfirm = confirm@ { playlistId ->
                            if (syncInProgress != null || operationInProgress || initialLoadInProgress) return@confirm
                            syncAction = null
                            syncInProgress = activeSyncAction
                            showSyncProgress = true
                            importMessage = if (activeSyncAction == SyncAction.Export) "正在导出歌单文件，请稍候…" else "正在导入歌单文件，请稍候…"
                            scope.launch {
                                try {
                                    val report = if (activeSyncAction == SyncAction.Export) {
                                        services.exportSyncPackage(playlistId)
                                    } else {
                                        persistence.run { }
                                        services.importSyncPackage()
                                    }
                                    importMessage = report.message
                                    if (report.success && activeSyncAction == SyncAction.Import) {
                                        val loadedTracks = services.loadLibrary()
                                        val loadedPlaylists = rematchCatalogTracks(services.loadPlaylists(), loadedTracks)
                                        importedTracks = tracksWithPlaylistArtwork(loadedTracks, loadedPlaylists)
                                        playlists = loadedPlaylists
                                        services.savePlaylists(loadedPlaylists)
                                        val restored = resolveSelectedPlaylistId(selectedPlaylistId, loadedPlaylists)
                                        selectPlaylist(restored)
                                    }
                                    if (!report.cancelled) syncResult = com.resonance.player.ui.SyncPackageOutcome(activeSyncAction == SyncAction.Export, report)
                                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                    throw cancelled
                                } catch (error: Throwable) {
                                    importMessage = "${if (activeSyncAction == SyncAction.Export) "导出" else "导入"}失败：${error.message ?: error::class.simpleName.orEmpty()}"
                                    syncResult = com.resonance.player.ui.SyncPackageOutcome(activeSyncAction == SyncAction.Export,
                                        com.resonance.player.model.SyncReport(false, importMessage.orEmpty()))
                                } finally {
                                    syncInProgress = null
                                    showSyncProgress = false
                                }
                            }
                        },
                    )
                }

                syncInProgress?.let { action ->
                    if (showSyncProgress) com.resonance.player.ui.SyncPackageProgressDialog(action == SyncAction.Export) {
                        showSyncProgress = false
                    }
                }
                syncResult?.let { result ->
                    com.resonance.player.ui.SyncPackageResultDialog(result, onDismiss = { syncResult = null },
                        onOpenLocation = services::openExportLocation)
                }

                com.resonance.player.ui.AssistantDeletionPrompt(features)

                trackPendingDeletion?.let { track ->
                    com.resonance.player.ui.DeleteTrackDialog(
                        track = track,
                        onDismiss = { trackPendingDeletion = null },
                        onConfirm = {
                            trackPendingDeletion = null
                            operationInProgress = true
                            scope.launch {
                                try {
                                    if (player.currentTrack?.id == track.id) {
                                        services.setPlaying(false)
                                        player = PlayerState(shuffleEnabled = player.shuffleEnabled, repeatMode = player.repeatMode)
                                    }
                                    val report = services.deleteLocalTrack(track)
                                    importMessage = report.message
                                    if (report.success) {
                                        customQueue = customQueue?.filterNot { it.id == track.id }
                                        val loadedTracks = services.loadLibrary()
                                        val updatedPlaylists = playlists.map { playlist ->
                                                val remaining = playlist.tracks.filterNot { it.id == track.id }
                                                playlist.copy(
                                                    tracks = remaining,
                                                    subtitle = if (playlist.id.startsWith("kugou-")) {
                                                        "${remaining.size} 首 · 已匹配 ${remaining.count { it.sourceUri != null }} 首本地音乐"
                                                    } else {
                                                        "${remaining.size} 首 · 自建歌单"
                                                    },
                                                )
                                            }
                                        importedTracks = tracksWithPlaylistArtwork(loadedTracks, updatedPlaylists)
                                        persistPlaylists(updatedPlaylists)
                                    }
                                } catch (error: Throwable) {
                                    importMessage = "删除失败：${error.message ?: error::class.simpleName.orEmpty()}"
                                } finally {
                                    operationInProgress = false
                                }
                            }
                        },
                    )
                }

                batchEnrichCandidateTracks?.let { candidateTracks ->
                    com.resonance.player.ui.BatchEnrichDialog(
                        selectedTracks = candidateTracks,
                        isRunning = batchEnrichRunning,
                        progress = batchEnrichProgress,
                        report = batchEnrichReport,
                        hasDeepSeekConfigured = features.settings.connections.any { it.provider == features.settings.assistantProvider && it.configured } || deepSeekConfig.isConfigured,
                        onDismiss = {
                            batchEnrichJob?.cancel()
                            batchEnrichJob = null
                            batchEnrichRunning = false
                            batchEnrichCandidateTracks = null
                            batchEnrichProgress = null
                            batchEnrichReport = null
                        },
                        onStartEnrich = { options ->
                            startBatchEnrich(candidateTracks, options)
                        },
                        onCancel = {
                            batchEnrichJob?.cancel()
                            batchEnrichJob = null
                            batchEnrichRunning = false
                            scope.launch {
                                val loadedTracks = services.loadLibrary()
                                val loadedPlaylists = rematchCatalogTracks(services.loadPlaylists(), loadedTracks)
                                importedTracks = tracksWithPlaylistArtwork(loadedTracks, loadedPlaylists)
                                playlists = loadedPlaylists
                            }
                        },
                    )
                }
            }
        }
        MusicFeatureDialogs()
        playbackIssue?.let { issue ->
            val current = services.isCurrentPlaybackIssue(issue) && (issue.trackId == null || issue.trackId == player.currentTrack?.id)
            com.resonance.player.ui.PlaybackErrorDialog(issue.message,
                onDismiss = { playbackIssue = null },
                onRetry = if (issue.interruptsPlayback && current && player.currentTrack != null && togetherRoom?.role in listOf(null, com.resonance.player.platform.TogetherRole.None)) {
                    { playbackIssue = null; if (services.isCurrentPlaybackIssue(issue)) player.currentTrack?.let { startTrack(it) } }
                } else null,
                title = when (issue.source) {
                    com.resonance.player.platform.PlaybackIssue.Source.Playback -> "播放未能继续"
                    com.resonance.player.platform.PlaybackIssue.Source.Preview -> "试听未能继续"
                    com.resonance.player.platform.PlaybackIssue.Source.Statistics -> "听歌记录未能保存"
                    com.resonance.player.platform.PlaybackIssue.Source.Preferences -> "设置未能保存"
                })
        }
        if (showSleepTimer) com.resonance.player.ui.SleepTimerDialog(sleepTimer.option, sleepTimer.remainingSeconds,
            onDismiss = { showSleepTimer = false }, onSelectOption = { option ->
                if (player.currentTrack != null || option == SleepTimerOption.Off) services.setSleepTimer(option)
                else features.message = "请先选择歌曲，再设置睡眠定时"
            })
        trackPendingRestore?.let { pending ->
            val track = pending.track
            val sourceState = featureTaskState
            val canRestore = sourceState?.origins?.containsKey(track.id) == true
            val explanation = when {
                track.id in features.unavailableOrigins -> "原来源暂时不可用，可以换来源查找，或重新定位本地文件。"
                sourceState?.releasedDownloadIds?.contains(track.id) == true -> "这首歌已释放本地下载，歌曲信息、收藏和歌单仍然保留。"
                track.sourceUri != null -> "原音频文件无法读取，可能已移动、删除或授权失效。"
                else -> "这首歌还没有匹配到本机音频。"
            }
            com.resonance.player.design.FrostedAlertDialog(onDismissRequest = { trackPendingRestore = null }, title = { androidx.compose.material3.Text(track.title) }, text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(explanation)
                    if (features.message.isNotBlank()) androidx.compose.material3.Text(features.message, color = MaterialTheme.colorScheme.error)
                    androidx.compose.material3.Text("重新定位会保留收藏和歌单，不修改所选音频文件。", style = MaterialTheme.typography.bodySmall)
                    androidx.compose.material3.TextButton(onClick = { features.launch {
                        val replacement = services.chooseReplacementAudio(track) ?: return@launch
                        services.saveReplacementAudio(replacement)
                        features.onReload(); features.unavailableOrigins = features.unavailableOrigins - track.id
                        trackPendingRestore = null
                        startTrack(replacement, pending.queue?.map { if (it.id == track.id) replacement else it }, pending.shuffle)
                    } }, enabled = featureTaskState?.task?.running != true) { androidx.compose.material3.Text("重新定位本地文件") }
                    androidx.compose.material3.TextButton(onClick = {
                        features.searchQuery = track.title + " " + track.artist; features.searchOnline = true
                        trackPendingRestore = null; destination = LibraryDestination.Discover
                    }) { androidx.compose.material3.Text("换来源查找") }
                }
            }, confirmButton = {
                if (canRestore) androidx.compose.material3.TextButton(onClick = { features.launch {
                    try {
                        val restored = services.musicAssistant?.restoreDownload(track.id) ?: error("下载服务不可用")
                        features.onReload(); features.unavailableOrigins = features.unavailableOrigins - track.id
                        trackPendingRestore = null
                        startTrack(restored, pending.queue?.map { if (it.id == track.id) restored else it }, pending.shuffle)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (error: Exception) { features.unavailableOrigins = features.unavailableOrigins + track.id; features.message = "恢复下载未完成：" + error.message }
                } }, enabled = featureTaskState?.task?.running != true) { androidx.compose.material3.Text("重新下载并播放") }
            }, dismissButton = { androidx.compose.material3.TextButton(onClick = { trackPendingRestore = null }) { androidx.compose.material3.Text("关闭") } })
        }
        }
        } // mainWindow
    }
}

}

private data class PendingTrackRestore(val track: Track, val queue: List<Track>?, val shuffle: Boolean)

/** The selected audio must be the same item the native player finds in its queue. */
internal fun resolveTrackPlaybackQueue(track: Track, queue: List<Track>): List<Track> {
    val playable = queue.map { if (it.id == track.id) track else it }
        .filter { !it.sourceUri.isNullOrBlank() }
        .distinctBy(Track::id)
    return if (playable.any { it.id == track.id } || track.sourceUri.isNullOrBlank()) playable else playable + track
}

internal fun resolvePlaybackQueue(
    customQueue: List<Track>?,
    fullLibraryQueue: List<Track>,
): List<Track> {
    if (customQueue == null) return fullLibraryQueue
    if (customQueue.isEmpty()) return emptyList()
    val playable = customQueue.filter { it.sourceUri != null }
    return if (playable.isEmpty()) fullLibraryQueue else playable
}

internal fun resolveSelectedPlaylistId(requestedId: String?, playlists: List<Playlist>): String? =
    playlists.firstOrNull { it.id == requestedId }?.id ?: playlists.firstOrNull()?.id

internal fun importReportMessage(report: ImportReport, conversionRequested: Boolean): String? {
    if (report.importedPackageCount > 0) return buildString {
        append(if (report.cancelled) "已取消；已保留" else "已导入")
        append(" ${report.tracks.size} 首音乐、${report.playlists.size} 个歌单")
        if (report.skippedCount > 0) append("；${report.skippedCount} 项未处理")
        report.warnings.firstOrNull()?.let { append("；$it") }
        if (conversionRequested) append("；歌单包内音频保留原格式")
    }
    if (report.cancelled) return "已取消；已完成 ${report.tracks.size} 首并保留，源文件未修改。"
    val completed = report.tracks.size
    val notProcessed = report.skippedCount
    val action = if (conversionRequested) "转换" else "导入"
    return when {
        completed > 0 && notProcessed > 0 -> buildString {
            append("处理完成：已${action} $completed 首，$notProcessed 首未处理。")
            report.warnings.firstOrNull()?.let(::append)
            if (conversionRequested) append(" 已存在的 MP3 不受影响。")
        }
        completed > 0 && report.warnings.isNotEmpty() ->
            "已${action} $completed 首；${report.warnings.first()}"
        completed > 0 -> "已${action} $completed 首音乐"
        report.warnings.isNotEmpty() -> report.warnings.first()
        else -> null
    }
}

internal fun rematchCatalogTracks(playlists: List<Playlist>, localTracks: List<Track>): List<Playlist> {
    val localByTitle = localTracks.groupBy { normalizeMetadata(it.title) }
    return playlists.map { playlist ->
        if (!playlist.id.startsWith("kugou-")) return@map playlist
        val rematched = playlist.tracks.map { track ->
            if (track.sourceUri != null) return@map track
            val local = localByTitle[normalizeMetadata(track.title)]?.firstOrNull { local ->
                val localArtist = normalizeMetadata(local.artist)
                val catalogArtist = normalizeMetadata(track.artist)
                localArtist == catalogArtist || localArtist.contains(catalogArtist) || catalogArtist.contains(localArtist)
            }
            if (local == null) track else local.copy(
                artworkPath = local.artworkPath ?: track.artworkPath,
                artworkSeed = if (local.artworkPath == null) track.artworkSeed else local.artworkSeed,
            )
        }
        val available = rematched.count { it.sourceUri != null }
        playlist.copy(tracks = rematched, subtitle = "${rematched.size} 首 · 已匹配 $available 首本地音乐")
    }
}

private data class TogetherLocalSnapshot(val player: PlayerState, val queue: List<Track>?, val playlistId: String?)

private fun normalizeMetadata(value: String): String = value.lowercase().filter(Char::isLetterOrDigit)

private fun friendlyPlaylistError(error: Throwable): String {
    val message = generateSequence(error) { it.cause }.mapNotNull(Throwable::message).firstOrNull().orEmpty()
    return when {
        message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("No address associated", ignoreCase = true) ||
            message.contains("无法解析酷狗服务器") ->
            "暂时无法连接酷狗服务器。请切换 Wi-Fi/移动网络，或暂停 VPN、私人 DNS 后重试。"
        else -> "歌单导入失败：${message.ifBlank { error::class.simpleName.orEmpty() }}"
    }
}
