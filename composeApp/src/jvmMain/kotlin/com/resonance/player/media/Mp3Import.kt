package com.resonance.player.media

import com.resonance.player.model.VideoImportRequest
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import com.resonance.player.io.readUpTo

object Mp3Import {
    fun arguments(input: Path, output: Path, request: VideoImportRequest, cover: Path?): Array<String> {
        val args = mutableListOf("-hide_banner", "-loglevel", "error", "-nostdin", "-y", "-threads", "0", "-i", input.toString())
        val extension = output.fileName.toString().substringAfterLast('.', "mp3").lowercase()
        val embedCover = cover != null && extension in setOf("mp3", "m4a", "flac")
        if (embedCover) args += listOf("-i", cover.toString())
        args += listOf("-map", "0:a:0")
        if (embedCover) args += listOf("-map", "1:v:0", "-c:v", "mjpeg", "-disposition:v:0", "attached_pic")
        args += if (request.preserveAudio) listOf("-c:a", "copy") else listOf("-c:a", "libmp3lame", "-b:a", "320k")
        if (extension == "mp3") args += listOf("-id3v2_version", "3")
        args += listOf("-metadata", "title=" + request.title.take(180), "-metadata", "artist=" + request.artist.take(180), "-metadata", "album=" + request.album.take(180), "-f", if (extension == "m4a") "ipod" else extension, output.toString())
        return args.toTypedArray()
    }
    fun originalExtension(path: Path): String? {
        val bytes = Files.newInputStream(path).use { it.readUpTo(16) }
        if (bytes.size < 4) return null
        if (bytes.copyOfRange(0, 4).contentEquals("fLaC".toByteArray())) return "flac"
        if (bytes.copyOfRange(0, 4).contentEquals("OggS".toByteArray())) return "ogg"
        if (bytes.size >= 8 && String(bytes, 4, 4, Charsets.US_ASCII) == "ftyp") return "m4a"
        if (String(bytes, 0, 3, Charsets.US_ASCII) == "ID3") return "mp3"
        val first = bytes[0].toInt() and 255; val second = bytes[1].toInt() and 255
        if (first == 255 && second and 0xf6 == 0xf0) return "m4a"
        if (first == 255 && second and 0xe0 == 0xe0) return "mp3"
        return null
    }
    fun mime(extension: String): String = when (extension.lowercase()) {
        "m4a" -> "audio/mp4"; "flac" -> "audio/flac"; "ogg" -> "audio/ogg"; else -> "audio/mpeg"
    }
    fun hash(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input -> val buffer = ByteArray(128 * 1024); while (true) { val size = input.read(buffer); if (size < 0) break; digest.update(buffer, 0, size) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
