package com.resonance.player.features

import com.resonance.player.media.Mp3Import
import com.resonance.player.model.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

/** Only positively recorded, application-created audio is eligible for cleanup. */
internal class ManagedMusicStorage(
    private val store: MusicJsonStore, private val directory: Path, private val library: MusicLibraryAccess,
) {
    fun register(track: Track, permanent: Boolean = false) {
        val path = path(track.sourceUri) ?: return
        store.update { root ->
            val previous = root.optJSONObject("ownedAudio")?.optJSONObject(track.id)
            root.put("ownedAudio", (root.optJSONObject("ownedAudio") ?: JSONObject()).put(track.id,
                JSONObject().put("path", path.toString()).put("track", MusicFeatureCodec.track(track))
                    .put("permanent", previous?.optBoolean("permanent") ?: permanent)
                    .put("createdAt", previous?.optLong("createdAt") ?: System.currentTimeMillis()).put("evicted", false)))
        }
    }
    suspend fun report(checkDuplicates: Boolean = false): MusicStorageReport {
        val tracks = library.tracks()
        val byId = tracks.associateBy(Track::id)
        // The old video origin index is positive evidence of an application download.
        val legacy = store.read().optJSONObject("videoOrigins")
        legacy?.keys()?.forEach { key ->
            val id = legacy.optString(key)
            if (store.read().optJSONObject("ownedAudio")?.has(id) != true) byId[id]?.let(::register)
        }
        val personalIds = library.playlists().filterNot { it.id == DAILY_PLAYLIST_ID }.flatMap { it.tracks }.map(Track::id).toSet()
        val owners = store.read().optJSONObject("ownedAudio") ?: JSONObject()
        val pathReferences = tracks.mapNotNull { track -> path(track.sourceUri)?.let { it to track } }.groupBy({ it.first }, { it.second })
        val entries = owners.keys().asSequence().mapNotNull { id ->
            val record = owners.optJSONObject(id) ?: return@mapNotNull null
            val track = byId[id] ?: runCatching { MusicFeatureCodec.track(record.getJSONObject("track")) }.getOrNull() ?: return@mapNotNull null
            val file = path(record.optString("path")) ?: return@mapNotNull null
            val references = pathReferences[file].orEmpty()
            val saved = record.optBoolean("permanent") || track.isFavorite || id in personalIds || references.any { it.isFavorite || it.id in personalIds }
            ManagedAudioEntry(track, if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) Files.size(file) else 0,
                saved, references.any { it.id == library.playingTrackId() } || id == library.playingTrackId())
        }.toList()
        val hashes = if (checkDuplicates) entries.filter { it.bytes > 0 }.groupBy {
            currentCoroutineContext().ensureActive()
            Mp3Import.hash(path(owners.getJSONObject(it.track.id).getString("path"))!!)
        } else emptyMap()
        val decorated = entries.map { entry -> entry.copy(duplicates = hashes.values.firstOrNull { group ->
            group.size > 1 && group.any { it.track.id == entry.track.id }
        }?.map { it.track.id }?.filterNot { it == entry.track.id }.orEmpty()) }
        val counted = decorated.distinctBy { owners.optJSONObject(it.track.id)?.optString("path") }
        val free = runCatching { Files.createDirectories(directory); Files.getFileStore(directory).usableSpace }.getOrDefault(0)
        return MusicStorageReport(decorated, counted.sumOf { it.bytes }, counted.filterNot { it.saved }.sumOf { it.bytes },
            counted.filter { it.saved }.sumOf { it.bytes }, free)
    }
    suspend fun cleanup(ids: List<String>, allowSaved: Boolean): MusicCleanupReport {
        val deleted = mutableListOf<String>(); val messages = mutableListOf<String>(); var freed = 0L
        for (id in ids.distinct().take(300)) {
            currentCoroutineContext().ensureActive()
            val entry = report().entries.firstOrNull { it.track.id == id } ?: continue
            if (entry.playing || entry.saved && !allowSaved) { messages += entry.track.title + "：已保存或正在播放，已保留"; continue }
            val record = store.read().optJSONObject("ownedAudio")?.optJSONObject(id) ?: continue
            val file = path(record.optString("path")) ?: continue
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue
            // Never detach another logical track's shared file implicitly.
            val others = library.tracks().filter { it.id != id && path(it.sourceUri) == file }
            if (others.isNotEmpty()) { messages += entry.track.title + "：同一文件仍被其他歌曲引用"; continue }
            val original = entry.track
            val offline = original.copy(sourceUri = null)
            suspend fun updateReferences(track: Track) {
                library.update(track)
                library.savePlaylists(library.playlists().map { playlist ->
                    playlist.copy(tracks = playlist.tracks.map { if (it.id == id) track else it })
                })
            }
            try {
                updateReferences(offline)
                if (Files.deleteIfExists(file)) {
                    freed += entry.bytes; deleted += id
                    store.update { root -> root.optJSONObject("ownedAudio")?.optJSONObject(id)?.put("evicted", true)?.put("track", MusicFeatureCodec.track(offline)) }
                } else updateReferences(original)
            } catch (error: Exception) {
                if (Files.exists(file)) runCatching { updateReferences(original) }
                messages += original.title + "：未能释放本地文件"
            }
        }
        return MusicCleanupReport(deleted.size, freed, messages)
    }
    suspend fun checkBudget(extra: Long, temporary: Boolean, limitMb: Int) {
        val report = report()
        require(report.freeBytes == 0L || report.freeBytes >= extra + 32L * 1024 * 1024) { "可用空间不足，已保留现有歌曲" }
        if (temporary) require(report.temporaryBytes + extra <= limitMb.coerceIn(64, 8192) * 1024L * 1024) {
            "临时下载已达到空间上限，请到“下载与空间”清理或调整上限"
        }
    }
    /** Resolve against the real root and reject symlinks, external paths and non-file URIs. */
    internal fun path(source: String?): Path? = runCatching {
        if (source.isNullOrBlank() || source.startsWith("content:") || source.startsWith("http")) return null
        val candidate = if (source.startsWith("file:")) java.nio.file.Paths.get(URI(source)) else java.nio.file.Paths.get(source)
        val root = directory.toAbsolutePath().normalize()
        val absolute = candidate.toAbsolutePath().normalize()
        if (!absolute.startsWith(root) || absolute == root || Files.isSymbolicLink(absolute)) return null
        if (Files.exists(absolute)) {
            if (!absolute.toRealPath().startsWith(root.toRealPath())) return null
        } else if (Files.exists(absolute.parent) && !absolute.parent.toRealPath().startsWith(root.toRealPath())) return null
        absolute
    }.getOrNull()
}
