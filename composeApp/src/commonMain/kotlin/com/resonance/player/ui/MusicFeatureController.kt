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
    var onReload: suspend () -> Unit = {}
    var beforeTask: suspend () -> Unit = {}
    var onPlay: (Track) -> Unit = {}
    var onNavigate: (LibraryDestination) -> Unit = {}
    var onToggleBalance: () -> Unit = {}
    val assistant get() = services.musicAssistant
    fun launch(operation: suspend () -> Unit) = scope.launch {
        try { message = ""; settingsWriter.run {}; beforeTask(); operation() }
        catch (_: CancellationException) { message = "已取消；已完成的歌曲保留"; withContext(NonCancellable) { runCatching { onReload() } } }
        catch (error: Exception) { message = error.message ?: "操作失败" }
    }
    suspend fun load() { settings = assistant?.settings() ?: MusicFeatureSettings(); loaded = true; services.configureBackgroundRecommendations(settings) }
    fun save(value: MusicFeatureSettings) {
        settings = value
        settingsWriter.submitLatest("music.settings") { assistant?.saveSettings(value); services.configureBackgroundRecommendations(value) }
    }
    fun toggleFloatingLyrics() = save(settings.copy(floatingLyrics = !settings.floatingLyrics))
    fun translate(force: Boolean = false) {
        val current = (lyrics as? LyricsUiState.Ready)?.lyrics ?: return
        val track = player.currentTrack ?: return
        if (translating) return
        launch {
            translating = true
            try { assistant?.translate(track, current, settings.translationLanguage, force)?.let { if (player.currentTrack?.id == track.id) translated = it } }
            finally { translating = false }
        }
    }
    fun dislike(track: Track) {
        reasonTrack = track
        launch { assistant?.dislike(track); onReload() }
    }
    fun favorite(track: Track) = launch { services.setTrackFavorite(track.id, true); assistant?.retain(track.id); onReload() }
}

val LocalMusicFeatures = staticCompositionLocalOf<MusicFeatureController?> { null }
