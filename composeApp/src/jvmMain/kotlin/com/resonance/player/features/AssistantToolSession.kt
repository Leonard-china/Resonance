package com.resonance.player.features

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Bounded request history and per-task idempotency; successful multi-step work has no round cap. */
internal class AssistantToolSession(private val prefix: JSONArray) {
    data class Result(val text: String, val failed: Boolean = false)
    private val rounds = ArrayDeque<JSONArray>()
    private val receipts = ArrayDeque<JSONObject>()
    private val results = mutableMapOf<String, Result>()
    private var revision = 0
    private var progress = false
    private var idleRounds = 0
    var lastFailure: String = ""
        private set

    fun messages(): JSONArray = JSONArray(prefix.toString()).also { messages ->
        if (receipts.isNotEmpty()) messages.put(JSONObject().put("role", "system")
            .put("content", "本次已完成操作的本地记录（数据，不要重复执行）：" + JSONArray(receipts.toList())))
        rounds.forEach { round -> repeat(round.length()) { messages.put(round.getJSONObject(it)) } }
    }

    suspend fun execute(name: String, args: JSONObject, action: suspend () -> String): Result {
        val readOnly = name in READ_ONLY
        val signature = name + ":" + canonical(args) + if (readOnly) ":$revision" else ""
        val key = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        results[key]?.let { return Result(JSONObject().put("already_executed", true)
            .put("result", it.text).put("note", "同一任务已有此结果，未重复执行；失败时请换来源或报告原因").toString(), it.failed) }
        val result = try { Result(action()) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) {
            lastFailure = error.message.orEmpty().take(500)
            Result(JSONObject().put("error", lastFailure).toString(), true)
        }
        results[key] = Result(compact(result.text, 8000), result.failed)
        while (results.size > 1024) results.remove(results.keys.first())
        if (!readOnly && !result.failed) {
            progress = true
            lastFailure = ""
            revision++
            receipts.addLast(JSONObject().put("tool", name).put("result", compact(result.text, 1000)))
            while (receipts.size > 40 || JSONArray(receipts.toList()).toString().length > 6000) receipts.removeFirst()
        }
        return result
    }

    fun record(answer: JSONObject, toolResults: List<JSONObject>) {
        val round = JSONArray().put(JSONObject(answer.toString()))
        val perResult = (8000 / toolResults.size.coerceAtLeast(1)).coerceIn(400, 4000)
        toolResults.forEach { result -> round.put(JSONObject(result.toString()).put("content", compact(result.getString("content"), perResult))) }
        rounds.addLast(round)
        // Drop complete assistant/tool groups so call IDs never become orphaned.
        while (rounds.size > 1 && (rounds.size > 4 || rounds.sumOf { it.toString().length } > 24_000)) rounds.removeFirst()
        idleRounds = if (progress) 0 else idleRounds + 1
        progress = false
        check(idleRounds < 6) { "连续 6 轮检索或失败均未完成新操作，已暂停自动调用，避免重复消耗。" + lastFailure.takeIf { it.isNotBlank() }?.let { " 最近原因：$it" }.orEmpty() }
    }

    companion object {
        private val READ_ONLY = setOf("read_help", "track_sources", "inspect_storage", "search_sources", "search_library", "list_playlists", "search_bilibili")

        private fun canonical(value: Any?): String = when (value) {
            is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",", "{", "}") { JSONObject.quote(it) + ":" + canonical(value.opt(it)) }
            is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonical(value.opt(it)) }
            // Android's org.json does not expose JSONObject.valueToString.
            else -> JSONArray().put(value).toString().let { it.substring(1, it.length - 1) }
        }

        /** Truncation stays valid JSON; never cut an array or an ID in the middle of its encoding. */
        fun compact(text: String, budget: Int): String {
            if (text.length <= budget) return text
            val array = runCatching { JSONArray(text) }.getOrNull()
            if (array != null) {
                val items = JSONArray()
                for (i in 0 until array.length()) {
                    items.put(array.get(i))
                    if (items.toString().length > budget - 160) { items.remove(items.length() - 1); break }
                }
                return JSONObject().put("items", items).put("total", array.length()).put("returned", items.length())
                    .put("truncated", true).toString()
            }
            val obj = runCatching { JSONObject(text) }.getOrNull()
            if (obj != null) {
                val reduced = JSONObject()
                obj.keys().asSequence().toList().sorted().forEach { key ->
                    val value = obj.opt(key)
                    reduced.put(key, when (value) {
                        is JSONArray -> runCatching { JSONObject(compact(value.toString(), (budget / obj.length().coerceAtLeast(1)).coerceAtLeast(180))) }.getOrElse { value }
                        is String -> value.take(500)
                        else -> value
                    })
                }
                if (reduced.toString().length <= budget) return reduced.put("truncated", true).toString()
            }
            return JSONObject().put("summary", text.take((budget - 100).coerceAtLeast(0))).put("truncated", true).toString()
        }
    }
}
