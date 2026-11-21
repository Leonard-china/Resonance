package com.resonance.player.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonance.player.design.ResonanceColors
import com.resonance.player.design.LocalReducedMotion
import com.resonance.player.design.ResonanceMotionTokens
import com.resonance.player.design.motionDuration
import com.resonance.player.design.galleryShadow
import com.resonance.player.design.rememberPlaybackVisualPhase
import kotlin.math.abs
import kotlin.math.min

/**
 * Tactile transport control tokens aligned with Clearwater / 清岚.
 */
object SoftNeumorphicTokens {
    // Primary playback accent ramp (fresh jade-mint, all >= 5:1 contrast with white)
    val HeroGradientStart = Color(0xFF0E947B)
    val HeroGradientMiddle = Color(0xFF0B7D67)
    val HeroGradientEnd = Color(0xFF086352)

    // Active positive accent
    val ActiveEmerald = Color(0xFF167557)
    val ActiveEmeraldGlow = Color(0xFF115E46)

    // Favorite heart accent (soft rose coral, reserved strictly for heart icon)
    val ActiveHeart = Color(0xFFE8566D)

    // Light tactile surface ramp
    val DiscLightGradStart = Color(0xFFFFFFFF)
    val DiscLightGradMid = Color(0xFFF5FAF9)
    val DiscLightGradEnd = Color(0xFFE4EFEC)

    // Dark tactile surface ramp
    val DiscDarkGradStart = Color(0xFF1E2D33)
    val DiscDarkGradMid = Color(0xFF162227)
    val DiscDarkGradEnd = Color(0xFF10191D)

    // High-contrast ink tokens
    val IconPetroleumTeal = Color(0xFF12201E)
    val IconLuminousMint = Color(0xFFEFF7F6)
}

/** Shallow tactile lighting; press motion stays on the graphics layer. */
@Composable
fun Modifier.softConvexDisc(
    isPressed: Boolean,
    elevationResting: Dp = 5.dp,
    elevationPressed: Dp = 2.dp,
    glowColor: Color? = null,
): Modifier {
    val elevation by animateDpAsState(if (isPressed) elevationPressed else elevationResting,
        tween(motionDuration(ResonanceMotionTokens.PressDuration)), label = "discElevation")
    val shadowColor = (glowColor ?: ResonanceColors.Shadow).copy(alpha = if (ResonanceColors.isDark) 0.26f else 0.12f)
    val surface = ResonanceColors.CanvasElevated
    val low = ResonanceColors.Soft
    return graphicsLayer {
        shadowElevation = elevation.toPx()
        shape = CircleShape
        ambientShadowColor = shadowColor
        spotShadowColor = shadowColor
    }.clip(CircleShape).background(Brush.linearGradient(listOf(surface, surface, low)))
}

@Composable
fun SoftHeroPlayButton(isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 58.dp, isLoading: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val reduced = LocalReducedMotion.current || LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduced) 0.90f else 1f,
        animationSpec = if (reduced) tween(0) else ResonanceMotionTokens.AppleBouncySpring,
        label = "playPress"
    )
    val outline = ResonanceColors.Primary
    val face = ResonanceColors.TextPrimary
    val glyph = ResonanceColors.Canvas
    Box(modifier.size(size.coerceAtLeast(48.dp)).semantics { contentDescription = if (isLoading) "加载中" else if (isPlaying) "暂停" else "播放" }
        .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).galleryShadow(CircleShape, depth = 3.dp).graphicsLayer {
            scaleX = scale; scaleY = scale
        }.clip(CircleShape).background(face)
            .border(if (focused) 3.dp else 0.dp, if (focused) outline else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center) {
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = glyph,
                    strokeWidth = if (size >= 50.dp) 2.5.dp else 2.dp,
                    modifier = Modifier.size(if (size >= 50.dp) 24.dp else 18.dp),
                )
            } else {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        if (reduced) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (scaleIn(initialScale = 0.72f, animationSpec = spring(dampingRatio = 0.62f, stiffness = 480f)) +
                            fadeIn(tween(120))) togetherWith
                            (scaleOut(targetScale = 0.72f, animationSpec = tween(100)) + fadeOut(tween(90)))
                    },
                    label = "heroPlayPauseMorph",
                ) { playing ->
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = glyph,
                        modifier = Modifier.size(if (size >= 50.dp) 28.dp else 22.dp).offset(x = if (playing) 0.dp else 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SoftSkipButton(isNext: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 54.dp,
    contentDescription: String = if (isNext) "下一首" else "上一首") {
    SoftUtilityButton(if (isNext) Icons.Default.SkipNext else Icons.Default.SkipPrevious,
        contentDescription, onClick, modifier, size = size, customTint = ResonanceColors.TextPrimary)
}

@Composable
fun SoftUtilityButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, size: Dp = 44.dp, isActive: Boolean = false,
    activeColor: Color = ResonanceColors.Primary, activeGlowColor: Color = ResonanceColors.Primary,
    customTint: Color? = null) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val reduced = LocalReducedMotion.current || LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val scale by animateFloatAsState(if (pressed && !reduced) 0.98f else 1f,
        tween(motionDuration(ResonanceMotionTokens.PressDuration), easing = ResonanceMotionTokens.ExitEasing), label = "utilityPress")
    val foreground = customTint ?: if (isActive) activeColor else ResonanceColors.Muted
    val focusColor = ResonanceColors.Primary
    Box(modifier.size(size.coerceAtLeast(48.dp)).semantics {
        this.contentDescription = contentDescription
        if (isActive) stateDescription = "已开启"
    }.clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape).background(if (isActive) activeColor.copy(alpha = 0.12f) else Color.Transparent)
            .border(if (focused) 2.dp else 0.dp, if (focused) focusColor else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, null, tint = foreground, modifier = Modifier.size(if (size >= 50.dp) 26.dp else 21.dp))
                if (isActive) {
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.size(3.dp).clip(CircleShape).background(activeColor))
                }
            }
        }
    }
}

@Composable
fun SoftMiniPlayButton(isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false) =
    SoftHeroPlayButton(isPlaying, onClick, modifier, size = 44.dp, isLoading = isLoading)

@Composable
fun SoftMiniSkipButton(isNext: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) =
    SoftSkipButton(isNext, onClick, modifier, size = 36.dp)

/** The platform slider owns touch, keyboard, RTL and accessibility behavior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefinedSlider(value: Float, onValueChange: (Float) -> Unit, label: String, modifier: Modifier = Modifier, onPreview: (Float?) -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val dragging by interaction.collectIsDraggedAsState()
    val active = ResonanceColors.Primary
    val inactive = ResonanceColors.DividerStrong
    var editing by remember { mutableStateOf(false) }
    var preview by remember { mutableFloatStateOf(value) }
    Slider(value = (if (editing) preview else value).coerceIn(0f, 1f),
        onValueChange = { editing = true; preview = it; onPreview(it); onValueChange(it) },
        onValueChangeFinished = { editing = false; onPreview(null) },
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = label },
        thumb = {
            // Slider measures the thumb to establish its semantic/input bounds.
            val thumbSize by animateDpAsState(
                if (focused || pressed || dragging) 15.dp else 12.dp,
                tween(motionDuration(ResonanceMotionTokens.PressDuration)),
                label = "refinedSliderThumb",
            )
            Box(Modifier.size(20.dp, 48.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(thumbSize).galleryShadow(CircleShape, depth = 2.dp).clip(CircleShape).background(active)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape))
            }
        },
        track = { slider ->
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val start = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) size.width else 0f
                val end = size.width - start
                val y = size.height / 2
                drawLine(inactive, Offset(start, y), Offset(end, y), size.height, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(active, Offset(start, y), Offset(start + (end - start) * slider.value, y), size.height,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onPreview: (Float?) -> Unit = {},
    isPlaying: Boolean = false,
    flat: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val dragging by interaction.collectIsDraggedAsState()
    val active = ResonanceColors.Primary
    val inactive = ResonanceColors.DividerStrong
    var editing by remember { mutableStateOf(false) }
    var preview by remember { mutableFloatStateOf(value) }
    val displayValue = (if (editing) preview else value).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val phase = rememberPlaybackVisualPhase(isPlaying && !flat, 3_600)
    val activePath = remember { Path() }
    val inactivePath = remember { Path() }

    val reduced = LocalReducedMotion.current
    val thumbActive = focused || pressed || dragging
    val flatTrackHeightDp by animateDpAsState(
        targetValue = if (thumbActive) 7.dp else 4.dp,
        animationSpec = if (reduced) tween(0) else spring(dampingRatio = 0.74f, stiffness = 420f),
        label = "flatScrubberHeight",
    )

    Slider(
        value = displayValue,
        onValueChange = {
            editing = true
            preview = it
            onPreview(it)
        },
        onValueChangeFinished = {
            if (editing) onValueChange(preview)
            editing = false
            onPreview(null)
        },
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = label },
        thumb = {
            val targetThumb = if (flat) {
                if (thumbActive) 15.dp else 9.dp
            } else {
                if (thumbActive) 16.dp else 13.dp
            }
            val thumbSize by animateDpAsState(
                targetValue = targetThumb,
                animationSpec = if (reduced) tween(0) else spring(dampingRatio = 0.65f, stiffness = 440f),
                label = "scrubberThumbSize",
            )
            Box(Modifier.size(28.dp, 48.dp), contentAlignment = Alignment.Center) {
                if (thumbActive) {
                    Box(
                        Modifier
                            .size(thumbSize + 10.dp)
                            .clip(CircleShape)
                            .background(active.copy(alpha = 0.22f))
                    )
                }
                Box(
                    Modifier
                        .size(thumbSize)
                        .galleryShadow(CircleShape, depth = if (thumbActive) 4.dp else 2.dp)
                        .clip(CircleShape)
                        .background(active)
                        .then(
                            if (!flat) Modifier.border(2.dp, if (ResonanceColors.isDark) Color(0xFFEFF7F6) else Color.White, CircleShape)
                            else Modifier
                        )
                )
            }
        },
        track = { sliderState ->
            val strokeActivePx = 2.4f * density.density
            val strokeInactivePx = 1.4f * density.density
            val amplitudePx = 2.2f * density.density
            val wavelengthPx = 34.0f * density.density
            val stepPx = 3.0f * density.density

            Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                val width = size.width
                val centerY = size.height / 2f
                if (width <= 0f) return@Canvas

                val currentX = (sliderState.value * width).coerceIn(0f, width)
                if (flat) {
                    val trackHeight = flatTrackHeightDp.toPx()
                    drawLine(inactive.copy(alpha = 0.68f), Offset(0f, centerY), Offset(width, centerY), trackHeight, cap = StrokeCap.Round)
                    drawLine(active, Offset(0f, centerY), Offset(currentX, centerY), trackHeight, cap = StrokeCap.Round)
                    return@Canvas
                }
                val twoPi = (2.0 * PI).toFloat()
                val offset = phase.value
                fun waveY(x: Float): Float {
                    val nearThumb = min(1f, abs(x - currentX) / (wavelengthPx * .35f))
                    val nearEdge = min(1f, x / 10.dp.toPx()) * min(1f, (width - x) / 10.dp.toPx())
                    return centerY + amplitudePx * nearThumb * nearEdge * sin((x / wavelengthPx) * twoPi - offset)
                }

                // Active wavy segment (played)
                if (currentX > 0f) {
                    val pathActive = activePath.apply { reset() }
                    pathActive.moveTo(0f, centerY)
                    var x = 0f
                    while (x < currentX) {
                        x = (x + stepPx).coerceAtMost(currentX)
                        val y = waveY(x)
                        pathActive.lineTo(x, y)
                    }
                    drawPath(
                        path = pathActive,
                        color = active,
                        style = Stroke(width = strokeActivePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }

                // Inactive wavy segment (unplayed)
                if (currentX < width) {
                    val pathInactive = inactivePath.apply { reset() }
                    val startY = waveY(currentX)
                    pathInactive.moveTo(currentX, startY)
                    var x = currentX
                    while (x < width) {
                        x = (x + stepPx).coerceAtMost(width)
                        val y = waveY(x)
                        pathInactive.lineTo(x, y)
                    }
                    drawPath(
                        path = pathInactive,
                        color = inactive,
                        style = Stroke(width = strokeInactivePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    )
}

@Composable
fun SoftRefinedPlaybackScrubber(progress: Float, durationText: String, onSeek: (Float) -> Unit,
    formatPosition: (String, Float) -> String, modifier: Modifier = Modifier, isPlaying: Boolean = false, flat: Boolean = false) {
    var preview by remember { mutableStateOf<Float?>(null) }
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(top = 36.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val timeStyle = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
            Text(formatPosition(durationText, (preview ?: progress).coerceIn(0f, 1f)), style = timeStyle, color = ResonanceColors.Muted)
            Text(durationText, style = timeStyle, color = ResonanceColors.Muted)
        }
        WavySlider(progress, onSeek, "播放进度", onPreview = { preview = it }, isPlaying = isPlaying, flat = flat)
    }
}

@Composable
fun SoftRefinedVolumeSlider(volume: Float, onVolumeChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    RefinedSlider(volume, onVolumeChange, "音量", modifier)
}
