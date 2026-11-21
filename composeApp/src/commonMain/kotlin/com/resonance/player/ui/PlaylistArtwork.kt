package com.resonance.player.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.model.Track

/**
 * Custom vector Resonance emblem: fresh jade-to-sky translucent gradient squircle
 * with concentric acoustic wave arcs radiating from a luminous core.
 */
@Composable
internal fun ResonanceBrandMark(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val start = if (ResonanceColors.isDark) Color(0xFF22B89A) else Color(0xFF0D947B)
    val end = if (ResonanceColors.isDark) Color(0xFF3E95E8) else Color(0xFF1F7AC8)
    val ink = Color.White
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(start, end))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w * 0.37f
            val cy = h * 0.52f
            val stroke = w * 0.075f
            drawCircle(color = ink, radius = w * 0.115f, center = Offset(cx, cy))
            val r1 = w * 0.25f
            drawArc(
                color = ink.copy(alpha = 0.94f),
                startAngle = -52f,
                sweepAngle = 104f,
                useCenter = false,
                topLeft = Offset(cx - r1, cy - r1),
                size = Size(r1 * 2f, r1 * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val r2 = w * 0.41f
            drawArc(
                color = ink.copy(alpha = 0.66f),
                startAngle = -46f,
                sweepAngle = 92f,
                useCenter = false,
                topLeft = Offset(cx - r2, cy - r2),
                size = Size(r2 * 2f, r2 * 2f),
                style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Curated 2x2 contact-sheet mosaic for playlists with multiple distinct tracks,
 * falling back cleanly to single artwork or bespoke generative sleeves.
 */
@Composable
internal fun PlaylistMosaicArtwork(
    tracks: List<Track>,
    fallbackSeed: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    gap: Dp = 1.dp,
) {
    val mosaicTracks = remember(tracks) {
        val withArt = tracks.filter { !it.artworkPath.isNullOrBlank() }.distinctBy { it.artworkPath }
        when {
            withArt.size >= 4 -> withArt.take(4)
            tracks.size >= 4 -> {
                val distinct = tracks.distinctBy { it.artworkPath?.takeIf(String::isNotBlank) ?: "seed:${it.artworkSeed}" }
                if (distinct.size >= 4) distinct.take(4) else emptyList()
            }
            else -> emptyList()
        }
    }

    if (mosaicTracks.size == 4) {
        val shape = RoundedCornerShape(cornerRadius)
        Column(
            modifier = modifier
                .clip(shape)
                .background(Color(0xFF142022)),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                AlbumArtwork(mosaicTracks[0].artworkSeed, Modifier.weight(1f).fillMaxHeight(), 0.dp, mosaicTracks[0].artworkPath, mosaicTracks[0].sourceUri)
                AlbumArtwork(mosaicTracks[1].artworkSeed, Modifier.weight(1f).fillMaxHeight(), 0.dp, mosaicTracks[1].artworkPath, mosaicTracks[1].sourceUri)
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                AlbumArtwork(mosaicTracks[2].artworkSeed, Modifier.weight(1f).fillMaxHeight(), 0.dp, mosaicTracks[2].artworkPath, mosaicTracks[2].sourceUri)
                AlbumArtwork(mosaicTracks[3].artworkSeed, Modifier.weight(1f).fillMaxHeight(), 0.dp, mosaicTracks[3].artworkPath, mosaicTracks[3].sourceUri)
            }
        }
    } else {
        val lead = tracks.firstOrNull { !it.artworkPath.isNullOrBlank() } ?: tracks.firstOrNull()
        AlbumArtwork(
            seed = lead?.artworkSeed ?: fallbackSeed,
            modifier = modifier,
            cornerRadius = cornerRadius,
            artworkPath = lead?.artworkPath,
            audioSource = lead?.sourceUri,
        )
    }
}
