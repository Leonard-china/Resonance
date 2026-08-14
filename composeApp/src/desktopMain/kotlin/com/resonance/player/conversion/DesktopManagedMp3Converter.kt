package com.resonance.player.conversion

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Imports audio into the managed library without ever overwriting or deleting the source. */
class DesktopManagedMp3Converter(private val managedDirectory: Path) {
    fun importAsMp3(source: Path): Path {
        require(Files.isRegularFile(source)) { "源音频不存在" }
        Files.createDirectories(managedDirectory)
        val target = targetFor(source)
        if (Files.isRegularFile(target)) return target

        val extension = source.fileName.toString().substringAfterLast('.', "").lowercase()
        if (extension == "mp3") return copyAtomically(source, target)

        val stagingDirectory = managedDirectory.resolve(".staging-${fingerprint(source)}")
        Files.createDirectories(stagingDirectory)
        var decodedSource: Path? = null
        try {
            val input = if (extension == "kgma") {
                DesktopKgmaConverter().decrypt(source, stagingDirectory).also { decodedSource = it.path }
            } else {
                null
            }

            if (input?.format == NativeAudioFormat.Mp3) {
                return copyAtomically(input.path, target)
            }

            transcodeWithFfmpeg(input?.path ?: source, target)
            return target
        } finally {
            decodedSource?.let(Files::deleteIfExists)
            Files.deleteIfExists(stagingDirectory)
        }
    }

    private fun copyAtomically(source: Path, target: Path): Path {
        val temporary = target.resolveSibling(".${target.fileName}.part")
        Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING)
        moveIntoPlace(temporary, target)
        return target
    }

    private fun transcodeWithFfmpeg(source: Path, target: Path) {
        val ffmpeg = locateFfmpeg()
        val temporary = target.resolveSibling(".${target.fileName}.part")
        val process = ProcessBuilder(
            ffmpeg.toString(),
            "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
            "-i", source.toString(),
            "-map", "0:a:0", "-map", "0:v?", "-map_metadata", "0",
            "-c:a", "libmp3lame", "-b:a", "320k",
            "-c:v", "copy", "-id3v2_version", "3", "-write_id3v1", "1",
            "-f", "mp3", temporary.toString(),
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        if (exitCode != 0 || !Files.isRegularFile(temporary)) {
            Files.deleteIfExists(temporary)
            error("FFmpeg 转换失败（$exitCode）：${output.takeLast(500).trim()}")
        }
        moveIntoPlace(temporary, target)
    }

    private fun locateFfmpeg(): Path {
        val configured = sequenceOf(
            System.getProperty("resonance.ffmpeg"),
            System.getenv("RESONANCE_FFMPEG"),
        ).filterNotNull().filter(String::isNotBlank).map(Path::of).firstOrNull(Files::isRegularFile)
        if (configured != null) return configured

        val composeResource = System.getProperty("compose.application.resources.dir")
            ?.let(Path::of)?.resolve("ffmpeg.exe")
        if (composeResource != null && Files.isRegularFile(composeResource)) return composeResource

        val packaged = System.getProperty("jpackage.app-path")?.let(Path::of)?.parent
            ?.resolve("app")?.resolve("resources")?.resolve("ffmpeg.exe")
        if (packaged != null && Files.isRegularFile(packaged)) return packaged

        // Development fallback: reuse the user's existing D: copy and avoid another 227 MB download.
        val flyMouse = Path.of(
            "D:\\Software\\Github\\FlyMouseFormat\\release\\exe\\FlyingMouse Format\\resources\\ffmpeg\\ffmpeg.exe",
        )
        if (Files.isRegularFile(flyMouse)) return flyMouse

        return Path.of("ffmpeg.exe")
    }

    private fun targetFor(source: Path): Path {
        val rawName = source.fileName.toString().substringBeforeLast('.').ifBlank { "未命名音乐" }
        val safeName = rawName.replace(Regex("""[<>:"/\\|?*\u0000-\u001f]"""), "_").trim().take(100)
        return managedDirectory.resolve("$safeName [${fingerprint(source)}].mp3")
    }

    private fun fingerprint(source: Path): String {
        val absolute = source.toAbsolutePath().normalize()
        val signature = "$absolute|${Files.size(source)}|${Files.getLastModifiedTime(source).toMillis()}"
        return MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray(StandardCharsets.UTF_8))
            .take(5)
            .joinToString("") { "%02x".format(it) }
    }

    private fun moveIntoPlace(temporary: Path, target: Path) {
        runCatching {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
