package com.resonance.player.features

import com.resonance.player.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Durable stage records, shared by foreground and workers through MusicJsonStore's monitor. */
internal class MusicTaskJournal(private val store: MusicJsonStore) {
    fun begin(title: String, kind: String = "", trackId: String = "", payload: JSONObject = JSONObject()): String {
        val id = UUID.randomUUID().toString()
        store.update { root ->
            val previous = root.optJSONArray("tasks") ?: JSONArray()
            val kept = (0 until previous.length()).map(previous::getJSONObject).takeLast(99)
            root.put("tasks", JSONArray(kept + JSONObject().put("id", id).put("title", title).put("kind", kind).put("trackId", trackId)
                .put("payload", payload).put("outcome", "Running").put("createdAt", System.currentTimeMillis())))
        }
        return id
    }
    fun update(id: String, progress: ImportProgress? = null, outcome: MusicTaskOutcome? = null, message: String? = null) {
        store.update { root ->
            val a = root.optJSONArray("tasks") ?: return@update
            (0 until a.length()).map(a::getJSONObject).firstOrNull { it.optString("id") == id }?.let { task ->
                progress?.let { p -> task.put("stage", p.stage).put("fileName", p.fileName).put("completed", p.completed).put("total", p.total).put("fraction", p.fraction).put("detail", p.detail) }
                outcome?.let { task.put("outcome", it.name) }
                message?.let { task.put("message", it.take(1500)) }
            }
        }
    }
    fun interrupted() {
        store.update { root ->
            val a = root.optJSONArray("tasks") ?: return@update
            (0 until a.length()).map(a::getJSONObject).filter { it.optString("outcome") == "Running" }.forEach {
                it.put("outcome", "Interrupted").put("message", "应用退出前此步骤未完成；已入库音频保留。")
            }
        }
    }
    fun record(id: String): JSONObject? = store.read().optJSONArray("tasks")?.let { a -> (0 until a.length()).map(a::getJSONObject).firstOrNull { it.optString("id") == id } }
    fun entries(): List<MusicTaskEntry> = store.read().optJSONArray("tasks")?.let { a -> (0 until a.length()).mapNotNull { i ->
        runCatching {
            val j = a.getJSONObject(i)
            MusicTaskEntry(j.getString("id"), j.getString("title"), j.optString("kind"), j.optString("trackId"),
                MusicTaskOutcome.entries.firstOrNull { it.name == j.optString("outcome") } ?: MusicTaskOutcome.Interrupted,
                ImportProgress(j.optString("stage", j.getString("title")), j.optString("fileName"), j.optInt("completed"), j.optInt("total"),
                    if (j.has("fraction") && !j.isNull("fraction")) j.optDouble("fraction").toFloat() else null, j.optString("detail")),
                j.optString("message"), j.optLong("createdAt"), j.optString("kind") in setOf("video", "cover", "lyrics", "syncLyrics"))
        }.getOrNull()
    } }.orEmpty()
}
