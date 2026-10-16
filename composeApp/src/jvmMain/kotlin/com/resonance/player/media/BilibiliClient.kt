package com.resonance.player.media

import com.resonance.player.model.*
import com.resonance.player.io.readUpTo
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

interface MusicVideoSource {
    suspend fun inspect(link: String): BilibiliVideo
    suspend fun search(song: SongRequest): List<BilibiliVideo>
    suspend fun download(video: BilibiliVideo, destination: Path, onProgress: (ImportProgress) -> Unit): Path
    suspend fun cover(url: String, destination: Path): Path?
}

/** Public video metadata/audio, with normal WBI signing. Access-denied responses are surfaced. */
class BilibiliClient(private val cookie: () -> String = { "" }) : MusicVideoSource {
    private val apiMutex = Mutex()
    private val signingMutex = Mutex()
    private var signingKey: Pair<Long, String>? = null
    private var lastRequest = 0L
    private fun connection(url: String): HttpURLConnection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000; readTimeout = 30_000; instanceFollowRedirects = false
        setRequestProperty("User-Agent", BILI_USER_AGENT)
        setRequestProperty("Referer", "https://www.bilibili.com/")
        if (URI(url).host == "bilibili.com" || URI(url).host.endsWith(".bilibili.com")) cookie().takeIf(String::isNotBlank)?.let { setRequestProperty("Cookie", it.replace("\r", "").replace("\n", "")) }
    }
    private suspend fun json(path: String, query: Map<String, String> = emptyMap(), sign: Boolean = false): JSONObject = withContext(Dispatchers.IO) {
        val parameters = if (sign) signed(query) else query
        apiMutex.withLock {
            delay((lastRequest + 500 - System.currentTimeMillis()).coerceAtLeast(0))
            lastRequest = System.currentTimeMillis()
        }
        val http = connection("https://api.bilibili.com$path?${encode(parameters)}")
        val watchdog = launch(Dispatchers.Default) { try { awaitCancellation() } finally { http.disconnect() } }
        try {
            coroutineContext.ensureActive()
            val codeHttp = http.responseCode
            check(codeHttp == 200) {
                if (codeHttp in listOf(401, 403, 412)) "B 站要求登录、验证或限制了当前网络（HTTP $codeHttp）；请在设置中配置自己的 Cookie，完成官方验证后重试"
                else "B 站返回 HTTP $codeHttp，请稍后重试"
            }
            val bytes = http.inputStream.use { it.readUpTo(4_000_001) }
            require(bytes.size <= 4_000_000) { "B 站响应过大" }
            val result = JSONObject(String(bytes, UTF_8))
            val code = result.optInt("code", -1)
            // nav returns useful WBI keys even for signed-out sessions.
            check(code == 0 || path.endsWith("/nav") && result.optJSONObject("data")?.has("wbi_img") == true) {
                when (code) {
                    -101, -104, -403, -352, -412 -> "B 站要求登录、验证或暂时限制访问（$code）；可在设置中配置自己的登录 Cookie 后重试"
                    else -> "B 站接口失败（$code）：${result.optString("message").take(180)}"
                }
            }
            result.getJSONObject("data")
        } finally { watchdog.cancel(); http.disconnect() }
    }
    private suspend fun signed(query: Map<String, String>): Map<String, String> {
        val key = signingMutex.withLock {
            signingKey?.takeIf { System.currentTimeMillis() - it.first < 3_600_000 }?.second ?: run {
        val nav = json("/x/web-interface/nav").getJSONObject("wbi_img")
        val source = listOf("img_url", "sub_url").joinToString("") { nav.getString(it).substringAfterLast('/').substringBefore('.') }
        require(source.length >= 64) { "无法读取 B 站签名信息" }
        // The public client permutation, also documented in yt-dlp's Bilibili extractor.
        val order = intArrayOf(46,47,18,2,53,8,23,32,15,50,10,31,58,3,45,35,27,43,5,49,33,9,42,19,29,28,14,39,12,38,41,13)
        order.map { source[it] }.joinToString("").also { signingKey = System.currentTimeMillis() to it }
            }
        }
        val sorted = (query + ("wts" to (System.currentTimeMillis() / 1000).toString())).toSortedMap()
            .mapValues { (_, value) -> value.filterNot { it in "!'()*" } }
        val digest = MessageDigest.getInstance("MD5").digest((encode(sorted) + key).toByteArray(UTF_8)).joinToString("") { "%02x".format(it) }
        return sorted + ("w_rid" to digest)
    }

    override suspend fun inspect(link: String): BilibiliVideo = withContext(Dispatchers.IO) {
        val raw = Regex("https?://[^\\s<>]+|BV[0-9A-Za-z]{10}").find(link.trim())?.value ?: error("请输入 B 站视频链接或 BV 号")
        var uri = URI(if (raw.startsWith("BV")) "https://www.bilibili.com/video/$raw" else raw.replaceFirst("http://", "https://"))
        repeat(5) {
            require(uri.scheme == "https" && isPageHost(uri.host) && uri.userInfo == null && uri.port in listOf(-1, 443)) { "仅支持 bilibili.com 或 b23.tv 视频链接" }
            if (uri.host == "b23.tv") {
                val http = connection(uri.toString())
                try {
                    if (http.responseCode in 300..399) uri = uri.resolve(http.getHeaderField("Location") ?: error("短链接没有跳转地址"))
                    else error("无法解析 B 站短链接")
                } finally { http.disconnect() }
            }
        }
        require(isPageHost(uri.host)) { "短链接跳转到不受支持的地址" }
        val bv = Regex("BV[0-9A-Za-z]{10}").find(uri.path)?.value
        val av = Regex("av(\\d+)").find(uri.path)?.groupValues?.get(1)
        require(bv != null || av != null) { "链接中没有有效 BV/AV 号" }
        val query = if (bv != null) mapOf("bvid" to bv) else mapOf("aid" to av!!)
        val data = json("/x/web-interface/view", query)
        val page = Regex("(?:^|&)p=(\\d+)").find(uri.rawQuery.orEmpty())?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pages = data.getJSONArray("pages")
        require(page <= pages.length()) { "视频没有第 $page 个分 P" }
        val part = pages.getJSONObject(page - 1)
        BilibiliVideo(data.getString("bvid"), part.getLong("cid"), if (pages.length() > 1) part.optString("part", data.getString("title")) else data.getString("title"),
            data.optJSONObject("owner")?.optString("name").orEmpty(), data.optString("pic").replace("http://", "https://"), part.optInt("duration", data.optInt("duration")), page)
    }

    override suspend fun search(song: SongRequest): List<BilibiliVideo> {
        val data = json("/x/web-interface/wbi/search/type", mapOf("search_type" to "video", "keyword" to "${song.title} ${song.artist}", "page" to "1"), sign = true)
        val results = data.optJSONArray("result") ?: return emptyList()
        return (0 until results.length()).mapNotNull { i ->
            val item = results.optJSONObject(i) ?: return@mapNotNull null
            val title = item.optString("title").replace(Regex("<[^>]*>"), "").replace("&amp;", "&")
            val author = item.optString("author")
            val score = candidateScore(song, title, author)
            val bvid = item.optString("bvid")
            val cover = item.optString("pic").let { if (it.startsWith("//")) "https:$it" else it.replace("http://", "https://") }
            if (score < 20 || !Regex("BV[0-9A-Za-z]{10}").matches(bvid)) null
            else score to BilibiliVideo(bvid, 0, title, author, cover, 0)
        }.sortedByDescending { it.first }.take(4).map { it.second }
    }

    override suspend fun download(video: BilibiliVideo, destination: Path, onProgress: (ImportProgress) -> Unit): Path = withContext(Dispatchers.IO) {
        val play = json("/x/player/wbi/playurl", mapOf("bvid" to video.bvid, "cid" to video.cid.toString(), "fnval" to "4048", "qn" to "80", "fnver" to "0", "fourk" to "1"), sign = true)
        val audios = play.optJSONObject("dash")?.optJSONArray("audio")
        val best = audios?.let { a -> (0 until a.length()).mapNotNull(a::optJSONObject).maxByOrNull { it.optLong("bandwidth") } }
        val progressive = play.optJSONArray("durl")
        val urls = mutableListOf<String>()
        best?.optString("baseUrl", best.optString("base_url"))?.takeIf(String::isNotBlank)?.let(urls::add)
        best?.optJSONArray("backupUrl")?.let { a -> (0 until a.length()).forEach { urls += a.optString(it) } }
        if (urls.isEmpty() && progressive?.length() == 1) urls += progressive.getJSONObject(0).getString("url")
        require(urls.isNotEmpty()) { "该视频没有可访问的音轨，可能需要登录或没有开放播放权限" }
        Files.createDirectories(destination.parent)
        var failure: Exception? = null
        for (url in urls.distinct()) {
            coroutineContext.ensureActive()
            try {
                transfer(url.replace("http://", "https://"), destination, 1024L * 1024 * 1024) { done, size ->
                    onProgress(ImportProgress("下载音频", video.title, fraction = if (size > 0) done.toFloat() / size else null, detail = "${done / 1024} KB / ${if (size > 0) "${size / 1024} KB" else "未知大小"}"))
                }
                return@withContext destination
            } catch (cancelled: CancellationException) { Files.deleteIfExists(destination); throw cancelled }
            catch (error: Exception) { Files.deleteIfExists(destination); failure = error }
        }
        throw failure ?: IllegalStateException("下载失败")
    }

    override suspend fun cover(url: String, destination: Path): Path? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        if (!url.startsWith("http")) return@withContext runCatching { java.nio.file.Paths.get(url).takeIf(Files::isRegularFile) }.getOrNull()
        try { transfer(url.replace("http://", "https://"), destination, 10_000_000) { _, _ -> }; destination }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { Files.deleteIfExists(destination); null }
    }
    private suspend fun transfer(url: String, destination: Path, limit: Long, redirects: Int = 3, progress: (Long, Long) -> Unit): Unit = withTimeout(1_200_000) {
        val uri = URI(url)
        require(uri.scheme == "https" && uri.userInfo == null && uri.port in listOf(-1, 443) && isMediaHost(uri.host)) { "媒体地址不在 B 站允许的域名中" }
        val http = connection(url)
        val watchdog = CoroutineScope(currentCoroutineContext()).launch(Dispatchers.Default) { try { awaitCancellation() } finally { http.disconnect() } }
        try {
            if (http.responseCode in 300..399) {
                require(redirects > 0) { "媒体跳转次数过多" }
                val target = uri.resolve(http.getHeaderField("Location") ?: error("媒体跳转地址缺失"))
                transfer(target.toString(), destination, limit, redirects - 1, progress)
                return@withTimeout
            }
            check(http.responseCode == 200) { "媒体下载失败（HTTP ${http.responseCode}）" }
            val size = http.contentLengthLong
            require(size <= limit) { "下载文件超过大小限制" }
            Files.createDirectories(destination.parent)
            BufferedInputStream(http.inputStream, 256 * 1024).use { input ->
                BufferedOutputStream(Files.newOutputStream(destination), 256 * 1024).use { output ->
                    val buffer = ByteArray(256 * 1024); var done = 0L; var last = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer); if (count < 0) break
                        done += count; require(done <= limit) { "下载文件超过大小限制" }
                        output.write(buffer, 0, count)
                        val now = System.currentTimeMillis()
                        if (now - last >= 100) { progress(done, size); last = now }
                    }
                    require(done > 0 && (size <= 0 || done == size)) { "音频下载不完整" }
                    progress(done, size)
                }
            }
        } finally { watchdog.cancel(); http.disconnect() }
    }
    companion object {
        const val BILI_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        private fun encode(query: Map<String, String>) = query.entries.joinToString("&") { "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}" }
        private fun isPageHost(host: String?) = host == "b23.tv" || host == "bilibili.com" || host?.endsWith(".bilibili.com") == true
        private fun isMediaHost(host: String?) = listOf("bilibili.com", "bilivideo.com", "bilivideo.cn", "hdslb.com", "akamaized.net").any { host == it || host?.endsWith(".$it") == true }
        fun candidateScore(song: SongRequest, title: String, uploader: String): Int {
            fun norm(s: String) = s.lowercase().filter(Char::isLetterOrDigit)
            val text = norm(title); val wanted = norm(song.title); val artist = norm(song.artist)
            if (wanted.isBlank() || !text.contains(wanted)) return -100
            var score = 25
            if (artist.isNotBlank() && (text.contains(artist) || norm(uploader).contains(artist))) score += 20
            else if (artist.isNotBlank()) score -= 15
            if (listOf("官方", "原版", "Official", "音源", "MV").any { (title + uploader).contains(it, true) }) score += 8
            val requested = "${song.title} ${song.artist}"
            val chineseVariant = listOf("演唱会", "现场", "翻唱", "伴奏", "混剪", "变速", "合集", "串烧").any { title.contains(it, true) && !requested.contains(it, true) }
            val englishVariant = Regex("(?<![a-z])(remix|live|cover)(?![a-z])", RegexOption.IGNORE_CASE).findAll(title).any { !requested.contains(it.value, true) }
            if (chineseVariant || englishVariant) score -= 80
            return score
        }
    }
}
