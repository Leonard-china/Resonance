package com.resonance.player.lyrics

import com.resonance.player.model.*
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList

fun interface SpeechLyricsTransport {
    suspend fun recognize(key: String, url: String, pcm: Path, onProgress: (ImportProgress) -> Unit): SpeechRecognition
}

/** DashScope's documented duplex protocol. Only final sentences contribute timestamps. */
class ParaformerLyricsClient : SpeechLyricsTransport {
    override suspend fun recognize(key: String, url: String, pcm: Path, onProgress: (ImportProgress) -> Unit): SpeechRecognition = coroutineScope {
        val uri = URI(url)
        require(uri.scheme == "wss" && uri.userInfo == null && uri.rawQuery == null && uri.host != null) { "语音接口必须是无凭据参数的 WSS 地址" }
        require(key.isNotBlank()) { "请在设置中配置百炼语音 Key，或配置千问 Key" }
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).pingInterval(20, TimeUnit.SECONDS).build()
        val started = CompletableDeferred<Unit>(); val finished = CompletableDeferred<Unit>()
        val lines = CopyOnWriteArrayList<LyricLine>(); val words = CopyOnWriteArrayList<SpeechWord>()
        val taskId = UUID.randomUUID().toString()
        fun command(action: String) = JSONObject().put("header", JSONObject().put("action", action).put("task_id", taskId).put("streaming", "duplex"))
            .put("payload", JSONObject().put("input", JSONObject()))
        val socket = client.newWebSocket(Request.Builder().url(url).header("Authorization", "Bearer $key").build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val run = command("run-task")
                run.getJSONObject("payload").put("task_group", "audio").put("task", "asr").put("function", "recognition").put("model", "paraformer-realtime-v2")
                    .put("parameters", JSONObject().put("format", "pcm").put("sample_rate", 16000).put("semantic_punctuation_enabled", false)
                        .put("max_sentence_silence", 500).put("inverse_text_normalization_enabled", false).put("heartbeat", true))
                webSocket.send(run.toString())
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    require(text.length <= 2_000_000) { "语音响应过大" }
                    val event = JSONObject(text); val header = event.getJSONObject("header")
                    when (header.optString("event")) {
                        "task-started" -> started.complete(Unit)
                        "task-finished" -> finished.complete(Unit)
                        "task-failed" -> error("语音识别失败：${header.optString("error_code").replace(key, "***").take(80)}")
                        "result-generated" -> parseSentence(event)?.let { result -> lines += result.first; words += result.second }
                    }
                } catch (error: Exception) { started.completeExceptionally(error); finished.completeExceptionally(error); webSocket.cancel() }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val error = IllegalStateException(if (response != null) "语音连接失败 HTTP ${response.code}" else "语音连接中断，请检查网络和接口配置")
                started.completeExceptionally(error); finished.completeExceptionally(error)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!finished.isCompleted) finished.completeExceptionally(IllegalStateException("语音连接提前关闭（$code）"))
            }
        })
        try {
            withTimeout(20_000) { started.await() }
            val total = Files.size(pcm); require(total in 1..57_600_000) { "音频超过 30 分钟或为空" }
            withContext(Dispatchers.IO) {
                Files.newInputStream(pcm).use { input ->
                    val buffer = ByteArray(3200); var sent = 0L
                    while (true) {
                        ensureActive(); if (finished.isCompleted) finished.await()
                        val n = input.read(buffer); if (n < 0) break
                        check(socket.send(buffer.toByteString(0, n))) { "语音上传连接已关闭" }
                        sent += n
                        onProgress(ImportProgress("识别人声与时间轴", completed = (sent / 32000).toInt(), total = ((total + 31999) / 32000).toInt(), fraction = sent.toFloat() / total, detail = "上传音频并接收识别结果"))
                        // Respect streaming pacing; this is input progress, not fabricated model progress.
                        delay(100)
                    }
                }
            }
            socket.send(command("finish-task").toString())
            onProgress(ImportProgress("等待语音识别完成"))
            withTimeout(120_000) { finished.await() }
            val wordLines = AudioLyricAlignment.fromWords(words.sortedBy { it.beginMs })
            val accepted = wordLines.ifEmpty { lines.toList() }.distinctBy { it.timestampMs }.sortedBy { it.timestampMs }
            require(accepted.isNotEmpty() && accepted.any { (it.timestampMs ?: 0) > 0 }) { "未识别出可用的人声时间轴，原歌词已保留" }
            SpeechRecognition(accepted, words.sortedBy { it.beginMs }, ((Files.size(pcm) + 31999) / 32000).toInt())
        } finally { socket.cancel(); client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown() }
    }
    companion object {
        internal fun parseSentence(event: JSONObject): Pair<LyricLine, List<SpeechWord>>? {
            val sentence = event.optJSONObject("payload")?.optJSONObject("output")?.optJSONObject("sentence") ?: return null
            if (!sentence.optBoolean("sentence_end") || sentence.optBoolean("heartbeat")) return null
            val text = sentence.optString("text").trim(); val begin = sentence.optLong("begin_time", -1)
            if (text.isBlank() || begin < 0) return null
            val array = sentence.optJSONArray("words")
            val words = array?.let { a -> (0 until a.length()).mapNotNull { n ->
                val w = a.optJSONObject(n) ?: return@mapNotNull null
                val start = w.optLong("begin_time", -1); val end = w.optLong("end_time", -1)
                if (start < 0 || end < start || w.optString("text").isBlank()) null else SpeechWord(w.getString("text") + w.optString("punctuation"), start, end)
            } }.orEmpty()
            return LyricLine(begin, text) to words
        }
    }
}
