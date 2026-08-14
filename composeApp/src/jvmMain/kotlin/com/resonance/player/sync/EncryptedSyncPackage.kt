package com.resonance.player.sync

import com.resonance.player.model.Playlist
import com.resonance.player.model.Track
import com.resonance.player.io.readUpTo
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class EncryptedSyncPackage {
    data class Imported(val tracks: List<Track>, val playlists: List<Playlist>)

    fun export(
        target: Path,
        passphrase: String,
        tracks: List<Track>,
        playlists: List<Playlist>,
        openAudio: (Track) -> InputStream?,
        openArtwork: (Track) -> InputStream?,
    ) {
        require(passphrase.length >= 8) { "同步口令至少需要 8 个字符" }
        target.parent?.let(Files::createDirectories)
        val temporary = target.resolveSibling(".${target.fileName}.part")
        Files.deleteIfExists(temporary)
        try {
            val salt = ByteArray(SALT_SIZE).also(SecureRandom()::nextBytes)
            val nonce = ByteArray(NONCE_SIZE).also(SecureRandom()::nextBytes)
            val cipher = cipher(Cipher.ENCRYPT_MODE, passphrase, salt, nonce)
            Files.newOutputStream(temporary).buffered().use { output ->
                output.write(MAGIC)
                output.write(byteArrayOf(VERSION.toByte()))
                output.write(salt)
                output.write(nonce)
                CipherOutputStream(output, cipher).use { encrypted ->
                    ZipOutputStream(encrypted).use { zip ->
                        writeArchive(zip, tracks, playlists, openAudio, openArtwork)
                    }
                }
            }
            moveIntoPlace(temporary, target)
        } catch (error: Throwable) {
            Files.deleteIfExists(temporary)
            throw error
        }
    }

    fun import(source: Path, passphrase: String, managedDirectory: Path): Imported {
        require(passphrase.length >= 8) { "同步口令至少需要 8 个字符" }
        require(Files.isRegularFile(source)) { "同步包不存在" }
        Files.createDirectories(managedDirectory)
        val verifiedZip = source.resolveSibling(".${source.fileName}.verified.zip")
        val staging = Files.createTempDirectory(managedDirectory, ".sync-import-")
        try {
            decryptAndVerify(source, verifiedZip, passphrase)
            extractWhitelisted(verifiedZip, staging)
            val manifestPath = staging.resolve(MANIFEST_ENTRY)
            require(Files.isRegularFile(manifestPath)) { "同步包缺少清单" }
            val properties = Properties()
            Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8).use(properties::load)
            require(properties.getProperty("format") == "resonance-sync-1") { "不支持的同步包版本" }

            val pendingTracks = validateTracks(properties, staging, managedDirectory)
            val tracks = commitTracks(pendingTracks)
            val tracksById = tracks.associateBy(Track::id)
            val playlists = readPlaylists(properties, tracksById)
            return Imported(tracks, playlists)
        } finally {
            Files.deleteIfExists(verifiedZip)
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
            val audioEntry = "audio/${safeId(track.id)}.mp3"
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
        manifest.store(zip, "Resonance encrypted sync package")
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
            setProperty(prefix + "audio", "audio/${safeId(track.id)}.mp3")
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

    private fun validateTracks(properties: Properties, staging: Path, managed: Path): List<PendingTrack> {
        val count = properties.getProperty("track.count")?.toIntOrNull() ?: error("曲目清单无效")
        require(count in 0..MAX_ITEMS) { "同步包曲目数量异常" }
        return (0 until count).map { index ->
            val prefix = "track.$index."
            val id = properties.required(prefix + "id")
            val audioEntry = properties.required(prefix + "audio")
            val stagedAudio = safeResolve(staging, audioEntry)
            require(Files.isRegularFile(stagedAudio)) { "曲目数据缺失：$id" }
            require(sha256(stagedAudio) == properties.required(prefix + "sha256")) { "曲目校验失败：$id" }
            val targetAudio = managed.resolve("${safeId(id)}.mp3")

            val stagedArtwork = properties.getProperty(prefix + "artwork")?.let { entry ->
                val stagedArtwork = safeResolve(staging, entry)
                stagedArtwork.takeIf(Files::isRegularFile)
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
                ),
                stagedAudio = stagedAudio,
                targetAudio = targetAudio,
                stagedArtwork = stagedArtwork,
                targetArtwork = targetArtwork,
            )
        }
    }

    private fun commitTracks(pendingTracks: List<PendingTrack>): List<Track> {
        pendingTracks.forEach { pending ->
            moveIntoPlace(pending.stagedAudio, pending.targetAudio)
            if (pending.stagedArtwork != null && pending.targetArtwork != null) {
                moveIntoPlace(pending.stagedArtwork, pending.targetArtwork)
            }
        }
        return pendingTracks.map(PendingTrack::track)
    }

    private fun readPlaylists(properties: Properties, tracks: Map<String, Track>): List<Playlist> {
        val count = properties.getProperty("playlist.count")?.toIntOrNull() ?: 0
        require(count in 0..MAX_ITEMS) { "同步包歌单数量异常" }
        return (0 until count).map { index ->
            val prefix = "playlist.$index."
            val id = properties.required(prefix + "id")
            val trackCount = properties.getProperty(prefix + "track.count")?.toIntOrNull() ?: 0
            Playlist(
                id = id,
                name = properties.required(prefix + "name"),
                subtitle = properties.getProperty(prefix + "subtitle") ?: "$trackCount 首",
                tracks = (0 until trackCount).mapNotNull { tracks[properties.getProperty(prefix + "track.$it")] },
                artworkSeed = properties.getProperty(prefix + "seed")?.toIntOrNull() ?: id.hashCode(),
            )
        }
    }

    private fun decryptAndVerify(source: Path, verifiedZip: Path, passphrase: String) {
        Files.newInputStream(source).buffered().use { input ->
            require(input.readUpTo(MAGIC.size).contentEquals(MAGIC)) { "不是 Resonance 同步包" }
            require(input.read() == VERSION) { "不支持的同步包版本" }
            val salt = input.readUpTo(SALT_SIZE).also { require(it.size == SALT_SIZE) { "同步包头不完整" } }
            val nonce = input.readUpTo(NONCE_SIZE).also { require(it.size == NONCE_SIZE) { "同步包头不完整" } }
            val cipher = cipher(Cipher.DECRYPT_MODE, passphrase, salt, nonce)
            try {
                // Some Android providers let CipherInputStream hide a failed GCM
                // tag on close. Stream through Cipher.update(), then observe
                // doFinal() explicitly so large libraries stay memory-bounded.
                Files.newOutputStream(verifiedZip).buffered().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        cipher.update(buffer, 0, read)?.takeIf(ByteArray::isNotEmpty)?.let(output::write)
                    }
                    cipher.doFinal().takeIf(ByteArray::isNotEmpty)?.let(output::write)
                }
            } catch (_: Throwable) {
                Files.deleteIfExists(verifiedZip)
                error("同步口令错误，或同步包已损坏")
            }
        }
    }

    private fun extractWhitelisted(zipFile: Path, staging: Path) {
        ZipInputStream(Files.newInputStream(zipFile).buffered()).use { zip ->
            var entries = 0
            var totalWritten = 0L
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                require(++entries <= MAX_ARCHIVE_ENTRIES) { "同步包条目数量过多" }
                require(entry.name == MANIFEST_ENTRY || entry.name.startsWith("audio/") || entry.name.startsWith("art/")) {
                    "同步包包含不允许的条目"
                }
                val target = safeResolve(staging, entry.name)
                target.parent?.let(Files::createDirectories)
                var written = 0L
                Files.newOutputStream(target).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        written += read
                        totalWritten += read
                        require(written <= MAX_ENTRY_BYTES) { "同步包中的单个文件过大" }
                        require(totalWritten <= MAX_ARCHIVE_BYTES) { "同步包解压后过大" }
                        output.write(buffer, 0, read)
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun safeResolve(root: Path, entry: String): Path {
        val resolved = root.resolve(entry).normalize()
        require(resolved.startsWith(root.normalize())) { "同步包路径越界" }
        return resolved
    }

    private fun cipher(mode: Int, passphrase: String, salt: ByteArray, nonce: ByteArray): Cipher {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            updateAAD(MAGIC + VERSION.toByte())
        }
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

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).buffered().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().hex()
    }

    private fun safeId(id: String): String = MessageDigest.getInstance("SHA-256")
        .digest(id.toByteArray(StandardCharsets.UTF_8)).take(12).toByteArray().hex()

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
        val MAGIC = "RESONPKG".toByteArray(StandardCharsets.US_ASCII)
        const val VERSION = 1
        const val SALT_SIZE = 16
        const val NONCE_SIZE = 12
        const val ITERATIONS = 210_000
        const val MANIFEST_ENTRY = "manifest.properties"
        const val MAX_ITEMS = 100_000
        const val MAX_ENTRY_BYTES = 4L * 1024 * 1024 * 1024
        const val MAX_ARCHIVE_BYTES = 512L * 1024 * 1024 * 1024
        const val MAX_ARCHIVE_ENTRIES = 200_001
    }
}
