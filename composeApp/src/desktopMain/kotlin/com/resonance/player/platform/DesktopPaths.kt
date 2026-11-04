package com.resonance.player.platform

import java.nio.file.Files
import java.nio.file.Path

internal object DesktopPaths {
    fun dataDirectory(
        osName: String = System.getProperty("os.name"),
        home: Path = Path.of(System.getProperty("user.home")),
        environment: Map<String, String> = System.getenv(),
        override: String? = System.getProperty("RESONANCE_DATA") ?: environment["RESONANCE_DATA"],
    ): Path {
        override?.takeIf(String::isNotBlank)?.let { return Path.of(it) }
        if (osName.startsWith("Linux", true)) {
            val xdg = environment["XDG_DATA_HOME"]?.takeIf(String::isNotBlank)?.let(Path::of)
                ?.takeIf(Path::isAbsolute)
            val target = (xdg ?: home.resolve(".local/share")).resolve("resonance")
            // Keep data from earlier JVM runs usable, without moving or merging files.
            val legacy = home.resolve("Resonance")
            return if (!Files.exists(target) && Files.isDirectory(legacy)) legacy else target
        }
        return (environment["LOCALAPPDATA"]?.takeIf(String::isNotBlank)?.let(Path::of) ?: home).resolve("Resonance")
    }
}
