package com.resonance.player.design

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.resonance.player.model.Track
import com.resonance.player.platform.ArtworkMemoryCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Album color is the material's light source, not an animated decorative overlay. */
@Composable
fun FrostedBackdrop(track: Track?, modifier: Modifier = Modifier, immersive: Boolean = false) {
    // The library uses the approved quiet canvas; only the player samples album imagery.
    // This keeps the navigation surface independent of large blurred bitmap layers.
    if (!immersive) {
        Box(modifier.background(ResonanceColors.Canvas))
        return
    }
    val revision by ArtworkMemoryCache.revision.collectAsState()
    val artwork by produceState<ImageBitmap?>(null, track?.artworkPath, track?.sourceUri, revision) {
        value = try {
            withContext(Dispatchers.IO) {
                track?.artworkPath?.takeIf(String::isNotBlank)?.let { ArtworkMemoryCache.load(it) }
                    ?: track?.sourceUri?.takeIf(String::isNotBlank)?.let { ArtworkMemoryCache.load("embedded:$it") }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            null
        }
    }
    val canvas = ResonanceColors.Canvas
    val dark = ResonanceColors.isDark
    val atmosphere = rememberAlbumAtmosphere(track?.artworkSeed ?: 0, track?.artworkPath, track?.sourceUri)
    Box(modifier.clipToBounds().background(canvas)) {
        Crossfade(artwork, animationSpec = tween(motionDuration(300)), label = "frostedAlbum",
            modifier = Modifier.matchParentSize()) { bitmap ->
            if (bitmap != null) {
                Image(bitmap, null, contentScale = ContentScale.Crop,
                    colorFilter = remember { androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                        androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(.65f) }) },
                    modifier = Modifier.fillMaxSize().scale(1.14f).blur(if (immersive) 64.dp else 56.dp))
            } else {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(atmosphere.edge.copy(alpha = .70f), atmosphere.glow.copy(alpha = .55f), canvas))))
            }
        }
        // The text always reads over a controlled luminance range, including pre-Android-12 fallback.
        val tint = if (immersive) { if (dark) .38f else .58f } else { if (dark) .48f else .52f }
        Box(Modifier.matchParentSize().background(canvas.copy(alpha = tint)))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
            canvas.copy(alpha = .08f), canvas.copy(alpha = if (immersive) .26f else .12f)))))
    }
}
