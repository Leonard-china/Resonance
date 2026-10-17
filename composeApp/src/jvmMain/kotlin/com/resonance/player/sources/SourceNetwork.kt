package com.resonance.player.sources

import com.resonance.player.io.readUpTo
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.InetAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

internal interface SourceNetwork {
    suspend fun request(options: JSONObject): JSONObject
    suspend fun download(url: String, target: Path, headers: JSONObject = JSONObject(), maxBytes: Long = PublicSourceNetwork.MAX_AUDIO_BYTES): Long
}

/** Plugins receive this public HTTP/HTTPS transport, never JVM objects, files, cookies or AI keys. */
internal class PublicSourceNetwork : SourceNetwork {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = InetAddress.getAllByName(hostname).toList().also { addresses ->
                require(addresses.isNotEmpty() && addresses.all(::publicAddress)) { "歌源只允许访问公网地址" }
            }
        }).build()

    override suspend fun request(options: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val method = options.optString("method", "GET").uppercase()
        require(method in setOf("GET", "POST", "HEAD")) { "歌源请求方法不支持" }
        var current = publicUrl(options.getString("url"))
        var currentHeaders = options.optJSONObject("headers") ?: JSONObject()
        val body = options.optString("body", "").takeIf { method == "POST" }
            ?.toRequestBody(currentHeaders.optString("Content-Type").toMediaTypeOrNull())
        repeat(5) {
            val builder = Request.Builder().url(current)
            applyHeaders(builder, currentHeaders)
            builder.method(method, body)
            val result = execute(builder.build()) { response ->
                if (response.isRedirect) {
                    val location = response.header("Location") ?: error("歌源接口跳转缺少地址")
                    val next = try {
                        URI(current).resolve(location).toString()
                    } catch (_: Exception) {
                        current.toHttpUrlOrNull()?.resolve(location)?.toString() ?: error("歌源接口跳转缺少地址")
                    }
                    publicUrl(next)
                    if (runCatching { URI(next).host != URI(current).host }.getOrDefault(true)) currentHeaders = JSONObject()
                    next to null
                } else {
                    val bytes = response.body?.byteStream()?.use { it.readUpTo(2_000_001) } ?: byteArrayOf()
                    require(bytes.size <= 2_000_000) { "歌源响应过大" }
                    val json = JSONObject().put("status", response.code).put("body", bytes.toString(Charsets.UTF_8))
                        .put("headers", JSONObject(response.headers.toMultimap().mapValues { it.value.joinToString(",") }))
                    "" to json
                }
            }
            if (result.second != null) return@withContext result.second!!
            current = result.first
        }
        error("歌源接口跳转次数过多")
    }

    override suspend fun download(url: String, target: Path, headers: JSONObject, maxBytes: Long): Long = withContext(Dispatchers.IO) {
        require(maxBytes in 1..MAX_AUDIO_BYTES) { "下载大小限制无效" }
        var current = publicUrl(url)
        var currentHeaders = headers
        repeat(5) {
            val builder = Request.Builder().url(current)
            applyHeaders(builder, currentHeaders)
            val result = execute(builder.build()) { response ->
                if (response.isRedirect) {
                    val location = response.header("Location") ?: error("下载跳转缺少地址")
                    val next = try {
                        URI(current).resolve(location).toString()
                    } catch (_: Exception) {
                        current.toHttpUrlOrNull()?.resolve(location)?.toString() ?: error("下载跳转缺少地址")
                    }
                    publicUrl(next)
                    if (runCatching { URI(next).host != URI(current).host }.getOrDefault(true)) currentHeaders = JSONObject()
                    next to -1L
                } else {
                    check(response.isSuccessful) { "歌源下载失败（HTTP " + response.code + "）" }
                    val length = response.body?.contentLength() ?: -1
                    require(length <= maxBytes) { "下载文件过大" }
                    Files.createDirectories(target.parent)
                    var size = 0L
                    val input = response.body?.byteStream() ?: error("下载内容为空")
                    input.use { source -> Files.newOutputStream(target).buffered().use { output ->
                        val buffer = ByteArray(128 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = source.read(buffer); if (count < 0) break
                            size += count
                            require(size <= maxBytes) { "下载文件过大" }
                            output.write(buffer, 0, count)
                        }
                    } }
                    require(size > 0 && (length < 0 || size == length)) { "音频下载不完整" }
                    "" to size
                }
            }
            if (result.second >= 0) return@withContext result.second
            current = result.first
        }
        error("歌源下载跳转次数过多")
    }

    private suspend fun <T> execute(request: Request, block: suspend (Response) -> T): T = coroutineScope {
        val call = client.newCall(request)
        val watchdog = launch(Dispatchers.Default) { try { awaitCancellation() } finally { call.cancel() } }
        try { call.execute().use { block(it) } } finally { watchdog.cancel() }
    }

    private fun applyHeaders(builder: Request.Builder, headers: JSONObject) {
        require(headers.length() <= 32) { "歌源请求头过多" }
        headers.keys().forEach { name ->
            require(name.lowercase() !in setOf("host", "connection", "content-length", "proxy-authorization")) { "歌源请求头不支持" }
            builder.header(name, headers.optString(name).take(8192))
        }
        if (!headers.has("User-Agent")) builder.header("User-Agent", "Resonance-MusicSource/1")
    }

    companion object {
        const val MAX_AUDIO_BYTES = 512L * 1024 * 1024
        fun publicUrl(value: String): String {
            val uri = try {
                URI(value)
            } catch (_: Exception) {
                val http = value.toHttpUrlOrNull() ?: error("歌源地址格式无效")
                http.toUri()
            }
            val scheme = uri.scheme?.lowercase()
            require(
                scheme in setOf("http", "https") &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.port in listOf(-1, 80, 443, 8080, 8443)
            ) { "歌源只支持公网 HTTP/HTTPS 地址" }
            require(uri.host.lowercase() !in setOf("localhost", "localhost.localdomain")) { "歌源只允许访问公网地址" }
            return value
        }
        internal fun publicAddress(address: InetAddress): Boolean {
            if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isSiteLocalAddress ||
                address.isLinkLocalAddress || address.isMulticastAddress) return false
            val b = address.address
            if (b.size == 16 && (b[0].toInt() and 0xfe) == 0xfc) return false
            if (b.size == 4) {
                val first = b[0].toInt() and 255; val second = b[1].toInt() and 255
                if (first == 0 || first >= 224 || first == 100 && second in 64..127 || first == 198 && second in 18..19) return false
            }
            return true
        }
    }
}
