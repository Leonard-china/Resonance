package com.resonance.player.audio

import com.resonance.player.features.MusicJsonStore
import org.json.JSONObject
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/** Same per-path locking, atomic replacement and backup recovery as the assistant store. */
class AudioFeatureStore(storageDir: Path) {
    private val store = MusicJsonStore(storageDir, "audio-features.json")
    private val memory = ConcurrentHashMap<String, AudioFeatures>()
    init {
        val json = store.read()
        for (id in json.keys()) {
            val item = json.optJSONObject(id) ?: continue
            val bpm = item.optInt("bpm", 0)
            if (bpm !in 0..240) continue
            memory[id] = AudioFeatures(id, bpm, item.optInt("energy", 0), item.optLong("calculatedAt", 0), item.optString("revision"))
        }
    }
    fun get(trackId: String): AudioFeatures? = memory[trackId]
    fun getAll(): Map<String, AudioFeatures> = memory.toMap()
    fun put(features: AudioFeatures) {
        store.update { json -> json.put(features.trackId, JSONObject().put("bpm", features.bpm).put("energy", features.energy)
            .put("calculatedAt", features.calculatedAt).put("revision", features.revision)) }
        memory[features.trackId] = features
    }
}
