package com.resonance.player.features

import com.resonance.player.model.*
import org.json.JSONArray
import org.json.JSONObject

object MusicFeatureCodec {
    fun settings(json: JSONObject?, decrypt: (String) -> String = { it }): MusicFeatureSettings {
        if (json == null) return MusicFeatureSettings()
        val connections = json.optJSONArray("connections")?.let { a -> (0 until a.length()).mapNotNull { i ->
            val c = a.optJSONObject(i) ?: return@mapNotNull null
            val provider = AiProvider.entries.firstOrNull { it.name == c.optString("provider") } ?: return@mapNotNull null
            AiConnection(provider, c.optString("baseUrl", provider.url), c.optString("model", provider.defaultModel), decrypt(c.optString("key")), c.optBoolean("vision", provider != AiProvider.DeepSeek))
        } }?.takeIf { it.isNotEmpty() } ?: MusicFeatureSettings().connections
        return MusicFeatureSettings(connections,
            AiProvider.entries.firstOrNull { it.name == json.optString("assistant") } ?: AiProvider.Qwen,
            AiProvider.entries.firstOrNull { it.name == json.optString("vision") } ?: AiProvider.Qwen,
            json.optBoolean("recommendationsEnabled"), json.optBoolean("backgroundRecommendations"),
            json.optInt("intervalHours", 24).coerceIn(1, 168), json.optInt("recommendationCount", 5).coerceIn(1, 30),
            json.optLong("monthlyTokenLimit", 1_000_000).coerceIn(10_000, 100_000_000),
            json.optBoolean("floatingLyrics"), json.optBoolean("floatingLyricsLocked"), json.optInt("floatingLyricsSize", 24).coerceIn(14, 56),
            json.optString("translationLanguage", "中文").take(40),
            LyricDisplay.entries.firstOrNull { it.name == json.optString("lyricDisplay") } ?: LyricDisplay.Bilingual,
            decrypt(json.optString("bilibiliCookie")))
    }
    fun encode(s: MusicFeatureSettings, encrypt: (String) -> String = { it }): JSONObject = JSONObject()
        .put("connections", JSONArray(s.connections.map { c -> JSONObject().put("provider", c.provider.name).put("baseUrl", c.baseUrl.trim()).put("model", c.model.trim()).put("key", encrypt(c.apiKey.trim())).put("vision", c.vision) }))
        .put("assistant", s.assistantProvider.name).put("vision", s.visionProvider.name)
        .put("recommendationsEnabled", s.recommendationsEnabled).put("backgroundRecommendations", s.backgroundRecommendations)
        .put("intervalHours", s.intervalHours.coerceIn(1, 168)).put("recommendationCount", s.recommendationCount.coerceIn(1, 30)).put("monthlyTokenLimit", s.monthlyTokenLimit)
        .put("floatingLyrics", s.floatingLyrics).put("floatingLyricsLocked", s.floatingLyricsLocked).put("floatingLyricsSize", s.floatingLyricsSize.coerceIn(14, 56))
        .put("translationLanguage", s.translationLanguage.take(40)).put("lyricDisplay", s.lyricDisplay.name).put("bilibiliCookie", encrypt(s.bilibiliCookie))
    fun track(t: Track): JSONObject = JSONObject().put("id", t.id).put("title", t.title).put("artist", t.artist).put("album", t.album)
        .put("duration", t.durationText).put("seed", t.artworkSeed).put("favorite", t.isFavorite).put("source", t.sourceUri).put("artwork", t.artworkPath).put("mime", t.mimeType)
    fun track(j: JSONObject): Track = Track(j.getString("id"), j.getString("title"), j.optString("artist", "未知歌手"), j.optString("album", "本地音乐"), j.optString("duration", "0:00"), j.optInt("seed"), j.optBoolean("favorite"), j.optString("source").takeIf { it.isNotBlank() && it != "null" }, j.optString("artwork").takeIf { it.isNotBlank() && it != "null" }, j.optString("mime", "audio/mpeg"))
    fun tracks(a: JSONArray?): List<Track> = if (a == null) emptyList() else (0 until a.length()).mapNotNull { runCatching { track(a.getJSONObject(it)) }.getOrNull() }
}
