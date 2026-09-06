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
        val extractedLink = extractUrlFromText(link)
        val target = resolveWithRetry(extractedLink)
        return when (target) {
            is ImportTarget.Collection -> {
                val response = requestPlaylistWithRetry(target.collectionId, installationId)
                parseOfficialResponse(response, target.collectionId, localTracks)
            }
            is ImportTarget.SingleSong -> {
                parseSingleSongResponse(target.hash, target.albumId, localTracks)
            }
            is ImportTarget.EmbeddedPlaylist -> {
                parseEmbeddedPlaylistResponse(target.name, target.id, target.songs, localTracks)
            }
        }
    }

    fun extractUrlFromText(raw: String): String {
        val regex = Regex("""https?://[A-Za-z0-9_.\-~%!$&'()*+,;=:@/?#]+""")
        val match = regex.find(raw)
        return match?.value ?: raw.trim()
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

    private fun resolveWithRetry(link: String): ImportTarget {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                return resolveTarget(link)
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

    internal fun parseSingleSongResponse(
        hash: String,
        albumId: String?,
        localTracks: List<Track>,
    ): PlaylistImportReport {
        val songInfo = requestSongInfo(hash, albumId)
        val rawTitle = songInfo.optString("song_name").ifBlank {
            songInfo.optString("songName").ifBlank {
                songInfo.optString("audio_name").ifBlank { "未知曲目" }
            }
        }
        val title = rawTitle.substringAfter(" - ", rawTitle).ifBlank { "未知曲目" }
        val artist = songInfo.optString("author_name").ifBlank {
            songInfo.optString("singerName").ifBlank {
                songInfo.optString("singer_name").ifBlank {
                    rawTitle.substringBefore(" - ", "未知歌手")
                }
            }
        }.ifBlank { "未知歌手" }
        val album = songInfo.optString("album_name").ifBlank { "未知专辑" }
        val timelenSec = songInfo.optLong("timelength", 0L).takeIf { it > 0 }?.div(1000L)
            ?: songInfo.optLong("timeLength", 0L).takeIf { it > 0 }
            ?: (songInfo.optLong("timelen", 0L) / 1000L)
        val artwork = songInfo.optString("img").ifBlank {
            songInfo.optString("imgUrl").ifBlank {
                songInfo.optString("cover")
            }
        }.takeIf(String::isNotBlank)?.replace("{size}", "400")?.replaceFirst("http://", "https://")

        val local = localTracks.firstOrNull { matchesTrack(it, title, artist) }

        val track = if (local != null) {
            local.copy(
                artworkPath = local.artworkPath ?: artwork,
                artworkSeed = if (local.artworkPath == null) hash.hashCode() else local.artworkSeed,
            )
        } else {
            Track(
                id = "catalog-kugou-${hash.lowercase()}",
                title = title,
                artist = artist,
                album = album,
                durationText = "%d:%02d".format(timelenSec / 60, timelenSec % 60),
                artworkSeed = hash.hashCode(),
                sourceUri = null,
                artworkPath = artwork,
                mimeType = CATALOG_MIME_TYPE,
            )
        }

        val matched = if (local != null) 1 else 0
        val playlist = Playlist(
            id = "kugou-single-${sha256(hash).take(20)}",
            name = "单曲: $title",
            subtitle = if (matched > 0) "1 首 · 已匹配本地音乐" else "1 首 · 待匹配",
            tracks = listOf(track),
            artworkSeed = hash.hashCode(),
        )

        return PlaylistImportReport(
            playlist = playlist,
            catalogTrackCount = 1,
            matchedTrackCount = matched,
            message = "已导入单曲「$title」${if (matched > 0) "（已匹配本地音乐）" else ""}",
        )
    }

    private fun requestSongInfo(hash: String, albumId: String?): JSONObject {
        val query = buildString {
            append("r=play/getdata&hash=").append(hash)
            if (!albumId.isNullOrBlank()) append("&album_id=").append(albumId)
            append("&dfid=-&mid=-&platid=4")
        }
        val uri = URI("https://wwwapi.kugou.com/yy/index.php?$query")
        return runCatching {
            val conn = openKugouConnection(uri).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
            }
            conn.useConnection {
                val body = inputStream.buffered().use { it.readUpTo(MAX_RESPONSE_BYTES).toString(StandardCharsets.UTF_8) }
                val root = JSONObject(body)
                if (root.optInt("status", -1) == 1) root.getJSONObject("data") else JSONObject()
            }
        }.getOrElse {
            // 备用接口
            val fallbackUri = URI("https://m.kugou.com/app/i/getSongInfo.php?cmd=playInfo&hash=$hash")
            val conn = openKugouConnection(fallbackUri).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
            }
            conn.useConnection {
                val body = inputStream.buffered().use { it.readUpTo(MAX_RESPONSE_BYTES).toString(StandardCharsets.UTF_8) }
                val root = JSONObject(body)
                if (root.optInt("error_code", 0) == 0 && root.has("data")) root.getJSONObject("data") else root
            }
        }
    }

    internal fun parseEmbeddedPlaylistResponse(
        name: String,
        id: String,
        songs: org.json.JSONArray,
        localTracks: List<Track>,
    ): PlaylistImportReport {
        val (tracks, matched) = parseSongs(songs, id, localTracks)
        val playlist = Playlist(
            id = "kugou-${sha256(id).take(24)}",
            name = name,
            subtitle = "${tracks.size} 首 · 已匹配 $matched 首本地音乐",
            tracks = tracks,
            artworkSeed = id.hashCode(),
        )
        return PlaylistImportReport(
            playlist = playlist,
            catalogTrackCount = tracks.size,
            matchedTrackCount = matched,
            message = "已导入「$name」：${tracks.size} 首，$matched 首可直接播放",
        )
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

        val (tracks, matched) = parseSongs(songs, globalCollectionId, localTracks)
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

    private fun parseSongs(
        songs: org.json.JSONArray,
        collectionId: String,
        localTracks: List<Track>,
    ): Pair<List<Track>, Int> {
        var matched = 0
        val tracks = buildList {
            for (index in 0 until songs.length()) {
                val song = songs.optJSONObject(index) ?: continue
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
                val local = localTracks.firstOrNull { matchesTrack(it, title, artist) }
                if (local != null) {
                    matched += 1
                    add(
                        local.copy(
                            artworkPath = local.artworkPath ?: artwork,
                            artworkSeed = if (local.artworkPath == null) {
                                song.optString("hash").ifBlank { "$collectionId-$index" }.hashCode()
                            } else {
                                local.artworkSeed
                            },
                        ),
                    )
                } else {
                    val hash = song.optString("hash").ifBlank { "$collectionId-$index" }
                    val milliseconds = song.optLong("timelen", 0L).takeIf { it > 0 }
                        ?: song.optLong("time_len", 0L).takeIf { it > 0 }
                        ?: (song.optLong("duration", 0L) * 1000L).coerceAtLeast(0L)
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
        return Pair(tracks, matched)
    }

    internal fun resolveTarget(link: String): ImportTarget {
        var uri = parseAllowedLink(link)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            // 1. 检查官方歌单 ID
            extractOfficialCollectionId(uri)?.let { return ImportTarget.Collection(it) }

            // 2. 检查单曲 Hash
            extractSongHash(uri)?.let { return it }

            require(redirectCount < MAX_REDIRECTS) { "酷狗分享链接重定向次数过多" }
            val connection = openKugouConnection(uri).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15")
            }
            connectWithFriendlyDnsError(connection, uri.host)
            connection.useConnection {
                val code = responseCode
                if (code in 300..399) {
                    val location = getHeaderField("Location") ?: error("酷狗分享链接缺少跳转地址")
                    uri = parseAllowedLink(uri.resolve(location).toString())
                } else if (code == 200) {
                    // 读取页面 HTML 内容判断是否包含 SSR window.$output、单曲 hash 或 collection ID
                    val body = inputStream.buffered().use {
                        it.readUpTo(MAX_RESPONSE_BYTES).toString(StandardCharsets.UTF_8)
                    }

                    // 检查 window.$output
                    val outputMarker = "window.\$output = "
                    val startIndex = body.indexOf(outputMarker)
                    if (startIndex != -1) {
                        val jsonStart = startIndex + outputMarker.length
                        val jsonEnd = body.indexOf("};\n", jsonStart).let { if (it != -1) it + 1 else body.indexOf("};", jsonStart) + 1 }
                        if (jsonEnd > jsonStart) {
                            val jsonStr = body.substring(jsonStart, jsonEnd)
                            val root = runCatching { JSONObject(jsonStr) }.getOrNull()
                            if (root != null) {
                                val info = root.optJSONObject("info")
                                val listinfo = info?.optJSONObject("listinfo")
                                val songs = info?.optJSONArray("songs")
                                val gcid = listinfo?.optString("global_collection_id")?.takeIf(::isValidOfficialCollectionId)
                                if (gcid != null) {
                                    return ImportTarget.Collection(gcid)
                                }
                                if (listinfo != null && songs != null && songs.length() > 0) {
                                    val name = listinfo.optString("name").ifBlank { "酷狗歌单" }
                                    val id = root.optString("encode_gic").ifBlank { "ugc-${System.currentTimeMillis()}" }
                                    return ImportTarget.EmbeddedPlaylist(name = name, id = id, songs = songs)
                                }
                            }
                        }
                    }

                    val hashMatch = Regex("""["']hash["']\s*:\s*["']([A-Fa-f0-9]{32})["']""").find(body)
                    if (hashMatch != null) {
                        return ImportTarget.SingleSong(hashMatch.groupValues[1], null)
                    }
                    val gcidMatch = Regex("""["']global_collection_id["']\s*:\s*["'](collection_[0-9]+_[0-9]+[^"']*)["']""").find(body)
                    if (gcidMatch != null) {
                        return ImportTarget.Collection(gcidMatch.groupValues[1])
                    }
                }
            }
        }

        // 最终检查
        extractOfficialCollectionId(uri)?.let { return ImportTarget.Collection(it) }
        extractSongHash(uri)?.let { return it }

        error("无法识别酷狗歌单或单曲链接")
    }

    private fun extractOfficialCollectionId(uri: URI): String? {
        queryParameter(uri, "global_collection_id")?.takeIf(::isValidOfficialCollectionId)?.let { return it }
        val path = uri.path.orEmpty()
        val collectionInPath = Regex("""/(collection_[0-9]+_[0-9]+[A-Za-z0-9_-]*)""").find(path)
        if (collectionInPath != null) {
            return collectionInPath.groupValues[1]
        }
        return null
    }

    private fun isValidOfficialCollectionId(value: String): Boolean =
        value.matches(Regex("collection_\\d+_\\d+[A-Za-z0-9_-]*"))

    private fun extractSongHash(uri: URI): ImportTarget.SingleSong? {
        val fragment = uri.fragment.orEmpty()
        val hash = queryParameter(uri, "hash")?.takeIf { it.matches(Regex("[A-Fa-f0-9]{32}")) }
            ?: Regex("""hash=([A-Fa-f0-9]{32})""").find(fragment)?.groupValues?.get(1)
        val albumId = queryParameter(uri, "album_id")
            ?: queryParameter(uri, "album_audio_id")
            ?: Regex("""album_id=([0-9]+)""").find(fragment)?.groupValues?.get(1)
            ?: Regex("""album_audio_id=([0-9]+)""").find(fragment)?.groupValues?.get(1)

        if (hash != null) {
            return ImportTarget.SingleSong(hash, albumId)
        }
        val path = uri.path.orEmpty()
        val hashInPath = Regex("""/mixsong/([A-Fa-f0-9]{32})""").find(path)
        if (hashInPath != null) {
            return ImportTarget.SingleSong(hashInPath.groupValues[1], albumId)
        }
        return null
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

    internal fun requestOfficialPlaylist(globalCollectionId: String, installationId: String, beginIndex: Int): String {
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
        val uri = runCatching { URI(raw.trim()) }.getOrElse { error("链接格式无效") }
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) { "只支持 HTTP/HTTPS 链接" }
        val host = uri.host?.lowercase() ?: error("链接缺少域名")
        require(host == "kugou.com" || host.endsWith(".kugou.com")) { "当前版本仅支持酷狗公开歌单与单曲链接" }
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

    private fun matchesTrack(candidate: Track, title: String, artist: String): Boolean {
        val localArtist = normalize(candidate.artist)
        val remoteArtist = normalize(artist)
        val artistMatches = localArtist == remoteArtist || localArtist.contains(remoteArtist) || remoteArtist.contains(localArtist)
        if (!artistMatches) return false

        val nTitle = normalize(title)
        val nLocalTitle = normalize(candidate.title)
        if (nTitle == nLocalTitle) return true

        val cleanRemote = normalize(title.replace(Regex("""\([^)]*\)|\[[^\]]*\]"""), ""))
        val cleanLocal = normalize(candidate.title.replace(Regex("""\([^)]*\)|\[[^\]]*\]"""), ""))
        if (cleanRemote.isNotBlank() && cleanRemote == cleanLocal) return true

        val fileName = candidate.sourceUri?.substringAfterLast('/')?.substringAfterLast('\\')
        val fileNameTitle = fileName
            ?.substringBeforeLast('.')
            ?.replace(Regex("""\s*\[[0-9a-fA-F]{10}\]$"""), "")
            ?.substringAfter(" - ")
        if (fileNameTitle != null) {
            val nFile = normalize(fileNameTitle)
            val cleanFile = normalize(fileNameTitle.replace(Regex("""\([^)]*\)|\[[^\]]*\]"""), ""))
            if (nFile == nTitle || (cleanFile.isNotBlank() && cleanFile == cleanRemote)) return true
        }

        return false
    }

    private fun normalize(value: String): String =
        value.replace(Regex("""\s*\[[0-9a-fA-F]{10}\]$"""), "").lowercase().filter(Char::isLetterOrDigit)
    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
    private fun md5(value: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(value)
    private fun md5Hex(value: String): String = md5(value.toByteArray(StandardCharsets.UTF_8)).toHex()
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8)).toHex()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private inline fun <T> HttpURLConnection.useConnection(block: HttpURLConnection.() -> T): T =
        try { block() } finally { disconnect() }

    sealed interface ImportTarget {
        data class Collection(val collectionId: String) : ImportTarget
        data class SingleSong(val hash: String, val albumId: String?) : ImportTarget
        data class EmbeddedPlaylist(val name: String, val id: String, val songs: org.json.JSONArray) : ImportTarget
    }

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
