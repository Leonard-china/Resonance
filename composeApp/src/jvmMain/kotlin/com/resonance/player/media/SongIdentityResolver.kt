package com.resonance.player.media

import com.resonance.player.io.readUpTo
import com.resonance.player.model.SongRequest
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

fun interface SongMetadataLookup {
    suspend fun search(song: SongRequest): List<SongRequest>
}

/** Catalog lookup returns candidates; the resolver, rather than a model, decides identity. */
class PublicSongMetadataLookup : SongMetadataLookup {
    override suspend fun search(song: SongRequest): List<SongRequest> = withContext(Dispatchers.IO) {
        withTimeoutOrNull(18_000) { lookup(song) }.orEmpty()
    }

    private suspend fun lookup(song: SongRequest): List<SongRequest> {
        val candidates = mutableListOf<SongRequest>()
        for (term in terms(song)) for (sourceName in listOf("qq", "itunes")) {
            val query = URLEncoder.encode(term, "UTF-8")
            val source = if (sourceName == "qq") "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?w=$query&format=json&p=1&n=30"
                else "https://itunes.apple.com/search?term=$query&entity=song&limit=50"
            currentCoroutineContext().ensureActive()
            val connection = URI(source).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 6_000; connection.readTimeout = 8_000; connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 Resonance")
            if (sourceName == "qq") connection.setRequestProperty("Referer", "https://y.qq.com/")
            val watchdog = CoroutineScope(currentCoroutineContext()).launch { try { awaitCancellation() } finally { connection.disconnect() } }
            try {
                if (connection.responseCode != 200) continue
                val bytes = connection.inputStream.use { it.readUpTo(1_000_001) }
                require(bytes.size <= 1_000_000)
                candidates += parse(bytes.toString(Charsets.UTF_8))
                if (candidates.any { SongIdentityResolver.matches(song, it) }) return candidates
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* A failed catalog is not permission to guess a title. */ }
            finally { watchdog.cancel(); connection.disconnect() }
        }
        return candidates
    }

    companion object {
        internal fun terms(song: SongRequest): List<String> {
            val title = SongIdentityResolver.prefix(song.title)
            val artist = SongIdentityResolver.prefix(song.artist)
            val relaxed = if (' ' in title) title.substringBeforeLast(' ').trim() else if (title.length > 3) title.dropLast(1) else title
            return listOf("$title $artist", "$relaxed $artist", artist).filter(String::isNotBlank).distinct()
        }

        internal fun parse(raw: String): List<SongRequest> {
            val data = JSONObject(raw)
            val items = data.optJSONArray("results") ?: data.optJSONObject("data")?.optJSONObject("song")?.optJSONArray("list") ?: return emptyList()
            return (0 until items.length()).mapNotNull { i ->
                val item = items.optJSONObject(i) ?: return@mapNotNull null
                fun clean(value: String) = value.replace(Regex("<[^>]*>"), "").replace("&amp;", "&").trim()
                val singers = item.optJSONArray("singer")
                val artist = if (singers == null) item.optString("artistName") else (0 until singers.length()).mapNotNull { singers.optJSONObject(it)?.optString("name") }.joinToString(" / ")
                SongRequest(clean(item.optString("trackName", item.optString("songname"))), clean(artist), clean(item.optString("collectionName", item.optString("albumname"))))
            }
        }
    }
}

object SongIdentityResolver {
    private val ellipsis = Regex("(?:\\.{2,}|…+|⋯+|︙+)\\s*$")
    fun truncated(value: String) = ellipsis.containsMatchIn(value)
    fun prefix(value: String) = value.replace(ellipsis, "").trim()
    fun searchTitle(value: String) = value.replace(Regex("\\s*[（(]\\s*(?i:feat\\.?|ft\\.?|featuring)\\s+[^）)]*[）)]"), "").trim()
    private fun key(value: String) = value.lowercase().filter(Char::isLetterOrDigit)

    fun matches(request: SongRequest, candidate: SongRequest): Boolean {
        if (truncated(candidate.title) || truncated(candidate.artist)) return false
        val title = key(prefix(request.title)); val artist = key(prefix(request.artist))
        if (title.length < 2 || artist.isBlank()) return false
        val foundTitle = key(candidate.title); val foundArtist = key(candidate.artist)
        val titleMatches = if (truncated(request.title)) foundTitle.startsWith(title) else foundTitle == title
        val artistMatches = if (truncated(request.artist)) foundArtist.startsWith(artist) else foundArtist == artist
        if (!titleMatches || !artistMatches) return false
        if (listOf('+', '＋').any { it in candidate.title && it !in request.title }) return false
        val versionSuffix = Regex("[（(\\[【][^）)\\]】]*版[^）)\\]】]*[）)\\]】]")
        if (request.title.none { it in "（([【" } && versionSuffix.containsMatchIn(candidate.title)) return false
        val variants = listOf("伴奏", "现场", "演唱会", "翻唱", "变速", "混音", "加速", "减速", "铃声", "剪辑", "片段", "串烧", "女声版", "男声版")
        if (variants.any { candidate.title.contains(it, true) && !request.title.contains(it, true) }) return false
        val english = Regex("(?<![a-z])(dj|live|remix|cover|instrumental|remaster(?:ed)?)(?![a-z])", RegexOption.IGNORE_CASE)
        return english.findAll(candidate.title).none { !request.title.contains(it.value, true) }
    }

    fun choose(request: SongRequest, candidates: List<SongRequest>): SongRequest? {
        val valid = candidates.filter { matches(request, it) }.distinctBy { key(it.title) + "|" + key(it.artist) }
        check(valid.size <= 1) { "歌名被截断且存在多个候选，请补充完整名称：${valid.take(4).joinToString("；") { "${it.artist} - ${it.title}" }}" }
        return valid.singleOrNull()?.let { it.copy(album = request.album.ifBlank { it.album }, reason = request.reason) }
    }

    fun videoNames(request: SongRequest, videos: List<com.resonance.player.model.BilibiliVideo>): List<SongRequest> = videos.flatMap { video ->
        // A video title cannot establish the missing artist from a request prefix alone.
        if (truncated(request.artist)) return@flatMap emptyList()
        val quoted = Regex("[《〈]([^》〉]+)[》〉]").findAll(video.title).map { it.groupValues[1] }.toList()
        val artistFirst = Regex("^\\s*${Regex.escape(prefix(request.artist))}\\s*[-—–]\\s*(.+?)(?:\\s*[|｜].*)?$", RegexOption.IGNORE_CASE).find(video.title)?.groupValues?.get(1)
        val evidence = key(video.title).contains(key(prefix(request.artist))) || key(video.uploader) == key(prefix(request.artist))
        if (!evidence) emptyList() else (quoted + listOfNotNull(artistFirst)).map { SongRequest(it.trim(), prefix(request.artist), request.album) }
    }
}
