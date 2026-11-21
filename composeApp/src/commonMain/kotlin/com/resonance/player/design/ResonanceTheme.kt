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
 * Clearwater / 清岚 native color roles: fresh misty porcelain (Light) and
 * deep lagoon twilight (Dark) paired with seafoam jade-mint or clear glacier sky.
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
    val Danger: Color = if (isDark) Color(0xFFFF8585) else Color(0xFFB92B2B),
    val DangerSoft: Color = Danger.copy(alpha = if (isDark) 0.16f else 0.10f),
)

private fun colors(dark: Boolean, palette: WeatherPalette): ResonanceColorTokens {
    val blue = palette == WeatherPalette.IceBlue
    val primary = Color(
        if (dark) {
            if (blue) 0xFF6EB6FF else 0xFF35D4B2
        } else {
            if (blue) 0xFF1A65C2 else 0xFF0B7D67
        }
    )
    val positive = Color(if (dark) 0xFF5CE0B8 else 0xFF167557)
    val danger = Color(if (dark) 0xFFFF8585 else 0xFFB92B2B)
    return ResonanceColorTokens(
        isDark = dark,
        Canvas = Color(if (dark) 0xFF0B1214 else 0xFFF3F7F6),
        CanvasElevated = Color(if (dark) 0xFF121C20 else 0xFFFFFFFF),
        Raised = Color(if (dark) 0xFF19262B else 0xFFFFFFFF),
        Soft = Color(if (dark) 0xFF203036 else 0xFFE7F0EE),
        SurfaceSubtle = Color(if (dark) 0xFF152126 else 0xFFEDF4F2),
        Glass = Color(if (dark) 0xCC121D21 else 0xEBFFFFFF),
        GlassLight = Color(if (dark) 0xB319272D else 0xD8FFFFFF),
        GlassUltra = Color(if (dark) 0xEB1A2930 else 0xFAFFFFFF),
        GlassBorder = Color(if (dark) 0x30D8F5EE else 0xF0FFFFFF),
        GlassBorderSubtle = Color(if (dark) 0x1AD8F5EE else 0x1A0F2E28),
        GlassBorderGlow = primary.copy(alpha = if (dark) 0.32f else 0.24f),
        Divider = Color(if (dark) 0x20FFFFFF else 0xFFDCE6E3),
        DividerStrong = Color(if (dark) 0x38FFFFFF else 0xFFBFCFCC),
        TextPrimary = Color(if (dark) 0xFFEFF7F6 else 0xFF12201E),
        Muted = Color(if (dark) 0xFF9EB6B3 else 0xFF475C59),
        Dim = Color(if (dark) 0xFF8CA6A3 else 0xFF516764),
        Dimmer = Color(if (dark) 0xFF829C99 else 0xFF59706C),
        Primary = primary,
        PrimaryGlow = primary,
        PrimarySoft = primary.copy(alpha = if (dark) 0.17f else 0.10f),
        Positive = positive,
        PositiveGlow = positive,
        PositiveSoft = positive.copy(alpha = if (dark) 0.16f else 0.10f),
        Notice = primary,
        Secondary = primary,
        SecondaryGlow = primary,
        SecondarySoft = primary.copy(alpha = 0.10f),
        Info = Color(if (dark) 0xFF79BEFF else 0xFF1E60A8),
        Shadow = Color(if (dark) 0xFF000000 else 0xFF0E2623),
        Danger = danger,
        DangerSoft = danger.copy(alpha = if (dark) 0.16f else 0.10f),
    )
}

val DarkResonanceColors = colors(true, WeatherPalette.Sage)
val LightResonanceColors = colors(false, WeatherPalette.Sage)

/** Album-lit ocean-ink stage palette for the immersive player, independent of host theme mode. */
val ImmersivePlayerColors = DarkResonanceColors.copy(
    Canvas = Color(0xFF0C1317),
    CanvasElevated = Color(0xFF141E24),
    Raised = Color(0xFF1B2830),
    Soft = Color(0xFF24343E),
    SurfaceSubtle = Color(0xFF152026),
    TextPrimary = Color(0xFFF5FAFA),
    Muted = Color(0xFFCFE0DF),
    Dim = Color(0xFFA9C0BF),
    Dimmer = Color(0xFF8CA4A3),
    Primary = Color(0xFFF5FAFA),
    PrimaryGlow = Color(0xFFF5FAFA),
    Notice = Color(0xFFF5FAFA),
    PrimarySoft = Color.White.copy(alpha = 0.14f),
    Secondary = Color(0xFFF5FAFA),
    Divider = Color.White.copy(alpha = 0.16f),
    DividerStrong = Color.White.copy(alpha = 0.28f),
    GlassBorder = Color.White.copy(alpha = 0.24f),
    GlassBorderSubtle = Color.White.copy(alpha = 0.11f),
    GlassBorderGlow = Color.White.copy(alpha = 0.24f),
)

/** Material3 dark scheme paired directly with [ImmersivePlayerColors] as a single source of truth. */
val ImmersivePlayerColorScheme: ColorScheme = darkColorScheme(
    primary = ImmersivePlayerColors.Primary,
    onPrimary = Color(0xFF0D171B),
    secondary = ImmersivePlayerColors.Secondary,
    primaryContainer = Color.White.copy(alpha = 0.15f),
    onPrimaryContainer = ImmersivePlayerColors.TextPrimary,
    surface = ImmersivePlayerColors.Canvas,
    onSurface = ImmersivePlayerColors.TextPrimary,
    onSurfaceVariant = ImmersivePlayerColors.Muted,
    background = ImmersivePlayerColors.Canvas,
    onBackground = ImmersivePlayerColors.TextPrimary,
    surfaceContainerLowest = Color(0xFF080E11),
    surfaceContainerLow = ImmersivePlayerColors.CanvasElevated,
    surfaceContainer = ImmersivePlayerColors.Raised,
    surfaceContainerHigh = ImmersivePlayerColors.Soft,
    surfaceContainerHighest = Color(0xFF2D404B),
    error = ImmersivePlayerColors.Danger,
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
    // Apple-inspired fluid spring for interactive pills, lyric focus scale & breathing artwork.
    val AppleFluidSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.78f, stiffness = 360f,
    )
    // Crisp tactile spring bounce for Play/Pause, favorite heart & button release.
    val AppleBouncySpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.56f, stiffness = 460f,
    )
    // Silky damped spring for Apple Music-style lyric scrolling (settles cleanly inside 450ms).
    val LyricScrollSpec: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.86f, stiffness = 220f,
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
    val Danger: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Danger
    val DangerSoft: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.DangerSoft
}

/** Atelier Acoustics shape scale: crisp archival mounts and refined tactile controls. */
object ResonanceShapes {
    val Button = RoundedCornerShape(12.dp)
    val Field = RoundedCornerShape(12.dp)
    val ArtworkSmall = RoundedCornerShape(8.dp)
    val Artwork = RoundedCornerShape(14.dp)
    val ArtworkLarge = RoundedCornerShape(18.dp)
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

/** Crisp Apple-style tactile spring feedback; keyboard navigation and reduced motion stay still. */
fun Modifier.resonancePressable(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
    restingScale: Float = 1f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val reduced = LocalReducedMotion.current || keyboard
    val scale by animateFloatAsState(
        targetValue = if (reduced) 1f else if (pressed) pressedScale.coerceIn(0.80f, 1f) else restingScale,
        animationSpec = if (reduced) tween(0) else spring(dampingRatio = 0.64f, stiffness = 480f),
        label = "resonancePressScale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Architectural tactile card: elevated warm surface with a quiet hairline outline. */
@Composable
fun Modifier.atelierCard(
    shape: Shape = ResonanceShapes.Card,
    backgroundColor: Color = ResonanceColors.Raised,
    borderColor: Color = ResonanceColors.Divider,
    shadowElevation: Dp = if (ResonanceColors.isDark) 0.dp else 1.dp,
): Modifier {
    val shadowTint = ResonanceColors.Shadow.copy(alpha = if (ResonanceColors.isDark) 0.30f else 0.06f)
    return this
        .then(
            if (shadowElevation > 0.dp) {
                Modifier.shadow(shadowElevation, shape, spotColor = shadowTint, ambientColor = shadowTint)
            } else Modifier
        )
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, borderColor, shape)
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
    shadowColor: Color = ResonanceColors.Shadow.copy(alpha = if (ResonanceColors.isDark) 0.36f else 0.09f),
): Modifier {
    val glassState = LocalGlassState.current
    val gradient = backgroundGradient ?: listOf(
        backgroundColor.copy(alpha = (backgroundColor.alpha + .04f).coerceAtMost(.95f)),
        backgroundColor.copy(alpha = (backgroundColor.alpha - .06f).coerceAtLeast(.45f)),
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
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.6).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.45).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.15).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 21.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.5.sp,
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
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp,
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
        target.copy(
            Primary = accent,
            PrimaryGlow = accent,
            Notice = accent,
            Secondary = accent,
            SecondaryGlow = accent,
            PrimarySoft = accent.copy(alpha = if (isDark) 0.16f else 0.10f),
            GlassBorderGlow = accent.copy(alpha = if (isDark) 0.30f else 0.24f),
            Canvas = canvas,
        )
    }

    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = colorTokens.Primary,
            onPrimary = Color(0xFF071B17),
            primaryContainer = colorTokens.PrimarySoft.compositeOver(colorTokens.CanvasElevated),
            onPrimaryContainer = colorTokens.TextPrimary,
            secondary = colorTokens.Positive,
            onSecondary = colorTokens.Canvas,
            secondaryContainer = colorTokens.Soft,
            onSecondaryContainer = colorTokens.TextPrimary,
            tertiary = colorTokens.Info,
            onTertiary = colorTokens.Canvas,
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
            error = colorTokens.Danger,
        )
    } else {
        lightColorScheme(
            primary = colorTokens.Primary,
            onPrimary = Color.White,
            primaryContainer = colorTokens.PrimarySoft.compositeOver(colorTokens.CanvasElevated),
            onPrimaryContainer = colorTokens.TextPrimary,
            secondary = colorTokens.Positive,
            onSecondary = Color.White,
            secondaryContainer = colorTokens.Soft,
            onSecondaryContainer = colorTokens.TextPrimary,
            tertiary = colorTokens.Info,
            onTertiary = Color.White,
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
            error = colorTokens.Danger,
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
