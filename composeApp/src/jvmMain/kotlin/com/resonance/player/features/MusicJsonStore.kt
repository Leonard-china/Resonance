package com.resonance.player.features

import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

/** Each update holds the same per-path monitor, including separate foreground/worker instances. */
class MusicJsonStore(val directory: Path) {
    private val file = directory.resolve("music-assistant.json")
    private val monitor get() = file.toAbsolutePath().normalize().toString().intern()
    @Suppress("NOTHING_TO_INLINE")
    fun read(): JSONObject = synchronized(monitor) { readUnlocked() }
    private fun readUnlocked(): JSONObject {
        fun parse(path: Path) = runCatching {
            if (Files.isRegularFile(path)) JSONObject(String(Files.readAllBytes(path), UTF_8)) else null
        }.getOrNull()
        parse(file)?.let { return it }
        val backup = file.resolveSibling("${file.fileName}.bak")
        parse(backup)?.let { recovered ->
            // Restore before the next update so a corrupt primary never replaces the valid backup.
            replaceJson(file, recovered)
            return recovered
        }
        check(!Files.exists(file) && !Files.exists(backup)) { "音乐助手数据损坏且没有可恢复备份，请保留数据文件后检查" }
        return JSONObject()
    }
    fun update(change: (JSONObject) -> Unit) = synchronized(monitor) {
        val json = readUnlocked()
        change(json)
        writeJson(file, json)
    }
    fun cache(key: String): JSONObject? = synchronized(monitor) {
        val path = directory.resolve("cache").resolve("${hash(key)}.json")
        runCatching { if (Files.isRegularFile(path)) JSONObject(String(Files.readAllBytes(path), UTF_8)) else null }.getOrNull()
    }
    fun cache(key: String, value: JSONObject) = synchronized(monitor) { writeJson(directory.resolve("cache").resolve("${hash(key)}.json"), value) }
    companion object {
        fun hash(text: String) = hash(text.toByteArray(UTF_8))
        fun hash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        fun writeJson(target: Path, json: JSONObject) {
            Files.createDirectories(target.parent)
            if (Files.isRegularFile(target)) {
                val backup = Files.createTempFile(target.parent, ".assistant-backup-", ".tmp")
                try {
                    Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING)
                    move(backup, target.resolveSibling("${target.fileName}.bak"))
                } finally { Files.deleteIfExists(backup) }
            }
            replaceJson(target, json)
        }
        private fun replaceJson(target: Path, json: JSONObject) {
            Files.createDirectories(target.parent)
            val temp = Files.createTempFile(target.parent, ".assistant-", ".tmp")
            try {
                Files.write(temp, json.toString().toByteArray(UTF_8))
                move(temp, target)
            } finally { Files.deleteIfExists(temp) }
        }
        private fun move(source: Path, target: Path) {
            try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING) }
        }
    }
}
