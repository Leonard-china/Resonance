package com.resonance.player.sync

import com.resonance.player.io.readUpTo
import com.resonance.player.model.Playlist
import com.resonance.player.model.Track
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 明文 Zip 歌单文件：直接把 manifest.properties、audio 目录下的 MP3、art 目录下的封面
 * 写入标准 Zip，不做口令加密。本地 MP3 没有保密需求，省去输入密码的步骤。
 * 仍然保留 SHA-256 完整性校验与路径白名单，防止损坏或恶意压缩包。
 */
class PlainSyncPackage {
    data class Imported(val tracks: List<Track>, val playlists: List<Playlist>)

    fun export(
        target: Path,
        tracks: List<Track>,
        playlists: List<Playlist>,
        openAudio: (Track) -> InputStream?,
        openArtwork: (Track) -> InputStream?,
    ) {
        target.parent?.let(Files::createDirectories)
        val temporary = target.resolveSibling(".${target.fileName}.part")
        Files.deleteIfExists(temporary)
        try {
            Files.newOutputStream(temporary).buffered().use { output ->
                ZipOutputStream(output).use { zip ->
                    writeArchive(zip, tracks, playlists, openAudio, openArtwork)
                }
            }
            moveIntoPlace(temporary, target)
        } catch (error: Throwable) {
            Files.deleteIfExists(temporary)
            throw error
        }
    }

    fun import(source: Path, managedDirectory: Path): Imported = import(source, managedDirectory, onCommit = {})

    fun import(source: Path, managedDirectory: Path, onCommit: (Imported) -> Unit,
        checkCancelled: () -> Unit = {},
    ): Imported {
        checkCancelled()
        require(Files.isRegularFile(source)) { "歌单文件不存在" }
        Files.createDirectories(managedDirectory)
        val managed = managedDirectory.toRealPath()
        val staging = Files.createTempDirectory(managed, ".sync-import-")
        try {
            Files.newInputStream(source).buffered().use { probe ->
                require(!probe.readUpTo(ENCRYPTED_MAGIC.size).contentEquals(ENCRYPTED_MAGIC)) {
                    "这是旧版加密歌单文件，请在对端设备重新导出免密码歌单文件后再导入"
                }
            }
            extractWhitelisted(source, staging, checkCancelled)
            val manifestPath = staging.resolve(MANIFEST_ENTRY)
            require(Files.isRegularFile(manifestPath)) { "歌单文件缺少清单" }
            val properties = Properties()
            Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8).use(properties::load)
            require(properties.getProperty("format") == "resonance-sync-1") { "不支持的歌单文件版本" }

            val pendingTracks = validateTracks(properties, staging, managed, checkCancelled)
            val tracks = pendingTracks.map(PendingTrack::track)
            val playlists = readPlaylists(properties, tracks.associateBy(Track::id), checkCancelled)
            val imported = Imported(tracks, playlists)
            val targets = pendingTracks.flatMap { listOfNotNull(it.targetAudio, it.targetArtwork) }
            require(targets.distinct().size == targets.size) { "歌单文件包含重复的目标文件" }
            targets.forEach { target ->
                var parent = target.parent
                while (parent != managed) {
                    require(parent.startsWith(managed) && !Files.isSymbolicLink(parent)) { "歌单文件目标路径越界" }
                    parent = parent.parent
                }
                require(!Files.isSymbolicLink(target)) { "歌单文件目标不能是符号链接" }
            }
            val backupBytes = targets.filter(Files::isRegularFile).sumOf(Files::size)
            require(Files.getFileStore(managed).usableSpace >= backupBytes + MIN_FREE_BYTES) { "空间不足，无法保留导入回退副本" }
            checkCancelled()
            return com.resonance.player.io.FileRollback.run(targets) {
                commitTracks(pendingTracks, checkCancelled)
                checkCancelled()
                onCommit(imported)
                imported
            }
        } finally {
            deleteTree(staging)
        }
    }

    private fun writeArchive(
        zip: ZipOutputStream,
        tracks: List<Track>,
        playlists: List<Playlist>,
        openAudio: (Track) -> InputStream?,
        openArtwork: (Track) -> InputStream?,
    ) {
        val exportedTracks = mutableListOf<Track>()
        val audioHashes = mutableMapOf<String, String>()
        val artworkEntries = mutableSetOf<String>()
        tracks.distinctBy(Track::id).forEach { track ->
            val audio = openAudio(track) ?: return@forEach
            val audioEntry = "audio/${safeId(track.id)}.${audioExtension(track)}"
            zip.putNextEntry(ZipEntry(audioEntry))
            audio.use { audioHashes[track.id] = copyAndDigest(it, zip) }
            zip.closeEntry()
            openArtwork(track)?.use { artwork ->
                val artworkEntry = "art/${safeId(track.id)}.cover"
                zip.putNextEntry(ZipEntry(artworkEntry))
                artwork.copyTo(zip)
                zip.closeEntry()
                artworkEntries += track.id
            }
            exportedTracks += track
        }

        val exportedIds = exportedTracks.mapTo(mutableSetOf(), Track::id)
        val exportedPlaylists = playlists.map { playlist ->
            playlist.copy(tracks = playlist.tracks.filter { it.id in exportedIds })
        }
        val manifest = createManifest(exportedTracks, exportedPlaylists, audioHashes, artworkEntries)
        zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
        manifest.store(zip, "Resonance sync package")
        zip.closeEntry()
    }

    private fun createManifest(
        tracks: List<Track>,
        playlists: List<Playlist>,
        hashes: Map<String, String>,
        artworkIds: Set<String>,
    ) = Properties().apply {
        setProperty("format", "resonance-sync-1")
        setProperty("track.count", tracks.size.toString())
        tracks.forEachIndexed { index, track ->
            val prefix = "track.$index."
            setProperty(prefix + "id", track.id)
            setProperty(prefix + "title", track.title)
            setProperty(prefix + "artist", track.artist)
            setProperty(prefix + "album", track.album)
            setProperty(prefix + "duration", track.durationText)
            setProperty(prefix + "seed", track.artworkSeed.toString())
            setProperty(prefix + "favorite", track.isFavorite.toString())
            setProperty(prefix + "audio", "audio/${safeId(track.id)}.${audioExtension(track)}")
            setProperty(prefix + "mime", track.mimeType)
            setProperty(prefix + "sha256", hashes.getValue(track.id))
            if (track.id in artworkIds) setProperty(prefix + "artwork", "art/${safeId(track.id)}.cover")
        }
        setProperty("playlist.count", playlists.size.toString())
        playlists.forEachIndexed { index, playlist ->
            val prefix = "playlist.$index."
            setProperty(prefix + "id", playlist.id)
            setProperty(prefix + "name", playlist.name)
            setProperty(prefix + "subtitle", playlist.subtitle)
            setProperty(prefix + "seed", playlist.artworkSeed.toString())
            setProperty(prefix + "track.count", playlist.tracks.size.toString())
            playlist.tracks.forEachIndexed { trackIndex, track ->
                setProperty(prefix + "track.$trackIndex", track.id)
            }
        }
    }

    private data class PendingTrack(
        val track: Track,
        val stagedAudio: Path,
        val targetAudio: Path,
        val stagedArtwork: Path?,
        val targetArtwork: Path?,
    )

    private fun validateTracks(properties: Properties, staging: Path, managed: Path, checkCancelled: () -> Unit): List<PendingTrack> {
        val count = properties.getProperty("track.count")?.toIntOrNull() ?: error("曲目清单无效")
        require(count in 0..MAX_ITEMS) { "歌单文件曲目数量异常" }
        val ids = hashSetOf<String>()
        return (0 until count).map { index ->
            checkCancelled()
            val prefix = "track.$index."
            val id = properties.required(prefix + "id")
            require(id.isNotBlank() && ids.add(id)) { "曲目ID为空或重复" }
            val audioEntry = properties.required(prefix + "audio")
            require(audioEntry.startsWith("audio/")) { "曲目音频路径无效" }
            val stagedAudio = safeResolve(staging, audioEntry)
            require(Files.isRegularFile(stagedAudio)) { "曲目数据缺失：$id" }
            require(sha256(stagedAudio, checkCancelled) == properties.required(prefix + "sha256")) { "曲目校验失败：$id" }
            val extension = audioEntry.substringAfterLast('.', "").lowercase()
            require(extension in AUDIO_EXTENSIONS) { "歌单文件包含不支持的音频类型" }
            val targetAudio = managed.resolve("${safeId(id)}.$extension")

            val stagedArtwork = properties.getProperty(prefix + "artwork")?.let { entry ->
                require(entry.startsWith("art/")) { "封面路径无效" }
                val staged = safeResolve(staging, entry)
                staged.takeIf(Files::isRegularFile)
            }
            val targetArtwork = stagedArtwork?.let { managed.resolve("art").resolve("${safeId(id)}.cover") }
            PendingTrack(
                track = Track(
                    id = id,
                    title = properties.required(prefix + "title"),
                    artist = properties.required(prefix + "artist"),
                    album = properties.required(prefix + "album"),
                    durationText = properties.getProperty(prefix + "duration") ?: "0:00",
                    artworkSeed = properties.getProperty(prefix + "seed")?.toIntOrNull() ?: id.hashCode(),
                    isFavorite = properties.getProperty(prefix + "favorite").toBoolean(),
                    sourceUri = targetAudio.toString(),
                    artworkPath = targetArtwork?.toString(),
                    mimeType = properties.getProperty(prefix + "mime") ?: "audio/mpeg",
                ),
                stagedAudio = stagedAudio,
                targetAudio = targetAudio,
                stagedArtwork = stagedArtwork,
                targetArtwork = targetArtwork,
            )
        }
    }

    private fun commitTracks(pendingTracks: List<PendingTrack>, checkCancelled: () -> Unit): List<Track> {
        pendingTracks.forEach { pending ->
            checkCancelled()
            moveIntoPlace(pending.stagedAudio, pending.targetAudio)
            if (pending.stagedArtwork != null && pending.targetArtwork != null) {
                moveIntoPlace(pending.stagedArtwork, pending.targetArtwork)
            }
        }
        return pendingTracks.map(PendingTrack::track)
    }

    private fun readPlaylists(properties: Properties, tracks: Map<String, Track>, checkCancelled: () -> Unit): List<Playlist> {
        val count = properties.getProperty("playlist.count")?.let { it.toIntOrNull() ?: error("歌单数量不是有效整数") } ?: 0
        require(count in 0..MAX_ITEMS) { "歌单文件歌单数量异常" }
        val ids = hashSetOf<String>()
        var references = 0L
        return (0 until count).map { index ->
            checkCancelled()
            val prefix = "playlist.$index."
            val id = properties.required(prefix + "id")
            require(id.isNotBlank() && ids.add(id)) { "歌单ID为空或重复" }
            val trackCount = properties.getProperty(prefix + "track.count")?.let { it.toIntOrNull() ?: error("歌单引用数量不是有效整数") } ?: 0
            require(trackCount in 0..MAX_ITEMS) { "歌单引用数量异常" }
            references += trackCount
            require(references <= MAX_REFERENCES) { "歌单总引用数量过多" }
            Playlist(
                id = id,
                name = properties.required(prefix + "name"),
                subtitle = properties.getProperty(prefix + "subtitle") ?: "$trackCount 首",
                tracks = (0 until trackCount).mapNotNull {
                    if (it % 256 == 0) checkCancelled()
                    tracks[properties.getProperty(prefix + "track.$it")]
                },
                artworkSeed = properties.getProperty(prefix + "seed")?.toIntOrNull() ?: id.hashCode(),
            )
        }
    }

    private fun extractWhitelisted(zipFile: Path, staging: Path, checkCancelled: () -> Unit) {
        ZipInputStream(Files.newInputStream(zipFile).buffered()).use { zip ->
            var entries = 0
            val seen = hashSetOf<Path>()
            var totalWritten = 0L
            val byteBudget = minOf(MAX_ARCHIVE_BYTES, (Files.getFileStore(staging).usableSpace - MIN_FREE_BYTES).coerceAtLeast(0))
            while (true) {
                checkCancelled()
                val entry = zip.nextEntry ?: break
                require(++entries <= MAX_ARCHIVE_ENTRIES) { "歌单文件条目数量过多" }
                require(entry.name == MANIFEST_ENTRY || entry.name.startsWith("audio/") || entry.name.startsWith("art/")) {
                    "歌单文件包含不允许的条目"
                }
                val target = safeResolve(staging, entry.name)
                require(seen.add(target)) { "歌单文件包含重复条目" }
                if (entry.isDirectory) { zip.closeEntry(); continue }
                require(entry.size <= byteBudget || entry.size < 0) { "可用空间不足以解压歌单文件" }
                target.parent?.let(Files::createDirectories)
                var written = 0L
                Files.newOutputStream(target).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        checkCancelled()
                        val read = zip.read(buffer)
                        if (read < 0) break
                        written += read
                        totalWritten += read
                        require(written <= MAX_ENTRY_BYTES) { "歌单文件中的单个文件过大" }
                        require(totalWritten <= byteBudget) { "歌单文件解压后过大或可用空间不足" }
                        if (entry.name == MANIFEST_ENTRY) require(written <= MAX_MANIFEST_BYTES) { "歌单清单过大" }
                        if (written % (4 * 1024 * 1024) < buffer.size)
                            require(Files.getFileStore(staging).usableSpace >= MIN_FREE_BYTES + read) { "可用空间不足" }
                        output.write(buffer, 0, read)
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun safeResolve(root: Path, entry: String): Path {
        val resolved = root.resolve(entry).normalize()
        require(resolved.startsWith(root.normalize())) { "歌单文件路径越界" }
        return resolved
    }

    private fun copyAndDigest(input: InputStream, output: OutputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(256 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
            output.write(buffer, 0, count)
        }
        return digest.digest().hex()
    }

    private fun sha256(path: Path, checkCancelled: () -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).buffered().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                checkCancelled()
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().hex()
    }

    private fun safeId(id: String): String = MessageDigest.getInstance("SHA-256")
            .digest(id.toByteArray(StandardCharsets.UTF_8)).take(12).toByteArray().hex()

    private fun audioExtension(track: Track): String {
        val extension = track.sourceUri.orEmpty().substringBefore('?').substringAfterLast('.', "").lowercase()
        if (extension in AUDIO_EXTENSIONS) return extension
        return when (track.mimeType) {
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/mp4", "audio/aac" -> "m4a"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/ogg", "audio/opus" -> "ogg"
            else -> "mp3"
        }
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private fun Properties.required(key: String) = getProperty(key) ?: error("同步清单缺少 $key")

    private fun moveIntoPlace(source: Path, target: Path) {
        target.parent?.let(Files::createDirectories)
        runCatching { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
            .getOrElse { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING) }
    }

    private fun deleteTree(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
    }

    private companion object {
        val AUDIO_EXTENSIONS = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "ape")
        val ENCRYPTED_MAGIC = "RESONPKG".toByteArray(StandardCharsets.US_ASCII)
        const val MANIFEST_ENTRY = "manifest.properties"
        const val MAX_ITEMS = 100_000
        const val MAX_REFERENCES = 1_000_000L
        const val MAX_MANIFEST_BYTES = 16L * 1024 * 1024
        const val MIN_FREE_BYTES = 16L * 1024 * 1024
        const val MAX_ENTRY_BYTES = 4L * 1024 * 1024 * 1024
        const val MAX_ARCHIVE_BYTES = 512L * 1024 * 1024 * 1024
        const val MAX_ARCHIVE_ENTRIES = 200_001
    }
}
