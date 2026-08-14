package com.resonance.player.platform

import android.Manifest
import android.content.ContentUris
import android.content.ContentResolver
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.resonance.player.PlaybackService
import com.resonance.player.LanSyncLinkBus
import com.resonance.player.model.ImportReport
import com.resonance.player.model.DeleteTrackReport
import com.resonance.player.model.LanShareInfo
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Playlist
import com.resonance.player.model.PlaylistImportReport
import com.resonance.player.model.SyncReport
import com.resonance.player.model.Track
import com.resonance.player.sync.EncryptedSyncPackage
import com.resonance.player.conversion.KgmaCipher
import com.resonance.player.playlist.KugouPlaylistImporter
import com.resonance.player.conversion.NativeAudioFormat
import com.resonance.player.io.readUpTo
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.charset.StandardCharsets
import java.util.Properties
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AndroidPlatformServices(private val activity: ComponentActivity) : PlatformServices {
    private val controllerFuture = MediaController.Builder(
        activity,
        SessionToken(activity, ComponentName(activity, PlaybackService::class.java)),
    ).buildAsync()
    private var player: MediaController? = null
    private val pendingPlayerActions = mutableListOf<(MediaController) -> Unit>()
    private var pendingDocumentResult: ((Uri?) -> Unit)? = null
    private var pendingTreeResult: ((Uri?) -> Unit)? = null
    private var pendingDocumentsResult: ((List<Uri>) -> Unit)? = null
    private var pendingAudioPermissionResult: ((Boolean) -> Unit)? = null
    private var pendingNotificationPermissionResult: ((Boolean) -> Unit)? = null
    private var pendingDeleteConfirmationResult: ((Boolean) -> Unit)? = null
    private var pendingDeleteTrack: Track? = null
    private val openSyncDocument = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingDocumentResult?.invoke(uri)
        pendingDocumentResult = null
    }
    private val openMusicTree = activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        pendingTreeResult?.invoke(uri)
        pendingTreeResult = null
    }
    private val openKgmaDocuments = activity.registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        pendingDocumentsResult?.invoke(uris)
        pendingDocumentsResult = null
    }
    private val requestAudioPermission = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingAudioPermissionResult?.invoke(granted)
        pendingAudioPermissionResult = null
    }
    private val requestNotificationPermission = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingNotificationPermissionResult?.invoke(granted)
        pendingNotificationPermissionResult = null
    }
    private val requestDeleteConfirmation = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        pendingDeleteConfirmationResult?.invoke(result.resultCode == android.app.Activity.RESULT_OK)
        pendingDeleteConfirmationResult = null
    }
    private val requestLegacyDeletePermission = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val track = pendingDeleteTrack
        pendingDeleteTrack = null
        pendingDeleteConfirmationResult?.invoke(result.resultCode == android.app.Activity.RESULT_OK && track != null)
        pendingDeleteConfirmationResult = null
    }
    private var cachedTracks = emptyList<Track>()
    private val endedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val progressEvents = MutableSharedFlow<Float>(extraBufferCapacity = 1)
    private val trackEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val playbackEnded: Flow<Unit> = endedEvents
    override val playbackProgress: Flow<Float> = progressEvents
    override val activeTrackChanges: Flow<String> = trackEvents
    override val incomingLanLinks: Flow<String> = LanSyncLinkBus.links

    init {
        AndroidArtworkResolver.context = activity.applicationContext
        AndroidArtworkResolver.contentResolver = activity.contentResolver
        controllerFuture.addListener({
            val controller = runCatching(controllerFuture::get).getOrNull() ?: return@addListener
            player = controller
            controller.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) endedEvents.tryEmit(Unit)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.mediaId?.takeIf(String::isNotBlank)?.let(trackEvents::tryEmit)
                }
            })
            synchronized(pendingPlayerActions) {
                pendingPlayerActions.forEach { it(controller) }
                pendingPlayerActions.clear()
            }
        }, ContextCompat.getMainExecutor(activity))
        uiScope.launch {
            while (isActive) {
                delay(500)
                val activePlayer = player ?: continue
                val duration = activePlayer.duration
                if (duration > 0) {
                    progressEvents.tryEmit((activePlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
                }
            }
        }
    }

    override suspend fun loadLibrary(): List<Track> {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            cachedTracks = withContext(Dispatchers.IO) { loadSyncedTracks() }
            return cachedTracks
        }
        cachedTracks = withContext(Dispatchers.IO) {
            (scanMediaStore() + loadSyncedTracks()).associateBy(Track::id).values.toList()
        }
        return cachedTracks
    }

    override suspend fun loadPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val preferences = activity.getSharedPreferences("resonance_playlists", android.content.Context.MODE_PRIVATE)
        val tracksById = cachedTracks.associateBy(Track::id)
        val count = preferences.getInt("count", 0)
        (0 until count).mapNotNull { index ->
            val prefix = "playlist.$index."
            val id = preferences.getString(prefix + "id", null) ?: return@mapNotNull null
            val trackIds = preferences.getString(prefix + "tracks", "").orEmpty().split(',').filter(String::isNotBlank)
            Playlist(
                id = id,
                name = preferences.getString(prefix + "name", null) ?: "未命名歌单",
                subtitle = preferences.getString(prefix + "subtitle", null) ?: "${trackIds.size} 首",
                tracks = trackIds.mapIndexedNotNull { trackIndex, trackId ->
                    val snapshot = readCatalogTrack(preferences, prefix, trackIndex, trackId)
                    tracksById[trackId]?.let { local ->
                        local.copy(
                            artworkPath = local.artworkPath ?: snapshot?.artworkPath,
                            artworkSeed = if (local.artworkPath == null && snapshot != null) snapshot.artworkSeed else local.artworkSeed,
                        )
                    } ?: snapshot
                },
                artworkSeed = preferences.getInt(prefix + "seed", id.hashCode()),
            )
        }
    }

    override suspend fun savePlaylists(playlists: List<Playlist>): Unit = withContext(Dispatchers.IO) {
        val editor = activity.getSharedPreferences("resonance_playlists", android.content.Context.MODE_PRIVATE).edit().clear()
        editor.putInt("count", playlists.size)
        playlists.forEachIndexed { index, playlist ->
            val prefix = "playlist.$index."
            editor.putString(prefix + "id", playlist.id)
            editor.putString(prefix + "name", playlist.name)
            editor.putString(prefix + "subtitle", playlist.subtitle)
            editor.putInt(prefix + "seed", playlist.artworkSeed)
            editor.putString(prefix + "tracks", playlist.tracks.joinToString(",", transform = Track::id))
            playlist.tracks.forEachIndexed { trackIndex, track -> writeCatalogTrack(editor, prefix, trackIndex, track) }
        }
        editor.commit()
        Unit
    }

    override suspend fun exportSyncPackage(passphrase: String): SyncReport = withContext(Dispatchers.IO) {
        val playlists = loadPlaylists()
        val tracks = tracksWithPlaylistArtwork(loadLibrary(), playlists)
        val temporary = activity.cacheDir.toPath().resolve("Resonance-Sync.resonance")
        try {
            EncryptedSyncPackage().export(
                target = temporary,
                passphrase = passphrase,
                tracks = tracks,
                playlists = playlists,
                openAudio = { track -> openTrackSource(track.sourceUri) },
                openArtwork = { track -> openArtworkSource(track.artworkPath) },
            )
            val destination = saveToDownloads(temporary)
            SyncReport(true, "已导出到下载/Resonance：$destination", tracks.size, playlists.size)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    override suspend fun importSyncPackage(passphrase: String): SyncReport = withContext(Dispatchers.IO) {
        val temporary = activity.cacheDir.toPath().resolve("Resonance-Import.resonance")
        val sourceUri = pickSyncPackage() ?: return@withContext SyncReport(false, "已取消导入")
        val sourceName = sourceUri.lastPathSegment?.substringAfterLast('/') ?: "同步包"
        activity.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "无法读取所选同步包" }
            Files.newOutputStream(temporary).use(input::copyTo)
        }
        try {
            val managed = (activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: activity.filesDir)
                .toPath().resolve("Resonance")
            val imported = EncryptedSyncPackage().import(temporary, passphrase, managed)
            val existingPlaylists = loadPlaylists()
            val androidTracks = imported.tracks.map { track ->
                track.copy(sourceUri = Paths.get(track.sourceUri!!).toUri().toString())
            }
            val syncedTracks = (loadSyncedTracks() + androidTracks).associateBy(Track::id).values.toList()
            saveSyncedTracks(syncedTracks)
            cachedTracks = (cachedTracks + syncedTracks).associateBy(Track::id).values.toList()
            savePlaylists((existingPlaylists + imported.playlists).associateBy(Playlist::id).values.toList())
            SyncReport(true, "已导入 $sourceName", androidTracks.size, imported.playlists.size)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    override suspend fun startLanShare(): LanShareInfo =
        LanShareInfo(false, "请在 Windows 版开启局域网分享，然后用手机系统相机扫描二维码")

    override suspend fun importLanShare(link: String): SyncReport = withContext(Dispatchers.IO) {
        val deepLink = Uri.parse(link)
        require(deepLink.scheme == "resonance" && deepLink.host == "sync") { "无效的 Resonance 同步链接" }
        val sourceUrl = deepLink.getQueryParameter("url") ?: error("同步链接缺少下载地址")
        val passphrase = deepLink.getQueryParameter("key") ?: error("同步链接缺少密钥")
        val source = java.net.URI(sourceUrl)
        require(source.scheme == "http") { "局域网同步仅允许 HTTP 临时端点" }
        require(source.host.matches(Regex("(?:\\d{1,3}\\.){3}\\d{1,3}"))) { "同步地址必须是局域网 IPv4" }
        val address = java.net.InetAddress.getByName(source.host)
        require(address.isSiteLocalAddress || address.isLoopbackAddress) { "同步地址不在私有局域网" }

        val temporary = activity.cacheDir.toPath().resolve("Resonance-LAN-Import.resonance")
        try {
            val connection = source.toURL().openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 120_000
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            connection.connect()
            require(connection.responseCode == 200) { "Windows 端已过期、已被使用或拒绝连接（${connection.responseCode}）" }
            val usableBytes = activity.cacheDir.usableSpace.coerceAtLeast(0)
            val maximumBytes = (usableBytes - 128L * 1024 * 1024).coerceAtLeast(0)
            val declaredBytes = connection.contentLengthLong
            require(declaredBytes < 0 || declaredBytes <= maximumBytes) { "同步包超过设备可用空间" }
            connection.inputStream.use { input ->
                Files.newOutputStream(temporary).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= maximumBytes) { "同步包超过设备可用空间" }
                        output.write(buffer, 0, read)
                    }
                }
            }
            val managed = (activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: activity.filesDir)
                .toPath().resolve("Resonance")
            val imported = EncryptedSyncPackage().import(temporary, passphrase, managed)
            val androidTracks = imported.tracks.map { track -> track.copy(sourceUri = Paths.get(track.sourceUri!!).toUri().toString()) }
            val syncedTracks = (loadSyncedTracks() + androidTracks).associateBy(Track::id).values.toList()
            saveSyncedTracks(syncedTracks)
            cachedTracks = (cachedTracks + syncedTracks).associateBy(Track::id).values.toList()
            val mergedPlaylists = (loadPlaylists() + imported.playlists).associateBy(Playlist::id).values.toList()
            savePlaylists(mergedPlaylists)
            SyncReport(true, "局域网同步完成", androidTracks.size, imported.playlists.size)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    override fun stopLanShare() = Unit

    override suspend fun importMusic(convertToMp3: Boolean): ImportReport {
        if (convertToMp3) {
            val treeUri = pickMusicTree() ?: return ImportReport(emptyList())
            val report = withContext(Dispatchers.IO) { scanDocumentTree(treeUri) }
            val merged = (loadSyncedTracks() + report.tracks).associateBy(Track::id).values.toList()
            saveSyncedTracks(merged)
            cachedTracks = (cachedTracks + merged).associateBy(Track::id).values.toList()
            return report
        }
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            if (!requestAudioPermission(permission)) {
                return ImportReport(emptyList(), warnings = listOf("未获得音频读取权限；你仍可使用“选择文件夹”授权单个目录"))
            }
        }
        cachedTracks = withContext(Dispatchers.IO) { scanMediaStore() }
        return ImportReport(
            tracks = cachedTracks,
            warnings = emptyList(),
        )
    }

    override suspend fun importPlaylistLink(link: String): PlaylistImportReport = withContext(Dispatchers.IO) {
        val preferences = activity.getSharedPreferences("resonance_installation", android.content.Context.MODE_PRIVATE)
        val installationId = preferences.getString("id", null) ?: java.util.UUID.randomUUID().toString().also {
            preferences.edit().putString("id", it).commit()
        }
        KugouPlaylistImporter().import(link, cachedTracks, installationId)
    }

    override suspend fun deleteLocalTrack(track: Track): DeleteTrackReport = withContext(Dispatchers.IO) {
        val rawSource = track.sourceUri ?: return@withContext DeleteTrackReport(false, false, "这首歌曲没有本地 MP3")
        val source = Uri.parse(rawSource)
        val fileDeleted = when (source.scheme?.lowercase()) {
            "content" -> try {
                deleteContentUri(source)
            } catch (recoverable: android.app.RecoverableSecurityException) {
                requestLegacyDeleteConfirmation(track, recoverable.userAction.actionIntent.intentSender)
            }
            "file" -> {
                val path = source.path?.let(Paths::get) ?: error("本地文件路径无效")
                require(isAppManagedPath(path)) { "为保护原始音乐，只能删除 Resonance 转换或同步生成的 MP3" }
                Files.deleteIfExists(path)
            }
            null -> {
                val path = Paths.get(rawSource)
                require(isAppManagedPath(path)) { "为保护原始音乐，只能删除 Resonance 转换或同步生成的 MP3" }
                Files.deleteIfExists(path)
            }
            else -> error("不支持删除该存储位置")
        }
        val remainingSynced = loadSyncedTracks().filterNot { it.id == track.id }
        saveSyncedTracks(remainingSynced)
        cachedTracks = cachedTracks.filterNot { it.id == track.id }
        track.artworkPath?.takeIf { !it.startsWith("content://") && !it.startsWith("http") }?.let { artwork ->
            runCatching { Files.deleteIfExists(Paths.get(artwork)) }
        }
        DeleteTrackReport(true, fileDeleted, if (fileDeleted) "已删除「${track.title}」的本地 MP3" else "已从音乐库移除「${track.title}」")
    }

    private fun isAppManagedPath(path: Path): Boolean {
        val normalized = path.toAbsolutePath().normalize()
        val roots = listOfNotNull(
            activity.filesDir.toPath(),
            activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.toPath(),
            activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.toPath(),
        ).map { it.toAbsolutePath().normalize() }
        return roots.any(normalized::startsWith)
    }

    private suspend fun deleteContentUri(uri: Uri): Boolean {
        if (Build.VERSION.SDK_INT >= 30 && uri.authority?.contains("media", ignoreCase = true) == true) {
            val request = MediaStore.createDeleteRequest(activity.contentResolver, listOf(uri))
            val confirmed = requestDeleteConfirmation(request.intentSender)
            require(confirmed) { "已取消删除" }
            return true
        }
        val document = DocumentFile.fromSingleUri(activity, uri)
        if (document != null && document.canWrite()) {
            require(document.delete()) { "系统未能删除该音频" }
            return true
        }
        val deleted = activity.contentResolver.delete(uri, null, null)
        require(deleted > 0) { "系统未允许删除该音频；请重新授权文件夹后再试" }
        return true
    }

    private suspend fun requestDeleteConfirmation(intentSender: android.content.IntentSender): Boolean =
        suspendCancellableCoroutine { continuation ->
            check(pendingDeleteConfirmationResult == null) { "已有删除确认正在进行" }
            pendingDeleteConfirmationResult = { confirmed -> if (continuation.isActive) continuation.resume(confirmed) }
            continuation.invokeOnCancellation { pendingDeleteConfirmationResult = null }
            activity.runOnUiThread {
                requestDeleteConfirmation.launch(IntentSenderRequest.Builder(intentSender).build())
            }
        }

    private suspend fun requestLegacyDeleteConfirmation(track: Track, intentSender: android.content.IntentSender): Boolean =
        suspendCancellableCoroutine { continuation ->
            check(pendingDeleteConfirmationResult == null) { "已有删除确认正在进行" }
            pendingDeleteTrack = track
            pendingDeleteConfirmationResult = { confirmed -> if (continuation.isActive) continuation.resume(confirmed) }
            continuation.invokeOnCancellation {
                pendingDeleteTrack = null
                pendingDeleteConfirmationResult = null
            }
            activity.runOnUiThread {
                requestLegacyDeletePermission.launch(IntentSenderRequest.Builder(intentSender).build())
            }
        }

    private fun readCatalogTrack(
        preferences: android.content.SharedPreferences,
        playlistPrefix: String,
        trackIndex: Int,
        id: String,
    ): Track? {
        val prefix = "${playlistPrefix}catalog.$trackIndex."
        val title = preferences.getString(prefix + "title", null) ?: return null
        return Track(
            id = id,
            title = title,
            artist = preferences.getString(prefix + "artist", null) ?: "未知歌手",
            album = preferences.getString(prefix + "album", null) ?: "未知专辑",
            durationText = preferences.getString(prefix + "duration", null) ?: "0:00",
            artworkSeed = preferences.getInt(prefix + "seed", id.hashCode()),
            artworkPath = preferences.getString(prefix + "artwork", null)?.takeIf(String::isNotBlank),
            mimeType = preferences.getString(prefix + "mime", null) ?: KugouPlaylistImporter.CATALOG_MIME_TYPE,
        )
    }

    private fun writeCatalogTrack(
        editor: android.content.SharedPreferences.Editor,
        playlistPrefix: String,
        trackIndex: Int,
        track: Track,
    ) {
        val prefix = "${playlistPrefix}catalog.$trackIndex."
        editor.putString(prefix + "title", track.title)
        editor.putString(prefix + "artist", track.artist)
        editor.putString(prefix + "album", track.album)
        editor.putString(prefix + "duration", track.durationText)
        editor.putInt(prefix + "seed", track.artworkSeed)
        editor.putString(prefix + "artwork", track.artworkPath)
        editor.putString(prefix + "mime", track.mimeType)
    }

    override fun play(track: Track, queue: List<Track>, shuffle: Boolean, repeatMode: RepeatMode) {
        val uri = track.sourceUri ?: return
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        withPlayer { activePlayer ->
            val playableQueue = queue.filter { it.sourceUri != null }
            val items = playableQueue.map(::mediaItemFor)
            val index = playableQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            activePlayer.setMediaItems(items, index, 0L)
            applyPlaybackMode(activePlayer, shuffle, repeatMode)
            activePlayer.prepare()
            activePlayer.play()
        }
    }

    override fun setPlaying(isPlaying: Boolean) {
        withPlayer { if (isPlaying) it.play() else it.pause() }
    }

    override fun setPlaybackMode(shuffle: Boolean, repeatMode: RepeatMode) {
        withPlayer { applyPlaybackMode(it, shuffle, repeatMode) }
    }

    override fun seekTo(fraction: Float) {
        withPlayer { activePlayer ->
            val duration = activePlayer.duration
            if (duration > 0) activePlayer.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
        }
    }

    override fun close() {
        pendingDocumentResult?.invoke(null)
        pendingDocumentResult = null
        pendingTreeResult?.invoke(null)
        pendingTreeResult = null
        pendingDocumentsResult?.invoke(emptyList())
        pendingDocumentsResult = null
        pendingAudioPermissionResult?.invoke(false)
        pendingAudioPermissionResult = null
        pendingNotificationPermissionResult?.invoke(false)
        pendingNotificationPermissionResult = null
        pendingDeleteConfirmationResult?.invoke(false)
        pendingDeleteConfirmationResult = null
        pendingDeleteTrack = null
        uiScope.cancel()
        player = null
        MediaController.releaseFuture(controllerFuture)
    }

    private fun mediaItemFor(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .apply { track.artworkPath?.let { setArtworkUri(Uri.parse(it)) } }
            .build()
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(requireNotNull(track.sourceUri))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun applyPlaybackMode(player: MediaController, shuffle: Boolean, repeatMode: RepeatMode) {
        player.shuffleModeEnabled = shuffle
        player.repeatMode = when (repeatMode) {
            RepeatMode.Off -> Player.REPEAT_MODE_OFF
            RepeatMode.All -> Player.REPEAT_MODE_ALL
            RepeatMode.One -> Player.REPEAT_MODE_ONE
        }
    }

    private fun withPlayer(action: (MediaController) -> Unit) {
        val activePlayer = player
        if (activePlayer != null) {
            action(activePlayer)
        } else {
            synchronized(pendingPlayerActions) { pendingPlayerActions += action }
        }
    }

    private suspend fun pickSyncPackage(): Uri? = suspendCancellableCoroutine { continuation ->
        check(pendingDocumentResult == null) { "已有文件选择操作正在进行" }
        pendingDocumentResult = { uri -> if (continuation.isActive) continuation.resume(uri) }
        continuation.invokeOnCancellation { pendingDocumentResult = null }
        activity.runOnUiThread {
            // Several Android document providers label the custom .resonance
            // extension as */*; a narrow MIME filter made valid packages vanish.
            openSyncDocument.launch(arrayOf("application/octet-stream", "application/zip", "*/*"))
        }
    }

    private suspend fun requestAudioPermission(permission: String): Boolean = suspendCancellableCoroutine { continuation ->
        check(pendingAudioPermissionResult == null) { "已有权限请求正在进行" }
        pendingAudioPermissionResult = { granted -> if (continuation.isActive) continuation.resume(granted) }
        continuation.invokeOnCancellation { pendingAudioPermissionResult = null }
        activity.runOnUiThread { requestAudioPermission.launch(permission) }
    }

    private suspend fun pickMusicTree(): Uri? {
        val uri = suspendCancellableCoroutine { continuation ->
            check(pendingTreeResult == null) { "已有文件夹选择操作正在进行" }
            pendingTreeResult = { selected -> if (continuation.isActive) continuation.resume(selected) }
            continuation.invokeOnCancellation { pendingTreeResult = null }
            activity.runOnUiThread { openMusicTree.launch(null) }
        }
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { activity.contentResolver.takePersistableUriPermission(uri, flags) }
        }
        return uri
    }

    private suspend fun pickKgmaDocuments(): List<Uri> = suspendCancellableCoroutine { continuation ->
        check(pendingDocumentsResult == null) { "已有 KGMA 文件选择操作正在进行" }
        pendingDocumentsResult = { uris -> if (continuation.isActive) continuation.resume(uris) }
        continuation.invokeOnCancellation { pendingDocumentsResult = null }
        activity.runOnUiThread { openKgmaDocuments.launch(arrayOf("application/octet-stream", "audio/*", "*/*")) }
    }

    private fun convertSelectedKgmaDocuments(documents: List<Uri>): ImportReport {
        val tracks = mutableListOf<Track>()
        val warnings = mutableListOf<String>()
        val cleanupWarnings = mutableListOf<String>()
        var skipped = 0
        documents.distinct().forEachIndexed { index, uri ->
            val document = DocumentFile.fromSingleUri(activity, uri)
            val name = document?.name ?: uri.lastPathSegment ?: "KGMA 文件"
            runCatching {
                val readableDocument = requireNotNull(document) { "无法读取所选文件" }
                readableDocument to decryptKgmaMp3(readableDocument)
            }
                .onSuccess { converted ->
                    tracks += converted.second
                    deleteSourceDocument(converted.first).onFailure {
                        if (cleanupWarnings.size < 5) cleanupWarnings += "已转换 $name，但无法删除源 KGMA：${it.message.orEmpty()}"
                    }
                }
                .onFailure {
                    skipped++
                    if (warnings.size < 5) warnings += "转换失败 ${index + 1}/${documents.size} $name：${it.message.orEmpty()}"
                }
        }
        return ImportReport(
            tracks = tracks.sortedWith(compareBy(Track::artist, Track::title)),
            skippedCount = skipped,
            warnings = warnings,
            cleanupWarnings = cleanupWarnings,
        )
    }

    private fun scanDocumentTree(treeUri: Uri): ImportReport {
        val root = DocumentFile.fromTreeUri(activity, treeUri)
            ?: return ImportReport(emptyList(), warnings = listOf("无法读取所选文件夹"))
        val tracks = mutableListOf<Track>()
        val warnings = mutableListOf<String>()
        val cleanupWarnings = mutableListOf<String>()
        var skipped = 0
        val queue = ArrayDeque<DocumentFile>().apply { add(root) }
        val kgmaFiles = mutableListOf<DocumentFile>()
        val seenDocumentIds = hashSetOf<String>()
        var visited = 0
        while (queue.isNotEmpty()) {
            val document = queue.removeFirst()
            require(++visited <= MAX_DOCUMENTS) { "文件夹中的项目过多，请选择更小的目录" }
            if (document.isDirectory) {
                runCatching(document::listFiles)
                    .onSuccess { queue.addAll(it) }
                    .onFailure { if (warnings.size < 5) warnings += "无法读取目录 ${document.name.orEmpty()}" }
                continue
            }
            if (!document.isFile) continue
            if (!seenDocumentIds.add(document.uri.toString())) continue
            val extension = document.name.orEmpty().substringAfterLast('.', "").lowercase()
            when (extension) {
                "mp3" -> runCatching { readAndroidTrack(document.uri) }
                    .onSuccess(tracks::add)
                    .onFailure {
                        skipped++
                        if (warnings.size < 5) warnings += "无法读取 ${document.name}: ${it.message.orEmpty()}"
                    }
                "kgma" -> kgmaFiles += document
            }
        }
        kgmaFiles.forEachIndexed { index, document ->
            runCatching { decryptKgmaMp3(document) }
                .onSuccess { converted ->
                    tracks += converted
                    deleteSourceDocument(document).onFailure {
                        if (cleanupWarnings.size < 5) cleanupWarnings += "已转换 ${document.name}，但无法删除源 KGMA：${it.message.orEmpty()}"
                    }
                }
                .onFailure {
                    skipped++
                    if (warnings.size < 5) warnings += "转换失败 ${index + 1}/${kgmaFiles.size} ${document.name}: ${it.message.orEmpty()}"
                }
        }
        return ImportReport(
            tracks = tracks.sortedWith(compareBy(Track::artist, Track::title)),
            skippedCount = skipped,
            warnings = warnings,
            cleanupWarnings = cleanupWarnings,
        )
    }

    private fun deleteSourceDocument(document: DocumentFile): Result<Unit> = runCatching {
        require(document.canWrite()) { "当前文件夹只有读取权限" }
        require(document.delete()) { "系统拒绝删除" }
    }

    private fun readAndroidTrack(uri: Uri): Track {
        val id = sha256Content(uri)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(activity, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf(String::isNotBlank) ?: "未知歌曲"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf(String::isNotBlank) ?: "未知歌手"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf(String::isNotBlank) ?: "未知专辑"
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0) ?: 0
            val artworkPath = retriever.embeddedPicture?.takeIf(ByteArray::isNotEmpty)?.let { bytes ->
                val directory = activity.filesDir.toPath().resolve("artwork")
                Files.createDirectories(directory)
                directory.resolve("$id.cover").also { Files.write(it, bytes) }.toString()
            }
            return Track(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationText = "%d:%02d".format(durationMs / 60_000, durationMs / 1_000 % 60),
                artworkSeed = id.take(8).toLong(16).toInt(),
                sourceUri = uri.toString(),
                artworkPath = artworkPath,
                mimeType = if (uri.scheme == "file") "audio/mpeg" else activity.contentResolver.getType(uri).orEmpty().ifBlank { "audio/mpeg" },
            )
        } finally {
            retriever.release()
        }
    }

    private fun decryptKgmaMp3(document: DocumentFile): Track {
        val managed = (activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: activity.filesDir)
            .toPath().resolve("Resonance")
        Files.createDirectories(managed)
        val header = ByteArray(KGMA_MINIMUM_HEADER)
        activity.contentResolver.openInputStream(document.uri).use { input ->
            requireNotNull(input) { "无法读取 KGMA" }
            readFully(input, header)
        }
        require(header.copyOfRange(0, KgmaCipher.header.size).contentEquals(KgmaCipher.header)) { "不是有效的 KGMA 文件" }
        val audioOffset = KgmaCipher.littleEndianInt(header, 0x10)
        require(KgmaCipher.littleEndianInt(header, 0x14) == 3) { "仅支持 KGMA v3" }
        require(KgmaCipher.littleEndianInt(header, 0x18) == 1) { "仅支持 KGMA slot 1" }
        require(audioOffset >= KGMA_MINIMUM_HEADER) { "KGMA 音频偏移无效" }
        val boxes = KgmaCipher.createBoxes(header.copyOfRange(0x2c, 0x3c)) { value ->
            MessageDigest.getInstance("MD5").digest(value)
        }
        val temporary = Files.createTempFile(managed, ".kgma-", ".part")
        try {
            activity.contentResolver.openInputStream(document.uri).use { input ->
                requireNotNull(input) { "无法读取 KGMA" }
                skipFully(input, audioOffset.toLong())
                Files.newOutputStream(temporary).buffered().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var position = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        KgmaCipher.decryptInPlace(buffer, read, position, boxes)
                        output.write(buffer, 0, read)
                        position += read
                    }
                }
            }
            val prefix = Files.newInputStream(temporary).use { it.readUpTo(16) }
            val nativeFormat = KgmaCipher.detectAudioFormat(prefix)
            require(nativeFormat != NativeAudioFormat.Unknown) { "KGMA 解密后不是可识别的音频格式" }
            val mp3Staging = if (nativeFormat == NativeAudioFormat.Mp3) {
                temporary
            } else {
                val output = Files.createTempFile(managed, ".kgma-transcode-", ".mp3")
                val session = FFmpegKit.executeWithArguments(
                    arrayOf(
                        "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                        "-i", temporary.toString(),
                        "-map", "0:a:0", "-map", "0:v?", "-map_metadata", "0",
                        "-c:a", "libmp3lame", "-b:a", "320k",
                        "-c:v", "copy", "-id3v2_version", "3", "-write_id3v1", "1",
                        "-f", "mp3", output.toString(),
                    ),
                )
                if (!ReturnCode.isSuccess(session.returnCode) || !Files.isRegularFile(output)) {
                    Files.deleteIfExists(output)
                    error("FFmpeg 高音质转换失败：${session.failStackTrace.orEmpty().takeLast(300)}")
                }
                output
            }
            val id = sha256File(mp3Staging)
            val target = managed.resolve("$id.mp3")
            Files.move(mp3Staging, target, StandardCopyOption.REPLACE_EXISTING)
            return readAndroidTrack(Uri.fromFile(target.toFile()))
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun readFully(input: java.io.InputStream, target: ByteArray) {
        var offset = 0
        while (offset < target.size) {
            val read = input.read(target, offset, target.size - offset)
            require(read >= 0) { "KGMA 文件头不完整" }
            offset += read
        }
    }

    private fun skipFully(input: java.io.InputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) remaining -= skipped
            else {
                require(input.read() >= 0) { "KGMA 音频数据不完整" }
                remaining--
            }
        }
    }

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

    private fun saveToDownloads(source: Path): String {
        val name = "Resonance-Sync-${System.currentTimeMillis()}.resonance"
        if (Build.VERSION.SDK_INT >= 29) {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Resonance")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = activity.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("无法创建下载文件")
            try {
                activity.contentResolver.openOutputStream(uri, "w").use { output ->
                    requireNotNull(output) { "无法写入下载文件" }
                    Files.newInputStream(source).use { it.copyTo(output) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                activity.contentResolver.update(uri, values, null, null)
            } catch (error: Throwable) {
                activity.contentResolver.delete(uri, null, null)
                throw error
            }
            return name
        }

        val directory = (activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: activity.filesDir)
            .toPath().resolve("Resonance")
        Files.createDirectories(directory)
        Files.copy(source, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING)
        return name
    }

    private fun openTrackSource(value: String?): java.io.InputStream? {
        value ?: return null
        return if (value.startsWith("content://")) {
            activity.contentResolver.openInputStream(android.net.Uri.parse(value))
        } else {
            val uri = android.net.Uri.parse(value)
            val path = if (uri.scheme == "file") Paths.get(uri.path!!) else Paths.get(value)
            if (Files.isRegularFile(path)) Files.newInputStream(path) else null
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
        return openTrackSource(value)
    }

    private fun loadSyncedTracks(): List<Track> {
        val index = activity.filesDir.toPath().resolve("synced-library.properties")
        if (!Files.isRegularFile(index)) return emptyList()
        val properties = Properties()
        Files.newBufferedReader(index, StandardCharsets.UTF_8).use(properties::load)
        val count = properties.getProperty("count")?.toIntOrNull() ?: return emptyList()
        return (0 until count).mapNotNull { indexValue ->
            val prefix = "track.$indexValue."
            val source = properties.getProperty(prefix + "source") ?: return@mapNotNull null
            val uri = android.net.Uri.parse(source)
            val path = uri.path?.let(Paths::get)
            if (uri.scheme == "file" && (path == null || !Files.isRegularFile(path))) return@mapNotNull null
            if (uri.scheme == "content" && !contentUriExists(uri)) return@mapNotNull null
            Track(
                id = properties.getProperty(prefix + "id") ?: return@mapNotNull null,
                title = properties.getProperty(prefix + "title") ?: "未知歌曲",
                artist = properties.getProperty(prefix + "artist") ?: "未知歌手",
                album = properties.getProperty(prefix + "album") ?: "未知专辑",
                durationText = properties.getProperty(prefix + "duration") ?: "0:00",
                artworkSeed = properties.getProperty(prefix + "seed")?.toIntOrNull() ?: 0,
                isFavorite = properties.getProperty(prefix + "favorite").toBoolean(),
                sourceUri = source,
                artworkPath = properties.getProperty(prefix + "artwork")?.takeIf(String::isNotBlank),
                mimeType = properties.getProperty(prefix + "mime") ?: "audio/mpeg",
            )
        }
    }

    private fun contentUriExists(uri: Uri): Boolean = runCatching {
        activity.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    }.getOrDefault(false)

    private fun saveSyncedTracks(tracks: List<Track>) {
        val index = activity.filesDir.toPath().resolve("synced-library.properties")
        val properties = Properties().apply { setProperty("count", tracks.size.toString()) }
        tracks.forEachIndexed { indexValue, track ->
            val prefix = "track.$indexValue."
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
        val temporary = index.resolveSibling("${index.fileName}.tmp")
        Files.newBufferedWriter(temporary, StandardCharsets.UTF_8).use { properties.store(it, "Resonance synced library") }
        Files.move(temporary, index, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun scanMediaStore(): List<Track> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val tracks = mutableListOf<Track>()
        activity.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val hashCache = activity.getSharedPreferences("resonance_audio_hashes", android.content.Context.MODE_PRIVATE)
            val hashEditor = hashCache.edit()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val durationMs = cursor.getLong(durationColumn).coerceAtLeast(0)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val cacheKey = "$id:${cursor.getLong(sizeColumn)}:${cursor.getLong(modifiedColumn)}"
                val stableId = hashCache.getString(cacheKey, null) ?: sha256Content(uri).also {
                    hashEditor.putString(cacheKey, it)
                }
                val albumArtwork = ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()
                AndroidArtworkResolver.audioArtwork[albumArtwork] = uri.toString()
                tracks += Track(
                    id = stableId,
                    title = cursor.getString(titleColumn)?.takeIf(String::isNotBlank) ?: "未知歌曲",
                    artist = cursor.getString(artistColumn)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "未知歌手",
                    album = cursor.getString(albumColumn)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "未知专辑",
                    durationText = "%d:%02d".format(durationMs / 60_000, durationMs / 1_000 % 60),
                    artworkSeed = id.toInt(),
                    sourceUri = uri.toString(),
                    artworkPath = albumArtwork,
                    mimeType = cursor.getString(mimeColumn)?.takeIf(String::isNotBlank) ?: "audio/mpeg",
                )
            }
            hashEditor.apply()
        }
        return tracks
    }

    private fun sha256Content(uri: android.net.Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        activity.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取音频：$uri" }
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MAX_DOCUMENTS = 100_000
        const val KGMA_MINIMUM_HEADER = 0x3c
        val ALBUM_ART_URI = android.net.Uri.parse("content://media/external/audio/albumart")
    }
}

internal object AndroidArtworkResolver {
    lateinit var context: android.content.Context
    lateinit var contentResolver: ContentResolver
    val audioArtwork = java.util.concurrent.ConcurrentHashMap<String, String>()
}
