package com.resonance.player.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.animateColorAsState
import com.resonance.player.model.WeatherPalette
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.blur.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonance.player.model.ThemeMode

/**
 * Native sage / ice-blue surfaces, legible text and restrained frosted glass.
 */
data class ResonanceColorTokens(
    val isDark: Boolean,
    val Canvas: Color,
    val CanvasElevated: Color,
    val Raised: Color,
    val Soft: Color,
    val SurfaceSubtle: Color,
    val Glass: Color,
    val GlassLight: Color,
    val GlassUltra: Color,
    val GlassBorder: Color,
    val GlassBorderSubtle: Color,
    val GlassBorderGlow: Color,
    val Divider: Color,
    val DividerStrong: Color,
    val TextPrimary: Color,
    val Muted: Color,
    val Dim: Color,
    val Dimmer: Color,
    val Primary: Color,
    val PrimaryGlow: Color,
    val PrimarySoft: Color,
    val Positive: Color,
    val PositiveGlow: Color,
    val PositiveSoft: Color,
    val Notice: Color,
    val Secondary: Color,
    val SecondaryGlow: Color,
    val SecondarySoft: Color,
    val Info: Color,
    val Shadow: Color,
)

private fun colors(dark: Boolean, palette: WeatherPalette): ResonanceColorTokens {
    val blue = palette == WeatherPalette.IceBlue
    val primary = Color(if (dark) { if (blue) 0xFFA9C5FF else 0xFF8CD0BC } else { if (blue) 0xFF315FD0 else 0xFF2F7467 })
    return ResonanceColorTokens(
        isDark = dark,
        Canvas = Color(if (dark) 0xFF111A1D else if (blue) 0xFFF3F7FB else 0xFFF1F6F3),
        CanvasElevated = Color(if (dark) 0xFF182327 else 0xFFFCFDFC),
        Raised = Color(if (dark) 0xFF1E2B30 else 0xFFFCFDFC),
        Soft = Color(if (dark) 0xFF26363C else if (blue) 0xFFE6EDF8 else 0xFFE5EFE9),
        SurfaceSubtle = Color(if (dark) 0xFF26363C else 0xFFE8EEEB),
        Glass = Color(if (dark) 0xDE1C292E else 0xDEFFFFFF),
        GlassLight = Color(if (dark) 0xE626353A else 0xE6FFFFFF),
        GlassUltra = Color(if (dark) 0xF226353A else 0xF2FFFFFF),
        GlassBorder = Color(if (dark) 0x32FFFFFF else 0xCFFFFFFF),
        GlassBorderSubtle = Color(if (dark) 0x1CFFFFFF else 0x18243C33),
        GlassBorderGlow = primary.copy(alpha = 0.30f),
        Divider = Color(if (dark) 0x24FFFFFF else 0x18243C33),
        DividerStrong = Color(if (dark) 0x40FFFFFF else 0x30243C33),
        TextPrimary = Color(if (dark) 0xFFF1F7F5 else if (blue) 0xFF172B3A else 0xFF1C302B),
        Muted = Color(if (dark) 0xFFB6C7C3 else 0xFF50645D),
        Dim = Color(if (dark) 0xFFACBCB7 else 0xFF596D66),
        Dimmer = Color(if (dark) 0xFF92A7A0 else 0xFF596D66),
        Primary = primary, PrimaryGlow = primary, PrimarySoft = primary.copy(alpha = if (dark) 0.16f else 0.10f),
        Positive = Color(if (dark) 0xFF8CD0BC else 0xFF2F7467),
        PositiveGlow = primary, PositiveSoft = primary.copy(alpha = 0.12f),
        Notice = Color(if (dark) 0xFFA9C5FF else 0xFF365FA8),
        Secondary = primary, SecondaryGlow = primary, SecondarySoft = primary.copy(alpha = 0.10f),
        Info = Color(if (dark) 0xFFA9C5FF else 0xFF365FA8),
        Shadow = Color(0xFF10281F),
    )
}

val DarkResonanceColors = colors(true, WeatherPalette.Sage)
val LightResonanceColors = colors(false, WeatherPalette.Sage)
val LocalGlassState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun <T> resonanceSpring(): FiniteAnimationSpec<T> = if (LocalReducedMotion.current) tween(0)
    else spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)

@Composable
fun motionDuration(duration: Int): Int = if (LocalReducedMotion.current) 0 else duration

/**
 * 界面切换规范动效：
 * 采用原生顶级工业质感（fluid cubic-bezier / smooth deceleration），
 * 杜绝僵硬突兀的瞬时跳变，赋予画面丝滑连贯的空间位置感。
 */
object ResonanceMotionTokens {
    // 页面横向滑动的流畅规范：320ms fluid cubic-bezier (0.2, 0.0, 0.0, 1.0)，丝滑优美，位移明确连贯
    val PageSlideSpec: FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = 320,
        easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    )

    // 层级钻取（Push-Pop）的规范：340ms 柔和减速曲线
    val DetailSlideSpec: FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = 340,
        easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    )

    // 页面横向滑动的弹簧规范：阻尼比 1.0f 确保无余震回弹，stiffness 300f 提供极佳的响应速度与如丝顺滑
    val PageSlideSpring: FiniteAnimationSpec<IntOffset> = spring(
        stiffness = 300f,
        dampingRatio = 1.0f,
    )

    // 层级钻取（Push-Pop）的弹簧规范：略微柔和（260f）
    val DetailSlideSpring: FiniteAnimationSpec<IntOffset> = spring(
        stiffness = 260f,
        dampingRatio = 1.0f,
    )

    // 伴随滑动的柔和渐变：避免画面过早消隐，确保滑动态势自然可见
    val PageFadeInSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = 280,
        easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f),
    )

    val PageFadeOutSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = 220,
        easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f),
    )
}

val LocalResonanceColors = staticCompositionLocalOf { DarkResonanceColors }

object ResonanceColors {
    val isDark: Boolean @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.isDark
    val Canvas: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Canvas
    val CanvasElevated: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.CanvasElevated
    val Raised: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Raised
    val Soft: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Soft
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.CanvasElevated
    val SurfaceSubtle: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.SurfaceSubtle
    val Glass: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Glass
    val GlassLight: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.GlassLight
    val GlassUltra: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.GlassUltra
    val GlassBorder: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.GlassBorder
    val GlassBorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.GlassBorderSubtle
    val GlassBorderGlow: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.GlassBorderGlow
    val Divider: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Divider
    val DividerStrong: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.DividerStrong
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.TextPrimary
    val Muted: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Muted
    val Dim: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Dim
    val Dimmer: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Dimmer
    val Primary: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Primary
    val PrimaryGlow: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.PrimaryGlow
    val PrimarySoft: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.PrimarySoft
    val Positive: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Positive
    val PositiveGlow: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.PositiveGlow
    val PositiveSoft: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.PositiveSoft
    val Notice: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Notice
    val Secondary: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Secondary
    val SecondaryGlow: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.SecondaryGlow
    val SecondarySoft: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.SecondarySoft
    val Info: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Info
    val Shadow: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Shadow
}

/** 统一圆角尺度 —— 优雅水润大圆角。 */
object ResonanceShapes {
    val Button = RoundedCornerShape(14.dp)
    val ArtworkSmall = RoundedCornerShape(10.dp)
    val Artwork = RoundedCornerShape(16.dp)
    val ArtworkLarge = RoundedCornerShape(24.dp)
    val Panel = RoundedCornerShape(18.dp)
    val Card = RoundedCornerShape(20.dp)
    val Capsule = RoundedCornerShape(999.dp)
}

/** 统一间距尺度。 */
object ResonanceSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 28.dp
}

/** 果冻级按压弹性物理反馈：触感灵动水润。 */
fun Modifier.resonancePressable(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
    restingScale: Float = 1f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (LocalReducedMotion.current) 1f else if (pressed) pressedScale.coerceIn(0.80f, 1f) else restingScale,
        animationSpec = resonanceSpring(),
        label = "resonancePressScale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * 现代声学磨砂玻璃修饰符（Frosted Glassmorphism）：
 * - 柔和漫反射环境阴影（Soft Drop Shadow）
 * - 半透明光感倾斜渐变（Translucent Specular Gradient）
 * - 物理高光双重倒角描边（Dual-stop Beveled Highlight Border）
 */
@Composable
fun Modifier.resonanceGlass(
    shape: Shape = ResonanceShapes.Panel,
    backgroundColor: Color = ResonanceColors.Glass,
    backgroundGradient: List<Color>? = null,
    borderColors: List<Color> = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
    borderWidth: Dp = 1.dp,
    shadowElevation: Dp = 5.dp,
    shadowColor: Color = ResonanceColors.Shadow.copy(alpha = if (ResonanceColors.isDark) 0.35f else 0.08f),
): Modifier {
    val glassState = LocalGlassState.current
    val gradient = backgroundGradient ?: if (ResonanceColors.isDark) {
        listOf(
            backgroundColor.copy(alpha = (backgroundColor.alpha * 1.22f).coerceAtMost(0.95f)),
            backgroundColor.copy(alpha = 0.86f),
        )
    } else {
        listOf(
            backgroundColor.copy(alpha = 0.95f),
            backgroundColor.copy(alpha = 0.86f),
        )
    }

    return this
        .then(
            if (shadowElevation > 0.dp) {
                Modifier.shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    spotColor = shadowColor,
                    ambientColor = shadowColor,
                )
            } else Modifier
        )
        .clip(shape)
        .then(if (glassState != null) {
            val glassStyle = HazeBlurStyle(
            backgroundColor = ResonanceColors.Canvas,
            colorEffects = listOf(HazeColorEffect.tint(Brush.verticalGradient(gradient))),
            blurRadius = 18.dp,
            noiseFactor = 0.025f,
            fallbackColorEffect = HazeColorEffect.tint(backgroundColor.copy(alpha = 0.96f)),
            )
            Modifier.hazeEffect(glassState) { blurEffect { style = glassStyle } }
        } else Modifier.background(Brush.verticalGradient(gradient)))
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                if (borderColors.size >= 2) borderColors
                else listOf(borderColors.first(), borderColors.first().copy(alpha = 0.2f))
            ),
            shape = shape,
        )
}

private val resonanceTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp,
    ),
)

@Composable
fun ResonanceTheme(
    themeMode: ThemeMode = ThemeMode.Light,
    palette: WeatherPalette = WeatherPalette.Sage,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemDark
    }

    val target = colors(isDark, palette)
    val accent by animateColorAsState(target.Primary, tween(if (reducedMotion) 0 else 260), label = "weatherAccent")
    val canvas by animateColorAsState(target.Canvas, tween(if (reducedMotion) 0 else 260), label = "weatherCanvas")
    val colorTokens = target.copy(Primary = accent, PrimaryGlow = accent,
        PrimarySoft = accent.copy(alpha = if (isDark) 0.16f else 0.10f), Canvas = canvas)

    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = colorTokens.Primary,
            onPrimary = colorTokens.Canvas,
            primaryContainer = colorTokens.PrimarySoft,
            onPrimaryContainer = colorTokens.TextPrimary,
            secondary = colorTokens.Positive,
            onSecondary = if (isDark) colorTokens.Canvas else Color.White,
            secondaryContainer = colorTokens.Soft,
            onSecondaryContainer = colorTokens.TextPrimary,
            tertiary = colorTokens.Info,
            onTertiary = if (isDark) colorTokens.Canvas else Color.White,
            tertiaryContainer = colorTokens.Soft,
            onTertiaryContainer = colorTokens.TextPrimary,
            surfaceTint = colorTokens.Primary,
            surfaceContainerLowest = colorTokens.Canvas,
            surfaceContainerLow = colorTokens.CanvasElevated,
            surfaceContainer = colorTokens.Raised,
            surfaceContainerHigh = colorTokens.Soft,
            surfaceContainerHighest = colorTokens.Soft,
            background = colorTokens.Canvas,
            onBackground = colorTokens.TextPrimary,
            surface = colorTokens.Raised,
            onSurface = colorTokens.TextPrimary,
            surfaceVariant = colorTokens.Soft,
            onSurfaceVariant = colorTokens.Muted,
            outline = colorTokens.Divider,
            outlineVariant = colorTokens.DividerStrong,
            error = Color(0xFFFFB4AB),
        )
    } else {
        lightColorScheme(
            primary = colorTokens.Primary,
            onPrimary = Color.White,
            primaryContainer = colorTokens.PrimarySoft,
            onPrimaryContainer = colorTokens.Primary,
            secondary = colorTokens.Positive,
            onSecondary = if (isDark) colorTokens.Canvas else Color.White,
            secondaryContainer = colorTokens.Soft,
            onSecondaryContainer = colorTokens.TextPrimary,
            tertiary = colorTokens.Info,
            onTertiary = if (isDark) colorTokens.Canvas else Color.White,
            tertiaryContainer = colorTokens.Soft,
            onTertiaryContainer = colorTokens.TextPrimary,
            surfaceTint = colorTokens.Primary,
            surfaceContainerLowest = colorTokens.Canvas,
            surfaceContainerLow = colorTokens.CanvasElevated,
            surfaceContainer = colorTokens.Raised,
            surfaceContainerHigh = colorTokens.Soft,
            surfaceContainerHighest = colorTokens.Soft,
            background = colorTokens.Canvas,
            onBackground = colorTokens.TextPrimary,
            surface = colorTokens.Raised,
            onSurface = colorTokens.TextPrimary,
            surfaceVariant = colorTokens.Soft,
            onSurfaceVariant = colorTokens.Muted,
            outline = colorTokens.Divider,
            outlineVariant = colorTokens.DividerStrong,
            error = Color(0xFFBA1A1A),
        )
    }

    CompositionLocalProvider(LocalResonanceColors provides colorTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = resonanceTypography,
            content = content,
        )
    }
}
