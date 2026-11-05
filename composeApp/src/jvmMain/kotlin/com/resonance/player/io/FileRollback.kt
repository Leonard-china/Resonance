package com.resonance.player.io

import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*

/** Runtime rollback for a bounded set of app-owned files; this is not a crash recovery journal. */
object FileRollback {
    fun <T> run(targets: List<Path>, action: () -> T): T {
        val paths = targets.map { it.toAbsolutePath().normalize() }.distinct()
        if (paths.isEmpty()) return action()
        paths.forEach { require(!Files.exists(it, NOFOLLOW_LINKS) || Files.isRegularFile(it, NOFOLLOW_LINKS)) {
            "无法替换非普通文件：${it.fileName}"
        } }
        val parent = paths.first().parent
        Files.createDirectories(parent)
        val backup = Files.createTempDirectory(parent, ".rollback-")
        val saved = linkedMapOf<Path, Path?>()
        var keepBackup = false
        try {
            paths.forEachIndexed { index, target ->
                saved[target] = if (Files.exists(target, NOFOLLOW_LINKS)) {
                    backup.resolve(index.toString()).also { Files.copy(target, it) }
                } else null
            }
            return try { action() } catch (error: Throwable) {
                saved.entries.toList().asReversed().forEach { (target, original) ->
                    try {
                        if (original == null) Files.deleteIfExists(target)
                        else {
                            val temporary = Files.createTempFile(target.parent, ".restore-", ".tmp")
                            try {
                                Files.copy(original, temporary, REPLACE_EXISTING)
                                try { Files.move(temporary, target, REPLACE_EXISTING, ATOMIC_MOVE) }
                                catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temporary, target, REPLACE_EXISTING) }
                            } finally { Files.deleteIfExists(temporary) }
                        }
                    } catch (rollback: Throwable) { keepBackup = true; error.addSuppressed(rollback) }
                }
                if (keepBackup) {
                    runCatching { Files.newBufferedWriter(backup.resolve("recovery-paths.txt"), java.nio.charset.StandardCharsets.UTF_8).use { writer -> writer.write(saved.entries.joinToString("\n") { "${it.value?.fileName ?: "absent"}=${it.key}" }) } }
                        .exceptionOrNull()?.let(error::addSuppressed)
                    error.addSuppressed(IllegalStateException("部分文件恢复失败，原文件副本保留在 $backup"))
                }
                throw error
            }
        } finally {
            // Cleanup cannot turn an already committed transaction into a reported failure.
            if (!keepBackup) runCatching { Files.walk(backup).use { entries -> entries.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
        }
    }
}
