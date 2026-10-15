package com.resonance.player.platform

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.net.URL
import com.resonance.player.io.readUpTo

actual fun decodeArtwork(path: String): ImageBitmap? = runCatching {
    val bitmap = if (path.startsWith("content://")) {
        runCatching { AndroidArtworkResolver.application.contentResolver.openInputStream(Uri.parse(path))?.use { decodeBounded(it.readUpTo(10_000_001)) } }.getOrNull()
            ?: AndroidArtworkResolver.audioArtwork[path]
                ?.let(Uri::parse)
                ?.let { audioUri -> embeddedArtwork(audioUri) }
    } else if (path.startsWith("https://")) {
        val connection = URL(path).openConnection().apply {
            connectTimeout = 8_000
            readTimeout = 12_000
        }
        connection.getInputStream().buffered().use { decodeBounded(it.readUpTo(10_000_001)) }
    } else {
        java.io.FileInputStream(path).use { decodeBounded(it.readUpTo(10_000_001)) }
    }
    bitmap?.asImageBitmap()
}.getOrNull()

private fun embeddedArtwork(audioUri: Uri) = runCatching {
    android.media.MediaMetadataRetriever().let { retriever ->
        try {
            retriever.setDataSource(AndroidArtworkResolver.application, audioUri)
            retriever.embeddedPicture?.let(::decodeBounded)
        } finally {
            retriever.release()
        }
    }
}.getOrNull()

private fun decodeBounded(bytes: ByteArray): android.graphics.Bitmap? {
    require(bytes.size <= 10_000_000) { "封面超过 10 MB" }
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    options.inJustDecodeBounds = false
    options.inSampleSize = 1
    while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 768) options.inSampleSize *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}
