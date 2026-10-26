package com.resonance.player.audio

import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

class AudioFeatureStore(private val storageDir: Path) {
    private val file = storageDir.resolve("audio-features.json")
    private val memory = ConcurrentHashMap<String, AudioFeatures>()

    init {
        loadFromDisk()
    }

    fun get(trackId: String): AudioFeatures? = memory[trackId]

    fun put(features: AudioFeatures) {
        memory[features.trackId] = features
        saveToDisk()
    }

    fun getAll(): Map<String, AudioFeatures> = memory.toMap()

    @Synchronized
    private fun loadFromDisk() {
        if (!Files.exists(file)) return
        try {
            val bytes = Files.readAllBytes(file)
            val content = String(bytes, StandardCharsets.UTF_8)
            val json = JSONObject(content)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = json.optJSONObject(key) ?: continue
                memory[key] = AudioFeatures(
                    trackId = key,
                    bpm = item.optInt("bpm", 120),
                    energy = item.optInt("energy", 50),
                    calculatedAt = item.optLong("calculatedAt", 0L),
                )
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun saveToDisk() {
        try {
            Files.createDirectories(storageDir)
            val json = JSONObject()
            for ((key, value) in memory) {
                val item = JSONObject()
                item.put("bpm", value.bpm)
                item.put("energy", value.energy)
                item.put("calculatedAt", value.calculatedAt)
                json.put(key, item)
            }
            val temp = storageDir.resolve("audio-features.json.tmp")
            Files.write(temp, json.toString(2).toByteArray(StandardCharsets.UTF_8))
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {}
    }
}
