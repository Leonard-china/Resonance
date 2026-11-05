package com.resonance.player.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
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
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonance.player.model.ThemeMode

/**
 * Native color roles matched to the AM++ reference palette; existing weather preferences stay selectable.
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
    val primary = Color(
        if (dark) {
            if (blue) 0xFFA9C5FF else 0xFFFF6B83
        } else {
            if (blue) 0xFF2563EB else 0xFFC91D40
        }
    )
    return ResonanceColorTokens(
        isDark = dark,
        Canvas = Color(if (dark) 0xFF0B0B0C else 0xFFF2F2F7),
        CanvasElevated = Color(if (dark) 0xFF1C1C1E else 0xFFFFFFFF),
        Raised = Color(if (dark) 0xFF2C2C2E else 0xFFFFFFFF),
        Soft = Color(if (dark) 0xFF2C2C2E else 0xFFF5F5F8),
        SurfaceSubtle = Color(if (dark) 0xFF2C2C2E else 0xFFF5F5F8),
        Glass = Color(if (dark) 0xB81C1C1E else 0xD9FFFFFF),
        GlassLight = Color(if (dark) 0xA62C2C2E else 0xC7FFFFFF),
        GlassUltra = Color(if (dark) 0xE61C1C1E else 0xEBFFFFFF),
        GlassBorder = Color(if (dark) 0x38FFFFFF else 0xDDFFFFFF),
        GlassBorderSubtle = Color(if (dark) 0x20FFFFFF else 0x90FFFFFF),
        GlassBorderGlow = primary.copy(alpha = 0.20f),
        Divider = Color(if (dark) 0x24FFFFFF else 0xFFD1D1D6),
        DividerStrong = Color(if (dark) 0x40FFFFFF else 0xFFB5B5BE),
        TextPrimary = Color(if (dark) 0xFFF5F5F7 else 0xFF1C1C1E),
        Muted = Color(if (dark) 0xFFB7B7BF else 0xFF55555D),
        Dim = Color(if (dark) 0xFF9999A2 else 0xFF55555D),
        Dimmer = Color(if (dark) 0xFF9999A2 else 0xFF55555D),
        Primary = primary,
        PrimaryGlow = primary,
        PrimarySoft = primary.copy(alpha = if (dark) 0.16f else 0.10f),
        Positive = Color(if (dark) 0xFF8CD0BC else 0xFF2F7467),
        PositiveGlow = primary,
        PositiveSoft = primary.copy(alpha = 0.12f),
        Notice = primary,
        Secondary = primary,
        SecondaryGlow = primary,
        SecondarySoft = primary.copy(alpha = 0.10f),
        Info = Color(if (dark) 0xFFA9C5FF else 0xFF365FA8),
        Shadow = Color(0xFF111318),
    )
}

val DarkResonanceColors = colors(true, WeatherPalette.Sage)
val LightResonanceColors = colors(false, WeatherPalette.Sage)
/** White-on-album player surface, independent of the library's light/dark preference. */
val ImmersivePlayerColors = DarkResonanceColors.copy(
    Canvas = Color(0xFF262126), TextPrimary = Color.White, Muted = Color(0xFFDED9DD),
    Dim = Color(0xFFDED9DD), Dimmer = Color(0xFFDED9DD), Primary = Color.White,
    Notice = Color.White, PrimarySoft = Color.White.copy(alpha = .12f),
    Secondary = Color.White, Divider = Color.White.copy(alpha = .18f),
)
val LocalGlassState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun <T> resonanceSpring(): FiniteAnimationSpec<T> = if (LocalReducedMotion.current) tween(0)
    else spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)

@Composable
fun motionDuration(duration: Int): Int = if (LocalReducedMotion.current) 0 else duration

/**
 * 界面切换规范动效：
 * Central timing for navigation, selection and press feedback.
 * Destination bounds stay fixed; only short offsets and opacity animate.
 */
object ResonanceMotionTokens {
    val ExitEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    const val PressDuration = 120
    const val StateDuration = 160
    const val TabDuration = 220
    const val ReducedDuration = 90
    // Shared by presentation, drag release and reversal; no bounce at the screen edges.
    val PlayerSpec: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow,
    )
    // Keep high-frequency page changes brief and interruptible, with no bounce.
    val PageEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    const val PageDuration = 280
    val PageTravel = 12.dp
    val PageSlideSpec: FiniteAnimationSpec<IntOffset> = tween(PageDuration, easing = PageEasing)

    val DetailSlideSpec: FiniteAnimationSpec<IntOffset> = PageSlideSpec

    val SelectionBounceSpec: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = Spring.StiffnessLow,
    )

    val PageSlideSpring: FiniteAnimationSpec<IntOffset> = PageSlideSpec

    val DetailSlideSpring: FiniteAnimationSpec<IntOffset> = DetailSlideSpec

    // 伴随滑动的柔和渐变：入场 280ms / 退场 180ms，杜绝提前消隐导致的黑色虚空割裂
    val PageFadeInSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = PageDuration,
        easing = PageEasing,
    )

    val PageFadeOutSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = PageDuration,
        easing = PageEasing,
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

/** Gallery mounts are nearly square; interactive controls retain gentler corners. */
object ResonanceShapes {
    val Button = RoundedCornerShape(12.dp)
    val ArtworkSmall = RoundedCornerShape(8.dp)
    val Artwork = RoundedCornerShape(14.dp)
    val ArtworkLarge = RoundedCornerShape(20.dp)
    val Panel = RoundedCornerShape(16.dp)
    val Card = RoundedCornerShape(16.dp)
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

/** Brief press feedback; keyboard navigation and reduced motion stay still. */
fun Modifier.resonancePressable(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
    restingScale: Float = 1f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val scale by animateFloatAsState(
        targetValue = if (LocalReducedMotion.current || keyboard) 1f else if (pressed) pressedScale.coerceIn(0.80f, 1f) else restingScale,
        animationSpec = tween(motionDuration(ResonanceMotionTokens.PressDuration), easing = ResonanceMotionTokens.ExitEasing),
        label = "resonancePressScale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Frosted material: sampled backdrop, a thin tint and a quiet illuminated edge. */
@Composable
fun Modifier.resonanceGlass(
    shape: Shape = ResonanceShapes.Panel,
    backgroundColor: Color = ResonanceColors.Glass,
    backgroundGradient: List<Color>? = null,
    borderColors: List<Color> = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
    borderWidth: Dp = 1.dp,
    shadowElevation: Dp = 2.dp,
    shadowColor: Color = ResonanceColors.Shadow.copy(alpha = if (ResonanceColors.isDark) 0.35f else 0.08f),
): Modifier {
    val glassState = LocalGlassState.current
    val gradient = backgroundGradient ?: listOf(
        backgroundColor.copy(alpha = (backgroundColor.alpha + .04f).coerceAtMost(.92f)),
        backgroundColor.copy(alpha = (backgroundColor.alpha - .08f).coerceAtLeast(.35f)),
    )

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
            blurRadius = 28.dp,
            noiseFactor = 0.012f,
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
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
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
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp),
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
    // Theme mode changes atomically; only weather palette changes interpolate.
    val colorTokens = key(isDark) {
        val accent by animateColorAsState(target.Primary, tween(if (reducedMotion) 0 else 260), label = "weatherAccent")
        val canvas by animateColorAsState(target.Canvas, tween(if (reducedMotion) 0 else 260), label = "weatherCanvas")
        target.copy(Primary = accent, PrimaryGlow = accent,
            PrimarySoft = accent.copy(alpha = if (isDark) 0.16f else 0.10f), Canvas = canvas)
    }

    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = colorTokens.Primary,
            onPrimary = colorTokens.Canvas,
            primaryContainer = colorTokens.PrimarySoft.compositeOver(colorTokens.CanvasElevated),
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
            outline = colorTokens.Dimmer,
            outlineVariant = colorTokens.Divider,
            inverseSurface = LightResonanceColors.CanvasElevated,
            inverseOnSurface = LightResonanceColors.TextPrimary,
            error = Color(0xFFFFB4AB),
        )
    } else {
        lightColorScheme(
            primary = colorTokens.Primary,
            onPrimary = Color.White,
            primaryContainer = colorTokens.PrimarySoft.compositeOver(colorTokens.CanvasElevated),
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
            outline = colorTokens.Dimmer,
            outlineVariant = colorTokens.Divider,
            error = Color(0xFFBA1A1A),
        )
    }

    CompositionLocalProvider(
        LocalResonanceColors provides colorTokens,
        LocalReducedMotion provides reducedMotion,
        androidx.compose.material3.LocalContentColor provides colorTokens.TextPrimary,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = resonanceTypography,
            shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)),
            content = content,
        )
    }
}
