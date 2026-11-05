package com.resonance.player.features

import com.resonance.player.model.*
import org.json.JSONArray
import org.json.JSONObject

internal object ScreenshotDraftCodec {
    fun encode(draft: ScreenshotImportDraft): JSONObject {
        require(draft.id.isNotBlank() && draft.id.length <= 100)
        require(draft.playlists.size in 1..20 && draft.playlists.all { it.name.length <= 80 && it.tracks.size <= 300 && it.tracks.all { s -> s.title.length <= 180 && s.artist.length <= 180 && s.album.length <= 180 } })
        val keys = draft.playlists.flatMapIndexed { i, list -> list.tracks.indices.map { "$i:$it" } }.toSet()
        require(draft.selectedTracks?.all { it in keys } != false)
        return JSONObject().put("id", draft.id).put("recognition", draft.recognition.take(300))
            .put("playlists", JSONArray(draft.playlists.map { list -> JSONObject().put("name", list.name).put("tracks",
                JSONArray(list.tracks.map { song -> JSONObject().put("title", song.title).put("artist", song.artist).put("album", song.album) })) }))
            .also { json -> draft.selectedTracks?.let { json.put("selected", JSONArray(it.toList())) } }
    }
    fun decode(json: JSONObject?): ScreenshotImportDraft? {
        if (json == null) return null
        return runCatching {
            val lists = json.getJSONArray("playlists")
            require(lists.length() in 1..20)
            val draft = ScreenshotImportDraft(json.getString("id"), (0 until lists.length()).map { i ->
                val list = lists.getJSONObject(i); val songs = list.getJSONArray("tracks"); require(songs.length() <= 300)
                ScreenshotPlaylistDraft(list.getString("name"), (0 until songs.length()).map { n ->
                    val song = songs.getJSONObject(n); SongRequest(song.getString("title"), song.getString("artist"), song.optString("album"))
                })
            }, json.optString("recognition"), json.optJSONArray("selected")?.let { a -> (0 until a.length()).map(a::getString).toSet() })
            encode(draft); draft
        }.getOrNull()
    }
}
