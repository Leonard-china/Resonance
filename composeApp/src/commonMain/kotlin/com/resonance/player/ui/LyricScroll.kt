package com.resonance.player.ui

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.resonance.player.design.ResonanceMotionTokens
import com.resonance.player.model.LyricLine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

/** Translation arrival changes row measurements but must not recreate the scrolling list. */
@Composable
internal fun rememberLyricListState(trackId: String, lines: List<LyricLine>, initialIndex: Int): LazyListState {
    val timeline = remember(lines) { lines.map { it.timestampMs to it.text } }
    return remember(trackId, timeline) { LazyListState(firstVisibleItemIndex = initialIndex.coerceAtLeast(0)) }
}

/** Item offsets and viewport offsets share a coordinate system, including negative top padding. */
internal fun lyricCenterOffset(itemSize: Int, viewportStart: Int, viewportEnd: Int): Int {
    val height = viewportEnd - viewportStart
    // Keep the beginning of a line readable when large text makes it taller than the viewport.
    return viewportStart + (height - itemSize).coerceAtLeast(0) / 2
}

internal suspend fun LazyListState.centerLyric(index: Int, animate: Boolean) {
    if (index !in 0 until layoutInfo.totalItemsCount) return
    if (layoutInfo.visibleItemsInfo.none { it.index == index }) {
        val estimatedSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
        val target = lyricCenterOffset(estimatedSize, layoutInfo.viewportStartOffset, layoutInfo.viewportEndOffset)
        if (animate) animateScrollToItem(index, -target) else scrollToItem(index, -target)
    }
    // Re-read the measured row after bringing it into view: bilingual/wrapped lines vary in height.
    val info = layoutInfo
    val row = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val delta = row.offset - lyricCenterOffset(row.size, info.viewportStartOffset, info.viewportEndOffset)
    if (abs(delta) <= 1) return
    if (animate) {
        animateScrollBy(delta.toFloat(), ResonanceMotionTokens.PlayerSpec)
    } else {
        scrollBy(delta.toFloat())
    }
}

/** Only a new target or changed viewport retargets motion, never the playback position ticker. */
@Composable
internal fun FollowLyric(
    listState: LazyListState,
    targetIndex: Int,
    enabled: Boolean,
    reducedMotion: Boolean,
    layoutKey: Any?,
    request: Int = 0,
) {
    var positioned by remember(listState) { mutableStateOf(false) }
    LaunchedEffect(listState, targetIndex, enabled, reducedMotion, layoutKey, request) {
        if (!enabled || targetIndex < 0) return@LaunchedEffect
        // Font/display changes must be measured before reading row heights.
        withFrameNanos { }
        snapshotFlow {
            val info = listState.layoutInfo
            if (targetIndex >= info.totalItemsCount || info.viewportSize.height == 0) null
            else listOf(info.viewportStartOffset, info.viewportEndOffset, info.viewportSize.width,
                info.visibleItemsInfo.firstOrNull { it.index == targetIndex }?.size ?: -1)
        }.filterNotNull().distinctUntilChanged().collectLatest {
            val animate = positioned && !reducedMotion
            positioned = true
            listState.centerLyric(targetIndex, animate = animate)
        }
    }
}
