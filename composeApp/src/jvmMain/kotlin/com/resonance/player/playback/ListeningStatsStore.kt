package com.resonance.player.playback

import com.resonance.player.model.AllListeningStats
import com.resonance.player.model.TrackListeningStats
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 听歌时长与播放次数统计持久化存储（原子写入与内存缓存）
 */
class ListeningStatsStore(private val storageDir: Path) {
    private val file = storageDir.resolve("listening-stats.json")
    private val trackMap = ConcurrentHashMap<String, TrackListeningStats>()
    private val dailyMap = ConcurrentHashMap<String, Long>()
    @Volatile private var totalListeningSeconds = 0L

    init {
        loadFromDisk()
    }

    fun getTrackStats(trackId: String): TrackListeningStats? = trackMap[trackId]

    fun getAllStats(): AllListeningStats = AllListeningStats(
        totalSeconds = totalListeningSeconds,
        trackStats = trackMap.toMap(),
        dailySeconds = dailyMap.toMap(),
    )

    @Synchronized
    fun recordPlayback(trackId: String, deltaSeconds: Int, isCompleted: Boolean = false) {
        if (deltaSeconds <= 0 && !isCompleted) return
        val current = trackMap[trackId] ?: TrackListeningStats(trackId)
        val updated = current.copy(
            totalSeconds = current.totalSeconds + deltaSeconds.coerceAtLeast(0),
            playCount = if (isCompleted) current.playCount + 1 else current.playCount,
            lastPlayedAt = System.currentTimeMillis(),
        )
        trackMap[trackId] = updated
        totalListeningSeconds += deltaSeconds.coerceAtLeast(0)

        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dailyMap.compute(dateKey) { _, old -> (old ?: 0L) + deltaSeconds.coerceAtLeast(0) }

        saveToDisk()
    }

    @Synchronized
    private fun loadFromDisk() {
        if (!Files.exists(file)) return
        try {
            val bytes = Files.readAllBytes(file)
            val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
            totalListeningSeconds = json.optLong("totalListeningSeconds", 0L)

            val tracksObj = json.optJSONObject("tracks")
            if (tracksObj != null) {
                val keys = tracksObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = tracksObj.optJSONObject(key) ?: continue
                    trackMap[key] = TrackListeningStats(
                        trackId = key,
                        totalSeconds = item.optLong("totalSeconds", 0L),
                        playCount = item.optInt("playCount", 0),
                        lastPlayedAt = item.optLong("lastPlayedAt", 0L),
                    )
                }
            }

            val dailyObj = json.optJSONObject("daily")
            if (dailyObj != null) {
                val keys = dailyObj.keys()
                while (keys.hasNext()) {
                    val dateKey = keys.next()
                    dailyMap[dateKey] = dailyObj.optLong(dateKey, 0L)
                }
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun saveToDisk() {
        try {
            Files.createDirectories(storageDir)
            val json = JSONObject()
            json.put("totalListeningSeconds", totalListeningSeconds)

            val tracksObj = JSONObject()
            for ((key, value) in trackMap) {
                val item = JSONObject()
                item.put("totalSeconds", value.totalSeconds)
                item.put("playCount", value.playCount)
                item.put("lastPlayedAt", value.lastPlayedAt)
                tracksObj.put(key, item)
            }
            json.put("tracks", tracksObj)

            val dailyObj = JSONObject()
            for ((dateKey, secs) in dailyMap) {
                dailyObj.put(dateKey, secs)
            }
            json.put("daily", dailyObj)

            val temp = storageDir.resolve("listening-stats.json.tmp")
            Files.write(temp, json.toString(2).toByteArray(StandardCharsets.UTF_8))
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {}
    }
}
