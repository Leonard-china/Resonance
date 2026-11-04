package com.resonance.player.audio

import java.nio.file.Files
import java.nio.file.Path

object FfmpegExecutable {
    fun locate(): String = resolve(
        System.getProperty("os.name"),
        listOfNotNull(System.getProperty("resonance.ffmpeg"), System.getenv("RESONANCE_FFMPEG")),
        System.getProperty("compose.application.resources.dir"),
        System.getProperty("jpackage.app-path"),
    )

    internal fun resolve(osName: String, overrides: List<String>, resources: String?, launcher: String?): String {
        val windows = osName.startsWith("Windows", true)
        val executable = if (windows) "ffmpeg.exe" else "ffmpeg"
        val candidates = overrides + listOfNotNull(
            resources?.let { Path.of(it).resolve(executable).toString() },
            launcher?.let {
                Path.of(it).parent.resolve(if (windows) "app/resources" else "../lib/app/resources")
                    .resolve(executable).normalize().toString()
            },
        )
        return candidates.firstOrNull { candidate ->
            candidate.isNotBlank() && runCatching {
                val path = Path.of(candidate)
                Files.isRegularFile(path) && (windows || Files.isExecutable(path))
            }.getOrDefault(false)
        } ?: executable // ProcessBuilder resolves this through PATH.
    }
}
