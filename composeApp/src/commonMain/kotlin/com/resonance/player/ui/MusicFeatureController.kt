package com.resonance.player.ui

import androidx.compose.runtime.*
import com.resonance.player.model.*
import com.resonance.player.platform.PlatformServices
import kotlinx.coroutines.*

class MusicFeatureController(val services: PlatformServices, private val scope: CoroutineScope) {
    private val settingsWriter = com.resonance.player.data.PersistenceQueue(scope) { message = "设置保存失败：${it.message}" }
    var settings by mutableStateOf(MusicFeatureSettings())
    var loaded by mutableStateOf(false)
    var message by mutableStateOf("")
    var showVideoImport by mutableStateOf(false)
    var library by mutableStateOf(emptyList<Track>())
    var playlists by mutableStateOf(emptyList<Playlist>())
    var player by mutableStateOf(PlayerState())
    var lyrics by mutableStateOf<LyricsUiState>(LyricsUiState.Idle)
    var reasonTrack by mutableStateOf<Track?>(null)
    var translated by mutableStateOf<Lyrics?>(null)
    var translating by mutableStateOf(false)
    var translationError by mutableStateOf("")
    private var translationJob: Job? = null
    private var translationGeneration = 0L
    var generatedLyrics by mutableStateOf<Lyrics?>(null)
    var generatingLyrics by mutableStateOf(false)
    var generatingTextLyrics by mutableStateOf(false)
    var textLyricsError by mutableStateOf("")
    private var textLyricsJob: Job? = null
    private var textLyricsGeneration = 0L
    var showGenerateLyrics by mutableStateOf(false)
    var showOverlayPermissionPrompt by mutableStateOf(false)
    var onReload: suspend () -> Unit = {}
    var beforeTask: suspend () -> Unit = {}
    var onPlay: (Track, List<Track>) -> Unit = { _, _ -> }
    private var recommendationPlayJob: Job? = null
    private var recommendationPlayGeneration = 0L
    var onQueueNext: (Track) -> Unit = {}
    var onAppendQueue: (List<Track>) -> Unit = {}
    var onNavigate: (LibraryDestination) -> Unit = {}
    var onAction: suspend (AssistantAction) -> Unit = {}
    var onToggleBalance: () -> Unit = {}
    var searchQuery by mutableStateOf("")
    var searchOnline by mutableStateOf(false)
    var searchFocusRequest by mutableStateOf(0)
    var instantNavigation by mutableStateOf(false)
    var onlineResults by mutableStateOf(emptyList<SourceSong>())
    var onlineResultQuery by mutableStateOf("")
    var onlineError by mutableStateOf("")
    var previewId by mutableStateOf<String?>(null)
    private var previewJob: Job? = null
    private var previewGeneration = 0
    var unavailableOrigins by mutableStateOf(emptySet<String>())
    val assistant get() = services.musicAssistant
    fun launch(operation: suspend () -> Unit) = scope.launch {
        try { message = ""; settingsWriter.run {}; beforeTask(); operation() }
        catch (_: CancellationException) { message = "已取消；已完成的歌曲保留"; withContext(NonCancellable) { runCatching { onReload() } } }
        catch (error: Exception) { message = error.message ?: "操作失败" }
    }
    suspend fun load() { settings = assistant?.settings() ?: MusicFeatureSettings(); loaded = true; services.configureBackgroundRecommendations(settings) }
    suspend fun finishPersistence() { settingsWriter.closeAndJoin() }
    fun save(value: MusicFeatureSettings) {
        settings = value
        settingsWriter.submitLatest("music.settings") { assistant?.saveSettings(value); services.configureBackgroundRecommendations(value) }
    }
    fun searchSources(query: String) = launch {
        stopPreview()
        onlineError = ""
        onlineResults = emptyList()
        onlineResultQuery = query.trim()
        try { onlineResults = assistant?.searchSources(onlineResultQuery).orEmpty() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { onlineError = error.message ?: "在线搜索未完成，请重试" }
    }
    fun startPreview(song: SourceSong) {
        stopPreview()
        val generation = ++previewGeneration
        previewId = song.sourceId + ":" + song.id
        previewJob = launch {
            try {
                val path = assistant?.prepareSourcePreview(song) ?: error("试听服务尚未准备好")
                if (generation != previewGeneration) return@launch
                services.setPlaying(false)
                services.previewAudio(path)
                delay(30_000)
            } finally {
                if (generation == previewGeneration) {
                    services.stopAudioPreview(); previewId = null
                    withContext(NonCancellable) { assistant?.clearSourcePreview() }
                }
            }
        }
    }
    fun stopPreview() {
        previewGeneration++
        previewJob?.cancel(); previewJob = null
        services.stopAudioPreview(); previewId = null
        scope.launch { assistant?.clearSourcePreview() }
    }
    fun playRecommendation(track: Track, tracks: List<Track>) {
        val generation = ++recommendationPlayGeneration
        recommendationPlayJob?.cancel()
        recommendationPlayJob = null
        if (!track.sourceUri.isNullOrBlank()) {
            onPlay(track, tracks)
            return
        }
        message = ""
        recommendationPlayJob = scope.launch {
            try {
                settingsWriter.run {}
                beforeTask()
                val restored = (assistant ?: error("下载服务不可用")).restoreDownload(track.id)
                currentCoroutineContext().ensureActive()
                onReload()
                currentCoroutineContext().ensureActive()
                if (generation == recommendationPlayGeneration) {
                    onPlay(restored, tracks.map { if (it.id == track.id) restored else it })
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == recommendationPlayGeneration) message = error.message ?: "恢复下载未完成"
            } finally {
                if (generation == recommendationPlayGeneration) recommendationPlayJob = null
            }
        }
    }
    fun toggleFloatingLyrics() {
        if (!settings.floatingLyrics && !services.canDrawOverlays()) {
            showOverlayPermissionPrompt = true
            return
        }
        save(settings.copy(floatingLyrics = !settings.floatingLyrics))
    }
    suspend fun reloadGeneratedLyrics() {
        val track = player.currentTrack ?: return
        val generation = textLyricsGeneration
        val result = assistant?.cachedSynchronizedLyrics(track)
        if (player.currentTrack?.id == track.id && generation == textLyricsGeneration && !generatingTextLyrics) generatedLyrics = result
    }
    fun cancelTextLyrics() {
        textLyricsGeneration++
        textLyricsJob?.cancel()
        textLyricsJob = null
        generatingTextLyrics = false
        textLyricsError = ""
    }
    fun generateTextLyrics() {
        val track = player.currentTrack ?: return
        if (generatingTextLyrics || generatingLyrics || track.id.startsWith("together:")) return
        cancelTranslation()
        val generation = ++textLyricsGeneration
        generatingTextLyrics = true
        textLyricsError = ""
        textLyricsJob = scope.launch {
            try {
                settingsWriter.run {}
                val result = services.requestAiLyrics(track)
                currentCoroutineContext().ensureActive()
                if (generation != textLyricsGeneration || player.currentTrack?.id != track.id) return@launch
                when (result) {
                    is LyricsFetchResult.Found -> { generatedLyrics = result.lyrics; translated = null }
                    is LyricsFetchResult.Unavailable -> textLyricsError = result.message
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == textLyricsGeneration && player.currentTrack?.id == track.id)
                    textLyricsError = "AI 歌词生成未完成，请检查模型配置或网络后重试"
            } finally {
                if (generation == textLyricsGeneration) { generatingTextLyrics = false; textLyricsJob = null }
            }
        }
    }
    fun generateLyrics() {
        val track = player.currentTrack ?: return
        if (generatingLyrics || generatingTextLyrics) return
        cancelTranslation()
        generatingLyrics = true
        launch {
            try {
                val result = assistant?.generateSynchronizedLyrics(track, (lyrics as? LyricsUiState.Ready)?.lyrics)
                if (player.currentTrack?.id == track.id) { generatedLyrics = result; translated = null }
            } finally { generatingLyrics = false }
        }
    }
    private fun cancelTranslation() {
        translationGeneration++
        translationJob?.cancel()
        translationJob = null
        translating = false
        translationError = ""
    }
    private fun translationMatches(current: Lyrics, target: String): Boolean =
        player.currentTrack?.id == current.trackId && settings.translationLanguage.trim().take(40) == target &&
            (lyrics as? LyricsUiState.Ready)?.lyrics?.let {
                it.trackId == current.trackId && it.lines.map { line -> line.text } == current.lines.map { line -> line.text }
            } == true

    fun translatedLyrics(original: Lyrics): Lyrics? = translated?.takeIf {
        original.trackId == player.currentTrack?.id && it.trackId == original.trackId &&
            it.translationLanguage == settings.translationLanguage.trim().take(40) &&
            it.lines.map { line -> line.text } == original.lines.map { line -> line.text }
    }?.let { result -> original.withTranslation(result.lines.map { it.translation.orEmpty() }, result.translationLanguage.orEmpty()) }

    suspend fun reloadTranslation() {
        cancelTranslation()
        translated = null
        val current = (lyrics as? LyricsUiState.Ready)?.lyrics ?: return
        val target = settings.translationLanguage.trim().take(40)
        val generation = translationGeneration
        try {
            val cached = assistant?.cachedTranslation(current, target)
            if (generation == translationGeneration && translationMatches(current, target)) translated = cached
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (generation == translationGeneration && translationMatches(current, target))
                translationError = "本地译文加载失败，请点击翻译重试"
        }
    }

    fun translate(force: Boolean = false) {
        val current = (lyrics as? LyricsUiState.Ready)?.lyrics ?: return
        val track = player.currentTrack ?: return
        if (translating || generatingTextLyrics || generatingLyrics || current.trackId != track.id) return
        val target = settings.translationLanguage.trim().take(40)
        val generation = ++translationGeneration
        translating = true
        translationError = ""
        translationJob = scope.launch {
            try {
                require(target.isNotBlank()) { "请先在「语言与显示」中填写翻译目标语言" }
                settingsWriter.run {}
                beforeTask()
                val result = (assistant ?: error("歌词翻译服务尚未准备好，请重新打开应用后重试"))
                    .translate(track, current, target, force)
                currentCoroutineContext().ensureActive()
                if (generation == translationGeneration && translationMatches(current, target)) {
                    translated = result
                    // A deliberate translation should be visible even when the saved mode was original-only.
                    if (settings.lyricDisplay == LyricDisplay.Original) save(settings.copy(lyricDisplay = LyricDisplay.Bilingual))
                }
            } catch (cancelled: CancellationException) {
                if (generation == translationGeneration && translationMatches(current, target)) translationError = "翻译已取消，可重试"
                throw cancelled
            } catch (error: Exception) {
                if (generation == translationGeneration && translationMatches(current, target))
                    translationError = "歌词翻译失败：${error.message?.takeIf { it.isNotBlank() } ?: "请检查模型配置或网络后重试"}"
            } finally {
                if (generation == translationGeneration) { translating = false; translationJob = null }
            }
        }
    }
    fun dislike(track: Track) {
        reasonTrack = track
        launch { assistant?.dislike(track); onReload() }
    }
    fun favorite(track: Track) = launch { services.setTrackFavorite(track.id, true); assistant?.retain(track.id); onReload() }
}

val LocalMusicFeatures = staticCompositionLocalOf<MusicFeatureController?> { null }
