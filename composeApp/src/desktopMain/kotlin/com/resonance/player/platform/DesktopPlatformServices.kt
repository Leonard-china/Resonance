package com.resonance.player.platform

import com.resonance.player.ai.DeepSeekClient
import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.DeepSeekTestResult
import com.resonance.player.model.ImportReport
import com.resonance.player.model.DeleteTrackReport
import com.resonance.player.model.Playlist
import com.resonance.player.model.PlaylistImportReport
import com.resonance.player.model.SyncReport
import com.resonance.player.model.LanShareInfo
import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Track
import com.resonance.player.lyrics.LrclibLyricsRepository
import com.resonance.player.conversion.DesktopManagedMp3Converter
import com.resonance.player.playlist.KugouPlaylistImporter
import com.resonance.player.sync.EncryptedSyncPackage
import com.resonance.player.sync.PlainSyncPackage
import com.resonance.player.sync.DesktopLanShare
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

class DesktopPlatformServices : PlatformServices {
    private val library = DesktopLibraryStore()
    private val lyricsRepository = LrclibLyricsRepository(library.lyricsCacheDirectory)
    private val deepSeekClient = DeepSeekClient()
    private val endedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val progressEvents = MutableSharedFlow<Float>(extraBufferCapacity = 1)
    private val isPlayingEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    private val player = DesktopAudioPlayer(
        onEnded = {
            isPlayingEvents.tryEmit(false)
            endedEvents.tryEmit(Unit)
        },
        onProgress = { progressEvents.tryEmit(it) },
        onPlayingChanged = { isPlayingEvents.tryEmit(it) },
    )
    private var lanShare: DesktopLanShare? = null

    override val playbackEnded: Flow<Unit> = endedEvents
    override val playbackProgress: Flow<Float> = progressEvents
    override val isPlayingChanges: Flow<Boolean> = isPlayingEvents
    override val activeTrackChanges: Flow<String> = emptyFlow()
    override val incomingLanLinks: Flow<String> = emptyFlow()

    override suspend fun loadLibrary(): List<Track> = withContext(Dispatchers.IO) { library.load() }

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

    override val libraryLocation: String
        get() = library.managedMusicDirectory.toString()

    override suspend fun loadDeepSeekConfig(): DeepSeekConfig = withContext(Dispatchers.IO) {
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
            aiFallback = { reqTrack ->
                val config = library.loadDeepSeekConfig()
                if (config.isConfigured && config.enabled) {
                    deepSeekClient.generateLyrics(config, reqTrack)
                } else null
            },
        )

    override suspend fun requestAiLyrics(track: Track): LyricsFetchResult = withContext(Dispatchers.IO) {
        val config = library.loadDeepSeekConfig()
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
        SyncReport(true, "同步包导入完成", imported.tracks.size, imported.playlists.size)
    }

    override suspend fun startLanShare(): LanShareInfo = withContext(Dispatchers.IO) {
        stopLanShare()
        val playlists = library.loadPlaylists()
        val tracks = tracksWithPlaylistArtwork(library.load(), playlists)
        if (tracks.isEmpty()) return@withContext LanShareInfo(false, "音乐库为空，无法开始同步")
        val passphrase = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(32).also(java.security.SecureRandom()::nextBytes))
        val shareDirectory = library.managedMusicDirectory.parent
        Files.createDirectories(shareDirectory)
        val temporary = Files.createTempFile(shareDirectory, ".resonance-lan-", ".resonance")
        try {
            EncryptedSyncPackage().export(
                temporary,
                passphrase,
                tracks,
                playlists,
                { track -> track.sourceUri?.let(Path::of)?.takeIf(Files::isRegularFile)?.let(Files::newInputStream) },
                { track -> openArtworkSource(track.artworkPath) },
            )
            val share = DesktopLanShare(temporary, passphrase)
            val qr = share.writeQr(shareDirectory.resolve("Resonance-LAN-QR.png"))
            lanShare = share
            LanShareInfo(true, "局域网分享已开启；扫码后会自动关闭", qr.toString(), "10 分钟")
        } catch (error: Throwable) {
            Files.deleteIfExists(temporary)
            throw error
        }
    }

    override suspend fun importLanShare(link: String): SyncReport =
        SyncReport(false, "Windows 版当前作为局域网发送端")

    override fun stopLanShare() {
        lanShare?.close()
        lanShare = null
    }

    override suspend fun importMusic(convertToMp3: Boolean): ImportReport = withContext(Dispatchers.IO) {
        val folder = chooseFolderOnEdt() ?: return@withContext ImportReport(emptyList())
        run {
            val report = DesktopMusicScanner(
                artworkDirectory = library.artworkDirectory,
                managedMusicDirectory = library.managedMusicDirectory,
            ).scan(folder, convertToMp3)
            if (report.tracks.isNotEmpty()) library.merge(report.tracks)
            report
        }
    }

    override suspend fun importPlaylistLink(link: String): PlaylistImportReport = withContext(Dispatchers.IO) {
        KugouPlaylistImporter().import(link, library.load(), library.installationId())
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
        val config = library.loadDeepSeekConfig()
        val customFolder = library.loadCustomLyricsFolder()
        val report = enricher.batchEnrich(options, tracks, config, customFolder, onProgress)
        if (report.updatedTracks.isNotEmpty()) {
            library.updateTracks(report.updatedTracks)
        }
        report
    }

    override suspend fun deleteLocalTrack(track: Track): DeleteTrackReport = withContext(Dispatchers.IO) {
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
        isPlayingEvents.tryEmit(true)
        player.play(track)
    }

    override fun setPlaying(isPlaying: Boolean) {
        isPlayingEvents.tryEmit(isPlaying)
        player.setPlaying(isPlaying)
    }

    override fun setPlaybackMode(shuffle: Boolean, repeatMode: RepeatMode) = Unit

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
        stopLanShare()
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
                dialogTitle = if (save) "导出同步包" else "导入同步包"
                fileSelectionMode = JFileChooser.FILES_ONLY
                isAcceptAllFileFilterUsed = false
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Resonance 同步包 (*.resonance)", "resonance")
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

    fun scan(root: Path, convertToMp3: Boolean): ImportReport {
        val tracks = mutableListOf<Track>()
        val warnings = mutableListOf<String>()
        var skipped = 0

        Files.walk(root).use { paths ->
            paths.filter(Files::isRegularFile).forEach { path ->
                val extension = path.fileName.toString().substringAfterLast('.', "").lowercase()
                if (extension !in audioExtensions) return@forEach
                val importPath = if (convertToMp3 || extension in listOf("kgma", "kgm", "kgg")) {
                    runCatching { converter.importAsMp3(path) }
                        .onFailure {
                            skipped += 1
                            if (warnings.size < 5) warnings += "转换失败 ${path.fileName}: ${it.message.orEmpty()}"
                        }
                        .getOrNull() ?: return@forEach
                } else {
                    path
                }
                runCatching { readAudioTrack(importPath) }
                    .onSuccess { track ->
                        tracks += track
                    }
                    .onFailure {
                        skipped += 1
                        if (warnings.size < 5) warnings += "无法读取 ${path.fileName}: ${it.message.orEmpty()}"
                    }
            }
        }

        return ImportReport(
            tracks = tracks.sortedWith(compareBy(Track::artist, Track::title)),
            skippedCount = skipped,
            warnings = warnings,
        )
    }

    private fun readAudioTrack(path: Path): Track {
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

internal class DesktopLibraryStore {
    private val appDirectory: Path = Path.of(
        System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"),
        "Resonance",
    )
    private val libraryFile = appDirectory.resolve("library.properties")
    private val playlistFile = appDirectory.resolve("playlists.properties")
    private val uiStateFile = appDirectory.resolve("ui-state.properties")
    val artworkDirectory: Path = appDirectory.resolve("artwork")
    val lyricsCacheDirectory: Path = appDirectory.resolve("lyrics-cache")
    val managedMusicDirectory: Path = preferredManagedMusicDirectory()

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

    fun load(): List<Track> {
        if (!Files.isRegularFile(libraryFile)) return emptyList()
        val properties = Properties()
        Files.newBufferedReader(libraryFile, StandardCharsets.UTF_8).use(properties::load)
        val count = properties.getProperty("count")?.toIntOrNull() ?: return emptyList()
        val usesContentIds = properties.getProperty("id.format") == "sha256-audio-v1"
        return (0 until count).mapNotNull { index ->
            val prefix = "track.$index."
            val source = properties.getProperty(prefix + "source") ?: return@mapNotNull null
            if (!Files.isRegularFile(Path.of(source))) return@mapNotNull null
            Track(
                id = if (usesContentIds) {
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
                sourceUri = source,
                artworkPath = properties.getProperty(prefix + "artwork")?.takeIf(String::isNotBlank),
                mimeType = properties.getProperty(prefix + "mime") ?: "audio/mpeg",
            )
        }.associateBy(Track::id).values.toList()
    }

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
        val temporary = libraryFile.resolveSibling("${libraryFile.fileName}.tmp")
        Files.newBufferedWriter(temporary, StandardCharsets.UTF_8).use { properties.store(it, "Resonance music library") }
        runCatching {
            Files.move(temporary, libraryFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(temporary, libraryFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun merge(imported: List<Track>) {
        val merged = (load() + imported).associateBy(Track::id).values.toList()
        save(merged)
    }

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
                            artworkPath = u.artworkPath ?: track.artworkPath,
                            artworkSeed = if (u.artworkPath != null) u.artworkSeed else track.artworkSeed,
                        )
                    } ?: track
                }
            )
        }
        savePlaylists(currentPlaylists)
    }

    fun removeMultiple(trackIds: Set<String>) {
        save(load().filterNot { it.id in trackIds })
        savePlaylists(loadPlaylists().map { playlist -> playlist.copy(tracks = playlist.tracks.filterNot { it.id in trackIds }) })
    }

    fun remove(trackId: String) {
        removeMultiple(setOf(trackId))
    }

    fun setFavorite(trackId: String, favorite: Boolean) {
        save(load().map { track -> if (track.id == trackId) track.copy(isFavorite = favorite) else track })
        savePlaylists(loadPlaylists().map { playlist ->
            playlist.copy(tracks = playlist.tracks.map { track ->
                if (track.id == trackId) track.copy(isFavorite = favorite) else track
            })
        })
    }

    fun loadPlaylists(): List<Playlist> {
        if (!Files.isRegularFile(playlistFile)) return emptyList()
        val properties = Properties()
        Files.newBufferedReader(playlistFile, StandardCharsets.UTF_8).use(properties::load)
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

    fun loadLastSelectedPlaylistId(): String? {
        if (!Files.isRegularFile(uiStateFile)) return null
        val properties = Properties()
        Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(properties::load)
        return properties.getProperty("library.selectedPlaylistId")?.takeIf(String::isNotBlank)
    }

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

    fun loadThemeMode(): com.resonance.player.model.ThemeMode {
        if (!Files.isRegularFile(uiStateFile)) return com.resonance.player.model.ThemeMode.Dark
        val properties = Properties()
        Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(properties::load)
        return when (properties.getProperty("app.themeMode")) {
            "Light" -> com.resonance.player.model.ThemeMode.Light
            "System" -> com.resonance.player.model.ThemeMode.System
            else -> com.resonance.player.model.ThemeMode.Dark
        }
    }

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

    fun loadCustomLyricsFolder(): String? {
        if (!Files.isRegularFile(uiStateFile)) return null
        val properties = Properties()
        Files.newBufferedReader(uiStateFile, StandardCharsets.UTF_8).use(properties::load)
        return properties.getProperty("lyrics.customFolder")?.takeIf(String::isNotBlank)
    }

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
            mimeType = properties.getProperty(prefix + "mime") ?: KugouPlaylistImporter.CATALOG_MIME_TYPE,
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
        val temporary = target.resolveSibling("${target.fileName}.tmp")
        Files.newBufferedWriter(temporary, StandardCharsets.UTF_8).use { properties.store(it, comment) }
        runCatching {
            Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun preferredManagedMusicDirectory(): Path {
        System.getenv("RESONANCE_LIBRARY")?.takeIf(String::isNotBlank)?.let { return Path.of(it) }
        val dDrive = Path.of("D:\\")
        return if (Files.isDirectory(dDrive)) dDrive.resolve("Music").resolve("Resonance")
        else Path.of(System.getProperty("user.home"), "Music", "Resonance")
    }
}

private class DesktopAudioPlayer(
    private val onEnded: () -> Unit,
    private val onProgress: (Float) -> Unit,
    private val onPlayingChanged: (Boolean) -> Unit,
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentVolume: Double = 1.0
    private var currentRate: Double = 1.0

    init { ensureJavaFx() }

    fun play(track: Track) {
        val source = track.sourceUri ?: return
        Platform.runLater {
            mediaPlayer?.dispose()
            val uriString = runCatching {
                if (source.startsWith("file:/")) java.net.URI(source).toString()
                else if (source.startsWith("http://") || source.startsWith("https://")) source
                else Path.of(source).toUri().toString()
            }.getOrElse { Path.of(source).toUri().toString() }
            mediaPlayer = MediaPlayer(Media(uriString)).also { activePlayer ->
                activePlayer.volume = currentVolume
                activePlayer.rate = currentRate
                activePlayer.setOnEndOfMedia(onEnded)
                activePlayer.currentTimeProperty().addListener { _, _, current ->
                    val duration = activePlayer.totalDuration.toMillis()
                    if (duration.isFinite() && duration > 0) {
                        onProgress((current.toMillis() / duration).toFloat().coerceIn(0f, 1f))
                    }
                }
                activePlayer.statusProperty().addListener { _, _, newStatus ->
                    onPlayingChanged(newStatus == MediaPlayer.Status.PLAYING)
                }
                activePlayer.play()
            }
        }
    }

    fun setPlaying(isPlaying: Boolean) {
        Platform.runLater { mediaPlayer?.let { if (isPlaying) it.play() else it.pause() } }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.toDouble().coerceIn(0.0, 1.0)
        Platform.runLater { mediaPlayer?.volume = currentVolume }
    }

    fun setPlaybackSpeed(speed: Float) {
        currentRate = speed.toDouble().coerceIn(0.25, 3.0)
        Platform.runLater { mediaPlayer?.rate = currentRate }
    }

    fun seekTo(fraction: Float) {
        Platform.runLater {
            mediaPlayer?.let { activePlayer ->
                val duration = activePlayer.totalDuration.toMillis()
                if (duration.isFinite() && duration > 0) {
                    activePlayer.seek(javafx.util.Duration.millis(duration * fraction.coerceIn(0f, 1f)))
                }
            }
        }
    }

    fun close() {
        Platform.runLater { mediaPlayer?.dispose(); mediaPlayer = null }
    }

    companion object {
        private val started = AtomicBoolean(false)
        private fun ensureJavaFx() {
            if (started.compareAndSet(false, true)) Platform.startup {}
        }
    }
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
