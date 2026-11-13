package com.resonance.player.ui

import com.resonance.player.design.lyricFocus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
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
    maximumHeight: Dp? = null,
) {
    val target = activeIndex + 1 // The first item is the instrumental intro label.
    val listState = rememberLyricListState(lyrics.trackId, lyrics.lines, target)
    val reducedMotion = LocalReducedMotion.current
    val fontScale = LocalDensity.current.fontScale
    val preferredHeight = (if (stage) 264.dp else 112.dp) * fontScale
    val viewportHeight = maximumHeight?.let { minOf(preferredHeight, it) } ?: preferredHeight
    FollowLyric(listState, target, true, reducedMotion, listOf(display, fontScale, stage, compact, lyrics.lines))
    BoxWithConstraints(modifier.fillMaxWidth().height(viewportHeight)) {
        val availableHeight = maxHeight
        LazyColumn(
            state = listState,
            userScrollEnabled = false,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = availableHeight / 2),
            modifier = Modifier.fillMaxWidth().height(availableHeight).clipToBounds().testTag("hero-lyrics-list"),
        ) {
            item(key = "intro") {
                PreviewLyricRow("前奏 · 等待人声", null, activeIndex < 0, stage, compact, -1, availableHeight)
            }
            itemsIndexed(lyrics.lines, key = { index, line -> "${line.timestampMs}-$index" }) { index, line ->
                PreviewLyricRow(
                    text = if (display == LyricDisplay.Translation) line.translation?.takeIf(String::isNotBlank) ?: line.text else line.text,
                    translation = line.translation?.takeIf { display == LyricDisplay.Bilingual && it.isNotBlank() },
                    active = index == activeIndex,
                    stage = stage,
                    compact = compact,
                    index = index,
                    availableHeight = availableHeight,
                )
            }
        }
    }
}

@Composable
private fun PreviewLyricRow(
    text: String,
    translation: String?,
    active: Boolean,
    stage: Boolean,
    compact: Boolean,
    index: Int,
    availableHeight: Dp,
) {
    val color by animateColorAsState(
        if (active) ResonanceColors.Notice else ResonanceColors.Muted,
        tween(motionDuration(ResonanceMotionTokens.TabDuration)), label = "previewLyricColor",
    )
    val scale by animateFloatAsState(
        if (active || LocalReducedMotion.current) 1f else .94f,
        tween(motionDuration(ResonanceMotionTokens.TabDuration), easing = ResonanceMotionTokens.ExitEasing),
        label = "previewLyricScale",
    )
    val verticalPadding = if (stage) 12.dp else 6.dp
    val minimumHeight = if (stage || compact) 88.dp else 50.dp
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = minOf(minimumHeight, availableHeight))
            .testTag("hero-lyric-$index").semantics { selected = active }
            // Scaling does not alter measured height or wrap text when focus changes.
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .lyricFocus(active)
            .padding(horizontal = 8.dp, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        val typography = MaterialTheme.typography
        val regular = when {
            stage -> typography.headlineLarge.copy(fontSize = 36.sp, lineHeight = 48.sp)
            compact -> typography.titleMedium
            else -> typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 30.sp)
        }.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        val secondary = (if (stage) typography.titleMedium else typography.bodySmall).copy(textAlign = TextAlign.Center)
        val smaller = regular.copy(fontSize = 16.sp, lineHeight = 21.sp)
        val smallerSecondary = secondary.copy(fontSize = 12.sp, lineHeight = 16.sp)
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val textWidth = Constraints(maxWidth = constraints.maxWidth)
        val textHeight = with(density) { (availableHeight - verticalPadding * 2).coerceAtLeast(0.dp).roundToPx() }
        val translationGap = 2.dp

        fun measure(mainStyle: TextStyle, translationStyle: TextStyle): Pair<TextLayoutResult, TextLayoutResult?>? {
            val main = measurer.measure(AnnotatedString(text), mainStyle, constraints = textWidth)
            val translated = translation?.let { measurer.measure(AnnotatedString(it), translationStyle, constraints = textWidth) }
            val translatedHeight = translated?.let { it.size.height + with(density) { translationGap.roundToPx() } } ?: 0
            return if (!main.hasVisualOverflow && translated?.hasVisualOverflow != true && main.size.height + translatedHeight <= textHeight) {
                main to translated
            } else null
        }

        val layout = measure(regular, secondary) ?: measure(smaller, smallerSecondary)
        if (layout != null) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(text, style = layout.first.layoutInput.style.copy(color = color), modifier = Modifier.fillMaxWidth())
                layout.second?.let {
                    val translatedColor = if (active) color else color.copy(alpha = color.alpha * .85f)
                    Box(Modifier.padding(top = translationGap)) {
                        BasicText(it.layoutInput.text, style = it.layoutInput.style.copy(color = translatedColor),
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        } else {
            Text("查看完整歌词", style = typography.labelMedium, color = color, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
