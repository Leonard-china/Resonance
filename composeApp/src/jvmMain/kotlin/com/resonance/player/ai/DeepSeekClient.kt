package com.resonance.player.ai

import com.resonance.player.io.readUpTo
import com.resonance.player.lyrics.parseLrc
import com.resonance.player.model.AiEnrichResult
import com.resonance.player.model.DeepSeekConfig
import com.resonance.player.model.DeepSeekTestResult
import com.resonance.player.model.LyricLine
import com.resonance.player.model.Lyrics
import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

class DeepSeekClient(private val completion: (suspend (List<Map<String, String>>, Int) -> String)? = null) {

    suspend fun testConnection(config: DeepSeekConfig): DeepSeekTestResult = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) {
            return@withContext DeepSeekTestResult(false, "请先配置 AI API Key")
        }
        try {
            val response = sendChatCompletion(
                config = config,
                messages = listOf(
                    mapOf("role" to "user", "content" to "Ping")
                ),
                maxTokens = 10,
            )
            if (response.isNotBlank()) {
                DeepSeekTestResult(true, "连接成功！AI API 响应正常。")
            } else {
                DeepSeekTestResult(false, "API 返回了空响应")
            }
        } catch (error: Throwable) {
            val msg = error.message ?: error::class.simpleName.orEmpty()
            DeepSeekTestResult(false, "连接失败：$msg")
        }
    }

    suspend fun generateLyrics(config: DeepSeekConfig, track: Track): LyricsFetchResult = withContext(Dispatchers.IO) {
        if (!config.isConfigured || !config.enabled) {
            return@withContext LyricsFetchResult.Unavailable("未配置 AI 接口", retryable = false)
        }
        try {
            val prompt = buildString {
                append("请为歌曲《${track.title}》（歌手：${track.artist.ifBlank { "未知" }}，专辑：${track.album.ifBlank { "未知" }}，时长：${track.durationText}）尽可能准确地提供纯文本歌词；不确定时明确说明未找到，不要编造。\n")
                append("要求：\n")
                append("1. 每行一句歌词，不要生成未经音频对齐验证的时间戳。\n")
                append("2. 如果该曲目是纯音乐/器乐演奏，请回复：[00:00.00]纯音乐，请欣赏\n")
                append("3. 请直接输出纯文本歌词，不要添加 Markdown 格式或额外解释。无法确认时仅回复未找到。")
            }
            val response = sendChatCompletion(
                config = config,
                messages = listOf(
                    mapOf("role" to "system", "content" to "你是一个专业的音乐歌词助理，仅提供可确认的歌词内容，无法确认时返回未找到，不生成猜测时间轴。"),
                    mapOf("role" to "user", "content" to prompt),
                ),
                maxTokens = 2048,
            )
            val cleaned = cleanLrcResponse(response)
            if (cleaned.isBlank() || Regex("未找到|无法确认|无法提供|没有找到|not found|cannot provide", RegexOption.IGNORE_CASE).containsMatchIn(cleaned.take(300))) {
                return@withContext LyricsFetchResult.Unavailable("AI 未找到可确认的歌词", retryable = true)
            }
            val lines = parseLrc(cleaned)
            val isInstrumental = cleaned.contains("纯音乐") && (lines.isEmpty() || lines.size <= 2)
            if (lines.isNotEmpty()) {
                LyricsFetchResult.Found(
                    Lyrics(
                        trackId = track.id,
                        lines = lines.map { it.copy(timestampMs = null) },
                        synchronized = false,
                        instrumental = isInstrumental,
                        source = "AI · ${config.model}",
                        fromCache = false,
                    )
                )
            } else {
                val plainLines = cleaned.lineSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .map { LyricLine(timestampMs = null, text = it) }
                    .toList()
                if (plainLines.isNotEmpty()) {
                    LyricsFetchResult.Found(
                        Lyrics(
                            trackId = track.id,
                            lines = plainLines,
                            synchronized = false,
                            instrumental = isInstrumental,
                            source = "AI · ${config.model}",
                            fromCache = false,
                        )
                    )
                } else {
                    LyricsFetchResult.Unavailable("AI 未能生成有效的歌词", retryable = false)
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) {
            LyricsFetchResult.Unavailable("AI 歌词检索失败：${error.message ?: "网络错误"}", retryable = true)
        }
    }

    suspend fun enrichMetadata(
        config: DeepSeekConfig,
        track: Track,
        needLyrics: Boolean = true,
    ): AiEnrichResult = withContext(Dispatchers.IO) {
        if (!config.isConfigured || !config.enabled) {
            return@withContext AiEnrichResult(false, message = "未配置 AI 接口")
        }
        try {
            val prompt = buildString {
                append("请根据音频信息（文件名/原曲名：“${track.title}”，原歌手：“${track.artist}”，专辑：“${track.album}”，时长：“${track.durationText}”），推断并提供规范的音乐元数据。\n")
                append("请直接输出纯 JSON 格式：\n")
                append("{\n")
                append("  \"title\": \"规范歌曲名\",\n")
                append("  \"artist\": \"准确歌手/演唱者名\",\n")
                append("  \"album\": \"所属专辑名\",\n")
                append("  \"isInstrumental\": false,\n")
                if (needLyrics) {
                    append("  \"lyrics\": \"可确认的纯文本歌词，无法确认就写空串，不要生成时间戳\"\n")
                } else {
                    append("  \"lyrics\": \"\"\n")
                }
                append("}")
            }
            val response = sendChatCompletion(
                config = config,
                messages = listOf(
                    mapOf("role" to "system", "content" to "你是一个精通华语及全球流行音乐的音乐元数据整理助手，能够准确识别文件名中的歌曲名、歌手名与专辑信息，并输出干净的 JSON。"),
                    mapOf("role" to "user", "content" to prompt),
                ),
                maxTokens = if (needLyrics) 2048 else 512,
            )
            val cleaned = cleanJsonResponse(response)
            val json = JSONObject(cleaned)
            val title = json.optString("title").takeIf(String::isNotBlank)
            val artist = json.optString("artist").takeIf(String::isNotBlank)
            val album = json.optString("album").takeIf(String::isNotBlank)
            val lyrics = json.optString("lyrics").takeIf(String::isNotBlank)
            val isInstrumental = json.optBoolean("isInstrumental", false)
            AiEnrichResult(
                success = true,
                title = title,
                artist = artist,
                album = album,
                lyrics = lyrics,
                isInstrumental = isInstrumental,
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (e: Exception) {
            AiEnrichResult(false, message = e.message ?: "AI 识别失败")
        }
    }

    internal fun cleanLrcResponse(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.substringAfter('\n')
        }
        if (text.endsWith("```")) {
            text = text.substringBeforeLast("```")
        }
        return text.trim()
    }

    internal fun cleanJsonResponse(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.substringAfter('\n')
        }
        if (text.endsWith("```")) {
            text = text.substringBeforeLast("```")
        }
        text = text.trim()
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        }
        return text
    }

    private suspend fun sendChatCompletion(
        config: DeepSeekConfig,
        messages: List<Map<String, String>>,
        maxTokens: Int = 1024,
    ): String {
        completion?.let { return it(messages, maxTokens) }
        val baseUrl = config.baseUrl.trim().removeSuffix("/")
        val endpoint = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"
        val url = URI(endpoint).toURL()

        val jsonBody = JSONObject().apply {
            put("model", config.model.ifBlank { "deepseek-chat" })
            put("messages", JSONArray(messages.map { JSONObject(it) }))
            put("max_tokens", maxTokens)
            put("temperature", 0.3)
        }

        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")

            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.buffered()?.use { input ->
                val bytes = input.readUpTo(MAX_RESPONSE_BYTES)
                bytes.toString(StandardCharsets.UTF_8)
            }.orEmpty()

            if (code !in 200..299) {
                val errorMsg = runCatching {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message")
                }.getOrNull()?.takeIf(String::isNotBlank) ?: "HTTP $code: $responseBody"
                throw IllegalStateException(errorMsg)
            }

            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                throw IllegalStateException("API 返回 choices 为空")
            }
            return choices.getJSONObject(0).getJSONObject("message").optString("content", "")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val MAX_RESPONSE_BYTES = 2_000_000
    }
}
