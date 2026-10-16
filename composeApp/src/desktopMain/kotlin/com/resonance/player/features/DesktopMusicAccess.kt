package com.resonance.player.features

import com.resonance.player.model.*
import com.resonance.player.platform.DesktopLibraryStore
import com.resonance.player.platform.DesktopMusicScanner
import com.resonance.player.audio.FfmpegExecutable
import com.resonance.player.media.Mp3Import
import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

internal class DesktopMusicAccess(private val store: DesktopLibraryStore, private val stopTrack: (String) -> Unit, private val activeTrack: () -> String?) : MusicLibraryAccess {
    override suspend fun tracks() = store.load()
    override suspend fun playlists() = store.loadPlaylists()
    override suspend fun savePlaylists(playlists: List<Playlist>) = store.savePlaylists(playlists)
    override suspend fun update(track: Track) = store.updateTracks(listOf(track))
    override suspend fun favorite(track: Track, value: Boolean) = store.setFavorite(track.id, value)
    override fun playingTrackId(): String? = activeTrack()
    override suspend fun delete(track: Track): DeleteTrackReport {
        stopTrack(track.id)
        val source = track.sourceUri?.let { if (it.startsWith("file:")) Path.of(java.net.URI(it)) else Path.of(it) }
        val deleted = source?.let { Files.deleteIfExists(it); !Files.exists(it) } ?: false
        store.remove(track.id)
        return DeleteTrackReport(true, deleted, if (deleted) "音频已删除" else "音频不存在")
    }
    override suspend fun importAudio(path: Path, request: VideoImportRequest, cover: Path?): Track {
        Files.createDirectories(store.managedMusicDirectory)
        val id = Mp3Import.hash(path)
        val target = store.managedMusicDirectory.resolve("$id.mp3")
        if (!Files.isRegularFile(target)) Files.move(path, target, StandardCopyOption.REPLACE_EXISTING)
        val track = DesktopMusicScanner(store.artworkDirectory, store.managedMusicDirectory).readAudioTrack(target)
            .copy(title = request.title, artist = request.artist, album = request.album)
        store.merge(listOf(track))
        return track
    }
    override suspend fun transcode(input: Path, output: Path, request: VideoImportRequest, cover: Path?) = withContext(Dispatchers.IO) {
        val log = Files.createTempFile("resonance-video-", ".log")
        val process = ProcessBuilder(listOf(FfmpegExecutable.locate()) + Mp3Import.arguments(input, output, request, cover)).redirectError(log.toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
        try {
            withTimeout(600_000) { while (!process.waitFor(200, TimeUnit.MILLISECONDS)) { ensureActive() } }
            check(process.exitValue() == 0) { "音频转换失败：${Files.readString(log).takeLast(400)}" }
        } finally { process.destroyForcibly(); process.waitFor(3, TimeUnit.SECONDS); Files.deleteIfExists(log) }
    }
    override suspend fun prepareSpeechAudio(track: Track, output: Path) = withContext(Dispatchers.IO) {
        val source = track.sourceUri ?: error("音频文件不可用")
        val input = if (source.startsWith("file:")) Path.of(java.net.URI(source)).toString() else source
        val log = Files.createTempFile("resonance-speech-", ".log")
        val process = ProcessBuilder(FfmpegExecutable.locate(), "-nostdin", "-y", "-v", "error", "-i", input, "-map", "0:a:0", "-vn", "-ac", "1", "-ar", "16000", "-t", "1801", "-f", "s16le", output.toString())
            .redirectError(log.toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
        try {
            withTimeout(180_000) { while (!process.waitFor(200, TimeUnit.MILLISECONDS)) ensureActive() }
            check(process.exitValue() == 0) { "准备语音音频失败" }
        } finally { process.destroyForcibly(); process.waitFor(3, TimeUnit.SECONDS); Files.deleteIfExists(log) }
    }
}

internal object DesktopCredentialVault {
    fun encrypt(value: String): String {
        if (value.isBlank()) return ""
        require(System.getProperty("os.name").startsWith("Windows")) { "当前凭据保护需要 Windows" }
        return "dpapi:" + java.util.Base64.getEncoder().encodeToString(com.sun.jna.platform.win32.Crypt32Util.cryptProtectData(value.toByteArray(Charsets.UTF_8)))
    }
    fun decrypt(value: String): String = if (!value.startsWith("dpapi:")) value else
        String(com.sun.jna.platform.win32.Crypt32Util.cryptUnprotectData(java.util.Base64.getDecoder().decode(value.substringAfter(':'))), Charsets.UTF_8)
}
