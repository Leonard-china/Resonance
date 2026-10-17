package com.resonance.player.lyrics

import com.resonance.player.features.MusicJsonStore
import com.resonance.player.model.Track
import com.resonance.player.model.Lyrics
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path

/** A user correction applies to the recording on this device, independent of lyric provider. */
internal object LyricOffsetStore {
    private fun path(directory: Path, track: Track): Path = directory.resolve("offsets").resolve(MusicJsonStore.hash(track.id) + ".json")
    fun read(directory: Path, track: Track): Long? = runCatching {
        val file = path(directory, track)
        if (!Files.isRegularFile(file)) return null
        JSONObject(String(Files.readAllBytes(file), Charsets.UTF_8)).getLong("offsetMs").coerceIn(-600_000L, 600_000L)
    }.getOrNull()
    fun write(directory: Path, track: Track, offset: Long) {
        MusicJsonStore.writeJson(path(directory, track), JSONObject().put("offsetMs", offset.coerceIn(-600_000L, 600_000L)))
    }
    fun apply(directory: Path, track: Track, lyrics: Lyrics): Lyrics =
        read(directory, track)?.let { lyrics.copy(offsetMs = it) } ?: lyrics
}
