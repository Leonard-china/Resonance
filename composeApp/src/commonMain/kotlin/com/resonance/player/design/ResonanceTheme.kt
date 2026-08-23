package com.resonance.player.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
    val Ivory: Color,
    val Muted: Color,
    val Dim: Color,
    val Dimmer: Color,
    val Coral: Color,
    val CoralGlow: Color,
    val CoralSoft: Color,
    val Mint: Color,
    val MintGlow: Color,
    val MintSoft: Color,
    val Amber: Color,
    val Violet: Color,
    val VioletGlow: Color,
    val VioletSoft: Color,
    val Azure: Color,
    val Shadow: Color,
)

val DarkResonanceColors = ResonanceColorTokens(
    isDark = true,
    Canvas = Color(0xFF06080D),
    CanvasElevated = Color(0xFF0B1019),
    Raised = Color(0xFF101622),
    Soft = Color(0xFF161E2E),
    SurfaceSubtle = Color(0xFF1C263A),
    Glass = Color(0xB8121826),
    GlassLight = Color(0x701D283E),
    GlassUltra = Color(0x40162032),
    GlassBorder = Color(0x40FFFFFF),
    GlassBorderSubtle = Color(0x20FFFFFF),
    GlassBorderGlow = Color(0x66FF715B),
    Divider = Color(0xFF202A3C),
    DividerStrong = Color(0xFF32415B),
    Ivory = Color(0xFFF9F6F0),
    Muted = Color(0xFFBAC5D8),
    Dim = Color(0xFF7B8BA6),
    Dimmer = Color(0xFF50607A),
    Coral = Color(0xFFFF6B55),
    CoralGlow = Color(0xFFFFA090),
    CoralSoft = Color(0xFF3D1E24),
    Mint = Color(0xFF4EE3B0),
    MintGlow = Color(0xFF88F2CE),
    MintSoft = Color(0xFF14382E),
    Amber = Color(0xFFFFB84D),
    Violet = Color(0xFF9885FF),
    VioletGlow = Color(0xFFBFB2FF),
    VioletSoft = Color(0xFF28204E),
    Azure = Color(0xFF4AC4F3),
    Shadow = Color(0xFF020306),
)

val LightResonanceColors = ResonanceColorTokens(
    isDark = false,
    Canvas = Color(0xFFF3F5FA),
    CanvasElevated = Color(0xFFFFFFFF),
    Raised = Color(0xFFFFFFFF),
    Soft = Color(0xFFE6EDF7),
    SurfaceSubtle = Color(0xFFDCE5F2),
    Glass = Color(0xD8FFFFFF),
    GlassLight = Color(0x99FFFFFF),
    GlassUltra = Color(0x60FFFFFF),
    GlassBorder = Color(0x25000000),
    GlassBorderSubtle = Color(0x10000000),
    GlassBorderGlow = Color(0x40FF5E48),
    Divider = Color(0xFFD8E1ED),
    DividerStrong = Color(0xFFC0CDDF),
    Ivory = Color(0xFF111827),
    Muted = Color(0xFF4B5B74),
    Dim = Color(0xFF71829B),
    Dimmer = Color(0xFF99A9BF),
    Coral = Color(0xFFFF5238),
    CoralGlow = Color(0xFFFF6E56),
    CoralSoft = Color(0xFFFFECE8),
    Mint = Color(0xFF0FB883),
    MintGlow = Color(0xFF1FCF95),
    MintSoft = Color(0xFFE0F8EF),
    Amber = Color(0xFFE68A00),
    Violet = Color(0xFF735BF2),
    VioletGlow = Color(0xFF8B75FF),
    VioletSoft = Color(0xFFEDE8FE),
    Azure = Color(0xFF0EA5E9),
    Shadow = Color(0x18101E36),
)

val LocalResonanceColors = staticCompositionLocalOf { DarkResonanceColors }

object ResonanceColors {
    val isDark: Boolean @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.isDark
    val Canvas: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Canvas
    val CanvasElevated: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.CanvasElevated
    val Raised: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Raised
    val Soft: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Soft
    val SurfaceSubtle: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.SurfaceSubtle
    val Glass: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Glass
    val GlassLight: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.GlassLight
    val GlassUltra: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.GlassUltra
    val GlassBorder: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.GlassBorder
    val GlassBorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.GlassBorderSubtle
    val GlassBorderGlow: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.GlassBorderGlow
    val Divider: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Divider
    val DividerStrong: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.DividerStrong
    val Ivory: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Ivory
    val Muted: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Muted
    val Dim: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Dim
    val Dimmer: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Dimmer
    val Coral: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Coral
    val CoralGlow: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.CoralGlow
    val CoralSoft: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.CoralSoft
    val Mint: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Mint
    val MintGlow: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.MintGlow
    val MintSoft: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.MintSoft
    val Amber: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Amber
    val Violet: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Violet
    val VioletGlow: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.VioletGlow
    val VioletSoft: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.VioletSoft
    val Azure: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Azure
    val Shadow: Color @Composable @ReadOnlyComposable get() = LocalResonanceColors.current.Shadow
}

/**
 * Emil Kowalski's Spring-physics pressable modifier for instantaneous (zero-delay)
 * and tactile active interaction feedback.
 */
fun Modifier.resonancePressable(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
    restingScale: Float = 1f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else restingScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "resonancePressScale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Reusable Glassmorphism Panel with luminous subtle gradient border and soft shadow.
 */
@Composable
fun Modifier.resonanceGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = ResonanceColors.Glass,
    borderColors: List<Color> = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
    shadowElevation: Dp = 8.dp,
    shadowColor: Color = ResonanceColors.Shadow,
): Modifier = this
    .shadow(shadowElevation, shape, ambientColor = shadowColor.copy(alpha = 0.35f), spotColor = shadowColor)
    .clip(shape)
    .background(backgroundColor)
    .border(
        BorderStroke(1.dp, Brush.linearGradient(borderColors)),
        shape,
    )

private val resonanceTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.4).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
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
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
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
)

@Composable
fun ResonanceTheme(
    themeMode: ThemeMode = ThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemDark
    }

    val colorTokens = if (isDark) DarkResonanceColors else LightResonanceColors

    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = colorTokens.Coral,
            onPrimary = Color(0xFF2B0C08),
            primaryContainer = colorTokens.CoralSoft,
            onPrimaryContainer = Color(0xFFFFDAD3),
            secondary = colorTokens.Mint,
            background = colorTokens.Canvas,
            onBackground = colorTokens.Ivory,
            surface = colorTokens.Raised,
            onSurface = colorTokens.Ivory,
            surfaceVariant = colorTokens.Soft,
            onSurfaceVariant = colorTokens.Muted,
            outline = colorTokens.Divider,
            outlineVariant = colorTokens.DividerStrong,
            error = Color(0xFFFFB4AB),
        )
    } else {
        lightColorScheme(
            primary = colorTokens.Coral,
            onPrimary = Color.White,
            primaryContainer = colorTokens.CoralSoft,
            onPrimaryContainer = Color(0xFF3F0400),
            secondary = colorTokens.Mint,
            background = colorTokens.Canvas,
            onBackground = colorTokens.Ivory,
            surface = colorTokens.Raised,
            onSurface = colorTokens.Ivory,
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

