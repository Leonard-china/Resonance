package com.resonance.player.playback

import com.resonance.player.model.AllListeningStats
import com.resonance.player.model.TrackListeningStats
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Call disk operations from IO. A failed load never becomes an empty writable history. */
class ListeningStatsStore(private val storageDir: Path, private val flushIntervalMillis: Long = 0) {
    private val file = storageDir.resolve("listening-stats.json")
    private val backup = storageDir.resolve("listening-stats.json.bak")
    private val trackMap = mutableMapOf<String, TrackListeningStats>()
    private val dailyMap = mutableMapOf<String, Long>()
    private var totalListeningSeconds = 0L
    private var dirty = false
    @get:Synchronized internal val hasPendingWrites: Boolean get() = dirty
    private var lastFlushNanos = 0L

    init { loadFromDisk() }

    @Synchronized fun getTrackStats(trackId: String): TrackListeningStats? = trackMap[trackId]
    @Synchronized fun getAllStats(): AllListeningStats = AllListeningStats(
        totalSeconds = totalListeningSeconds, trackStats = trackMap.toMap(), dailySeconds = dailyMap.toMap())

    @Synchronized
    fun recordPlayback(trackId: String, deltaSeconds: Int, isCompleted: Boolean = false) {
        if (deltaSeconds <= 0 && !isCompleted) return
        val current = trackMap[trackId] ?: TrackListeningStats(trackId)
        trackMap[trackId] = current.copy(
            totalSeconds = current.totalSeconds + deltaSeconds.coerceAtLeast(0),
            playCount = current.playCount + if (isCompleted) 1 else 0,
            lastPlayedAt = System.currentTimeMillis())
        totalListeningSeconds += deltaSeconds.coerceAtLeast(0)
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        dailyMap[day] = (dailyMap[day] ?: 0L) + deltaSeconds.coerceAtLeast(0)
        dirty = true
        if (lastFlushNanos == 0L || isCompleted ||
            (System.nanoTime() - lastFlushNanos) / 1_000_000 >= flushIntervalMillis) flush()
    }

    private fun parse(path: Path): JSONObject? = runCatching {
        if (!Files.isRegularFile(path)) return@runCatching null
        JSONObject(String(Files.readAllBytes(path), Charsets.UTF_8)).also { json ->
            require(json.getLong("totalListeningSeconds") >= 0)
            val tracks = json.getJSONObject("tracks")
            tracks.keys().forEach { key -> tracks.getJSONObject(key).let {
                require(it.getLong("totalSeconds") >= 0 && it.getInt("playCount") >= 0 && it.getLong("lastPlayedAt") >= 0)
            } }
            val daily = json.getJSONObject("daily")
            daily.keys().forEach { require(daily.getLong(it) >= 0) }
        }
    }.getOrNull()

    private fun loadFromDisk() {
        val primary = parse(file)
        val json = primary ?: parse(backup)?.also {
            if (Files.isRegularFile(file)) {
                val preserved = Files.createTempFile(storageDir, "listening-stats-corrupt-", ".json")
                Files.copy(file, preserved, StandardCopyOption.REPLACE_EXISTING)
            }
            replace(file, it.toString().toByteArray(Charsets.UTF_8))
        }
        if (json == null) {
            check(!Files.exists(file) && !Files.exists(backup)) { "听歌记录损坏且没有可恢复备份，请保留记录文件后检查" }
            return
        }
        totalListeningSeconds = json.getLong("totalListeningSeconds")
        val tracks = json.getJSONObject("tracks")
        tracks.keys().forEach { key -> val item = tracks.getJSONObject(key)
            trackMap[key] = TrackListeningStats(key, item.getLong("totalSeconds"), item.getInt("playCount"), item.getLong("lastPlayedAt"))
        }
        val daily = json.getJSONObject("daily")
        daily.keys().forEach { dailyMap[it] = daily.getLong(it) }
    }

    /** Flush accepted increments on pause, track change and composition disposal; keep dirty on failure. */
    @Synchronized fun flush() {
        if (!dirty) return
        val tracks = JSONObject()
        trackMap.forEach { (key, value) -> tracks.put(key, JSONObject()
            .put("totalSeconds", value.totalSeconds).put("playCount", value.playCount).put("lastPlayedAt", value.lastPlayedAt)) }
        val daily = JSONObject()
        dailyMap.forEach { (day, seconds) -> daily.put(day, seconds) }
        val bytes = JSONObject().put("totalListeningSeconds", totalListeningSeconds)
            .put("tracks", tracks).put("daily", daily).toString().toByteArray(Charsets.UTF_8)
        // Never replace a valid backup with a corrupt primary.
        parse(file)?.let { replace(backup, it.toString().toByteArray(Charsets.UTF_8)) }
        replace(file, bytes)
        dirty = false
        lastFlushNanos = System.nanoTime()
    }

    private fun replace(target: Path, bytes: ByteArray) {
        Files.createDirectories(storageDir)
        val temp = Files.createTempFile(storageDir, ".listening-", ".tmp")
        try {
            Files.write(temp, bytes)
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING) }
        } finally { Files.deleteIfExists(temp) }
    }
}
