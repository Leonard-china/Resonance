package com.resonance.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceMotionTokens
import com.resonance.player.design.motionDuration
import com.resonance.player.model.LyricDisplay
import com.resonance.player.model.Lyrics

/** Keep each line alive as it moves through focus; never crossfade a replacement three-line group. */
@Composable
internal fun SyncedLyricsPreview(
    lyrics: Lyrics,
    activeIndex: Int,
    display: LyricDisplay,
    stage: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    maximumHeight: androidx.compose.ui.unit.Dp? = null,
) {
    val target = activeIndex + 1 // The first item is the instrumental intro label.
    val listState = rememberLyricListState(lyrics.trackId, lyrics.lines, target)
    val reducedMotion = LocalReducedMotion.current
    val fontScale = LocalDensity.current.fontScale
    val preferredHeight = (if (stage) 280.dp else if (compact) 76.dp else 96.dp) * fontScale
    val viewportHeight = maximumHeight?.let { minOf(preferredHeight, it) } ?: preferredHeight
    FollowLyric(listState, target, true, reducedMotion, listOf(display, fontScale, stage, compact, lyrics.lines))

    val edgeFade = if (stage) {
        Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.18f to Color.Black,
            0.82f to Color.Black,
            1.0f to Color.Transparent,
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.30f to Color.Black,
            0.70f to Color.Black,
            1.0f to Color.Transparent,
        )
    }

    LazyColumn(
        state = listState,
        userScrollEnabled = false,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = viewportHeight / 2),
        modifier = modifier
            .fillMaxWidth()
            .height(viewportHeight)
            .clipToBounds()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(brush = edgeFade, blendMode = BlendMode.DstIn)
            }
            .testTag("hero-lyrics-list"),
    ) {
        item(key = "intro") {
            PreviewLyricRow("前奏 · 等待人声", null, activeIndex < 0, stage, compact, -1)
        }
        itemsIndexed(lyrics.lines, key = { index, line -> "${line.timestampMs}-$index" }) { index, line ->
            PreviewLyricRow(
                text = if (display == LyricDisplay.Translation) line.translation?.takeIf(String::isNotBlank) ?: line.text else line.text,
                translation = line.translation?.takeIf { display == LyricDisplay.Bilingual && it.isNotBlank() },
                active = index == activeIndex,
                stage = stage,
                compact = compact,
                index = index,
            )
        }
    }
}

@Composable
private fun PreviewLyricRow(text: String, translation: String?, active: Boolean, stage: Boolean, compact: Boolean, index: Int) {
    val activeColor = ResonanceColors.TextPrimary
    val contextColor = ResonanceColors.TextPrimary.copy(alpha = 0.42f)
    val color by animateColorAsState(
        if (active) activeColor else contextColor,
        tween(motionDuration(ResonanceMotionTokens.TabDuration)), label = "previewLyricColor",
    )
    val scale by animateFloatAsState(
        if (active || LocalReducedMotion.current) 1f else .93f,
        tween(motionDuration(ResonanceMotionTokens.TabDuration), easing = ResonanceMotionTokens.ExitEasing),
        label = "previewLyricScale",
    )
    Column(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = if (stage) 88.dp else if (compact) 72.dp else 48.dp)
            .testTag("hero-lyric-$index").semantics { selected = active }
            // Scaling does not alter measured height or wrap text when focus changes.
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .padding(horizontal = if (stage) 16.dp else 8.dp, vertical = if (stage) 14.dp else 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text,
            style = if (stage) MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 42.sp, letterSpacing = (-0.4).sp)
                else if (compact) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = if (stage) 4 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (translation != null) Text(
            translation,
            style = if (stage) MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp) else MaterialTheme.typography.bodySmall,
            color = if (active) color.copy(alpha = 0.82f) else color.copy(alpha = 0.70f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
