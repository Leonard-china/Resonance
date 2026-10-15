package com.resonance.player.platform

import com.resonance.player.io.readUpTo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path
import java.net.URL
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual fun decodeArtwork(path: String): ImageBitmap? = runCatching {
    val bytes = if (path.startsWith("https://")) {
        URL(path).openConnection().apply {
            connectTimeout = 8_000
            readTimeout = 12_000
        }.getInputStream().buffered().use { it.readUpTo(5_000_001) }.also {
            require(it.size <= 5_000_000) { "封面文件过大" }
        }
    } else {
        Files.newInputStream(Path.of(path)).use { it.readUpTo(10_000_001) }
    }
    require(bytes.size <= 10_000_000) { "封面超过 10 MB" }
    ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { stream ->
        val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: return@runCatching decodeSkiaArtwork(bytes)
        try {
            reader.input = stream
            val sample = ((maxOf(reader.getWidth(0), reader.getHeight(0)) + 767) / 768).coerceAtLeast(1)
            val parameters = reader.defaultReadParam.apply { setSourceSubsampling(sample, sample, 0, 0) }
            val resized = reader.read(0, parameters)
            val encoded = ByteArrayOutputStream().also { ImageIO.write(resized, "png", it) }.toByteArray()
            Image.makeFromEncoded(encoded).toComposeImageBitmap()
        } finally { reader.dispose() }
    }
}.getOrNull()

// Java ImageIO has no built-in WebP reader; Skia can decode it directly at the target size.
private fun decodeSkiaArtwork(bytes: ByteArray): ImageBitmap = org.jetbrains.skia.Data.makeFromBytes(bytes).use { data ->
    org.jetbrains.skia.Codec.makeFromData(data).use { codec ->
        val scale = minOf(1.0, 768.0 / maxOf(codec.width, codec.height))
        org.jetbrains.skia.Bitmap().use { bitmap ->
            check(bitmap.allocPixels(org.jetbrains.skia.ImageInfo.makeN32Premul((codec.width * scale).toInt().coerceAtLeast(1), (codec.height * scale).toInt().coerceAtLeast(1))))
            codec.readPixels(bitmap)
            Image.makeFromBitmap(bitmap).toComposeImageBitmap()
        }
    }
}
