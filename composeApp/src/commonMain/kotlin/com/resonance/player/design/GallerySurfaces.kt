package com.resonance.player.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** Natural tactile tokens: warm archival surfaces, crisp corners, soft ambient shadows. */
object GalleryTokens {
    val ArtworkCorner = 14.dp
    val PlinthCorner = 16.dp
    val ShelfHeight = 0.dp
    val Face: Color @Composable @ReadOnlyComposable get() = ResonanceColors.Raised
    val Edge: Color @Composable @ReadOnlyComposable get() = ResonanceColors.Divider
    val Highlight: Color @Composable @ReadOnlyComposable get() =
        if (ResonanceColors.isDark) Color(0xFF34343B) else Color.White
    val Cast: Color @Composable @ReadOnlyComposable get() =
        if (ResonanceColors.isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF1C1814).copy(alpha = 0.11f)
}

/** Static shadows use Compose's cached shadow renderer; no per-frame blur animation. */
@Composable
fun Modifier.galleryShadow(shape: Shape, depth: Dp = 8.dp): Modifier =
    dropShadow(shape, Shadow(radius = depth * 0.75f, color = GalleryTokens.Cast,
        offset = DpOffset(0.dp, depth * 0.40f)))
        .dropShadow(shape, Shadow(radius = 1.dp, color = GalleryTokens.Cast.copy(alpha = 0.14f),
            offset = DpOffset(0.dp, 1.dp)))

/** Elevated tactile card container with clean rounded corners and subtle border. */
@Composable
fun Modifier.galleryPlinth(corner: Dp = GalleryTokens.PlinthCorner, depth: Dp = 2.dp): Modifier {
    val face = GalleryTokens.Face
    val edge = GalleryTokens.Edge
    val shape = RoundedCornerShape(corner)
    return this
        .galleryShadow(shape, depth)
        .clip(shape)
        .background(face)
        .border(1.dp, edge, shape)
}

/** Mounted artwork card with crisp corners, subtle rim, and gentle depth. */
@Composable
fun GalleryArtworkMount(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(GalleryTokens.ArtworkCorner)
    Box(
        modifier = modifier
            .padding(bottom = 4.dp)
            .aspectRatio(1f)
            .galleryShadow(shape, depth = 7.dp)
            .clip(shape)
            .background(ResonanceColors.Soft)
            .border(
                1.dp,
                if (ResonanceColors.isDark) Color.White.copy(alpha = 0.10f) else Color(0xFF1C1814).copy(alpha = 0.08f),
                shape,
            ),
        content = content,
    )
}

/** A small chrome control over a quiet canvas; crisp hairline outline and 48dp touch target. */
@Composable
fun Modifier.glassControlSurface(): Modifier = this
    .size(48.dp).padding(4.dp)
    .clip(CircleShape)
    .background(ResonanceColors.Raised)
    .border(1.dp, ResonanceColors.Divider, CircleShape)
