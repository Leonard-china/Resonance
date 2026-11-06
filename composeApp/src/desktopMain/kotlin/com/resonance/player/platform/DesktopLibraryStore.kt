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

internal class DesktopLibraryStore(
    val appDirectory: Path = DesktopPaths.dataDirectory(),
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
            Track(
                // Availability is playback state, never permission to discard a catalog entry.
                id = properties.getProperty(prefix + "id")?.takeIf { usesContentIds || source.isBlank() }
                    ?: runCatching { sha256File(Path.of(source)) }.getOrElse {
                        properties.getProperty(prefix + "id")?.takeIf(String::isNotBlank)
                            ?: "legacy-path-" + MessageDigest.getInstance("SHA-256")
                                .digest(source.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
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

    @Synchronized
    fun mergeImported(tracks: List<Track>, playlists: List<Playlist>) {
        val files = listOf(libraryFile, playlistFile).flatMap { listOf(it, it.resolveSibling("${it.fileName}.bak")) }
        com.resonance.player.io.FileRollback.run(files) {
            if (tracks.isNotEmpty()) merge(tracks)
            if (playlists.isNotEmpty()) savePlaylists((loadPlaylists() + playlists).associateBy(Playlist::id).values.toList())
        }
    }

    /** Relinking preserves the original identity, favorite and playlist snapshots. */
    @Synchronized
    fun replaceAudio(track: Track) {
        save(load().filterNot { it.id == track.id } + track)
        savePlaylists(loadPlaylists().map { playlist -> playlist.copy(tracks = playlist.tracks.map { if (it.id == track.id) track else it }) })
    }

    /** Source-only repair: playlist snapshots resolve sources through library IDs. */
    @Synchronized
    fun replaceAudioBatch(matches: List<AudioRelinkMatch>, checkActive: () -> Unit = {}): Pair<Int, List<String>> {
        val tracks = load()
        val (updates, errors) = prepareAudioRelinkUpdates(matches, tracks, available = { track ->
            runCatching {
                val source = track.sourceUri ?: return@runCatching false
                val path = if (source.startsWith("file:")) Path.of(java.net.URI(source)) else Path.of(source)
                Files.isRegularFile(path) && Files.isReadable(path)
            }.getOrDefault(false)
        }, checkActive = checkActive)
        if (updates.isNotEmpty()) {
            val byId = updates.associateBy(Track::id)
            checkActive()
            save(tracks.map { byId[it.id] ?: it })
        }
        return updates.size to errors
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
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
        return properties.getProperty("library.selectedPlaylistId")?.takeIf(String::isNotBlank)
    }

    @Synchronized
    fun saveLastSelectedPlaylistId(playlistId: String?) {
        Files.createDirectories(appDirectory)
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
        if (playlistId.isNullOrBlank()) {
            properties.remove("library.selectedPlaylistId")
        } else {
            properties.setProperty("library.selectedPlaylistId", playlistId)
        }
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }

    @Synchronized
    fun loadThemeMode(): com.resonance.player.model.ThemeMode {
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
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
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
        properties.setProperty("app.themeMode", mode.name)
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }

    @Synchronized
    fun loadPlaybackMode(): PlaybackModePreference {
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
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
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
        properties.setProperty("playback.shuffleEnabled", shuffle.toString())
        properties.setProperty("playback.repeatMode", repeatMode.name)
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }

    @Synchronized
    fun loadDeepSeekConfig(): DeepSeekConfig {
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
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
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
        properties.setProperty("deepseek.apiKey", config.apiKey)
        properties.setProperty("deepseek.baseUrl", config.baseUrl)
        properties.setProperty("deepseek.model", config.model)
        properties.setProperty("deepseek.enabled", config.enabled.toString())
        writePropertiesAtomically(uiStateFile, properties, "Resonance UI state")
    }

    @Synchronized
    fun loadCustomLyricsFolder(): String? {
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
        return properties.getProperty("lyrics.customFolder")?.takeIf(String::isNotBlank)
    }

    @Synchronized
    fun saveCustomLyricsFolder(folder: String?) {
        Files.createDirectories(appDirectory)
        val properties = com.resonance.player.io.AtomicPropertiesFile.read(uiStateFile)
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
        return if (System.getProperty("os.name").startsWith("Windows", true) && Files.isDirectory(dDrive)) dDrive.resolve("Music").resolve("Resonance")
        else Path.of(System.getProperty("user.home"), "Music", "Resonance")
    }
}

