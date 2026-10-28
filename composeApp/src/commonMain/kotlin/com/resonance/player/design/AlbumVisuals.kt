package com.resonance.player.design

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonance.player.platform.ArtworkMemoryCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class AlbumAtmosphere(val base: Color, val glow: Color, val edge: Color)

private val sleevePalettes = listOf(
    AlbumAtmosphere(Color(0xFF173B40), Color(0xFF70D5C6), Color(0xFFE4C68F)),
    AlbumAtmosphere(Color(0xFF3D2A35), Color(0xFFDC9E78), Color(0xFFE0B8C5)),
    AlbumAtmosphere(Color(0xFF232D4F), Color(0xFF95A7E1), Color(0xFFE7B0A4)),
    AlbumAtmosphere(Color(0xFF193B39), Color(0xFF88BCAF), Color(0xFFDAD3A8)),
)

fun albumAtmosphere(seed: Int): AlbumAtmosphere = sleevePalettes[((seed % sleevePalettes.size) + sleevePalettes.size) % sleevePalettes.size]
val LocalAlbumAtmosphere = staticCompositionLocalOf { albumAtmosphere(0) }

/** A bounded 8x8 sample, read once off the UI thread; white/black borders do not dominate. */
internal fun atmosphereFromBitmap(bitmap: ImageBitmap, fallback: AlbumAtmosphere): AlbumAtmosphere {
    val pixels = bitmap.toPixelMap()
    fun sample(right: Boolean): Color? {
        var r = 0f; var g = 0f; var b = 0f; var total = 0f
        for (row in 1..8) for (column in 1..8) {
            if ((column > 4) != right) continue
            val color = pixels[(bitmap.width * column / 9).coerceIn(0, bitmap.width - 1),
                (bitmap.height * row / 9).coerceIn(0, bitmap.height - 1)]
            val high = max(color.red, max(color.green, color.blue))
            val low = min(color.red, min(color.green, color.blue))
            if (color.alpha < .75f || high < .08f || high - low < .04f) continue
            val saturation = (high - low) / high
            val weight = saturation * (.3f + high * (1f - high) * 2f)
            r += color.red * weight; g += color.green * weight; b += color.blue * weight; total += weight
        }
        if (total == 0f) return null
        val peak = max(r, max(g, b))
        return Color((r / peak * .92f).coerceIn(0f, 1f), (g / peak * .92f).coerceIn(0f, 1f), (b / peak * .92f).coerceIn(0f, 1f))
    }
    val left = sample(false)
    val right = sample(true)
    val glow = left ?: right ?: return fallback
    return AlbumAtmosphere(lerp(Color(0xFF0B0E14), glow, .15f), glow, right ?: glow)
}

@Composable
fun rememberAlbumAtmosphere(seed: Int, artworkPath: String?, audioSource: String?): AlbumAtmosphere {
    val revision by ArtworkMemoryCache.revision.collectAsState()
    val fallback = remember(seed) { albumAtmosphere(seed) }
    return produceState(fallback, seed, artworkPath, audioSource, revision) {
        value = fallback
        if (artworkPath.isNullOrBlank() && audioSource.isNullOrBlank()) return@produceState
        try {
            val bitmap = withContext(Dispatchers.IO) {
                artworkPath?.takeIf(String::isNotBlank)?.let { ArtworkMemoryCache.load(it) }
                    ?: audioSource?.takeIf(String::isNotBlank)?.let { ArtworkMemoryCache.load("embedded:$it") }
            }
            if (bitmap != null) value = withContext(Dispatchers.Default) { atmosphereFromBitmap(bitmap, fallback) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            value = fallback
        }
    }.value
}

/** Original vector sleeve art for missing covers, generated entirely with native drawing. */
@Composable
fun GenerativeAlbumSleeve(seed: Int, modifier: Modifier = Modifier, corner: Dp = 12.dp) {
    val colors = remember(seed) { albumAtmosphere(seed) }
    Box(modifier.clip(RoundedCornerShape(corner)).drawWithCache {
        val unit = size.minDimension.coerceAtLeast(1f)
        val background = Brush.linearGradient(listOf(lerp(colors.base, Color.Black, .22f), colors.base,
            lerp(colors.base, colors.glow, .2f)), Offset.Zero, Offset(size.width, size.height))
        val sunCenter = Offset(size.width * .70f, size.height * .30f)
        val haze = Brush.radialGradient(listOf(colors.edge.copy(alpha = .24f), Color.Transparent), sunCenter, unit * .56f)
        val sun = Brush.linearGradient(listOf(colors.edge, lerp(colors.edge, colors.glow, .3f)),
            sunCenter - Offset(0f, unit * .18f), sunCenter + Offset(0f, unit * .18f))
        val phase = ((seed % 11) / 11f) * 1.5f
        val ridges = List(18) { row ->
            Path().apply {
                for (point in 0..48) {
                    val x = point / 48f
                    val y = .48f + row * .035f + sin(x * 5.5f + phase + row * .08f) * (.12f + row * .002f) + x * .08f
                    if (point == 0) moveTo(x * size.width, y * size.height) else lineTo(x * size.width, y * size.height)
                }
            }
        }
        val ridgeInk = Brush.linearGradient(listOf(colors.glow.copy(alpha = .65f), colors.edge.copy(alpha = .36f)),
            Offset(0f, size.height * .5f), Offset(size.width, size.height))
        val floor = Brush.verticalGradient(listOf(Color.Transparent, colors.base.copy(alpha = .75f)), startY = size.height * .7f)
        onDrawBehind {
            drawRect(background); drawRect(haze)
            drawCircle(sun, unit * .16f, sunCenter)
            ridges.forEachIndexed { index, path ->
                drawPath(path, ridgeInk, style = Stroke(width = unit * (if (index % 4 == 0) .005f else .002f)))
            }
            drawRect(floor)
        }
    })
}
