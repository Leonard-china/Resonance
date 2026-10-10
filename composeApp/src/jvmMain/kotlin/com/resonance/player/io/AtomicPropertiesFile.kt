package com.resonance.player.io

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.util.Properties

/** Each caller serializes its read/modify/write transaction. The last valid revision is recoverable. */
object AtomicPropertiesFile {
    fun read(path: Path, valid: (Properties) -> Boolean = { true }): Properties {
        fun readValid(candidate: Path): Properties? = try {
            if (!Files.isRegularFile(candidate)) null else Properties().apply {
                Files.newBufferedReader(candidate, UTF_8).use(::load)
            }.takeIf(valid)
        } catch (_: Exception) { null }
        readValid(path)?.let { return it }
        val backup = path.resolveSibling("${path.fileName}.bak")
        readValid(backup)?.let {
            replace(path, it, "Recovered Resonance data")
            return it
        }
        if (Files.exists(path) || Files.exists(backup)) error("本地数据损坏且没有可恢复备份：${path.fileName}")
        return Properties()
    }

    fun write(path: Path, properties: Properties, comment: String) {
        Files.createDirectories(path.parent)
        if (Files.isRegularFile(path)) {
            val backup = path.resolveSibling("${path.fileName}.bak")
            val stagedBackup = Files.createTempFile(path.parent, "${path.fileName}-backup-", ".tmp")
            try {
                Files.copy(path, stagedBackup, REPLACE_EXISTING)
                move(stagedBackup, backup)
            } finally { Files.deleteIfExists(stagedBackup) }
        }
        replace(path, properties, comment)
    }

    private fun replace(path: Path, properties: Properties, comment: String) {
        Files.createDirectories(path.parent)
        val temporary = Files.createTempFile(path.parent, "${path.fileName}-", ".tmp")
        try {
            Files.newBufferedWriter(temporary, UTF_8).use { properties.store(it, comment) }
            move(temporary, path)
        } finally { Files.deleteIfExists(temporary) }
    }

    private fun move(source: Path, target: Path) {
        try { Files.move(source, target, REPLACE_EXISTING, ATOMIC_MOVE) }
        catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(source, target, REPLACE_EXISTING) }
    }

    fun validIndexed(properties: Properties, prefix: String, required: String): Boolean {
        val count = properties.getProperty("count")?.toIntOrNull() ?: return false
        return count in 0..1_000_000 && (0 until count).all { properties.containsKey("$prefix.$it.$required") }
    }
}
