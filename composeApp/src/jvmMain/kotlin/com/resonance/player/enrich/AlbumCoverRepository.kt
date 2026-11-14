package com.resonance.player.enrich

import com.resonance.player.features.MusicJsonStore
import com.resonance.player.lyrics.LrclibLyricsRepository
import com.resonance.player.model.catalogTrackTitle
import com.resonance.player.model.SongRequest
import com.resonance.player.io.readUpTo
import kotlinx.coroutines.*
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path

data class AlbumCover(val path: Path, val album: String, val source: String)
fun interface MusicArtworkSource { suspend fun find(song: SongRequest, force: Boolean): AlbumCover? }

/** Cache real catalog artwork per song and album, rather than shipping an arbitrary artist picture. */
class AlbumCoverRepository(
    private val store: MusicJsonStore,
    private val catalogSources: List<suspend (SongRequest) -> MetadataEnricher.OnlineSearchResult?>? = null,
    private val saveArtwork: (suspend (String, String) -> String?)? = null,
) : MusicArtworkSource {
    private val directory = store.directory.resolve("album-artwork")
    private val lookup = MetadataEnricher(directory, LrclibLyricsRepository(store.directory.resolve("unused-lyrics")))
    override suspend fun find(song: SongRequest, force: Boolean): AlbumCover? = withContext(Dispatchers.IO) {
        val request = song.copy(title = catalogTrackTitle(song.title, song.artist))
        val query = "cover-v2:${normalize(request.artist)}:${normalize(request.title)}:${normalize(request.album)}"
        if (!force) store.cache(query)?.let { cached ->
            if (cached.optBoolean("miss") && System.currentTimeMillis() - cached.optLong("at") in 0..900_000) return@withContext null
            cached.optString("path").takeIf(String::isNotBlank)?.let { path ->
                if (Files.isRegularFile(java.nio.file.Paths.get(path))) return@withContext AlbumCover(java.nio.file.Paths.get(path), cached.optString("album"), cached.optString("source"))
            }
        }
        val sources = catalogSources ?: listOf<suspend (SongRequest) -> MetadataEnricher.OnlineSearchResult?>(
            { searchQq(it) }, { lookup.searchItunes(it.title, it.artist) }, { lookup.searchKugou(it.title, it.artist) })
        var foundCandidate = false
        for (source in sources) {
            currentCoroutineContext().ensureActive()
            val found = try { source(request)?.takeIf { matches(request, it) } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            if (found == null) continue
            val url = found.artworkUrl?.takeIf(String::isNotBlank) ?: continue
            foundCandidate = true
            val albumKey = MusicJsonStore.hash(normalize(request.artist) + ":" + normalize(found.album.orEmpty()).ifBlank { normalize(request.title) })
            val path = directory.resolve("$albumKey.cover")
            val local = try {
                if (!force && Files.isRegularFile(path)) path.toString()
                else if (saveArtwork != null) saveArtwork.invoke(albumKey, url)
                else lookup.downloadAndSaveArtwork(albumKey, url)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
            currentCoroutineContext().ensureActive()
            if (local == null) continue
            store.cache(query, JSONObject().put("path", local).put("album", found.album.orEmpty()).put("source", found.source))
            return@withContext AlbumCover(java.nio.file.Paths.get(local), found.album.orEmpty(), found.source)
        }
        // An image transport failure is retryable; it is not evidence that the song is absent.
        if (!foundCandidate) store.cache(query, JSONObject().put("miss", true).put("at", System.currentTimeMillis()))
        null
    }

    private fun searchQq(song: SongRequest): MetadataEnricher.OnlineSearchResult? = runCatching {
        val query = java.net.URLEncoder.encode("${song.title} ${song.artist}", "UTF-8")
        val connection = java.net.URI("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?w=$query&format=json&p=1&n=10").toURL().openConnection() as java.net.HttpURLConnection
        try {
            connection.connectTimeout = 6000; connection.readTimeout = 8000; connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 Resonance")
            connection.setRequestProperty("Referer", "https://y.qq.com/")
            if (connection.responseCode != 200) return@runCatching null
            val bytes = connection.inputStream.use { it.readUpTo(1_000_001) }
            require(bytes.size <= 1_000_000) { "专辑目录响应过大" }
            parseQqCatalog(String(bytes, Charsets.UTF_8), song)
        } finally { connection.disconnect() }
    }.getOrNull()
    internal fun parseQqCatalog(raw: String, song: SongRequest): MetadataEnricher.OnlineSearchResult? {
        val root = JSONObject(raw)
        if (root.optInt("code", -1) != 0) return null
        val array = root.optJSONObject("data")?.optJSONObject("song")?.optJSONArray("list") ?: return null
        return (0 until array.length()).mapNotNull { n ->
            val item = array.optJSONObject(n) ?: return@mapNotNull null
            val singers = item.optJSONArray("singer") ?: return@mapNotNull null
            val artist = (0 until singers.length()).mapNotNull { singers.optJSONObject(it)?.optString("name") }.joinToString(" / ")
            val mid = item.optString("albummid").takeIf { it.matches(Regex("[A-Za-z0-9]{10,32}")) } ?: return@mapNotNull null
            MetadataEnricher.OnlineSearchResult(item.optString("songname"), artist, item.optString("albumname"), "https://y.gtimg.cn/music/photo_new/T002R800x800M000$mid.jpg", "QQ音乐目录")
                .takeIf { matches(song, it) }
        }.maxByOrNull { candidate ->
            val album = candidate.album.orEmpty()
            when {
                normalize(song.album).isNotBlank() && normalize(album) == normalize(song.album) -> 2
                compilationTitle.containsMatchIn(album) -> 0
                else -> 1
            }
        }
    }
    internal fun matches(song: SongRequest, result: MetadataEnricher.OnlineSearchResult): Boolean {
        if (normalize(song.title) != normalize(result.title.orEmpty())) return false
        val artist = normalize(song.artist)
        val found = result.artist.orEmpty()
        return artist.isNotBlank() && (normalize(found) == artist ||
            found.split(" / ").any { normalize(it) == artist })
    }
    private val compilationTitle = Regex("(?i)greatest hits|the best of|精选|合辑|合集|金曲")
    private fun normalize(value: String) = value.lowercase().filter(Char::isLetterOrDigit)
}
