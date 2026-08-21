package com.resonance.player.platform

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.net.URL

actual fun decodeArtwork(path: String): ImageBitmap? = runCatching {
    val bitmap = if (path.startsWith("content://")) {
        AndroidArtworkResolver.application.contentResolver.openInputStream(Uri.parse(path)).use(BitmapFactory::decodeStream)
            ?: AndroidArtworkResolver.audioArtwork[path]
                ?.let(Uri::parse)
                ?.let { audioUri -> embeddedArtwork(audioUri) }
    } else if (path.startsWith("https://")) {
        val connection = URL(path).openConnection().apply {
            connectTimeout = 8_000
            readTimeout = 12_000
        }
        connection.getInputStream().buffered().use(BitmapFactory::decodeStream)
    } else {
        BitmapFactory.decodeFile(path)
    }
    bitmap?.asImageBitmap()
}.getOrNull()

private fun embeddedArtwork(audioUri: Uri) = runCatching {
    android.media.MediaMetadataRetriever().let { retriever ->
        try {
            retriever.setDataSource(AndroidArtworkResolver.application, audioUri)
            retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } finally {
            retriever.release()
        }
    }
}.getOrNull()
