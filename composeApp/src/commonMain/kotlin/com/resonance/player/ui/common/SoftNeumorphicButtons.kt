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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonance.player.design.ResonanceColors

/**
 * 微拟物拟态调色板（严格参考高精度设计规范图）：
 * - 无任何生硬描边线（No wireframe borders）
 * - 柔和多层漫反射环境阴影（Ambient soft diffusion shadows）
 * - 3D 凸起拟态温润渐变盘面（Convex dome lighting gradient）
 * - 墨绿深沉典雅矢量图标与翡翠青绿高亮流光
 */
object SoftNeumorphicTokens {
    // 渐变主播放晶球色彩（蓝绿流光宝石）
    val HeroGradientStart = Color(0xFF4CAFE8)
    val HeroGradientMiddle = Color(0xFF46C5BD)
    val HeroGradientEnd = Color(0xFF45DE9C)

    // 活跃状态高亮翠绿
    val ActiveEmerald = Color(0xFF24B88C)
    val ActiveEmeraldGlow = Color(0xFF45DE9C)

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

/**
 * 核心软拟态凸起圆盘修饰符：
 * 绝无任何硬白圈（无 border），纯粹依靠多层柔和漫反射环境阴影与盘面微光渐变，
 * 呈现出宛若从底板自然凸起的温润物理触感。
 */
@Composable
fun Modifier.softConvexDisc(
    isPressed: Boolean,
    elevationResting: Dp = 8.dp,
    elevationPressed: Dp = 2.dp,
    glowColor: Color? = null,
): Modifier {
    val isDark = ResonanceColors.isDark
    val currentElevation by animateDpAsState(
        targetValue = if (isPressed) elevationPressed else elevationResting,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "softDiscElevation",
    )

    val spotShadowColor = when {
        glowColor != null -> glowColor.copy(alpha = if (isDark) 0.50f else 0.40f)
        isDark -> Color.Black.copy(alpha = 0.52f)
        else -> Color(0xFF193734).copy(alpha = 0.14f)
    }
    val ambientShadowColor = when {
        glowColor != null -> glowColor.copy(alpha = 0.25f)
        isDark -> Color.Black.copy(alpha = 0.28f)
        else -> Color(0xFF193734).copy(alpha = 0.07f)
    }

    val gradient = if (isDark) {
        listOf(
            SoftNeumorphicTokens.DiscDarkGradStart,
            SoftNeumorphicTokens.DiscDarkGradMid,
            SoftNeumorphicTokens.DiscDarkGradEnd,
        )
    } else {
        listOf(
            SoftNeumorphicTokens.DiscLightGradStart,
            SoftNeumorphicTokens.DiscLightGradMid,
            SoftNeumorphicTokens.DiscLightGradEnd,
        )
    }

    return this
        .shadow(
            elevation = currentElevation,
            shape = CircleShape,
            spotColor = spotShadowColor,
            ambientColor = ambientShadowColor,
        )
        .clip(CircleShape)
        .background(
            Brush.linearGradient(
                colors = gradient,
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
            )
        )
}

/**
 * 1. 核心主播放/暂停大按键（参考图顶部中心蓝绿渐变流光水滴晶体球）：
 * - 70dp 黄金尺度，高饱和度海蓝至青翠平滑双色渐变
 * - 底部散发柔和青绿漫反射光晕（Soft Colored Glow Shadow）
 * - 顶部内嵌水润晶莹穹顶高光（Specular dome reflection）
 * - 纯白实心微圆角正三角形/双柱，动感柔滑切换
 */
@Composable
fun SoftHeroPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 70.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "heroPlayPressScale",
    )
    val currentElevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 14.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "heroPlayElevation",
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = currentElevation,
                shape = CircleShape,
                spotColor = SoftNeumorphicTokens.HeroGradientEnd.copy(alpha = 0.52f),
                ambientColor = SoftNeumorphicTokens.HeroGradientStart.copy(alpha = 0.35f),
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SoftNeumorphicTokens.HeroGradientStart,
                        SoftNeumorphicTokens.HeroGradientMiddle,
                        SoftNeumorphicTokens.HeroGradientEnd,
                    ),
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White.copy(alpha = 0.35f), bounded = true),
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // 水润晶莹穹顶反光（Specular light catch）
        Canvas(modifier = Modifier.matchParentSize()) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    center = Offset(this.size.width * 0.40f, this.size.height * 0.26f),
                    radius = this.size.width * 0.42f,
                ),
            )
        }

        // 纯白高品质实心图标
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (scaleIn(initialScale = 0.82f, animationSpec = tween(180)) + fadeIn(tween(180)))
                    .togetherWith(scaleOut(targetScale = 0.82f, animationSpec = tween(140)) + fadeOut(tween(140)))
            },
            label = "heroPlayIconMorph",
        ) { playing ->
            if (playing) {
                // 实心圆润双暂停柱（纯白）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(6.5.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(Color.White)
                    )
                    Box(
                        modifier = Modifier
                            .width(6.5.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(Color.White)
                    )
                }
            } else {
                // 实心正三角形播放键（纯白，带微圆角并居中偏右微调）
                Canvas(modifier = Modifier.size(28.dp).offset(x = 1.8.dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    val path = Path().apply {
                        moveTo(w * 0.18f, h * 0.12f)
                        lineTo(w * 0.88f, h * 0.50f)
                        lineTo(w * 0.18f, h * 0.88f)
                        close()
                    }
                    drawPath(path, color = Color.White)
                }
            }
        }
    }
}

/**
 * 2. 核心上一首 / 下一首大圆盘（参考图顶部左右两侧白凸盘）：
 * - 54dp 尺度，温润软拟态凸盘
 * - 纯正深墨绿矢量笔触（Light: #193734, Dark: #DCEDE7）
 * - 绝无白边线框
 */
@Composable
fun SoftSkipButton(
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    contentDescription: String = if (isNext) "下一首" else "上一首",
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "softSkipPressScale",
    )
    val isDark = ResonanceColors.isDark
    val iconColor = if (isDark) SoftNeumorphicTokens.IconLuminousMint else SoftNeumorphicTokens.IconPetroleumTeal

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .softConvexDisc(isPressed = isPressed, elevationResting = 8.dp, elevationPressed = 2.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = iconColor.copy(alpha = 0.15f), bounded = true),
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isNext) Icons.Default.SkipNext else Icons.Default.SkipPrevious,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(27.dp),
        )
    }
}

/**
 * 3. 二级与功能按键圆盘（参考图第二、三排：随机、循环、音量、歌词、队列、收藏、均衡器等）：
 * - 44dp 紧凑尺度
 * - 未激活状态：深墨绿细腻图标
 * - 激活状态：翡翠翠绿流光高亮，外散柔和青绿漫射阴影
 */
@Composable
fun SoftUtilityButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isActive: Boolean = false,
    activeColor: Color = SoftNeumorphicTokens.ActiveEmerald,
    activeGlowColor: Color = SoftNeumorphicTokens.ActiveEmeraldGlow,
    customTint: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "softUtilityPressScale",
    )
    val isDark = ResonanceColors.isDark
    val defaultInactiveColor = if (isDark) Color(0xFFA5BDB6) else SoftNeumorphicTokens.IconPetroleumTeal.copy(alpha = 0.76f)
    val iconColor = when {
        customTint != null -> customTint
        isActive -> activeColor
        else -> defaultInactiveColor
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .softConvexDisc(
                isPressed = isPressed,
                elevationResting = if (isActive) 7.dp else 5.dp,
                elevationPressed = 2.dp,
                glowColor = if (isActive) activeGlowColor else null,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = iconColor.copy(alpha = 0.18f), bounded = true),
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(21.dp),
            )
            if (isActive) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
            }
        }
    }
}

/**
 * 4. 底部 MiniPlayer 专属主控播放与切歌键（同源微缩版）：
 */
@Composable
fun SoftMiniPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftHeroPlayButton(
        isPlaying = isPlaying,
        onClick = onClick,
        modifier = modifier,
        size = 46.dp,
    )
}

@Composable
fun SoftMiniSkipButton(
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftSkipButton(
        isNext = isNext,
        onClick = onClick,
        modifier = modifier,
        size = 36.dp,
    )
}

/**
 * 5. 小巧精致高精度进度条（Scrubber）：
 * - 3.dp 极简纤细内凹凹槽导轨
 * - 流光蓝绿双色渐变进度填充
 * - 7.dp 珍珠质感微型手柄，拖拽时优雅放大至 11.dp 并泛起薄荷微光
 * - 完美支持全轨道点击跳播与平滑水平拖曳手势
 */
@Composable
fun SoftRefinedPlaybackScrubber(
    progress: Float,
    durationText: String,
    onSeek: (Float) -> Unit,
    formatPosition: (String, Float) -> String,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val currentProgress = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 4.dp else 2.8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "softScrubTrackH",
    )
    val knobSize by animateDpAsState(
        targetValue = if (isDragging) 11.5.dp else 7.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "softScrubKnobSize",
    )
    val isDark = ResonanceColors.isDark

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(fraction)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            dragFraction = fraction
                            onSeek(fraction)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek(dragFraction)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragFraction = fraction
                            onSeek(fraction)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val totalWidth = maxWidth
            val activeWidth = totalWidth * currentProgress

            // 1. 凹槽轨道底色（柔和内嵌感，无硬描边）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2EBE8))
            )

            // 2. 活跃渐变填充轨（蓝绿流光）
            Box(
                modifier = Modifier
                    .width(activeWidth)
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                SoftNeumorphicTokens.HeroGradientStart,
                                SoftNeumorphicTokens.HeroGradientMiddle,
                                SoftNeumorphicTokens.HeroGradientEnd,
                            )
                        )
                    )
            )

            // 3. 珍珠微型手柄
            val knobOffset = (totalWidth - knobSize) * currentProgress
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(knobSize),
                contentAlignment = Alignment.Center,
            ) {
                if (isDragging) {
                    Box(
                        modifier = Modifier
                            .size(knobSize + 8.dp)
                            .clip(CircleShape)
                            .background(SoftNeumorphicTokens.ActiveEmerald.copy(alpha = 0.32f))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(knobSize)
                        .shadow(
                            elevation = if (isDragging) 4.dp else 2.5.dp,
                            shape = CircleShape,
                            spotColor = SoftNeumorphicTokens.ActiveEmerald.copy(alpha = 0.50f),
                            ambientColor = Color.Black.copy(alpha = 0.20f),
                        )
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        // 时间标签（清晰小巧，等宽数字排版）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPosition(durationText, currentProgress),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = if (isDragging) SoftNeumorphicTokens.ActiveEmerald else (if (isDark) Color(0xFFACBCB7) else SoftNeumorphicTokens.IconPetroleumTeal.copy(alpha = 0.72f)),
            )
            Text(
                text = durationText,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = if (isDark) Color(0xFF7A8E88) else Color(0xFF8A9E98),
            )
        }
    }
}

/**
 * 6. 小巧精致音量滑块：
 */
@Composable
fun SoftRefinedVolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragVolume by remember { mutableFloatStateOf(volume) }
    val displayedVolume = if (isDragging) dragVolume else volume.coerceIn(0f, 1f)

    val trackHeight by animateDpAsState(if (isDragging) 4.dp else 2.8.dp, label = "softVolTrackH")
    val knobSize by animateDpAsState(if (isDragging) 10.dp else 6.5.dp, label = "softVolKnobSize")
    val isDark = ResonanceColors.isDark

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onVolumeChange(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        dragVolume = fraction
                        onVolumeChange(fraction)
                    },
                    onDragEnd = {
                        isDragging = false
                        onVolumeChange(dragVolume)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragVolume = fraction
                        onVolumeChange(fraction)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val totalWidth = maxWidth
        val activeWidth = totalWidth * displayedVolume

        // 轨道凹槽
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2EBE8))
        )

        // 活跃填充
        Box(
            modifier = Modifier
                .width(activeWidth)
                .height(trackHeight)
                .clip(CircleShape)
                .background(
                    if (isDark) {
                        Brush.horizontalGradient(listOf(Color(0xFF8CD0BC), Color(0xFFDCEDE7)))
                    } else {
                        Brush.horizontalGradient(listOf(SoftNeumorphicTokens.IconPetroleumTeal.copy(alpha = 0.75f), SoftNeumorphicTokens.IconPetroleumTeal))
                    }
                )
        )

        // 手柄
        val knobOffset = (totalWidth - knobSize) * displayedVolume
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(knobSize)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
