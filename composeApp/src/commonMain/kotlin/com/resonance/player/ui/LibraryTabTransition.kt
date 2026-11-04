package com.resonance.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.ResonanceMotionTokens

/** Retains both actual pages through an interrupted transition; never replaces the moving tree. */
@Composable
internal fun <T> ContinuousPageTransition(
    target: T,
    modifier: Modifier = Modifier,
    immediate: Boolean = false,
    direction: (T, T) -> Int,
    content: @Composable (T) -> Unit,
) {
    val still = immediate || LocalReducedMotion.current || LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val travel = with(LocalDensity.current) { ResonanceMotionTokens.PageTravel.roundToPx() }
    AnimatedContent(targetState = target, modifier = modifier.clipToBounds(), transitionSpec = {
        if (still) (EnterTransition.None togetherWith ExitTransition.None) using null
        else {
            val sign = direction(initialState, targetState)
            (slideInHorizontally(ResonanceMotionTokens.PageSlideSpec) { sign * travel } +
                fadeIn(ResonanceMotionTokens.PageFadeInSpec)) togetherWith
                (slideOutHorizontally(ResonanceMotionTokens.PageSlideSpec) { -sign * travel } +
                    fadeOut(ResonanceMotionTokens.PageFadeOutSpec)) using null
        }
    }, label = "continuousPage") { page ->
        val active = page == target
        Box(Modifier.fillMaxSize().focusProperties { canFocus = active }
            .then(if (active) Modifier else Modifier.clearAndSetSemantics { })
            .pointerInput(active) {
                if (!active) awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }) { content(page) }
    }
}

@Composable
internal fun LibraryTabTransition(index: Int, content: @Composable (Int) -> Unit) =
    ContinuousPageTransition(index, Modifier.fillMaxSize(), direction = { from, to -> if (to >= from) 1 else -1 }, content = content)
