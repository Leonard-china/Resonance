package com.resonance.player.lyrics

import com.resonance.player.io.readUpTo
import com.resonance.player.model.LyricLine
import com.resonance.player.model.Lyrics
import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.Track
import com.resonance.player.model.durationTextToSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.math.abs

/** Fetches synchronized lyrics without requiring users to paste lyric text. */
class LrclibLyricsRepository(
    private val cacheDirectory: Path,
    private val apiBase: URI = URI("https://lrclib.net"),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun fetch(track: Track, forceRefresh: Boolean = false): LyricsFetchResult = withContext(Dispatchers.IO) {
        if (!forceRefresh) readCache(track)?.let { return@withContext it }

        val result = try {
            fetchRemote(track)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            LyricsFetchResult.Unavailable(
                message = when {
                    error.message.orEmpty().contains("429") -> "歌词服务请求较多，请稍后再试"
                    else -> "自动歌词暂时不可用：${error.message ?: "网络连接失败"}"
                },
                retryable = true,
            )
        }
        if (result is LyricsFetchResult.Found || (result is LyricsFetchResult.Unavailable && !result.retryable)) {
            writeCache(track, result)
        }
        result
    }

    private fun fetchRemote(track: Track): LyricsFetchResult {
        val duration = durationTextToSeconds(track.durationText)
        if (duration > 0) {
            when (val exact = request(
                "/api/get",
                mapOf(
                    "track_name" to track.title,
                    "artist_name" to track.artist,
                    "album_name" to track.album,
                    "duration" to duration.toString(),
                ),
            )) {
                is HttpResult.Success -> parseRecord(exact.body, track, fromCache = false)?.let {
                    return LyricsFetchResult.Found(it)
                }
                is HttpResult.Failure -> if (exact.code != HttpURLConnection.HTTP_NOT_FOUND) {
                    throw IllegalStateException("歌词服务返回 HTTP ${exact.code}")
                }
            }
        }

        val searchParameters = buildMap {
            put("track_name", track.title)
            if (normalize(track.artist) != normalize("未知歌手")) put("artist_name", track.artist)
        }
        val search = request("/api/search", searchParameters)
        if (search is HttpResult.Failure) {
            if (search.code == HttpURLConnection.HTTP_NOT_FOUND) {
                return LyricsFetchResult.Unavailable("暂未找到这首歌的歌词", retryable = false)
            }
            throw IllegalStateException("歌词服务返回 HTTP ${search.code}")
        }
        val candidates = JSONArray((search as HttpResult.Success).body)
        val best = (0 until candidates.length())
            .mapNotNull(candidates::optJSONObject)
            .map { it to score(it, track, duration) }
            .filter { it.second >= MINIMUM_MATCH_SCORE }
            .maxByOrNull { it.second }
            ?.first
            ?: return LyricsFetchResult.Unavailable("暂未找到匹配的歌词", retryable = false)

        return parseRecord(best.toString(), track, fromCache = false)
            ?.let(LyricsFetchResult::Found)
            ?: LyricsFetchResult.Unavailable("歌词记录为空", retryable = false)
    }

    private fun request(path: String, parameters: Map<String, String>): HttpResult {
        val query = parameters.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val connection = apiBase.resolve("$path?$query").toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.buffered()?.use { input ->
                val bytes = input.readUpTo(MAX_RESPONSE_BYTES + 1)
                require(bytes.size <= MAX_RESPONSE_BYTES) { "歌词响应过大" }
                bytes.toString(StandardCharsets.UTF_8)
            }.orEmpty()
            if (code in 200..299) HttpResult.Success(body) else HttpResult.Failure(code, body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRecord(raw: String, track: Track, fromCache: Boolean): Lyrics? {
        val record = JSONObject(raw)
        val instrumental = record.optBoolean("instrumental", false)
        val synchronizedLines = parseLrc(record.optString("syncedLyrics"))
        val lines = if (synchronizedLines.isNotEmpty()) {
            synchronizedLines
        } else {
            record.optString("plainLyrics")
                .lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map { LyricLine(timestampMs = null, text = it) }
                .toList()
        }
        if (!instrumental && lines.isEmpty()) return null
        return Lyrics(
            trackId = track.id,
            lines = lines,
            synchronized = synchronizedLines.isNotEmpty(),
            instrumental = instrumental,
            source = "LRCLIB",
            fromCache = fromCache,
        )
    }

    private fun score(candidate: JSONObject, track: Track, requestedDuration: Int): Int {
        val title = normalize(candidate.optString("trackName"))
        val requestedTitle = normalize(track.title)
        if (title != requestedTitle) return Int.MIN_VALUE

        val artist = normalize(candidate.optString("artistName"))
        val requestedArtist = normalize(track.artist)
        val unknownArtist = requestedArtist.isBlank() || requestedArtist == normalize("未知歌手")
        if (!unknownArtist && artist != requestedArtist && !artist.contains(requestedArtist) && !requestedArtist.contains(artist)) {
            return Int.MIN_VALUE
        }

        var score = 8
        if (artist == requestedArtist) score += 5 else if (!unknownArtist) score += 2
        if (normalize(candidate.optString("albumName")) == normalize(track.album)) score += 2
        if (requestedDuration > 0) {
            val difference = abs(candidate.optDouble("duration", 0.0) - requestedDuration)
            score += when {
                difference <= 2.0 -> 6
                difference <= 8.0 -> 3
                difference <= 15.0 -> 1
                else -> -8
            }
        }
        if (candidate.optString("syncedLyrics").isNotBlank()) score += 2
        return score
    }

    private fun readCache(track: Track): LyricsFetchResult? {
        val file = cacheFile(track)
        if (!Files.isRegularFile(file)) return null
        return runCatching {
            val raw = Files.newBufferedReader(file, StandardCharsets.UTF_8).use { it.readText() }
            val root = JSONObject(raw)
            val fetchedAt = root.getLong("fetchedAt")
            val status = root.getString("status")
            val maxAge = if (status == "found") FOUND_CACHE_MAX_AGE_MS else MISS_CACHE_MAX_AGE_MS
            if (nowMillis() - fetchedAt !in 0..maxAge) return@runCatching null
            if (status == "found") {
                val lyrics = parseRecord(root.getJSONObject("record").toString(), track, fromCache = true)
                    ?: return@runCatching null
                LyricsFetchResult.Found(lyrics)
            } else {
                LyricsFetchResult.Unavailable(
                    message = root.optString("message").ifBlank { "暂未找到匹配的歌词" },
                    retryable = root.optBoolean("retryable", false),
                )
            }
        }.getOrNull()
    }

    private fun writeCache(track: Track, result: LyricsFetchResult) {
        runCatching {
            Files.createDirectories(cacheDirectory)
            val root = JSONObject().put("fetchedAt", nowMillis())
            when (result) {
                is LyricsFetchResult.Found -> {
                    // Re-fetching the selected record keeps the on-disk format identical to the API contract.
                    val record = JSONObject()
                        .put("instrumental", result.lyrics.instrumental)
                        .put(
                            "syncedLyrics",
                            if (result.lyrics.synchronized) result.lyrics.lines.joinToString("\n") { line ->
                                val timestamp = requireNotNull(line.timestampMs)
                                "[%02d:%02d.%02d] %s".format(
                                    timestamp / 60_000,
                                    timestamp / 1_000 % 60,
                                    timestamp / 10 % 100,
                                    line.text,
                                )
                            } else "",
                        )
                        .put(
                            "plainLyrics",
                            if (result.lyrics.synchronized) "" else result.lyrics.lines.joinToString("\n", transform = LyricLine::text),
                        )
                    root.put("status", "found").put("record", record)
                }
                is LyricsFetchResult.Unavailable -> root
                    .put("status", "unavailable")
                    .put("message", result.message)
                    .put("retryable", result.retryable)
            }
            val target = cacheFile(track)
            val temporary = target.resolveSibling("${target.fileName}.tmp")
            Files.newBufferedWriter(temporary, StandardCharsets.UTF_8).use { it.write(root.toString()) }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun cacheFile(track: Track): Path {
        val signature = "${track.id}\u0000${track.title}\u0000${track.artist}\u0000${track.album}\u0000${track.durationText}"
        val name = MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return cacheDirectory.resolve("$name.json")
    }

    @Suppress("DEPRECATION")
    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    private fun normalize(value: String): String = value.lowercase().filter(Char::isLetterOrDigit)

    private sealed interface HttpResult {
        data class Success(val body: String) : HttpResult
        data class Failure(val code: Int, val body: String) : HttpResult
    }

    companion object {
        private const val USER_AGENT = "Resonance/0.2.0 (https://github.com/Leonard-china/Resonance)"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 18_000
        private const val MAX_RESPONSE_BYTES = 1_500_000
        private const val MINIMUM_MATCH_SCORE = 10
        private const val FOUND_CACHE_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1_000
        private const val MISS_CACHE_MAX_AGE_MS = 24L * 60 * 60 * 1_000
    }
}

internal fun parseLrc(raw: String): List<LyricLine> {
    if (raw.isBlank()) return emptyList()
    val timestampPattern = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    return raw.lineSequence().flatMap { rawLine ->
        val matches = timestampPattern.findAll(rawLine).toList()
        if (matches.isEmpty()) return@flatMap emptySequence()
        val text = timestampPattern.replace(rawLine, "").trim()
        if (text.isEmpty()) return@flatMap emptySequence()
        matches.asSequence().map { match ->
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val fraction = match.groupValues[3]
            val milliseconds = fraction.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            LyricLine(timestampMs = minutes * 60_000 + seconds * 1_000 + milliseconds, text = text)
        }
    }.sortedBy { it.timestampMs }.distinctBy { it.timestampMs to it.text }.toList()
}
