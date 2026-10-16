package com.resonance.player.media

import com.resonance.player.model.VideoImportRequest
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

object Mp3Import {
    fun arguments(input: Path, output: Path, request: VideoImportRequest, cover: Path?): Array<String> {
        val args = mutableListOf("-hide_banner", "-loglevel", "error", "-nostdin", "-y", "-threads", "0", "-i", input.toString())
        if (cover != null) args += listOf("-i", cover.toString())
        args += listOf("-map", "0:a:0")
        if (cover != null) args += listOf("-map", "1:v:0", "-c:v", "mjpeg", "-disposition:v:0", "attached_pic")
        args += listOf("-c:a", "libmp3lame", "-b:a", "320k", "-id3v2_version", "3", "-metadata", "title=${request.title.take(180)}", "-metadata", "artist=${request.artist.take(180)}", "-metadata", "album=${request.album.take(180)}", "-f", "mp3", output.toString())
        return args.toTypedArray()
    }
    fun hash(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input -> val buffer = ByteArray(128 * 1024); while (true) { val size = input.read(buffer); if (size < 0) break; digest.update(buffer, 0, size) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
