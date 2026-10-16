package com.resonance.player.media

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.resonance.player.model.BiliQrPollResult
import com.resonance.player.model.BiliQrPollStatus
import com.resonance.player.model.BiliQrSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

class BilibiliLoginService(
    private val httpGet: (String, Map<String, String>) -> HttpResponse = ::defaultHttpGet
) {
    data class HttpResponse(
        val statusCode: Int,
        val headers: Map<String, List<String>>,
        val body: String,
    )

    suspend fun createQrSession(): BiliQrSession? = withContext(Dispatchers.IO) {
        try {
            val response = httpGet(
                "https://passport.bilibili.com/x/passport-login/web/qrcode/generate",
                mapOf("User-Agent" to BILI_LOGIN_USER_AGENT, "Referer" to "https://passport.bilibili.com/")
            )
            if (response.statusCode != 200) return@withContext null
            val json = JSONObject(response.body)
            if (json.optInt("code", -1) != 0) return@withContext null
            val data = json.getJSONObject("data")
            val qrcodeKey = data.getString("qrcode_key")
            val loginUrl = data.getString("url")

            val writer = QRCodeWriter()
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val matrix = writer.encode(loginUrl, BarcodeFormat.QR_CODE, 0, 0, hints)
            val width = matrix.width
            val height = matrix.height
            val bits = BooleanArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    bits[y * width + x] = matrix.get(x, y)
                }
            }
            BiliQrSession(
                qrcodeKey = qrcodeKey,
                loginUrl = loginUrl,
                matrixSize = width,
                bits = bits
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pollQrStatus(qrcodeKey: String): BiliQrPollResult = withContext(Dispatchers.IO) {
        try {
            val url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=$qrcodeKey"
            val response = httpGet(
                url,
                mapOf("User-Agent" to BILI_LOGIN_USER_AGENT, "Referer" to "https://passport.bilibili.com/")
            )
            if (response.statusCode != 200) {
                return@withContext BiliQrPollResult(
                    status = BiliQrPollStatus.ERROR,
                    message = "网络响应异常 (${response.statusCode})"
                )
            }
            val json = JSONObject(response.body)
            val rootCode = json.optInt("code", -1)
            if (rootCode != 0) {
                return@withContext BiliQrPollResult(
                    status = BiliQrPollStatus.ERROR,
                    message = json.optString("message", "接口返回异常 ($rootCode)")
                )
            }
            val data = json.getJSONObject("data")
            when (val pollCode = data.optInt("code", -1)) {
                0 -> {
                    // 登录成功，从响应头抓取 Set-Cookie
                    val cookies = extractCookies(response.headers)
                    val crossDomainUrl = data.optString("url")
                    val mergedCookies = if (crossDomainUrl.isNotBlank() && crossDomainUrl.startsWith("http")) {
                        try {
                            val crossResp = httpGet(
                                crossDomainUrl,
                                mapOf(
                                    "User-Agent" to BILI_LOGIN_USER_AGENT,
                                    "Referer" to "https://passport.bilibili.com/",
                                    "Cookie" to cookies
                                )
                            )
                            mergeCookies(cookies, extractCookies(crossResp.headers))
                        } catch (_: Exception) {
                            cookies
                        }
                    } else {
                        cookies
                    }
                    BiliQrPollResult(
                        status = BiliQrPollStatus.SUCCESS,
                        cookie = mergedCookies,
                        message = "登录成功"
                    )
                }
                86101 -> BiliQrPollResult(
                    status = BiliQrPollStatus.WAITING,
                    message = "请使用哔哩哔哩手机客户端扫码"
                )
                86090 -> BiliQrPollResult(
                    status = BiliQrPollStatus.SCANNED,
                    message = "已扫码，请在手机端确认登录"
                )
                86038 -> BiliQrPollResult(
                    status = BiliQrPollStatus.EXPIRED,
                    message = "二维码已失效，请点击刷新"
                )
                else -> BiliQrPollResult(
                    status = BiliQrPollStatus.ERROR,
                    message = data.optString("message", "状态码 $pollCode")
                )
            }
        } catch (e: Exception) {
            BiliQrPollResult(
                status = BiliQrPollStatus.ERROR,
                message = e.message?.take(80) ?: "网络异常"
            )
        }
    }

    companion object {
        const val BILI_LOGIN_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

        fun extractCookies(headers: Map<String, List<String>>): String {
            val setCookieHeaders = headers.entries
                .filter { it.key.equals("Set-Cookie", ignoreCase = true) }
                .flatMap { it.value }
            val cookieMap = LinkedHashMap<String, String>()
            for (header in setCookieHeaders) {
                val cookiePart = header.substringBefore(';').trim()
                val eqIdx = cookiePart.indexOf('=')
                if (eqIdx > 0) {
                    val name = cookiePart.substring(0, eqIdx).trim()
                    val value = cookiePart.substring(eqIdx + 1).trim()
                    if (name.isNotEmpty() && value.isNotEmpty()) {
                        cookieMap[name] = value
                    }
                }
            }
            return cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
        }

        fun mergeCookies(c1: String, c2: String): String {
            val map = LinkedHashMap<String, String>()
            fun put(cookies: String) {
                cookies.split(';').forEach { part ->
                    val eq = part.indexOf('=')
                    if (eq > 0) {
                        val k = part.substring(0, eq).trim()
                        val v = part.substring(eq + 1).trim()
                        if (k.isNotEmpty() && v.isNotEmpty()) map[k] = v
                    }
                }
            }
            put(c1)
            put(c2)
            return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
        }

        private fun defaultHttpGet(urlStr: String, headers: Map<String, String>): HttpResponse {
            val conn = (URI(urlStr).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = false
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            try {
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
                val body = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
                return HttpResponse(code, conn.headerFields.orEmpty(), body)
            } finally {
                conn.disconnect()
            }
        }
    }
}
