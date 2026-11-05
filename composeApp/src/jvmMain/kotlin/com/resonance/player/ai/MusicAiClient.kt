package com.resonance.player.ai

import com.resonance.player.model.*
import com.resonance.player.io.readUpTo
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

data class AiReply(val message: JSONObject, val usage: AssistantUsage)
interface MusicAiTransport {
    suspend fun complete(connection: AiConnection, messages: JSONArray, tools: JSONArray? = null, maxTokens: Int = 3000): AiReply
}

/** Provider-neutral wire client. Native music tools execute locally, never model-provided shell code. */
class MusicAiClient(private val openConnection: (URI) -> HttpURLConnection = { it.toURL().openConnection() as HttpURLConnection }) : MusicAiTransport {
    override suspend fun complete(connection: AiConnection, messages: JSONArray, tools: JSONArray?, maxTokens: Int): AiReply = withContext(Dispatchers.IO) {
        require(connection.configured) { "请在设置中填写 ${connection.provider.label} 的 HTTPS 接口、模型和 API Key" }
        val resolved = effectiveConnection(connection)
        val base = resolved.baseUrl.trim().trimEnd('/')
        val endpoint = URI(if (base.endsWith("/chat/completions")) base else "$base/chat/completions")
        require(endpoint.scheme == "https" && endpoint.userInfo == null && !endpoint.host.isNullOrBlank()) { "AI 接口必须是有效 HTTPS 地址" }
        val isReasoner = connection.model.contains("reasoner", ignoreCase = true)
        val effectiveTokens = if (isReasoner && maxTokens < 1024) 1024 else maxTokens
        val request = JSONObject().put("model", resolved.model.trim()).put("messages", messages)
            .put("max_tokens", effectiveTokens).put("stream", false)
        if (!isReasoner) request.put("temperature", 0.3)
        // The official Flash/V4 endpoints now default to high-effort thinking. These short
        // music/tool requests expect final content; keep legacy reasoner selections unchanged.
        if (endpoint.host.equals("api.deepseek.com", ignoreCase = true) && connection.model.trim().lowercase() in
            setOf("deepseek-flash", "deepseek-v4-flash", "deepseek-v4-flash-vision-exp", "deepseek-v4-pro")) {
            request.put("thinking", JSONObject().put("type", "disabled"))
        }
        if (resolved.model != connection.model) request.put("thinking", JSONObject().put("type", if (isReasoner) "enabled" else "disabled"))
        if (tools != null) request.put("tools", tools).put("tool_choice", "auto")
        val http = openConnection(endpoint)
        val cancellation = CoroutineScope(currentCoroutineContext()).launch(Dispatchers.Default) { try { awaitCancellation() } finally { http.disconnect() } }
        try {
            http.requestMethod = "POST"
            http.instanceFollowRedirects = false
            http.connectTimeout = 15_000
            http.readTimeout = 90_000
            http.doOutput = true
            http.setRequestProperty("Authorization", "Bearer ${connection.apiKey.trim()}")
            http.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val payload = request.toString().toByteArray(UTF_8)
            // A non-replayable POST: a broken connection must not silently send a paid request twice.
            http.setFixedLengthStreamingMode(payload.size)
            http.outputStream.use { it.write(payload) }
            coroutineContext.ensureActive()
            val code = http.responseCode
            val stream = if (code in 200..299) http.inputStream else http.errorStream
            val raw = stream?.use { it.readUpTo(4_000_001) } ?: byteArrayOf()
            require(raw.size <= 4_000_000) { "AI 返回内容过大" }
            val json = runCatching { JSONObject(String(raw, UTF_8)) }.getOrElse { error("AI 接口返回了无法解析的内容（HTTP $code）") }
            check(code in 200..299) { "${connection.provider.label} 请求失败（HTTP $code）：${json.optJSONObject("error")?.optString("message").orEmpty().replace(connection.apiKey, "[已隐藏]").take(250)}" }
            val message = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: error("AI 没有返回消息")
            val usage = json.optJSONObject("usage") ?: JSONObject()
            val input = usage.optLong("prompt_tokens", usage.optLong("input_tokens", 0))
            val cached = usage.optLong("prompt_cache_hit_tokens", usage.optJSONObject("prompt_tokens_details")?.optLong("cached_tokens") ?: usage.optJSONObject("input_tokens_details")?.optLong("cached_tokens") ?: 0)
            AiReply(message, AssistantUsage(input, cached, usage.optLong("completion_tokens", usage.optLong("output_tokens", 0))))
        } catch (error: java.io.IOException) {
            coroutineContext.ensureActive()
            throw java.io.IOException("AI 连接中断，未自动重发；请检查网络后手动重试（未取得本次用量）", error)
        } finally { cancellation.cancel(); http.disconnect() }
    }

    companion object {
        /** Retired official aliases keep their original thinking intent; custom endpoints stay intact. */
        fun effectiveConnection(connection: AiConnection): AiConnection {
            val official = runCatching { URI(connection.baseUrl.trim()).host.equals("api.deepseek.com", true) }.getOrDefault(false)
            return if (official && connection.model.trim().lowercase() in setOf("deepseek-chat", "deepseek-reasoner")) connection.copy(model = "deepseek-flash") else connection
        }
        fun cleanJson(text: String): String = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        fun objects(text: String): JSONArray {
            val cleaned = cleanJson(text)
            val start = cleaned.indexOf('['); val end = cleaned.lastIndexOf(']')
            require(start >= 0 && end > start) { "AI 未返回可解析的歌曲列表" }
            return JSONArray(cleaned.substring(start, end + 1))
        }
    }
}
