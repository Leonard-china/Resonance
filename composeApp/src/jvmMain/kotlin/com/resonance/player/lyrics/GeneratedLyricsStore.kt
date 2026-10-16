package com.resonance.player.lyrics

import com.resonance.player.features.MusicJsonStore
import com.resonance.player.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Path
import java.nio.file.Files

object GeneratedLyricsStore {
    private fun file(directory: Path, track: Track) = directory.resolve(MusicJsonStore.hash("${track.id}|${track.sourceUri}|${track.title}|${track.artist}|${track.durationText}") + ".json")
    fun read(directory: Path, track: Track): Lyrics? = runCatching {
        val path = file(directory, track)
        if (!Files.isRegularFile(path)) return null
        val json = JSONObject(String(Files.readAllBytes(path), Charsets.UTF_8))
        val array = json.getJSONArray("lines")
        val lines = (0 until array.length()).map { n -> val line = array.getJSONObject(n); LyricLine(line.getLong("ms"), line.getString("text")) }
        require(lines.isNotEmpty() && lines.all { it.timestampMs!! >= 0 } && lines.zipWithNext().all { (a,b) -> a.timestampMs!! <= b.timestampMs!! })
        Lyrics(track.id, lines, true, false, "音频识别 · Paraformer", fromCache = true)
    }.getOrNull()
    fun write(directory: Path, track: Track, lyrics: Lyrics, original: Lyrics?) {
        val json = JSONObject().put("lines", JSONArray(lyrics.lines.map { JSONObject().put("ms", it.timestampMs).put("text", it.text) }))
            .put("original", JSONArray(original?.lines.orEmpty().map { JSONObject().put("ms", it.timestampMs).put("text", it.text) }))
        MusicJsonStore.writeJson(file(directory, track), json)
    }
}
