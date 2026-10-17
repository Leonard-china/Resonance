package com.resonance.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.resonance.player.design.LocalReducedMotion
import kotlinx.coroutines.launch
import kotlin.math.abs

internal fun swipePageTarget(index: Int, count: Int, distance: Float, threshold: Float): Int? {
    if (abs(distance) < threshold || threshold <= 0f) return null
    return (index + if (distance < 0) 1 else -1).takeIf { it in 0 until count }
}

/**
 * 具有即时跟手视觉反馈与自然物理回弹的屏幕横向滑动手势：
 * 1. 拖动过程中内容实时随手指平移（graphicsLayer 高性能硬件变换）；
 * 2. 首末边界施加阻尼橡皮筋拉伸；
 * 3. 抬手达到阈值时顺畅触发目标页面切换；
 * 4. 未达阈值时采用柔和物理弹簧平滑回弹归位；
 * 5. 严格保留系统返回边距（24dp）及竖向滚动不抢占机制。
 */
internal fun Modifier.horizontalPageSwipe(
    index: Int,
    count: Int,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
): Modifier = composed {
    val currentIndex by rememberUpdatedState(index)
    val select by rememberUpdatedState(onSelect)
    val reducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val edge = with(LocalDensity.current) { 24.dp.toPx() }
    val minimum = with(LocalDensity.current) { 48.dp.toPx() }

    if (!enabled) this else this
        .graphicsLayer {
            translationX = if (reducedMotion) 0f else dragOffset.value
        }
        .pointerInput(count, edge, minimum) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.position.x < edge || down.position.x > size.width - edge) return@awaitEachGesture
                var distance = Offset.Zero
                var claimed = false
                val startIndex = currentIndex
                val threshold = maxOf(minimum, minOf(size.width * 0.18f, minimum * 2))
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } > 1) {
                        scope.launch { dragOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) }
                        break
                    }
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null) {
                        scope.launch { dragOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) }
                        break
                    }
                    if (change.isConsumed || currentIndex != startIndex) {
                        scope.launch { dragOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) }
                        break
                    }
                    distance += change.positionChange()
                    if (!change.pressed) {
                        if (claimed) {
                            change.consume()
                            val target = swipePageTarget(startIndex, count, distance.x, threshold)
                            if (target != null) {
                                scope.launch { dragOffset.snapTo(0f) }
                                select(target)
                            } else {
                                scope.launch { dragOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) }
                            }
                        }
                        break
                    }
                    if (!claimed) {
                        if (abs(distance.y) > viewConfiguration.touchSlop && abs(distance.y) >= abs(distance.x)) break
                        if (abs(distance.x) > viewConfiguration.touchSlop && abs(distance.x) > abs(distance.y) * 1.25f) {
                            // 位于边界且无法再同向翻页时交由外层处理
                            if (swipePageTarget(startIndex, count, distance.x, 1f) == null) break
                            claimed = true
                        }
                    }
                    if (claimed) {
                        change.consume()
                        if (!reducedMotion) {
                            val canAdvance = distance.x < 0 && startIndex < count - 1
                            val canRetreat = distance.x > 0 && startIndex > 0
                            val offset = if (canAdvance || canRetreat) distance.x else distance.x * 0.32f
                            scope.launch { dragOffset.snapTo(offset) }
                        }
                    }
                }
            }
        }
}
