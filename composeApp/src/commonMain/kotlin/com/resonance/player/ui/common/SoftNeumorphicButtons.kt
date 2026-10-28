package com.resonance.player.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
 * 微拟物拟态调色板（严格参考高精度设计规范图）：
 * - 无任何生硬描边线（No wireframe borders）
 * - 柔和多层漫反射环境阴影（Ambient soft diffusion shadows）
 * - 3D 凸起拟态温润渐变盘面（Convex dome lighting gradient）
 * - 墨绿深沉典雅矢量图标与翡翠青绿高亮流光
 */
object SoftNeumorphicTokens {
    // 渐变主播放晶球色彩（蓝绿流光宝石）
    val HeroGradientStart = Color(0xFF347D79)
    val HeroGradientMiddle = Color(0xFF286E68)
    val HeroGradientEnd = Color(0xFF1C5F5B)

    // 活跃状态高亮翠绿
    val ActiveEmerald = Color(0xFF24B88C)
    val ActiveEmeraldGlow = Color(0xFF247B61)

    // 收藏红心高亮
    val ActiveHeart = Color(0xFFFF385C)

    // 浅色模式按键盘面凸起照明（左上向右下平滑漫射）
    val DiscLightGradStart = Color(0xFFFFFFFF)
    val DiscLightGradMid = Color(0xFFF7FBF9)
    val DiscLightGradEnd = Color(0xFFE2EBE8)

    // 深色模式按键盘面
    val DiscDarkGradStart = Color(0xFF28393F)
    val DiscDarkGradMid = Color(0xFF1E2C31)
    val DiscDarkGradEnd = Color(0xFF141F23)

    // 浅色模式深墨绿典雅图标色彩
    val IconPetroleumTeal = Color(0xFF193734)

    // 深色模式柔光薄荷白图标色彩
    val IconLuminousMint = Color(0xFFDCEDE7)
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
fun SoftHeroPlayButton(isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 70.dp) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val reduced = LocalReducedMotion.current || LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val scale by animateFloatAsState(if (pressed && !reduced) 0.98f else 1f,
        tween(motionDuration(ResonanceMotionTokens.PressDuration), easing = ResonanceMotionTokens.ExitEasing), label = "playPress")
    val outline = ResonanceColors.Primary
    val face = ResonanceColors.TextPrimary
    val glyph = ResonanceColors.Canvas
    Box(modifier.size(size.coerceAtLeast(48.dp)).semantics { contentDescription = if (isPlaying) "暂停" else "播放" }
        .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).galleryShadow(CircleShape, depth = 6.dp).graphicsLayer {
            scaleX = scale; scaleY = scale
            translationY = ((1f - scale) / 0.02f) * 1.5.dp.toPx()
        }.clip(CircleShape).background(face)
            .border(if (focused) 3.dp else 0.dp, if (focused) outline else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                tint = glyph, modifier = Modifier.size(if (size >= 60.dp) 34.dp else 25.dp))
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
fun SoftMiniPlayButton(isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) =
    SoftHeroPlayButton(isPlaying, onClick, modifier, size = 44.dp)

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
            Box(Modifier.size(20.dp, 48.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(16.dp).galleryShadow(CircleShape, depth = 3.dp).clip(CircleShape).background(active)
                    .border(if (focused || pressed || dragging) 3.dp else 2.dp, MaterialTheme.colorScheme.surface, CircleShape))
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
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val dragging by interaction.collectIsDraggedAsState()
    val active = ResonanceColors.Primary
    val inactive = if (ResonanceColors.isDark) Color(0xFF2A3D39) else Color(0xFFCAD8D3)
    var editing by remember { mutableStateOf(false) }
    var preview by remember { mutableFloatStateOf(value) }
    val displayValue = (if (editing) preview else value).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val phase = rememberPlaybackVisualPhase(isPlaying, 3_600)
    val activePath = remember { Path() }
    val inactivePath = remember { Path() }

    Slider(
        value = displayValue,
        onValueChange = {
            editing = true
            preview = it
            onPreview(it)
            onValueChange(it)
        },
        onValueChangeFinished = {
            editing = false
            onPreview(null)
        },
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = label },
        thumb = {
            val thumbActive = focused || pressed || dragging
            val thumbSize = if (thumbActive) 17.dp else 14.dp
            Box(Modifier.size(24.dp, 48.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(thumbSize)
                        .galleryShadow(CircleShape, depth = 4.dp)
                        .clip(CircleShape)
                        .background(active)
                        .border(2.dp, if (ResonanceColors.isDark) Color(0xFFE4ECE8) else Color.White, CircleShape)
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
    formatPosition: (String, Float) -> String, modifier: Modifier = Modifier, isPlaying: Boolean = false) {
    var preview by remember { mutableStateOf<Float?>(null) }
    Column(modifier.fillMaxWidth()) {
        WavySlider(progress, onSeek, "播放进度", onPreview = { preview = it }, isPlaying = isPlaying)
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val timeStyle = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
            Text(formatPosition(durationText, (preview ?: progress).coerceIn(0f, 1f)), style = timeStyle, color = ResonanceColors.Muted)
            Text(durationText, style = timeStyle, color = ResonanceColors.Muted)
        }
    }
}

@Composable
fun SoftRefinedVolumeSlider(volume: Float, onVolumeChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    RefinedSlider(volume, onVolumeChange, "音量", modifier)
}
