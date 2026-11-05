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

internal fun desktopMusicImportFilter() = javax.swing.filechooser.FileNameExtensionFilter(
    "音乐与 Resonance 歌单包 (*.resonance)", *(DESKTOP_AUDIO_EXTENSIONS + "resonance").toTypedArray(),
)

internal class DesktopMusicScanner(
    private val artworkDirectory: Path,
    private val managedMusicDirectory: Path,
    private val commitPackage: (PlainSyncPackage.Imported) -> Unit = {},
) {
    private val audioExtensions = DESKTOP_AUDIO_EXTENSIONS
    private val converter = DesktopManagedMp3Converter(managedMusicDirectory)

    fun scan(root: Path, convertToMp3: Boolean,
        control: com.resonance.player.io.ImportControl = com.resonance.player.io.ImportControl(),
        progress: (ImportProgress) -> Unit = {},
    ): ImportReport = scan(listOf(root), convertToMp3, control, progress)

    fun scan(roots: List<Path>, convertToMp3: Boolean,
        control: com.resonance.player.io.ImportControl = com.resonance.player.io.ImportControl(),
        progress: (ImportProgress) -> Unit = {},
    ): ImportReport {
        val tracks = mutableListOf<Track>()
        val playlists = mutableListOf<Playlist>()
        var importedPackageCount = 0
        val warnings = mutableListOf<String>()
        var skipped = 0
        fun failed(path: Path, error: Exception) {
            skipped++
            if (warnings.size < 10) warnings += "${path.fileName}：${error.message.orEmpty()}"
        }
        try {
            val candidates = linkedSetOf<Path>()
            fun candidate(path: Path) {
                control.check()
                val extension = path.fileName.toString().substringAfterLast('.', "").lowercase()
                if (extension in audioExtensions || extension == "resonance") {
                    try { candidates.add(path.toRealPath()) } catch (error: java.io.IOException) { failed(path, error) }
                    if (candidates.size % 100 == 0) progress(ImportProgress("扫描文件", completed = candidates.size, detail = "已发现 ${candidates.size} 个音频文件或歌单包"))
                }
            }
            roots.forEach { root ->
                control.check()
                try {
                    if (!Files.isDirectory(root)) {
                        if (!Files.isRegularFile(root)) failed(root, java.io.IOException("文件不存在或无法读取"))
                        else candidate(root)
                    } else Files.walkFileTree(root.toRealPath(), object : java.nio.file.SimpleFileVisitor<Path>() {
                        override fun preVisitDirectory(dir: Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                            control.check()
                            return java.nio.file.FileVisitResult.CONTINUE
                        }
                        override fun visitFile(file: Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                            if (Files.isRegularFile(file)) candidate(file)
                            return java.nio.file.FileVisitResult.CONTINUE
                        }
                        override fun visitFileFailed(file: Path, exc: java.io.IOException): java.nio.file.FileVisitResult {
                            control.check()
                            failed(file, exc)
                            return java.nio.file.FileVisitResult.CONTINUE
                        }
                    })
                } catch (error: java.io.IOException) { failed(root, error) }
            }
            if (candidates.isEmpty() && warnings.isEmpty()) warnings += "未找到支持的音频文件或歌单包，请选择 MP3、FLAC、WAV、.resonance 文件或文件夹。"
            candidates.forEachIndexed { index, path ->
                control.check()
                val name = path.fileName.toString()
                val ext = name.substringAfterLast('.', "").lowercase()
                val stage = if (ext == "resonance") "导入歌单包" else "处理音频"
                progress(ImportProgress(stage, name, index, candidates.size, index.toFloat() / candidates.size.coerceAtLeast(1)))
                try {
                    if (ext == "resonance") {
                        // Archives keep their own audio format, metadata and playlist identities.
                        val imported = PlainSyncPackage().import(path, managedMusicDirectory, onCommit = commitPackage, checkCancelled = control::check)
                        tracks += imported.tracks
                        playlists += imported.playlists
                        importedPackageCount++
                    } else {
                        val source = if (convertToMp3 || ext in listOf("kgma", "kgm", "kgg")) converter.importAsMp3(path, control) { done, total ->
                            progress(ImportProgress("解密音频", name, index, candidates.size,
                                (index + (if (total > 0) done.toFloat() / total * 0.7f else 0f)) / candidates.size.coerceAtLeast(1), "${done / 1024} / ${total / 1024} KB"))
                        } else path
                        control.check()
                        tracks += readAudioTrack(source)
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { skipped++; if (warnings.size < 10) warnings += "$name：${error.message.orEmpty()}" }
                progress(ImportProgress(stage, name, index + 1, candidates.size, (index + 1f) / candidates.size.coerceAtLeast(1)))
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { warnings += "已取消；已完成 ${tracks.size} 首，源文件保留" }
        return ImportReport(tracks.associateBy(Track::id).values.sortedWith(compareBy(Track::artist, Track::title)),
            skipped, warnings, control.isCancelled, playlists.associateBy(Playlist::id).values.toList(), importedPackageCount)
    }

    fun readAudioTrack(path: Path): Track {
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

