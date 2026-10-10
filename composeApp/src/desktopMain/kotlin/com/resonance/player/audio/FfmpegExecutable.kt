package com.resonance.player.audio

import java.nio.file.Files
import java.nio.file.Path

object FfmpegExecutable {
    fun locate(): String {
        val candidates = listOfNotNull(System.getProperty("resonance.ffmpeg"), System.getenv("RESONANCE_FFMPEG"),
            System.getProperty("compose.application.resources.dir")?.let { Path.of(it, "ffmpeg.exe").toString() },
            System.getProperty("jpackage.app-path")?.let { Path.of(it).parent.resolve("app/resources/ffmpeg.exe").toString() },
            "D:/Software/Github/FlyMouseFormat/release/exe/FlyingMouse Format/resources/ffmpeg/ffmpeg.exe")
        return candidates.firstOrNull { Files.isRegularFile(Path.of(it)) } ?: "ffmpeg.exe"
    }
}
