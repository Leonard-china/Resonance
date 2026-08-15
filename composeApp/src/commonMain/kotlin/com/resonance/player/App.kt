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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.resonance.player.data.PreviewLibrary
import com.resonance.player.design.ResonanceTheme
import com.resonance.player.model.LibraryDestination
import com.resonance.player.model.ImportReport
import com.resonance.player.model.PlayerState
import com.resonance.player.model.Playlist
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Track
import com.resonance.player.platform.PlatformServices
import com.resonance.player.platform.tracksWithPlaylistArtwork
import com.resonance.player.ui.LibraryShell
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class SyncAction { Export, Import }

@Composable
fun App(services: PlatformServices) {
    ResonanceTheme {
        var destination by remember { mutableStateOf(LibraryDestination.Library) }
        var playlists by remember { mutableStateOf(emptyList<Playlist>()) }
        var importedTracks by remember { mutableStateOf(emptyList<com.resonance.player.model.Track>()) }
        var player by remember { mutableStateOf(PlayerState()) }
        var showCreateDialog by remember { mutableStateOf(false) }
        var showPlaylistLinkDialog by remember { mutableStateOf(false) }
        var trackPendingDeletion by remember { mutableStateOf<Track?>(null) }
        var operationInProgress by remember { mutableStateOf(false) }
        var previewMode by remember { mutableStateOf(false) }
        var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
        var importMessage by remember { mutableStateOf<String?>(null) }
        var syncAction by remember { mutableStateOf<SyncAction?>(null) }
        var lanQrPath by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun selectPlaylist(playlistId: String?, persist: Boolean = true) {
            selectedPlaylistId = playlistId
            if (persist && !previewMode) {
                scope.launch { services.saveLastSelectedPlaylistId(playlistId) }
            }
        }

        LaunchedEffect(services) {
            val loadedTracks = services.loadLibrary()
            val loadedPlaylists = rematchCatalogTracks(services.loadPlaylists(), loadedTracks)
            val restoredPlaylistId = resolveSelectedPlaylistId(
                requestedId = services.loadLastSelectedPlaylistId(),
                playlists = loadedPlaylists,
            )
            importedTracks = tracksWithPlaylistArtwork(loadedTracks, loadedPlaylists)
            playlists = loadedPlaylists
            selectedPlaylistId = restoredPlaylistId
            services.savePlaylists(loadedPlaylists)
            services.saveLastSelectedPlaylistId(restoredPlaylistId)
        }
        DisposableEffect(services) {
            onDispose { services.close() }
        }

        val visiblePlaylists = if (previewMode) PreviewLibrary.playlists else playlists
        val playbackQueue = (importedTracks + playlists.flatMap(Playlist::tracks))
            .filter { it.sourceUri != null }
            .distinctBy(Track::id)

        fun persistPlaylists(updated: List<Playlist>) {
            playlists = updated
            scope.launch { services.savePlaylists(updated) }
        }

        fun startTrack(track: Track) {
            if (track.sourceUri == null) {
                importMessage = "本机尚未找到「${track.title}」的音频文件；扫描音乐后会自动匹配。"
                return
            }
            services.play(track, playbackQueue, player.shuffleEnabled, player.repeatMode)
            player = player.copy(currentTrack = track, isPlaying = true, progress = 0f)
        }

        fun moveInQueue(direction: Int, automatic: Boolean = false) {
            if (playbackQueue.isEmpty()) return
            val current = player.currentTrack ?: playbackQueue.firstOrNull() ?: return
            if (automatic && player.repeatMode == RepeatMode.One) {
                startTrack(current)
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
            startTrack(target)
        }

        LaunchedEffect(services, playbackQueue, player.currentTrack, player.repeatMode, player.shuffleEnabled) {
            services.playbackEnded.collect { moveInQueue(direction = 1, automatic = true) }
        }
        LaunchedEffect(services) {
            services.playbackProgress.collect { progress -> player = player.copy(progress = progress) }
        }
        LaunchedEffect(services, playbackQueue) {
            services.activeTrackChanges.collect { trackId ->
                playbackQueue.firstOrNull { it.id == trackId }?.let { track ->
                    player = player.copy(currentTrack = track, isPlaying = true, progress = 0f)
                }
            }
        }
        LaunchedEffect(services) {
            services.incomingLanLinks.collect { link ->
                try {
                    importMessage = "正在从 Windows 接收加密音乐库…"
                    val report = services.importLanShare(link)
                    importMessage = report.message
                    if (report.success) {
                        val loadedTracks = services.loadLibrary()
                        val loadedPlaylists = rematchCatalogTracks(services.loadPlaylists(), loadedTracks)
                        importedTracks = tracksWithPlaylistArtwork(loadedTracks, loadedPlaylists)
                        playlists = loadedPlaylists
                        services.savePlaylists(loadedPlaylists)
                        val restored = resolveSelectedPlaylistId(selectedPlaylistId, loadedPlaylists)
                        selectPlaylist(restored)
                    }
                } catch (error: Throwable) {
                    importMessage = "局域网同步失败：${error.message ?: error::class.simpleName.orEmpty()}"
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
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
                        playerState = player,
                        onTogglePlay = {
                            val nextPlaying = !player.isPlaying
                            services.setPlaying(nextPlaying)
                            player = player.copy(isPlaying = nextPlaying)
                        },
                        onTrackSelected = { track ->
                            startTrack(track)
                        },
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
                        onPrevious = { moveInQueue(direction = -1) },
                        onNext = { moveInQueue(direction = 1) },
                        onToggleShuffle = {
                            val next = !player.shuffleEnabled
                            services.setPlaybackMode(next, player.repeatMode)
                            player = player.copy(shuffleEnabled = next)
                        },
                        onCycleRepeat = {
                            val nextRepeat = when (player.repeatMode) {
                                    RepeatMode.Off -> RepeatMode.All
                                    RepeatMode.All -> RepeatMode.One
                                    RepeatMode.One -> RepeatMode.Off
                            }
                            services.setPlaybackMode(player.shuffleEnabled, nextRepeat)
                            player = player.copy(repeatMode = nextRepeat)
                        },
                        onSeek = { progress ->
                            services.seekTo(progress)
                            player = player.copy(progress = progress)
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
                        onImportPlaylist = { showPlaylistLinkDialog = true },
                        onExportSync = { syncAction = SyncAction.Export },
                        onImportSync = { syncAction = SyncAction.Import },
                        onStartLanSync = {
                            scope.launch {
                                try {
                                    val info = services.startLanShare()
                                    importMessage = info.message
                                    lanQrPath = info.qrPath
                                } catch (error: Throwable) {
                                    importMessage = "无法开启局域网同步：${error.message ?: error::class.simpleName.orEmpty()}"
                                }
                            }
                        },
                        lanQrPath = lanQrPath,
                        onExitPreview = {
                            services.setPlaying(false)
                            previewMode = false
                            player = PlayerState()
                        },
                        previewMode = previewMode,
                        message = importMessage,
                        operationInProgress = operationInProgress,
                    )

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
                        onDismiss = { syncAction = null },
                        onConfirm = { passphrase ->
                            syncAction = null
                            scope.launch {
                                try {
                                    val report = if (activeSyncAction == SyncAction.Export) {
                                        services.exportSyncPackage(passphrase)
                                    } else {
                                        services.importSyncPackage(passphrase)
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

                if (showPlaylistLinkDialog) {
                    com.resonance.player.ui.PlaylistLinkDialog(
                        onDismiss = { showPlaylistLinkDialog = false },
                        onConfirm = { link ->
                            showPlaylistLinkDialog = false
                            operationInProgress = true
                            importMessage = "正在读取酷狗公开歌单…"
                            scope.launch {
                                try {
                                    val report = services.importPlaylistLink(link)
                                    val updated = playlists.filterNot { it.id == report.playlist.id } + report.playlist
                                    persistPlaylists(updated)
                                    importedTracks = tracksWithPlaylistArtwork(importedTracks, updated)
                                    selectPlaylist(report.playlist.id)
                                    destination = LibraryDestination.Library
                                    importMessage = report.message
                                } catch (error: Throwable) {
                                    importMessage = friendlyPlaylistError(error)
                                } finally {
                                    operationInProgress = false
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
                                        player = PlayerState()
                                    }
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
            }
        }
    }
}

internal fun resolveSelectedPlaylistId(requestedId: String?, playlists: List<Playlist>): String? =
    playlists.firstOrNull { it.id == requestedId }?.id ?: playlists.firstOrNull()?.id

internal fun importReportMessage(report: ImportReport, conversionRequested: Boolean): String? {
    val completed = report.tracks.size
    val notProcessed = report.skippedCount
    val notDeleted = report.cleanupWarnings.size
    val action = if (conversionRequested) "转换" else "导入"
    return when {
        completed > 0 && notProcessed > 0 -> buildString {
            append("处理完成：已${action} $completed 首，$notProcessed 首未处理")
            if (notDeleted > 0) append("；$notDeleted 个源 KGMA 未删除")
            append("。")
            report.warnings.firstOrNull()?.let(::append)
            if (conversionRequested) append(" 已存在的 MP3 不受影响。")
        }
        completed > 0 && notDeleted > 0 ->
            "转换完成：$completed 首 MP3 已保存；$notDeleted 个源 KGMA 因文件提供方未授予删除权限而保留。MP3 不受影响。"
        completed > 0 && report.warnings.isNotEmpty() ->
            "已${action} $completed 首；${report.warnings.first()}"
        completed > 0 -> "已${action} $completed 首音乐"
        report.warnings.isNotEmpty() -> report.warnings.first()
        report.cleanupWarnings.isNotEmpty() -> report.cleanupWarnings.first()
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
