package com.resonance.player.design

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.resonance.player.model.Track
import com.resonance.player.platform.ArtworkMemoryCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/**
 * Album color is the stage's living light source.
 * When playing (foreground & motion enabled), two soft Apple-style liquid light orbs
 * gently orbit in the draw phase without recomposing the UI tree.
 */
@Composable
fun FrostedBackdrop(
    track: Track?,
    modifier: Modifier = Modifier,
    immersive: Boolean = false,
    isPlaying: Boolean = false,
) {
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
    val atmosphere = rememberAlbumAtmosphere(track?.artworkSeed ?: 0, track?.artworkPath, track?.sourceUri)
    val driftPhase = rememberPlaybackVisualPhase(
        playing = isPlaying && !LocalReducedMotion.current && LocalAppForeground.current,
        periodMillis = 14_000,
    )

    Box(modifier.clipToBounds().background(canvas)) {
        Crossfade(
            artwork,
            animationSpec = tween(motionDuration(380)),
            label = "frostedAlbum",
            modifier = Modifier.matchParentSize(),
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = remember {
                        androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                            androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0.92f) }
                        )
                    },
                    modifier = Modifier.fillMaxSize().scale(1.18f).blur(72.dp),
                )
            } else {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(atmosphere.base.copy(alpha = 0.90f), canvas))))
            }
        }
        // Living Apple Music-style drifting liquid light orbs (pure draw-phase animation).
        Box(
            Modifier.matchParentSize().drawBehind {
                val angle = driftPhase.value
                val orb1X = size.width * (0.26f + 0.14f * cos(angle))
                val orb1Y = size.height * (0.24f + 0.10f * sin(angle))
                val orb2X = size.width * (0.76f - 0.14f * sin(angle * 0.85f))
                val orb2Y = size.height * (0.58f + 0.12f * cos(angle * 0.85f))
                val orbAlpha1 = if (artwork != null) 0.24f else 0.56f
                val orbAlpha2 = if (artwork != null) 0.18f else 0.42f
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(atmosphere.glow.copy(alpha = orbAlpha1), atmosphere.base.copy(alpha = orbAlpha1 * 0.5f), Color.Transparent),
                        center = Offset(orb1X, orb1Y),
                        radius = size.maxDimension * 0.82f,
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(atmosphere.edge.copy(alpha = orbAlpha2), atmosphere.base.copy(alpha = orbAlpha2 * 0.4f), Color.Transparent),
                        center = Offset(orb2X, orb2Y),
                        radius = size.maxDimension * 0.76f,
                    )
                )
            }
        )
        // Sculpted contrast veil: preserves rich luminous color up top while grounding controls cleanly.
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to canvas.copy(alpha = 0.24f),
                        0.45f to canvas.copy(alpha = 0.38f),
                        0.82f to canvas.copy(alpha = 0.62f),
                        1.0f to canvas.copy(alpha = 0.74f),
                    )
                )
            )
        )
    }
}
