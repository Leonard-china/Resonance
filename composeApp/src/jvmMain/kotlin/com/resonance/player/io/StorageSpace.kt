package com.resonance.player.io

import java.nio.file.Files
import java.nio.file.Path

/**
 * Queries usable disk space safely across Android (including FUSE/SELinux scoped storage),
 * Linux, and Windows.
 *
 * Uses [java.io.File.getUsableSpace] first (direct `statvfs` on the path without reading
 * `/proc/mounts`, which fails with `IOException: Mount point not found` or
 * `AccessDeniedException` on many Android 11–16 devices), falling back to
 * [Files.getFileStore] only if needed.
 */
fun usableSpaceBytes(path: Path): Long {
    var cursor: Path? = runCatching { path.toAbsolutePath().normalize() }.getOrDefault(path)
    while (cursor != null && !Files.exists(cursor)) {
        cursor = cursor.parent
    }
    val existing = cursor ?: return 0L
    val direct = runCatching { existing.toFile().usableSpace }.getOrDefault(0L)
    if (direct > 0L) return direct
    return runCatching { Files.getFileStore(existing).usableSpace }.getOrDefault(direct.coerceAtLeast(0L))
}
