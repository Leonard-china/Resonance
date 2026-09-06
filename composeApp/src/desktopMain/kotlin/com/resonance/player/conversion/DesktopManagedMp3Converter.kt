package com.resonance.player.conversion

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Imports audio into the managed library without ever overwriting or deleting the source. */
class DesktopManagedMp3Converter(
    private val managedDirectory: Path,
    private val ffmpegOverride: Path? = null,
) {
    fun importAsMp3(source: Path): Path {
        require(Files.isRegularFile(source)) { "源音频不存在" }
        Files.createDirectories(managedDirectory)
        val target = targetFor(source)
        if (Files.isRegularFile(target) && Files.size(target) > 50_000) return target

        val extension = source.fileName.toString().substringAfterLast('.', "").lowercase()
        if (extension == "mp3") return copyAtomically(source, target)

        val stagingDirectory = managedDirectory.resolve(".staging-${fingerprint(source)}")
        Files.createDirectories(stagingDirectory)
        var decodedSource: Path? = null
        try {
            val input = when (extension) {
                "kgma", "kgm" -> DesktopKgmaConverter().decrypt(source, stagingDirectory)
                    .also { decodedSource = it.path }
                "kgg" -> decryptKgg(source, stagingDirectory)
                    .also { decodedSource = it.path }
                else -> null
            }

            if (input?.format == NativeAudioFormat.Mp3) {
                return copyAtomically(input.path, target)
            }

            transcodeWithFfmpeg(input?.path ?: source, target)
            return target
        } finally {
            decodedSource?.let(Files::deleteIfExists)
            // Use recursive delete to handle non-empty staging dirs (e.g. partial KGG outputs)
            deleteTree(stagingDirectory)
        }
    }

    /**
     * Decrypt a KGG v5 file into the staging directory.
     * Reads the audio hash from the file header, looks up the ekey in the KuGou local database,
     * then stream-decrypts the audio payload to a temporary file.
     */
    private fun decryptKgg(source: Path, stagingDirectory: Path): DesktopKgmaConverter.DecryptedAudio {
        val minHeader = ByteArray(72)
        val audioHash: String
        val audioOffset: Long
        java.io.BufferedInputStream(Files.newInputStream(source)).use { s ->
            var off = 0
            while (off < minHeader.size) {
                val r = s.read(minHeader, off, minHeader.size - off)
                require(r >= 0) { "KGG 文件头不完整" }
                off += r
            }

            val mode = KggCipher.littleEndianInt(minHeader, 20)
            require(mode == 5) { "暂不支持 KGG v$mode（当前支持 v5）" }

            audioOffset = (KggCipher.littleEndianInt(minHeader, 16).toLong()) and 0xFFFFFFFFL
            val hashLen = KggCipher.littleEndianInt(minHeader, 68)
            require(hashLen in 1..256) { "KGG 文件头中的哈希长度无效: $hashLen" }

            val hashBytes = ByteArray(hashLen)
            var hashOff = 0
            while (hashOff < hashLen) {
                val r = s.read(hashBytes, hashOff, hashLen - hashOff)
                require(r >= 0) { "KGG 文件音频哈希不完整" }
                hashOff += r
            }
            audioHash = hashBytes.toString(Charsets.UTF_8).trim()
        }

        val ekey = KggKeyDatabase.getEkey(audioHash)
            ?: error("在酷狗密钥库中找不到这首歌的密钥：可能是密钥库过期，或歌曲不是在本机酷狗客户端下载的。")

        val cipher = KggCipher.createQMC2(ekey)
            ?: error("KGG 密钥解析失败（ekey 解码后为空）。")

        // Peek at first 16 bytes to detect format before writing everything
        val prefixBuf = ByteArray(16)
        val prefixDecoded: NativeAudioFormat
        java.io.BufferedInputStream(Files.newInputStream(source)).use { s ->
            var rem = audioOffset
            while (rem > 0) {
                val skipped = s.skip(rem)
                if (skipped > 0) rem -= skipped else { check(s.read() >= 0); rem-- }
            }
            val read = s.read(prefixBuf).coerceAtLeast(0)
            val tmp = prefixBuf.copyOf(read)
            when (cipher) {
                is KggCipher.QMC2Cipher.Map -> cipher.decrypt(tmp, 0, read)
                is KggCipher.QMC2Cipher.Rc4 -> cipher.decrypt(tmp, 0L, read)
            }
            prefixDecoded = KggCipher.detectAudioFormat(tmp)
        }
        require(prefixDecoded != NativeAudioFormat.Unknown) { "KGG 解密结果不是可识别的音频格式" }

        val base = source.fileName.toString().substringBeforeLast('.')
        val temporary = stagingDirectory.resolve(".$base.${prefixDecoded.extension}.part")
        val output = stagingDirectory.resolve("$base.${prefixDecoded.extension}")

        Files.newOutputStream(temporary).buffered().use { out ->
            KggCipher.decryptStream(source, audioOffset, cipher, out)
        }
        Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING)
        return DesktopKgmaConverter.DecryptedAudio(output, prefixDecoded)
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
            "-err_detect", "ignore_err",
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
        if (ffmpegOverride != null && Files.isRegularFile(ffmpegOverride)) return ffmpegOverride

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

        val devFallback = Path.of(
            "D:\\Software\\Github\\FlyMouseFormat\\release\\exe\\FlyingMouse Format\\resources\\ffmpeg\\ffmpeg.exe",
        )
        if (Files.isRegularFile(devFallback)) return devFallback

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

    /** Recursively delete a directory tree (no-op if path does not exist). */
    private fun deleteTree(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::deleteIfExists)
    }
}
