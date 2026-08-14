package com.resonance.player.playlist

import com.resonance.player.model.Playlist
import com.resonance.player.model.PlaylistImportReport
import com.resonance.player.model.Track
import com.resonance.player.io.readUpTo
import org.json.JSONObject
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.UnknownHostException
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.TreeMap

class KugouPlaylistImporter {
    fun import(link: String, localTracks: List<Track>, installationId: String): PlaylistImportReport {
        val globalCollectionId = resolveWithRetry(link)
        val response = requestPlaylistWithRetry(globalCollectionId, installationId)
        return parseOfficialResponse(response, globalCollectionId, localTracks)
    }

    private fun requestPlaylistWithRetry(globalCollectionId: String, installationId: String): String {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                return requestAllPlaylistPages(globalCollectionId, installationId)
            } catch (error: Throwable) {
                lastError = error
                if (!error.isTransientNetworkFailure() || attempt == 2) throw error
                Thread.sleep(350L * (attempt + 1))
            }
        }
        throw requireNotNull(lastError)
    }

    private fun resolveWithRetry(link: String): String {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                return resolveGlobalCollectionId(link)
            } catch (error: Throwable) {
                lastError = error
                if (!error.isTransientNetworkFailure() || attempt == 2) throw error
                Thread.sleep(250L * (attempt + 1))
            }
        }
        throw requireNotNull(lastError)
    }

    private fun Throwable.isTransientNetworkFailure(): Boolean = when (this) {
        is UnknownHostException -> true
        is IOException -> true
        else -> cause?.isTransientNetworkFailure() == true
    }

    internal fun parseOfficialResponse(
        response: String,
        globalCollectionId: String,
        localTracks: List<Track>,
    ): PlaylistImportReport {
        val root = JSONObject(response)
        require(root.optInt("error_code", -1) == 0) {
            root.optString("error_msg").ifBlank { "酷狗返回了错误" }
        }
        val data = root.getJSONObject("data")
        val listInfo = data.getJSONObject("list_info")
        val songs = data.getJSONArray("songs")
        require(songs.length() in 0..MAX_TRACKS) { "歌单曲目数量异常" }

        val localByTitle = localTracks.groupBy { normalize(it.title) }
        var matched = 0
        val tracks = buildList {
            for (index in 0 until songs.length()) {
                val song = songs.getJSONObject(index)
                val displayName = song.optString("name")
                val singerInfo = song.optJSONArray("singerinfo")
                val artist = singerInfo?.optJSONObject(0)?.optString("name")
                    ?.takeIf(String::isNotBlank)
                    ?: displayName.substringBefore(" - ", "未知歌手").ifBlank { "未知歌手" }
                val title = displayName.substringAfter(" - ", displayName).ifBlank { "未知曲目" }
                val album = song.optJSONObject("albuminfo")?.optString("name")
                    ?.takeIf(String::isNotBlank) ?: "未知专辑"
                val artwork = song.optString("cover")
                    .takeIf(String::isNotBlank)
                    ?.replace("{size}", "400")
                    ?.replaceFirst("http://", "https://")
                val local = localByTitle[normalize(title)]?.firstOrNull { candidate ->
                    val localArtist = normalize(candidate.artist)
                    val remoteArtist = normalize(artist)
                    localArtist == remoteArtist || localArtist.contains(remoteArtist) || remoteArtist.contains(localArtist)
                }
                if (local != null) {
                    matched += 1
                    add(
                        local.copy(
                            artworkPath = local.artworkPath ?: artwork,
                            artworkSeed = if (local.artworkPath == null) {
                                song.optString("hash").ifBlank { "$globalCollectionId-$index" }.hashCode()
                            } else {
                                local.artworkSeed
                            },
                        ),
                    )
                } else {
                    val hash = song.optString("hash").ifBlank { "$globalCollectionId-$index" }
                    val milliseconds = song.optLong("timelen", 0L).coerceAtLeast(0L)
                    val seconds = milliseconds / 1_000L
                    add(
                        Track(
                            id = "catalog-kugou-${hash.lowercase()}",
                            title = title,
                            artist = artist,
                            album = album,
                            durationText = "%d:%02d".format(seconds / 60, seconds % 60),
                            artworkSeed = hash.hashCode(),
                            sourceUri = null,
                            artworkPath = artwork,
                            mimeType = CATALOG_MIME_TYPE,
                        ),
                    )
                }
            }
        }
        val name = listInfo.optString("name").ifBlank { "酷狗歌单" }.take(80)
        val playlist = Playlist(
            id = "kugou-${sha256(globalCollectionId).take(24)}",
            name = name,
            subtitle = "${tracks.size} 首 · 已匹配 $matched 首本地音乐",
            tracks = tracks,
            artworkSeed = globalCollectionId.hashCode(),
        )
        return PlaylistImportReport(
            playlist = playlist,
            catalogTrackCount = tracks.size,
            matchedTrackCount = matched,
            message = "已导入「$name」：${tracks.size} 首，$matched 首可直接播放",
        )
    }

    internal fun resolveGlobalCollectionId(link: String): String {
        var uri = parseAllowedLink(link)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            queryParameter(uri, "global_collection_id")?.takeIf(::isValidCollectionId)?.let { return it }
            require(redirectCount < MAX_REDIRECTS) { "酷狗分享链接重定向次数过多" }
            val connection = openKugouConnection(uri).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
            }
            connectWithFriendlyDnsError(connection, uri.host)
            connection.useConnection {
                val code = responseCode
                require(code in 300..399) { "无法从该酷狗链接读取歌单标识（HTTP $code）" }
                val location = getHeaderField("Location") ?: error("酷狗分享链接缺少跳转地址")
                uri = parseAllowedLink(uri.resolve(location).toString())
            }
        }
        error("无法识别酷狗歌单")
    }

    private fun openKugouConnection(uri: URI): HttpURLConnection {
        return try {
            uri.toURL().openConnection() as HttpURLConnection
        } catch (error: UnknownHostException) {
            throw IllegalStateException("暂时无法解析酷狗服务器 ${uri.host}；请检查 VPN、私人 DNS 或网络后重试", error)
        }
    }

    private fun connectWithFriendlyDnsError(connection: HttpURLConnection, host: String) {
        try {
            connection.connect()
        } catch (error: UnknownHostException) {
            // Trigger a second resolver lookup so Android's resolver cache is refreshed;
            // the caller can then retry without seeing a raw Java networking exception.
            runCatching { InetAddress.getAllByName(host) }
            throw IllegalStateException("暂时无法解析酷狗服务器 $host；请检查 VPN、私人 DNS 或网络后重试", error)
        }
    }

    private fun requestAllPlaylistPages(globalCollectionId: String, installationId: String): String {
        val first = JSONObject(requestOfficialPlaylist(globalCollectionId, installationId, 0))
        val data = first.getJSONObject("data")
        val songs = data.getJSONArray("songs")
        val total = data.optInt("count", songs.length())
        require(total in 0..MAX_TRACKS) { "歌单曲目过多，当前最多支持 $MAX_TRACKS 首" }
        var offset = songs.length()
        while (offset < total) {
            val next = JSONObject(requestOfficialPlaylist(globalCollectionId, installationId, offset))
                .getJSONObject("data").getJSONArray("songs")
            require(next.length() > 0) { "酷狗歌单分页数据不完整" }
            for (index in 0 until next.length()) songs.put(next.getJSONObject(index))
            offset += next.length()
        }
        return first.toString()
    }

    private fun requestOfficialPlaylist(globalCollectionId: String, installationId: String, beginIndex: Int): String {
        val identity = md5Hex(installationId)
        val mid = BigInteger(1, md5(installationId.toByteArray(StandardCharsets.UTF_8))).toString()
        val params = TreeMap<String, String>().apply {
            put("appid", APP_ID)
            put("begin_idx", beginIndex.toString())
            put("clienttime", (System.currentTimeMillis() / 1_000L).toString())
            put("clientver", CLIENT_VERSION)
            put("dfid", "-")
            put("global_collection_id", globalCollectionId)
            put("mid", mid)
            put("mode", "1")
            put("module", "CloudMusic")
            put("need_rd", "0")
            put("need_sort", "1")
            put("pagesize", PAGE_SIZE.toString())
            put("personal_switch", "1")
            put("plat", "1")
            put("token", "")
            put("type", "1")
            put("userid", "0")
            put("uuid", identity)
        }
        val material = params.entries.joinToString("") { "${it.key}=${it.value}" }
        params["signature"] = md5Hex(ANDROID_SALT + material + ANDROID_SALT)
        val query = params.entries.joinToString("&") {
            "${encode(it.key)}=${encode(it.value)}"
        }
        val uri = URI("https://gateway.kugou.com/pubsongs/v2/get_other_list_file_nofilt?$query")
        val connection = openKugouConnection(uri).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
        }
        connectWithFriendlyDnsError(connection, uri.host)
        return connection.useConnection {
            require(responseCode == HttpURLConnection.HTTP_OK) { "酷狗歌单接口不可用（HTTP $responseCode）" }
            val declaredLength = contentLengthLong
            require(declaredLength < 0 || declaredLength <= MAX_RESPONSE_BYTES) { "酷狗歌单响应过大" }
            inputStream.buffered().use { input ->
                val bytes = input.readUpTo(MAX_RESPONSE_BYTES + 1)
                require(bytes.size <= MAX_RESPONSE_BYTES) { "酷狗歌单响应过大" }
                bytes.toString(StandardCharsets.UTF_8)
            }
        }
    }

    private fun parseAllowedLink(raw: String): URI {
        val uri = runCatching { URI(raw.trim()) }.getOrElse { error("歌单链接格式无效") }
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) { "只支持 HTTP/HTTPS 歌单链接" }
        val host = uri.host?.lowercase() ?: error("歌单链接缺少域名")
        require(host == "kugou.com" || host.endsWith(".kugou.com")) { "当前版本仅支持酷狗公开歌单链接" }
        return uri
    }

    private fun queryParameter(uri: URI, name: String): String? = uri.rawQuery
        ?.split('&')
        ?.asSequence()
        ?.map { it.substringBefore('=') to it.substringAfter('=', "") }
        ?.firstOrNull { it.first == name }
        ?.second
        ?.let { java.net.URLDecoder.decode(it, StandardCharsets.UTF_8) }

    private fun isValidCollectionId(value: String): Boolean =
        value.matches(Regex("collection_[A-Za-z0-9_-]{3,120}"))

    private fun normalize(value: String): String = value.lowercase().filter(Char::isLetterOrDigit)
    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
    private fun md5(value: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(value)
    private fun md5Hex(value: String): String = md5(value.toByteArray(StandardCharsets.UTF_8)).toHex()
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8)).toHex()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private inline fun <T> HttpURLConnection.useConnection(block: HttpURLConnection.() -> T): T =
        try { block() } finally { disconnect() }

    companion object {
        const val CATALOG_MIME_TYPE = "application/vnd.resonance.catalog+audio"
        private const val APP_ID = "1005"
        private const val CLIENT_VERSION = "20489"
        private const val ANDROID_SALT = "OIlwieks28dk2k092lksi2UIkp"
        private const val USER_AGENT = "Android15-1070-20489-46-0-DiscoveryDRADProtocol-wifi"
        private const val MAX_TRACKS = 500
        private const val PAGE_SIZE = 100
        private const val MAX_REDIRECTS = 5
        private const val MAX_RESPONSE_BYTES = 2_000_000
        private const val CONNECT_TIMEOUT_MS = 12_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
