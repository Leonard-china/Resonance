package com.resonance.player.features

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import com.resonance.player.io.AtomicPropertiesFile
import com.resonance.player.media.Mp3Import
import com.resonance.player.model.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties
import kotlin.coroutines.resume

class AndroidMusicAccess(private val context: Context) : MusicLibraryAccess {
    private val root = context.filesDir.toPath()
    private val json = MusicJsonStore(root.resolve("music-assistant"))
    val musicDirectory: Path = (context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir).toPath().resolve("Resonance")
    override suspend fun tracks(): List<Track> = tracksNow()
    override suspend fun automaticDownloadsAllowed(unmeteredOnly: Boolean): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) && (!unmeteredOnly || !manager.isActiveNetworkMetered)
    }
    override suspend fun audioAvailable(track: Track): Boolean = withContext(Dispatchers.IO) {
        val source = track.sourceUri ?: return@withContext false
        try {
            if (source.startsWith("content:")) context.contentResolver.openAssetFileDescriptor(Uri.parse(source), "r")?.use { true } ?: false
            else java.io.File(if (source.startsWith("file:")) java.net.URI(source).path else source).isFile
        } catch (_: Exception) { false }
    }
    private fun tracksNow(): List<Track> = synchronized(lock(context)) {
        val managed = readManaged(context)
        val removed = json.read().optJSONArray("removedIds")?.let { a -> (0 until a.length()).map(a::getString).toSet() }.orEmpty()
        val favorites = context.getSharedPreferences("resonance_favorites", Context.MODE_PRIVATE).getStringSet("ids", emptySet()).orEmpty()
        (MusicFeatureCodec.tracks(json.read().optJSONArray("librarySnapshot")) + managed).associateBy(Track::id).values.filterNot { it.id in removed }
            .map { it.copy(isFavorite = it.id in favorites) }
    }
    override suspend fun playlists(): List<Playlist> = playlistsNow()
    private fun playlistsNow(): List<Playlist> = synchronized(lock(context)) {
        val current = context.getSharedPreferences("resonance_playlists", Context.MODE_PRIVATE)
        val backup = context.getSharedPreferences("resonance_playlists_backup", Context.MODE_PRIVATE)
        val p = if (current.contains("count")) current else backup
        val tracks = tracksNow().associateBy(Track::id)
        (0 until p.getInt("count", 0).coerceIn(0, 100_000)).mapNotNull { index ->
            val prefix = "playlist.$index."
            val id = p.getString(prefix + "id", null) ?: return@mapNotNull null
            val ids = p.getString(prefix + "tracks", "").orEmpty().split(',').filter(String::isNotBlank)
            Playlist(id, p.getString(prefix + "name", "未命名歌单").orEmpty(), p.getString(prefix + "subtitle", "").orEmpty(), ids.mapIndexedNotNull { n, trackId ->
                tracks[trackId] ?: p.getString(prefix + "catalog.$n.title", null)?.let { title ->
                    Track(trackId, title, p.getString(prefix + "catalog.$n.artist", "未知歌手").orEmpty(), p.getString(prefix + "catalog.$n.album", "未知专辑").orEmpty(), p.getString(prefix + "catalog.$n.duration", "0:00").orEmpty(), p.getInt(prefix + "catalog.$n.seed", 0), artworkPath = p.getString(prefix + "catalog.$n.artwork", null))
                }
            }, p.getInt(prefix + "seed", 0))
        }
    }
    override suspend fun savePlaylists(playlists: List<Playlist>) = savePlaylistsNow(playlists)
    private fun savePlaylistsNow(playlists: List<Playlist>) = synchronized(lock(context)) {
        val prefs = context.getSharedPreferences("resonance_playlists", Context.MODE_PRIVATE)
        val backup = context.getSharedPreferences("resonance_playlists_backup", Context.MODE_PRIVATE).edit().clear()
        prefs.all.forEach { (key, value) -> when (value) { is String -> backup.putString(key, value); is Int -> backup.putInt(key, value); is Boolean -> backup.putBoolean(key, value) } }
        if (prefs.contains("count")) check(backup.commit()) { "歌单备份失败" }
        val edit = prefs.edit().clear().putInt("count", playlists.size)
        playlists.forEachIndexed { index, list ->
            val prefix = "playlist.$index."
            edit.putString(prefix + "id", list.id).putString(prefix + "name", list.name).putString(prefix + "subtitle", list.subtitle).putInt(prefix + "seed", list.artworkSeed).putString(prefix + "tracks", list.tracks.joinToString(",") { it.id })
            list.tracks.forEachIndexed { n, track ->
                val t = prefix + "catalog.$n."
                edit.putString(t + "title", track.title).putString(t + "artist", track.artist).putString(t + "album", track.album).putString(t + "duration", track.durationText).putInt(t + "seed", track.artworkSeed).putBoolean(t + "favorite", track.isFavorite).putString(t + "artwork", track.artworkPath).putString(t + "mime", track.mimeType)
            }
        }
        check(edit.commit()) { "歌单保存失败" }
    }
    override suspend fun update(track: Track) = synchronized(lock(context)) {
        val managed = readManaged(context)
        writeManaged(context, (managed.filterNot { it.id == track.id } + track))
        savePlaylistsNow(playlistsNow().map { list -> list.copy(tracks = list.tracks.map { if (it.id == track.id) track else it }) })
    }
    override suspend fun favorite(track: Track, value: Boolean) = synchronized(lock(context)) {
        val prefs = context.getSharedPreferences("resonance_favorites", Context.MODE_PRIVATE)
        val ids = prefs.getStringSet("ids", emptySet()).orEmpty().toMutableSet()
        if (value) ids += track.id else ids -= track.id
        check(prefs.edit().putStringSet("ids", ids).commit())
        writeManaged(context, readManaged(context).map { if (it.id == track.id) it.copy(isFavorite = value) else it })
    }
    override fun playingTrackId(): String? = com.resonance.player.PlaybackService.activeMusicId
    override suspend fun delete(track: Track): DeleteTrackReport {
        com.resonance.player.PlaybackService.stopTrack(track.id)
        val deleted = withContext(Dispatchers.IO) {
            val source = track.sourceUri ?: return@withContext false
            val uri = Uri.parse(source)
            when (uri.scheme) {
                "content" -> context.contentResolver.delete(uri, null, null) > 0
                "file" -> uri.path?.let { Files.deleteIfExists(java.nio.file.Paths.get(it)); !Files.exists(java.nio.file.Paths.get(it)) } ?: false
                null -> { Files.deleteIfExists(java.nio.file.Paths.get(source)); !Files.exists(java.nio.file.Paths.get(source)) }
                else -> false
            }
        }
        synchronized(lock(context)) {
            writeManaged(context, readManaged(context).filterNot { it.id == track.id })
            savePlaylistsNow(playlistsNow().map { it.copy(tracks = it.tracks.filterNot { t -> t.id == track.id }) })
            json.update { root -> val a = root.optJSONArray("removedIds") ?: org.json.JSONArray(); val ids = (0 until a.length()).map(a::getString).toMutableSet(); ids += track.id; root.put("removedIds", org.json.JSONArray(ids.toList())) }
        }
        return DeleteTrackReport(true, deleted, if (deleted) "音频已删除" else "未能删除音频")
    }
    override suspend fun importAudio(path: Path, request: VideoImportRequest, cover: Path?): Track = withContext(Dispatchers.IO) {
        Files.createDirectories(musicDirectory)
        val id = Mp3Import.hash(path)
        val extension = path.fileName.toString().substringAfterLast('.', "mp3")
        val target = musicDirectory.resolve(id + "." + extension)
        if (!Files.isRegularFile(target)) Files.move(path, target, StandardCopyOption.REPLACE_EXISTING)
        val picture = cover?.takeIf(Files::isRegularFile)?.let { c ->
            val dir = root.resolve("artwork"); Files.createDirectories(dir)
            dir.resolve("$id.cover").also { Files.copy(c, it, StandardCopyOption.REPLACE_EXISTING) }.toString()
        }
        val duration = MediaMetadataRetriever().let { r -> try { r.setDataSource(target.toString()); (r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0) / 1000 } finally { r.release() } }
        val track = Track(id, request.title, request.artist, request.album, "%d:%02d".format(duration / 60, duration % 60), id.hashCode(), sourceUri = target.toUri().toString(), artworkPath = picture, mimeType = Mp3Import.mime(extension))
        synchronized(lock(context)) { writeManaged(context, (readManaged(context) + track).associateBy(Track::id).values.toList()) }
        track
    }
    override suspend fun adoptRestoredAudio(previous: Track, imported: Track): Track = synchronized(lock(context)) {
        val restored = imported.copy(id = previous.id, isFavorite = previous.isFavorite)
        writeManaged(context, readManaged(context).filterNot { it.id == imported.id || it.id == previous.id } + restored)
        savePlaylistsNow(playlistsNow().map { list -> list.copy(tracks = list.tracks.map { if (it.id == previous.id) restored else it }) })
        restored
    }
    override suspend fun audioInfo(track: Track): AudioFileInfo = withContext(Dispatchers.IO) {
        val source = track.sourceUri ?: return@withContext AudioFileInfo()
        val extractor = android.media.MediaExtractor()
        try {
            extractor.setDataSource(context, Uri.parse(source), null)
            val format = (0 until extractor.trackCount).map(extractor::getTrackFormat)
                .firstOrNull { it.getString(android.media.MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: return@withContext AudioFileInfo()
            fun integer(key: String): Int = if (format.containsKey(key)) format.getInteger(key) else 0
            var bitrate = integer(android.media.MediaFormat.KEY_BIT_RATE).toLong()
            if (bitrate <= 0) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(source))
                    bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0
                } finally { retriever.release() }
            }
            AudioFileInfo(bitrate, integer(android.media.MediaFormat.KEY_SAMPLE_RATE), integer(android.media.MediaFormat.KEY_CHANNEL_COUNT))
        } catch (_: Exception) { AudioFileInfo() } finally { extractor.release() }
    }
    override suspend fun transcode(input: Path, output: Path, request: VideoImportRequest, cover: Path?) {
        withTimeout(600_000) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val session = FFmpegKit.executeWithArgumentsAsync(Mp3Import.arguments(input, output, request, cover)) { done ->
                    if (continuation.isActive) {
                        if (ReturnCode.isSuccess(done.returnCode)) continuation.resume(Unit)
                        else continuation.resumeWith(Result.failure(IllegalStateException("音频转换失败：${done.failStackTrace.orEmpty().takeLast(300)}")))
                    }
                }
                continuation.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
            }
        }
    }
    override suspend fun prepareSpeechAudio(track: Track, output: Path) = withContext(Dispatchers.IO) {
        val source = track.sourceUri ?: error("音频文件不可用")
        val temporary = if (source.startsWith("content:")) Files.createTempFile(context.cacheDir.toPath(), "speech-source-", ".audio") else null
        try {
            if (temporary != null) {
                context.contentResolver.openInputStream(Uri.parse(source))?.use { input ->
                    Files.newOutputStream(temporary).use { out ->
                        val bytes = ByteArray(65536); var size = 0L
                        while (true) { ensureActive(); val n = input.read(bytes); if (n < 0) break; size += n; require(size <= 200_000_000) { "音频文件超过 200 MB" }; out.write(bytes, 0, n) }
                    }
                } ?: error("无法读取音频文件")
            }
            val input = temporary?.toString() ?: if (source.startsWith("file:")) java.nio.file.Paths.get(java.net.URI(source)).toString() else source
            withTimeout(180_000) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val args = arrayOf("-nostdin", "-y", "-v", "error", "-i", input, "-map", "0:a:0", "-vn", "-ac", "1", "-ar", "16000", "-t", "1801", "-f", "s16le", output.toString())
                    val session = FFmpegKit.executeWithArgumentsAsync(args) { done ->
                        if (continuation.isActive) {
                            if (ReturnCode.isSuccess(done.returnCode)) continuation.resume(Unit)
                            else continuation.resumeWith(Result.failure(IllegalStateException("准备语音音频失败")))
                        }
                    }
                    continuation.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
                }
            }
        } finally { temporary?.let { Files.deleteIfExists(it) } }
    }
    companion object {
        fun lock(context: Context): Any = context.filesDir.absolutePath.intern()
        fun readManaged(context: Context): List<Track> = synchronized(lock(context)) {
            val props = AtomicPropertiesFile.read(context.filesDir.toPath().resolve("synced-library.properties")) { AtomicPropertiesFile.validIndexed(it, "track", "source") }
            (0 until (props.getProperty("count")?.toIntOrNull() ?: 0)).mapNotNull { n ->
                val p = "track.$n."; val id = props.getProperty(p + "id") ?: return@mapNotNull null
                val source = props.getProperty(p + "source") ?: return@mapNotNull null
                if (source.startsWith("file:") && !Files.isRegularFile(java.nio.file.Paths.get(java.net.URI(source)))) return@mapNotNull null
                Track(id, props.getProperty(p + "title", "未知歌曲"), props.getProperty(p + "artist", "未知歌手"), props.getProperty(p + "album", "本地音乐"), props.getProperty(p + "duration", "0:00"), props.getProperty(p + "seed", "0").toIntOrNull() ?: 0, props.getProperty(p + "favorite", "false").toBoolean(), source.takeIf(String::isNotBlank), props.getProperty(p + "artwork")?.takeIf(String::isNotBlank), props.getProperty(p + "mime", "audio/mpeg"))
            }
        }
        fun writeManaged(context: Context, tracks: List<Track>) = synchronized(lock(context)) {
            val props = Properties().apply { setProperty("count", tracks.size.toString()) }
            tracks.forEachIndexed { n, t ->
                val p = "track.$n."
                mapOf("id" to t.id, "source" to t.sourceUri.orEmpty(), "title" to t.title, "artist" to t.artist, "album" to t.album, "duration" to t.durationText, "seed" to t.artworkSeed.toString(), "favorite" to t.isFavorite.toString(), "artwork" to t.artworkPath.orEmpty(), "mime" to t.mimeType).forEach { (k, v) -> props.setProperty(p + k, v) }
            }
            AtomicPropertiesFile.write(context.filesDir.toPath().resolve("synced-library.properties"), props, "Resonance managed library")
        }
        fun runtime(context: Context, delete: (suspend (Track) -> DeleteTrackReport)? = null): JvmMusicAssistant {
            val app = context.applicationContext
            val access = AndroidMusicAccess(app)
            val operations = if (delete == null) access else object : MusicLibraryAccess by access {
                override suspend fun delete(track: Track) = delete.invoke(track)
            }
            return JvmMusicAssistant(MusicJsonStore(app.filesDir.toPath().resolve("music-assistant")), access.musicDirectory, operations, AndroidCredentialVault::encrypt, AndroidCredentialVault::decrypt,
                screenshotOcr = androidScreenshotOcr(app), autoInstallBuiltinSources = true)
        }
    }
}

object AndroidCredentialVault {
    private fun key(): javax.crypto.SecretKey {
        val store = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("resonance-ai", null) as? javax.crypto.SecretKey)?.let { return it }
        return javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(android.security.keystore.KeyGenParameterSpec.Builder("resonance-ai", android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun encrypt(value: String): String {
        if (value.isBlank()) return ""
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding").apply { init(javax.crypto.Cipher.ENCRYPT_MODE, key()) }
        return "keystore:" + android.util.Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), android.util.Base64.NO_WRAP)
    }
    fun decrypt(value: String): String {
        if (!value.startsWith("keystore:")) return value
        val data = android.util.Base64.decode(value.substringAfter(':'), android.util.Base64.NO_WRAP)
        require(data.size > 12)
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding").apply { init(javax.crypto.Cipher.DECRYPT_MODE, key(), javax.crypto.spec.GCMParameterSpec(128, data.copyOfRange(0, 12))) }
        return String(cipher.doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8)
    }
}
