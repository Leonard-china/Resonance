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
    AlbumAtmosphere(Color(0xFF133A3C), Color(0xFF4CD3B8), Color(0xFFBCEEE2)), // Seafoam Lagoon & Dew
    AlbumAtmosphere(Color(0xFF193552), Color(0xFF63B3F2), Color(0xFFD1EBFF)), // Glacier Sky & Ice
    AlbumAtmosphere(Color(0xFF213C30), Color(0xFF7BC89C), Color(0xFFD9F0D8)), // Kyoto Bamboo & Mist
    AlbumAtmosphere(Color(0xFF21334A), Color(0xFF7FA6E8), Color(0xFFDAE8FC)), // Coastal Dusk & Cloud
    AlbumAtmosphere(Color(0xFF1A3A47), Color(0xFF56C2CF), Color(0xFFC8EFF2)), // Aqua Ceramic & Foam
    AlbumAtmosphere(Color(0xFF263B3D), Color(0xFF88C8B8), Color(0xFFE2EFE4)), // Morning Sage & Linen
    AlbumAtmosphere(Color(0xFF242D4B), Color(0xFF8EA1F0), Color(0xFFD9E0FA)), // Twilight Indigo & Silver
    AlbumAtmosphere(Color(0xFF1E3236), Color(0xFF62BEB0), Color(0xFFDCEEE9)), // Deep Mint Vinyl
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
    return AlbumAtmosphere(lerp(Color(0xFF121114), glow, .18f), glow, right ?: glow)
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

/** Curated archival vinyl sleeve generator with four deterministic geometric acoustic motifs. */
@Composable
fun GenerativeAlbumSleeve(seed: Int, modifier: Modifier = Modifier, corner: Dp = 12.dp) {
    val colors = remember(seed) { albumAtmosphere(seed) }
    val motif = remember(seed) { ((seed % 4) + 4) % 4 }
    Box(modifier.clip(RoundedCornerShape(corner)).drawWithCache {
        val w = size.width
        val h = size.height
        val unit = size.minDimension.coerceAtLeast(1f)
        val deep = lerp(colors.base, Color.Black, .30f)
        val lifted = lerp(colors.base, colors.glow, .22f)
        val background = Brush.linearGradient(listOf(deep, colors.base, lifted), Offset.Zero, Offset(w, h))
        val phase = (((seed % 11) + 11) % 11 / 11f) * 1.8f

        val ridges = if (motif == 1) List(14) { row ->
            Path().apply {
                for (point in 0..42) {
                    val x = point / 42f
                    val y = .50f + row * .038f + sin(x * 5.2f + phase + row * .12f) * (.095f + row * .002f) + x * .05f
                    if (point == 0) moveTo(x * w, y * h) else lineTo(x * w, y * h)
                }
            }
        } else emptyList()

        val glowCenter = when (motif) {
            0 -> Offset(w * .36f, h * .64f)
            1 -> Offset(w * .72f, h * .28f)
            2 -> Offset(w * .78f, h * .76f)
            else -> Offset(w * .50f, h * .50f)
        }
        val bloom = Brush.radialGradient(
            listOf(colors.edge.copy(alpha = .32f), colors.glow.copy(alpha = .12f), Color.Transparent),
            glowCenter,
            unit * .75f,
        )
        val accentOrb = Brush.linearGradient(
            listOf(colors.edge, lerp(colors.edge, colors.glow, .45f)),
            Offset(0f, 0f),
            Offset(w, h),
        )
        val ridgeInk = Brush.linearGradient(
            listOf(colors.glow.copy(alpha = .62f), colors.edge.copy(alpha = .32f)),
            Offset(0f, h * .5f),
            Offset(w, h),
        )
        val floor = Brush.verticalGradient(
            listOf(Color.Transparent, deep.copy(alpha = .68f)),
            startY = h * .68f,
        )

        onDrawBehind {
            drawRect(background)
            drawRect(bloom)
            when (motif) {
                0 -> {
                    // Concentric acoustic resonance rings
                    val center = Offset(w * .62f, h * .44f)
                    for (ring in 1..6) {
                        val radius = unit * (0.14f + ring * 0.115f)
                        drawCircle(
                            color = colors.edge.copy(alpha = (0.42f - ring * 0.05f).coerceAtLeast(0.08f)),
                            radius = radius,
                            center = center,
                            style = Stroke(width = unit * if (ring == 2 || ring == 4) .010f else .004f),
                        )
                    }
                    drawCircle(accentOrb, unit * .115f, center)
                    drawLine(
                        color = colors.edge.copy(alpha = .24f),
                        start = Offset(w * .10f, h * .82f),
                        end = Offset(w * .44f, h * .82f),
                        strokeWidth = unit * .008f,
                    )
                }
                1 -> {
                    // Harmonic horizon waves
                    drawCircle(accentOrb, unit * .14f, Offset(w * .72f, h * .28f))
                    ridges.forEachIndexed { index, path ->
                        drawPath(path, ridgeInk, style = Stroke(width = unit * (if (index % 4 == 0) .007f else .003f)))
                    }
                }
                2 -> {
                    // Bauhaus acoustic arches
                    drawCircle(colors.glow.copy(alpha = .26f), unit * .44f, Offset(w * .20f, h * .88f))
                    drawCircle(accentOrb, unit * .28f, Offset(w * .76f, h * .30f))
                    drawCircle(
                        color = colors.edge.copy(alpha = .45f),
                        radius = unit * .34f,
                        center = Offset(w * .76f, h * .30f),
                        style = Stroke(width = unit * .006f),
                    )
                    drawLine(
                        color = colors.edge.copy(alpha = .30f),
                        start = Offset(w * .12f, h * .16f),
                        end = Offset(w * .12f, h * .84f),
                        strokeWidth = unit * .006f,
                    )
                }
                else -> {
                    // Minimalist archival vinyl record
                    val center = Offset(w * .50f, h * .50f)
                    drawCircle(deep.copy(alpha = .72f), unit * .38f, center)
                    for (groove in 1..5) {
                        drawCircle(
                            color = colors.glow.copy(alpha = 0.14f + groove * 0.03f),
                            radius = unit * (0.17f + groove * 0.042f),
                            center = center,
                            style = Stroke(width = unit * .0035f),
                        )
                    }
                    drawCircle(accentOrb, unit * .135f, center)
                    drawCircle(deep, unit * .028f, center)
                }
            }
            drawRect(floor)
        }
    })
}
