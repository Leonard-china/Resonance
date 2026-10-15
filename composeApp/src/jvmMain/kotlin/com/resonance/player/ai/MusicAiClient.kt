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
class MusicAiClient : MusicAiTransport {
    override suspend fun complete(connection: AiConnection, messages: JSONArray, tools: JSONArray?, maxTokens: Int): AiReply = withContext(Dispatchers.IO) {
        require(connection.configured) { "请在设置中填写 ${connection.provider.label} 的 HTTPS 接口、模型和 API Key" }
        val base = connection.baseUrl.trim().trimEnd('/')
        val endpoint = URI(if (base.endsWith("/chat/completions")) base else "$base/chat/completions")
        require(endpoint.scheme == "https" && endpoint.userInfo == null && !endpoint.host.isNullOrBlank()) { "AI 接口必须是有效 HTTPS 地址" }
        val request = JSONObject().put("model", connection.model.trim()).put("messages", messages)
            .put("max_tokens", maxTokens).put("temperature", 0.3).put("stream", false)
        if (tools != null) request.put("tools", tools).put("tool_choice", "auto")
        val http = endpoint.toURL().openConnection() as HttpURLConnection
        val cancellation = CoroutineScope(currentCoroutineContext()).launch(Dispatchers.Default) { try { awaitCancellation() } finally { http.disconnect() } }
        try {
            http.requestMethod = "POST"
            http.instanceFollowRedirects = false
            http.connectTimeout = 15_000
            http.readTimeout = 90_000
            http.doOutput = true
            http.setRequestProperty("Authorization", "Bearer ${connection.apiKey.trim()}")
            http.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            http.outputStream.use { it.write(request.toString().toByteArray(UTF_8)) }
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
        } finally { cancellation.cancel(); http.disconnect() }
    }

    companion object {
        fun cleanJson(text: String): String = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        fun objects(text: String): JSONArray {
            val cleaned = cleanJson(text)
            val start = cleaned.indexOf('['); val end = cleaned.lastIndexOf(']')
            require(start >= 0 && end > start) { "AI 未返回可解析的歌曲列表" }
            return JSONArray(cleaned.substring(start, end + 1))
        }
    }
}
