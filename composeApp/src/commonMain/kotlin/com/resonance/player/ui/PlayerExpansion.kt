package com.resonance.player.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ImmersivePlayerColors
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.ResonanceMotionTokens
import com.resonance.player.model.Track
import kotlinx.coroutines.launch

/** Geometry stays in the untransformed layout; animation only changes rendering layers. */
@Stable
internal class PlayerExpansionState {
    val animation = Animatable(0f)
    private var dragFraction by mutableStateOf<Float?>(null)
    val fraction: Float get() = (dragFraction ?: animation.value).coerceIn(0f, 1f)
    var settled by mutableStateOf(false)
    var sourceBounds by mutableStateOf<Rect?>(null)
    var sourceArtwork by mutableStateOf<Rect?>(null)
    var targetArtwork by mutableStateOf<Rect?>(null)
    val canFlyArtwork: Boolean get() = sourceArtwork?.let { it.width > 0f && it.height > 0f } == true &&
        targetArtwork?.let { it.width > 0f && it.height > 0f } == true
    var backgroundCoordinates: LayoutCoordinates? = null
    var playerCoordinates: LayoutCoordinates? = null
    var sourceCoordinates: LayoutCoordinates? = null
    var sourceArtworkCoordinates: LayoutCoordinates? = null
    var targetArtworkCoordinates: LayoutCoordinates? = null
    var travel = 1f
    var releaseVelocity: Float? = null
    private var carriedVelocity = 0f

    fun updateSourceBounds() {
        val parent = backgroundCoordinates?.takeIf { it.isAttached } ?: return
        sourceCoordinates?.takeIf { it.isAttached }?.let { sourceBounds = parent.localBoundingBoxOf(it, false) }
        sourceArtworkCoordinates?.takeIf { it.isAttached }?.let { sourceArtwork = parent.localBoundingBoxOf(it, false) }
    }

    fun updateTargetBounds() {
        val parent = playerCoordinates?.takeIf { it.isAttached } ?: return
        targetArtworkCoordinates?.takeIf { it.isAttached }?.let { targetArtwork = parent.localBoundingBoxOf(it, false) }
    }

    fun beginDrag() {
        dragFraction = fraction
        settled = false
    }

    fun dragBy(delta: Float) {
        dragFraction = ((dragFraction ?: animation.value) - delta / travel).coerceIn(0f, 1f)
    }

    suspend fun endDrag() {
        if (dragFraction != null) {
            animation.snapTo(fraction)
            dragFraction = null
        }
    }

    suspend fun settle(expanded: Boolean, reducedMotion: Boolean, velocity: Float? = releaseVelocity, immediate: Boolean = false) {
        releaseVelocity = null
        settled = false
        // An external close/navigation can also interrupt an active drag.
        endDrag()
        val target = if (expanded) 1f else 0f
        if (immediate) animation.snapTo(target) else animation.animateTo(target,
            if (reducedMotion) tween(ResonanceMotionTokens.ReducedDuration) else ResonanceMotionTokens.PlayerSpec,
            initialVelocity = if (reducedMotion) 0f else velocity ?: carriedVelocity) {
            // LaunchedEffect cancellation clears Animatable.velocity. Keep the last visible velocity
            // so a new presentation target can reverse the same spring without a velocity cut.
            carriedVelocity = this.velocity
        }
        carriedVelocity = 0f
        settled = expanded
    }
}

private val LocalPlayerExpansion = staticCompositionLocalOf<PlayerExpansionState?> { null }
private val LocalPlayerDismissGesture = staticCompositionLocalOf<Modifier> { Modifier }

@Composable
internal fun playerDismissGestureModifier(): Modifier = LocalPlayerDismissGesture.current

@Composable
internal fun Modifier.playerExpansionSource(artwork: Boolean = false): Modifier {
    val state = LocalPlayerExpansion.current ?: return this
    return this.onGloballyPositioned { coordinates ->
        if (artwork) state.sourceArtworkCoordinates = coordinates else state.sourceCoordinates = coordinates
        state.updateSourceBounds()
    }.graphicsLayer {
        alpha = (1f - state.fraction * 6f).coerceIn(0f, 1f)
    }
}

@Composable
internal fun Modifier.playerExpansionArtwork(): Modifier {
    val state = LocalPlayerExpansion.current ?: return this
    val reducedMotion = LocalReducedMotion.current || LocalInputModeManager.current.inputMode == InputMode.Keyboard
    DisposableEffect(state) { onDispose { state.targetArtwork = null; state.targetArtworkCoordinates = null } }
    return this.onGloballyPositioned { coordinates ->
        state.targetArtworkCoordinates = coordinates
        state.updateTargetBounds()
    }.graphicsLayer {
        val flying = !reducedMotion && state.fraction > 0f && state.fraction < 1f && state.canFlyArtwork
        alpha = if (flying && !state.settled) 0f else 1f
    }
}

/** Flick projection is distance-aware and honours a last-moment reversal. */
internal fun shouldDismissPlayer(fraction: Float, velocityPx: Float, travelPx: Float): Boolean {
    val decelerationRate = .995f
    val projectedDistance = velocityPx / 1000f * decelerationRate / (1f - decelerationRate)
    return fraction - projectedDistance / travelPx.coerceAtLeast(1f) < .72f
}

private class PlayerClip(private val width: Float, private val height: Float, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(0f, 0f, width, height, CornerRadius(radius)))
}

/** A native mini-player to full-player transition, with one moving cover and a fixed-size player. */
@Composable
internal fun PlayerExpansionLayout(
    expanded: Boolean,
    track: Track?,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit,
    player: @Composable (settled: Boolean) -> Unit,
) {
    val state = remember { PlayerExpansionState() }
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val reducedMotion = LocalReducedMotion.current || keyboard
    val currentReducedMotion by rememberUpdatedState(reducedMotion)
    val currentKeyboard by rememberUpdatedState(keyboard)
    val currentExpanded by rememberUpdatedState(expanded)
    val changeExpansion by rememberUpdatedState(onExpandedChange)
    val scope = rememberCoroutineScope()
    var mounted by remember { mutableStateOf(expanded) }
    val density = LocalDensity.current
    val sourceColor = ResonanceColors.CanvasElevated
    val flight by remember(reducedMotion) { derivedStateOf {
        !reducedMotion && state.fraction > 0f && state.fraction < 1f &&
            state.canFlyArtwork
    } }

    LaunchedEffect(expanded, reducedMotion, keyboard) {
        if (expanded && !mounted) {
            mounted = true
            // Measure both endpoints once before revealing/moving the player.
            if (!keyboard) {
                withFrameNanos { }
                withFrameNanos { }
            }
        }
        state.settle(expanded, reducedMotion, immediate = keyboard)
        if (!expanded) mounted = false
    }

    val gesture = Modifier.pointerInput(state) {
        val tracker = VelocityTracker()
        var position = Offset.Zero
        detectDragGestures(
            orientationLock = Orientation.Vertical,
            onDragStart = { _, change, _ ->
                tracker.resetTracking()
                position = Offset.Zero
                tracker.addPosition(change.uptimeMillis, position)
                state.beginDrag()
                scope.launch { state.animation.stop() }
            },
            onDrag = { change, delta ->
                change.consume()
                position += Offset(0f, delta.y)
                tracker.addPosition(change.uptimeMillis, position)
                state.dragBy(delta.y)
            },
            onDragCancel = {
                scope.launch { state.settle(currentExpanded, currentReducedMotion, 0f, currentKeyboard) }
            },
            onDragEnd = { change ->
                tracker.addPosition(change.uptimeMillis, position)
                val velocity = tracker.calculateVelocity().y
                val close = shouldDismissPlayer(state.fraction, velocity, state.travel)
                scope.launch {
                    state.endDrag()
                    if (!close != currentExpanded) {
                        state.releaseVelocity = -velocity / state.travel
                        changeExpansion(!close)
                    } else state.settle(currentExpanded, currentReducedMotion, -velocity / state.travel, currentKeyboard)
                }
            },
        )
    }

    CompositionLocalProvider(LocalPlayerExpansion provides state, LocalPlayerDismissGesture provides gesture) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            val width = with(density) { maxWidth.toPx() }
            val height = with(density) { maxHeight.toPx() }
            val full = Rect(0f, 0f, width, height)
            val source = state.sourceBounds ?: Rect(0f, height * .9f, width, height)
            state.travel = source.top.coerceAtLeast(height * .5f)
            Box(Modifier.fillMaxSize().graphicsLayer {
                    val scale = if (reducedMotion) 1f else 1f - .04f * state.fraction
                    scaleX = scale
                    scaleY = scale
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                    clip = !reducedMotion && state.fraction > 0f
                }) {
                Box(Modifier.fillMaxSize().onGloballyPositioned {
                    state.backgroundCoordinates = it
                    state.updateSourceBounds()
                }) { background() }
            }

            if (mounted && track != null) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = state.fraction }
                    .background(Color.Black.copy(alpha = .32f))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = { onExpandedChange(false) }))

                Box(Modifier.fillMaxSize().graphicsLayer {
                        val frame = if (reducedMotion) full else lerp(source, full, state.fraction)
                        translationX = frame.left
                        translationY = frame.top
                        shape = PlayerClip(frame.width, frame.height, with(density) { 30.dp.toPx() } * (1f - state.fraction))
                        clip = true
                        alpha = if (reducedMotion) state.fraction else if (state.fraction > 0f) 1f else 0f
                    }
                    .testTag("player-expansion")
                    .drawBehind {
                        drawRect(androidx.compose.ui.graphics.lerp(sourceColor, ImmersivePlayerColors.Canvas, state.fraction))
                    }) {
                    Box(Modifier.fillMaxSize().onGloballyPositioned {
                        state.playerCoordinates = it
                        state.updateTargetBounds()
                    }.graphicsLayer {
                        alpha = if (reducedMotion) 1f else (state.fraction / .65f).coerceIn(0f, 1f)
                    }) { player(state.settled) }
                }

                if (flight) {
                    val from = state.sourceArtwork!!
                    val to = state.targetArtwork!!
                    val coverSize = with(density) { to.width.toDp() }
                    Box(Modifier.size(coverSize).then(gesture).graphicsLayer {
                        val bounds = lerp(from, to, state.fraction)
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = bounds.left
                        translationY = bounds.top
                        scaleX = bounds.width / to.width
                        scaleY = bounds.height / to.height
                        shadowElevation = with(density) { 16.dp.toPx() } * state.fraction
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            (9f + 5f * state.fraction).dp / scaleX.coerceAtLeast(.01f))
                        clip = true
                    }.testTag("player-transition-artwork")) {
                        AlbumArtwork(track.artworkSeed, Modifier.fillMaxSize(), 0.dp, track.artworkPath, track.sourceUri)
                    }
                }
            }
        }
    }
}
