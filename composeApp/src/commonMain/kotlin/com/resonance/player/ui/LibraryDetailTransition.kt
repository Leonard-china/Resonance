package com.resonance.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceMotionTokens

/** A detail page covers the list during push/pop; the ambient canvas returns after settling. */
@Composable
internal fun <T> LibraryDetailTransition(
    detail: T?,
    modifier: Modifier = Modifier,
    content: @Composable (T?) -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val transition = updateTransition(detail, label = "libraryDetail")
    val moving = transition.isRunning || transition.currentState != transition.targetState
    transition.AnimatedContent(
        modifier = modifier.clipToBounds(),
        transitionSpec = {
            val returning = targetState == null && initialState != null
            val transform = if (reducedMotion) {
                // Sequential fades keep both sets of text from being visible together.
                fadeIn(tween(120, delayMillis = 80)) togetherWith fadeOut(tween(80))
            } else if (returning) {
                slideInHorizontally(ResonanceMotionTokens.DetailSlideSpec) { -it / 3 } togetherWith
                    slideOutHorizontally(ResonanceMotionTokens.DetailSlideSpec) { it }
            } else {
                slideInHorizontally(ResonanceMotionTokens.DetailSlideSpec) { it } togetherWith
                    slideOutHorizontally(ResonanceMotionTokens.DetailSlideSpec) { -it / 3 }
            }
            transform.apply { targetContentZIndex = if (returning && !reducedMotion) -1f else 1f } using null
        },
    ) { page ->
        Box(Modifier.fillMaxSize().background(if (moving) ResonanceColors.Canvas else Color.Transparent)) {
            content(page)
        }
    }
}
