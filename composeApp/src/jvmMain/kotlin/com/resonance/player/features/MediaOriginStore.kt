package com.resonance.player.features

import com.resonance.player.model.*
import org.json.JSONObject

internal object MediaOriginCodec {
    fun encode(value: MediaOrigin): JSONObject = JSONObject()
        .put("source", value.source).put("sourceId", value.sourceId).put("itemId", value.itemId)
        .put("album", value.album).put("albumId", value.albumId).put("version", value.version)
        .put("evidence", value.evidence).put("coverKind", value.coverKind).put("coverSource", value.coverSource)
        .put("format", value.format).put("preserved", value.preserved).put("importedAt", value.importedAt)
        .put("bitrate", value.bitrate).put("sampleRate", value.sampleRate).put("channels", value.channels)
    fun decode(value: JSONObject) = MediaOrigin(value.optString("source"), value.optString("sourceId"),
        value.optString("itemId"), value.optString("album"), value.optString("albumId"),
        value.optString("version", "版本未确认"), value.optString("evidence"), value.optString("coverKind", "待匹配"),
        value.optString("coverSource"), value.optString("format"), value.optBoolean("preserved"), value.optLong("importedAt"),
        value.optLong("bitrate"), value.optInt("sampleRate"), value.optInt("channels"))
    fun all(root: JSONObject): Map<String, MediaOrigin> = root.optJSONObject("mediaOrigins")?.let { origins ->
        origins.keys().asSequence().mapNotNull { id -> origins.optJSONObject(id)?.let { id to decode(it) } }.toMap()
    }.orEmpty()
}
