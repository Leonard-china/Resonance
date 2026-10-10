package com.resonance.player.playback

import org.json.JSONArray
import org.json.JSONObject

object PlaybackSnapshotCodec {
    fun encode(snapshot: PlaybackSnapshot): String = JSONObject().apply {
        put("trackId", snapshot.trackId)
        put("queueIds", JSONArray(snapshot.queueIds))
        put("progress", snapshot.progress.coerceIn(0f, 1f))
        put("playlistId", snapshot.playlistId)
    }.toString()

    fun decode(value: String?): PlaybackSnapshot? = try {
        value?.let { JSONObject(it) }?.let { json ->
            val id = json.optString("trackId").takeIf(String::isNotBlank) ?: return null
            val queue = json.optJSONArray("queueIds") ?: return null
            PlaybackSnapshot(id, (0 until queue.length()).mapNotNull {
                queue.optString(it).takeIf(String::isNotBlank)
            }.distinct(), json.optDouble("progress", 0.0).takeIf(Double::isFinite)?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
                json.optString("playlistId").takeIf { it.isNotBlank() && it != "null" })
        }
    } catch (_: Exception) { null }
}
