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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import com.resonance.player.playback.PlaybackStateStore
import com.resonance.player.platform.isTogetherTrack
import com.resonance.player.playback.PlaybackSnapshot
import com.resonance.player.data.PersistenceQueue
import kotlin.random.Random

private enum class SyncAction { Export, Import }

@Composable
fun App(services: PlatformServices) {
    var themeMode by remember { mutableStateOf(ThemeMode.Light) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(services) {
        try {
            themeMode = services.loadThemeMode()
        } catch (_: Throwable) {}
    }

    WeatherAppearanceTheme(services, themeMode) {
        var destination by remember { mutableStateOf(LibraryDestination.Library) }
        var playlists by remember { mutableStateOf(emptyList<Playlist>()) }
        var importedTracks by remember { mutableStateOf(emptyList<com.resonance.player.model.Track>()) }
        var showCreateDialog by remember { mutableStateOf(false) }
        var trackPendingDeletion by remember { mutableStateOf<Track?>(null) }
        var trackPendingRestore by remember { mutableStateOf<Track?>(null) }
        var playRequest by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
        var showSleepTimer by remember { mutableStateOf(false) }
        var operationInProgress by remember { mutableStateOf(false) }
        var initialLoadInProgress by remember { mutableStateOf(true) }
        var previewMode by remember { mutableStateOf(false) }
        var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
        var importMessage by remember { mutableStateOf<String?>(null) }
        val playback = remember(services) { PlaybackStateStore(services, scope) {
            importMessage = "音频设置未能完成：${it.message.orEmpty()}"
        } }
        var player by playback.state
        val sleepTimer by services.sleepTimer.collectAsState()
        LaunchedEffect(sleepTimer) { player = player.copy(sleepTimerOption = sleepTimer.option, sleepTimerRemainingSeconds = sleepTimer.remainingSeconds) }
        var customQueue by remember { mutableStateOf<List<Track>?>(null) }
        var activePlaylistId by remember { mutableStateOf<String?>(null) }
        com.resonance.player.ui.TogetherPlaybackBinding(services, playback, player) { track ->
            customQueue = track?.let(::listOf)
            activePlaylistId = null
        }
        val persistence = remember(services) { PersistenceQueue(scope) {
            importMessage = "保存失败：${it.message.orEmpty()}"
        } }
        val tutorial = remember(services) { TutorialController { value ->
            persistence.submitLatest("tutorial.progress") { services.saveTutorialProgress(value) }
        } }
        tutorial.onNavigate = { destination = it }
        tutorial.currentDestination = destination
        tutorial.hasTrack = player.currentTrack != null
        LaunchedEffect(services) {
            try { tutorial.restore(services.loadTutorialProgress()) }
            catch (error: Exception) { importMessage = "教程进度读取失败：${error.message.orEmpty()}" }
        }
        var syncAction by remember { mutableStateOf<SyncAction?>(null) }
        var lyricsState by remember { mutableStateOf<LyricsUiState>(LyricsUiState.Idle) }
        var deepSeekConfig by remember { mutableStateOf(com.resonance.player.model.DeepSeekConfig()) }
        var customLyricsFolder by remember { mutableStateOf<String?>(null) }
        var batchEnrichCandidateTracks by remember { mutableStateOf<List<Track>?>(null) }
        var batchEnrichRunning by remember { mutableStateOf(false) }
        var batchEnrichProgress by remember { mutableStateOf<com.resonance.player.model.BatchEnrichProgress?>(null) }
        var batchEnrichReport by remember { mutableStateOf<com.resonance.player.model.BatchEnrichReport?>(null) }
        var batchEnrichJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

        val features = remember(services) { MusicFeatureController(services, scope) }
        LaunchedEffect(tutorial.activeChapterId, tutorial.step?.id, importedTracks, playlists, player.currentTrack, features.settings) {
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
            customQueue = customQueue?.mapNotNull { queued -> loaded.firstOrNull { it.id == queued.id } }
            player.currentTrack?.takeUnless { it.isTogetherTrack() }?.let { current ->
                val updated = loaded.firstOrNull { it.id == current.id }
                player = if (updated != null) player.copy(currentTrack = updated) else { services.setPlaying(false); PlayerState(autoLoudnessEnabled = player.autoLoudnessEnabled, volume = player.volume) }
            }
            services.musicAssistant?.updateLibrarySnapshot(importedTracks, playlists)
        }
        LaunchedEffect(services) {
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
        LaunchedEffect(importedTracks, playlists) { if (!initialLoadInProgress) services.musicAssistant?.updateLibrarySnapshot(importedTracks, playlists) }
        val foreground by services.foreground.collectAsState()
        LaunchedEffect(foreground, features.loaded, features.settings.recommendationsEnabled, features.settings.intervalHours, features.settings.recommendationsUnmeteredOnly, initialLoadInProgress) {
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
        LaunchedEffect(featureTaskState?.task?.running) {
            if (featureTaskState?.task?.running == false && !initialLoadInProgress) {
                try { features.onReload() }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { features.message = "曲库刷新失败：${error.message}" }
            }
        }
        val originalLyrics = features.generatedLyrics?.takeIf { it.trackId == player.currentTrack?.id }?.let { LyricsUiState.Ready(it) } ?: lyricsState
        LaunchedEffect(player.currentTrack?.id) { features.generatedLyrics = null; features.reloadGeneratedLyrics() }
        val translatedLyrics = features.translated?.takeIf { translated ->
            val original = (originalLyrics as? LyricsUiState.Ready)?.lyrics
            translated.trackId == player.currentTrack?.id && translated.translationLanguage == features.settings.translationLanguage && original?.lines?.map { it.text } == translated.lines.map { it.text }
        }
        features.lyrics = originalLyrics
        val displayLyrics = translatedLyrics?.let { LyricsUiState.Ready(it) } ?: originalLyrics
        LaunchedEffect(originalLyrics, features.settings.translationLanguage) {
            val original = (originalLyrics as? LyricsUiState.Ready)?.lyrics
            features.translated = original?.let { services.musicAssistant?.cachedTranslation(it, features.settings.translationLanguage) }
        }
        LaunchedEffect(player, displayLyrics, features.settings) { services.updateFloatingLyrics(player, displayLyrics, features.settings) }

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
                scope.launch { services.saveLastSelectedPlaylistId(playlistId) }
            }
        }


        LaunchedEffect(services) {
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
                services.saveLastSelectedPlaylistId(selectedPlaylistId)
            } catch (error: Throwable) {
                importMessage = "音乐库加载失败：${error.message ?: error::class.simpleName.orEmpty()}"
            } finally {
                initialLoadInProgress = false
            }
        }
        DisposableEffect(services) {
            onDispose { playback.close(); persistence.close(); services.close() }
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

        fun persistPlaylists(updated: List<Playlist>) {
            playlists = updated
            persistence.submit { services.savePlaylists(updated) }
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

        fun lyricsUiState(result: LyricsFetchResult): LyricsUiState = when (result) {
            is LyricsFetchResult.Found -> LyricsUiState.Ready(result.lyrics)
            is LyricsFetchResult.Unavailable -> LyricsUiState.Unavailable(result.message, result.retryable)
        }

        fun allowLocalPlayback(): Boolean {
            if (services.listenTogether?.state?.value?.isGuest == true) {
                importMessage = "跟听由房主控制。退出一起听后可独立播放。"
                return false
            }
            return true
        }

        fun playAvailableTrack(track: Track, newQueue: List<Track>? = null, shuffle: Boolean = player.shuffleEnabled) {
            val targetQueue = when {
                newQueue != null -> newQueue.filter { it.sourceUri != null }.takeIf { it.isNotEmpty() }
                customQueue?.any { it.id == track.id } == true -> customQueue
                else -> null
            }
            customQueue = targetQueue
            val effectiveQueue = targetQueue ?: fullLibraryQueue
            val playlistId = selectedPlaylistId
            activePlaylistId = playlistId
            services.setPlaying(false)
            player = player.copy(isPlaying = false, shuffleEnabled = shuffle)
            playback.activate(track, playlistId, expectNativeTrack = true, prepare = {
                persistence.run { services.savePlaybackSnapshot(PlaybackSnapshot(track.id, effectiveQueue.map(Track::id), playlistId = playlistId)) }
            }) {
                services.play(track, effectiveQueue, shuffle, player.repeatMode)
            }
        }

        fun startTrack(track: Track, newQueue: List<Track>? = null, shuffle: Boolean = player.shuffleEnabled) {
            if (!allowLocalPlayback()) return
            playRequest?.cancel()
            playRequest = scope.launch {
                if (!services.audioFileAvailable(track)) { trackPendingRestore = track; return@launch }
                trackPendingRestore = null
                playAvailableTrack(track, newQueue, shuffle)
            }
        }

        fun startShufflePlayback(tracks: List<Track>) {
            if (!allowLocalPlayback()) return
            val playable = tracks.filter { it.sourceUri != null }
            val pick = playable.takeIf { it.isNotEmpty() }?.random() ?: return
            val nextShuffle = true
            if (!player.shuffleEnabled) {
                player = player.copy(shuffleEnabled = nextShuffle)
                services.setPlaybackMode(nextShuffle, player.repeatMode)
                scope.launch { services.savePlaybackMode(nextShuffle, player.repeatMode) }
            }
            startTrack(pick, playable, shuffle = nextShuffle)
        }

        fun moveInQueue(direction: Int, automatic: Boolean = false) {
            if (!allowLocalPlayback()) return
            if (playbackQueue.isEmpty()) return
            val current = player.currentTrack ?: playbackQueue.firstOrNull() ?: return
            if (automatic && player.repeatMode == RepeatMode.One) {
                startTrack(current, playbackQueue)
                return
            }

            val currentIndex = playbackQueue.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
            val atBoundary = direction > 0 && currentIndex == playbackQueue.lastIndex
            if (automatic && atBoundary && player.repeatMode == RepeatMode.Off && !player.shuffleEnabled) {
                services.setPlaying(false)
                player = player.copy(isPlaying = false, progress = 1f)
                return
            }

            val target = if (player.shuffleEnabled && playbackQueue.size > 1) {
                playbackQueue.filterNot { it.id == current.id }.random(Random.Default)
            } else {
                val nextIndex = (currentIndex + direction).mod(playbackQueue.size)
                playbackQueue[nextIndex]
            }
            startTrack(target, playbackQueue)
        }

        LaunchedEffect(services, playbackQueue, player.currentTrack, player.repeatMode, player.shuffleEnabled) {
            services.playbackEnded.collect { moveInQueue(direction = 1, automatic = true) }
        }
        LaunchedEffect(services) {
            services.playbackProgress.collect { progress -> player = player.copy(progress = progress, positionMs = services.playbackPositionMs) }
        }
        LaunchedEffect(services) {
            services.isPlayingChanges.collect { isPlaying -> player = player.copy(isPlaying = isPlaying) }
        }
        LaunchedEffect(services) {
            services.playbackErrors.collect { importMessage = "播放失败：$it"; playback.playbackFailed() }
        }
        LaunchedEffect(services) {
            services.currentAudioEffectPreset.collect { preset -> player = player.copy(audioEffectPreset = preset) }
        }
        LaunchedEffect(player.isPlaying, player.currentTrack?.id) {
            val trackId = player.currentTrack?.id ?: return@LaunchedEffect
            if (player.currentTrack?.isTogetherTrack() == true) return@LaunchedEffect
            if (!player.isPlaying) return@LaunchedEffect
            var completedRecorded = false
            while (true) {
                delay(1000L)
                if (player.isPlaying && player.currentTrack?.id == trackId) {
                    val isNearEnd = player.progress >= 0.95f
                    val markComplete = isNearEnd && !completedRecorded
                    if (markComplete) completedRecorded = true
                    services.recordListeningDuration(trackId, 1, isCompleted = markComplete)
                }
            }
        }
        LaunchedEffect(services, playbackQueue, player.currentTrack?.id) {
            while (true) {
                delay(2_000)
                player.currentTrack?.let { track ->
                    if (track.isTogetherTrack()) return@let
                    val snapshot = PlaybackSnapshot(track.id, playbackQueue.map(Track::id), player.progress, activePlaylistId)
                    persistence.submitLatest("playback.snapshot") { services.savePlaybackSnapshot(snapshot) }
                }
            }
        }
        LaunchedEffect(services, player.currentTrack?.id) {
            val track = player.currentTrack
            if (track?.isTogetherTrack() == true) {
                lyricsState = LyricsUiState.Unavailable("一起听当前仅同步音频与播放进度", retryable = false)
                return@LaunchedEffect
            }
            if (track == null || track.sourceUri == null) {
                lyricsState = LyricsUiState.Idle
            } else {
                lyricsState = LyricsUiState.Loading
                val result = services.loadLyrics(track)
                if (player.currentTrack?.id == track.id) lyricsState = lyricsUiState(result)
            }
        }

        fun requestAiLyrics() {
            val track = player.currentTrack ?: return
            lyricsState = LyricsUiState.Loading
            scope.launch {
                try {
                    val result = services.requestAiLyrics(track)
                    if (player.currentTrack?.id == track.id) {
                        lyricsState = lyricsUiState(result)
                    }
                } catch (error: Throwable) {
                    lyricsState = LyricsUiState.Unavailable("AI 检索失败：${error.message ?: error::class.simpleName}", retryable = true)
                }
            }
        }
        LaunchedEffect(services, playbackQueue, selectedPlaylistId) {
            services.activeTrackChanges.collect { trackId ->
                if (!playback.acceptNativeTrack(trackId)) return@collect
                playbackQueue.firstOrNull { it.id == trackId }?.let { track ->
                    if (!track.isTogetherTrack()) playback.activate(track, activePlaylistId, resetGain = false)
                }
            }
        }
        features.onPlay = { track -> startTrack(track, importedTracks) }
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
                    check(allowLocalPlayback()) { "跟听由房主控制" }
                    check(player.currentTrack != null) { "请先选择歌曲" }
                    val playing = action.kind == "resume"
                    services.setPlaying(playing); player = player.copy(isPlaying = playing)
                }
                "next" -> moveInQueue(direction = 1)
                "previous" -> moveInQueue(direction = -1)
                "seek" -> {
                    check(allowLocalPlayback()) { "跟听由房主控制" }
                    val seconds = action.target.toFloatOrNull() ?: error("播放位置无效")
                    val duration = player.currentTrack?.let { com.resonance.player.model.durationTextToSeconds(it.durationText) } ?: 0
                    check(duration > 0) { "当前歌曲时长尚不可用" }
                    val fraction = (seconds / duration).coerceIn(0f, 1f)
                    services.seekTo(fraction); player = player.copy(progress = fraction, positionMs = (fraction * duration * 1000).toLong())
                }
                else -> error("操作不支持")
            }
        }

        val automaticAction = featureTaskState?.actions?.firstOrNull { !it.kind.startsWith("suggest_") }
        LaunchedEffect(automaticAction?.id) {
            val action = automaticAction ?: return@LaunchedEffect
            try { features.onAction(action) }
            catch (error: Exception) { features.message = "操作未完成：" + error.message }
            finally { withContext(kotlinx.coroutines.NonCancellable) { services.musicAssistant?.acknowledgeAction(action.id) } }
        }
        CompositionLocalProvider(LocalMusicFeatures provides features, LocalTutorial provides tutorial) {
        val keyboardFocus = remember { FocusRequester() }
        val pressedShortcuts = remember { mutableSetOf<Key>() }
        LaunchedEffect(services) { if (services.supportsKeyboardShortcuts) keyboardFocus.requestFocus() }
        LaunchedEffect(foreground) { pressedShortcuts.clear() }
        LaunchedEffect(features.instantNavigation) { if (features.instantNavigation) { delay(400); features.instantNavigation = false } }
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
                            if (pressedShortcuts.add(event.key)) { val playing = !player.isPlaying; services.setPlaying(playing); player = player.copy(isPlaying = playing) }; true
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
                            if (!allowLocalPlayback()) return@toggle
                            val nextPlaying = !player.isPlaying
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
                                lyricsState = LyricsUiState.Loading
                                scope.launch {
                                    val result = services.loadLyrics(track, forceRefresh = true)
                                    if (player.currentTrack?.id == track.id) lyricsState = lyricsUiState(result)
                                }
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
                                        lyricsState = LyricsUiState.Ready(updated)
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
                            scope.launch { services.savePlaybackMode(next, player.repeatMode) }
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
                            scope.launch { services.savePlaybackMode(player.shuffleEnabled, nextRepeat) }
                        },
                        onSeek = seek@{ progress ->
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
                        onImport = { convertToMp3 ->
                            scope.launch {
                                operationInProgress = true
                                try {
                                    val report = services.importMusic(convertToMp3)
                                    if (report.tracks.isNotEmpty()) {
                                        val loadedTracks = services.loadLibrary()
                                        val rematchedPlaylists = rematchCatalogTracks(playlists, loadedTracks)
                                        importedTracks = tracksWithPlaylistArtwork(loadedTracks, rematchedPlaylists)
                                        persistPlaylists(rematchedPlaylists)
                                        previewMode = false
                                        destination = LibraryDestination.Library
                                    }
                                    importMessage = importReportMessage(report, convertToMp3)
                                } catch (error: Throwable) {
                                    importMessage = "导入失败：${error.message ?: error::class.simpleName.orEmpty()}"
                                } finally {
                                    operationInProgress = false
                                }
                            }
                        },
                        onExportSync = { syncAction = SyncAction.Export },
                        onImportSync = { syncAction = SyncAction.Import },
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
                            scope.launch {
                                services.saveThemeMode(newMode)
                            }
                        },
                        deepSeekConfig = deepSeekConfig,
                        onSaveDeepSeekConfig = { cfg ->
                            deepSeekConfig = cfg
                            scope.launch { services.saveDeepSeekConfig(cfg) }
                        },
                        onTestDeepSeek = { cfg -> services.testDeepSeek(cfg) },
                        customLyricsFolder = customLyricsFolder,
                        onSaveCustomLyricsFolder = { fld ->
                            customLyricsFolder = fld
                            scope.launch { services.saveCustomLyricsFolder(fld) }
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
                                    customQueue = customQueue?.filterNot { t -> candidateTracks.any { it.id == t.id } }
                                    val report = services.batchDeleteTracks(candidateTracks)
                                    importMessage = report.message
                                    if (report.success) {
                                        val loadedTracks = services.loadLibrary()
                                        val updatedPlaylists = playlists.map { playlist ->
                                            val remaining = playlist.tracks.filterNot { t -> candidateTracks.any { it.id == t.id } }
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
                        operationInProgress = operationInProgress || initialLoadInProgress,
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
                        onConfirm = { playlistId ->
                            syncAction = null
                            scope.launch {
                                try {
                                    val report = if (activeSyncAction == SyncAction.Export) {
                                        services.exportSyncPackage(playlistId)
                                    } else {
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
                                } catch (error: Throwable) {
                                    importMessage = "同步失败：${error.message ?: error::class.simpleName.orEmpty()}"
                                }
                            }
                        },
                    )
                }

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
                                    customQueue = customQueue?.filterNot { it.id == track.id }
                                    val report = services.deleteLocalTrack(track)
                                    importMessage = report.message
                                    if (report.success) {
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
        FloatingLyricsHost(player, displayLyrics, features.settings, features::save,
            onTogglePlay = { if (allowLocalPlayback()) services.setPlaying(!player.isPlaying) }, onPrevious = { moveInQueue(-1) }, onNext = { moveInQueue(1) })
        MusicFeatureDialogs()
        if (showSleepTimer) com.resonance.player.ui.SleepTimerDialog(sleepTimer.option, sleepTimer.remainingSeconds,
            onDismiss = { showSleepTimer = false }, onSelectOption = { option ->
                if (player.currentTrack != null || option == SleepTimerOption.Off) services.setSleepTimer(option)
                else features.message = "请先选择歌曲，再设置睡眠定时"
            })
        trackPendingRestore?.let { track ->
            val sourceState = featureTaskState
            val canRestore = sourceState?.origins?.containsKey(track.id) == true
            val explanation = when {
                track.id in features.unavailableOrigins -> "原来源暂时不可用，可以换来源查找，或重新定位本地文件。"
                sourceState?.releasedDownloadIds?.contains(track.id) == true -> "这首歌已释放本地下载，歌曲信息、收藏和歌单仍然保留。"
                track.sourceUri != null -> "原音频文件无法读取，可能已移动、删除或授权失效。"
                else -> "这首歌还没有匹配到本机音频。"
            }
            androidx.compose.material3.AlertDialog(onDismissRequest = { trackPendingRestore = null }, title = { androidx.compose.material3.Text(track.title) }, text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(explanation)
                    if (features.message.isNotBlank()) androidx.compose.material3.Text(features.message, color = MaterialTheme.colorScheme.error)
                    androidx.compose.material3.Text("重新定位会保留收藏和歌单，不修改所选音频文件。", style = MaterialTheme.typography.bodySmall)
                    androidx.compose.material3.TextButton(onClick = { features.launch {
                        val replacement = services.chooseReplacementAudio(track) ?: return@launch
                        services.saveReplacementAudio(replacement)
                        features.onReload(); features.unavailableOrigins = features.unavailableOrigins - track.id
                        trackPendingRestore = null; startTrack(replacement)
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
                        trackPendingRestore = null; startTrack(restored)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (error: Exception) { features.unavailableOrigins = features.unavailableOrigins + track.id; features.message = "恢复下载未完成：" + error.message }
                } }, enabled = featureTaskState?.task?.running != true) { androidx.compose.material3.Text("重新下载并播放") }
            }, dismissButton = { androidx.compose.material3.TextButton(onClick = { trackPendingRestore = null }) { androidx.compose.material3.Text("关闭") } })
        }
        }
    }
}

internal fun resolvePlaybackQueue(
    customQueue: List<Track>?,
    fullLibraryQueue: List<Track>,
): List<Track> = customQueue?.filter { it.sourceUri != null }?.takeIf { it.isNotEmpty() } ?: fullLibraryQueue

internal fun resolveSelectedPlaylistId(requestedId: String?, playlists: List<Playlist>): String? =
    playlists.firstOrNull { it.id == requestedId }?.id ?: playlists.firstOrNull()?.id

internal fun importReportMessage(report: ImportReport, conversionRequested: Boolean): String? {
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
