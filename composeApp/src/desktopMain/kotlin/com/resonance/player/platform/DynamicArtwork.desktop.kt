package com.resonance.player.platform

import com.resonance.player.io.readUpTo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path
import java.net.URL

actual fun decodeArtwork(path: String): ImageBitmap? = runCatching {
    val bytes = if (path.startsWith("https://")) {
        URL(path).openConnection().apply {
            connectTimeout = 8_000
            readTimeout = 12_000
        }.getInputStream().buffered().use { it.readUpTo(5_000_001) }.also {
            require(it.size <= 5_000_000) { "封面文件过大" }
        }
    } else {
        Files.readAllBytes(Path.of(path))
    }
    Image.makeFromEncoded(bytes).toComposeImageBitmap()
}.getOrNull()
