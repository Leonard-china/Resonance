package com.resonance.player.enrich

import com.resonance.player.features.MusicJsonStore
import com.resonance.player.lyrics.LrclibLyricsRepository
import com.resonance.player.model.SongRequest
import com.resonance.player.io.readUpTo
import kotlinx.coroutines.*
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path

data class AlbumCover(val path: Path, val album: String, val source: String)
fun interface MusicArtworkSource { suspend fun find(song: SongRequest, force: Boolean): AlbumCover? }

/** Cache real catalog artwork per song and album, rather than shipping an arbitrary artist picture. */
class AlbumCoverRepository(private val store: MusicJsonStore) : MusicArtworkSource {
    private val directory = store.directory.resolve("album-artwork")
    private val lookup = MetadataEnricher(directory, LrclibLyricsRepository(store.directory.resolve("unused-lyrics")))
    override suspend fun find(song: SongRequest, force: Boolean): AlbumCover? = withContext(Dispatchers.IO) {
        val query = "cover-v1:${normalize(song.artist)}:${normalize(song.title)}:${normalize(song.album)}"
        if (!force) store.cache(query)?.let { cached ->
            if (cached.optBoolean("miss") && System.currentTimeMillis() - cached.optLong("at") in 0..86_400_000) return@withContext null
            cached.optString("path").takeIf(String::isNotBlank)?.let { path ->
                if (Files.isRegularFile(java.nio.file.Paths.get(path))) return@withContext AlbumCover(java.nio.file.Paths.get(path), cached.optString("album"), cached.optString("source"))
            }
        }
        currentCoroutineContext().ensureActive()
        val found = searchQq(song)
            ?: lookup.searchItunes(song.title, song.artist)?.takeIf { matches(song, it) }
            ?: lookup.searchKugou(song.title, song.artist)?.takeIf { matches(song, it) }
        currentCoroutineContext().ensureActive()
        val url = found?.artworkUrl
        if (url.isNullOrBlank()) {
            store.cache(query, JSONObject().put("miss", true).put("at", System.currentTimeMillis()))
            return@withContext null
        }
        val albumKey = MusicJsonStore.hash(normalize(song.artist) + ":" + normalize(found.album.orEmpty()).ifBlank { normalize(song.title) })
        val path = directory.resolve("$albumKey.cover")
        val local = if (!force && Files.isRegularFile(path)) path.toString() else lookup.downloadAndSaveArtwork(albumKey, url)
        if (local == null) return@withContext null
        store.cache(query, JSONObject().put("path", local).put("album", found.album.orEmpty()).put("source", found.source))
        AlbumCover(java.nio.file.Paths.get(local), found.album.orEmpty(), found.source)
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
        }.maxByOrNull { if (normalize(it.album.orEmpty()) == normalize(song.album)) 1 else 0 }
    }
    internal fun matches(song: SongRequest, result: MetadataEnricher.OnlineSearchResult): Boolean {
        if (normalize(song.title) != normalize(result.title.orEmpty())) return false
        val artist = normalize(song.artist); val found = normalize(result.artist.orEmpty())
        return artist.isNotBlank() && found.isNotBlank() && (found == artist || found.contains(artist))
    }
    private fun normalize(value: String) = value.lowercase().filter(Char::isLetterOrDigit)
}
