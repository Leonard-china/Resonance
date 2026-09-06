package com.resonance.player.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
 * Resonance Design System — Minimal / Music-first / Hi-Fi.
 *
 * 克制、高信息密度、封面优先。页面背景为纯色（浅 #FAFAFA / 深 #101010），
 * 珊瑚红仅作强调色。不允许大面积渐变、厚阴影与玻璃拟态。
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
    Canvas = Color(0xFF101010),
    CanvasElevated = Color(0xFF161616),
    Raised = Color(0xFF1B1B1B),
    Soft = Color(0xFF242424),
    SurfaceSubtle = Color(0xFF2E2E2E),
    Glass = Color(0xFF1A1A1A),
    GlassLight = Color(0xFF222222),
    GlassUltra = Color(0xFF2A2A2A),
    GlassBorder = Color(0x1FFFFFFF),
    GlassBorderSubtle = Color(0x12FFFFFF),
    GlassBorderGlow = Color(0x66FF6B55),
    Divider = Color(0x1FFFFFFF),
    DividerStrong = Color(0x33FFFFFF),
    Ivory = Color(0xFFF5F5F5),
    Muted = Color(0xFFA6A6A6),
    Dim = Color(0xFF757575),
    Dimmer = Color(0xFF545454),
    Coral = Color(0xFFFF6B55),
    CoralGlow = Color(0xFFFF8A76),
    CoralSoft = Color(0x2EFF6B55),
    Mint = Color(0xFF4EE3B0),
    MintGlow = Color(0xFF88F2CE),
    MintSoft = Color(0x2E4EE3B0),
    Amber = Color(0xFFFFB84D),
    Violet = Color(0xFF9885FF),
    VioletGlow = Color(0xFFBFB2FF),
    VioletSoft = Color(0x2E9885FF),
    Azure = Color(0xFF4AC4F3),
    Shadow = Color(0xFF000000),
)

val LightResonanceColors = ResonanceColorTokens(
    isDark = false,
    Canvas = Color(0xFFFAFAFA),
    CanvasElevated = Color(0xFFFFFFFF),
    Raised = Color(0xFFFFFFFF),
    Soft = Color(0xFFF0F0F0),
    SurfaceSubtle = Color(0xFFE6E6E6),
    Glass = Color(0xFFFFFFFF),
    GlassLight = Color(0xFFF4F4F4),
    GlassUltra = Color(0xFFECECEC),
    GlassBorder = Color(0x14000000),
    GlassBorderSubtle = Color(0x0D000000),
    GlassBorderGlow = Color(0x40FF5238),
    Divider = Color(0x14000000),
    DividerStrong = Color(0x24000000),
    Ivory = Color(0xFF1A1A1A),
    Muted = Color(0xFF5F5F5F),
    Dim = Color(0xFF8A8A8A),
    Dimmer = Color(0xFFB4B4B4),
    Coral = Color(0xFFFF5238),
    CoralGlow = Color(0xFFFF5238),
    CoralSoft = Color(0x1AFF5238),
    Mint = Color(0xFF0FB883),
    MintGlow = Color(0xFF0FB883),
    MintSoft = Color(0x1A0FB883),
    Amber = Color(0xFFE68A00),
    Violet = Color(0xFF735BF2),
    VioletGlow = Color(0xFF735BF2),
    VioletSoft = Color(0x1A735BF2),
    Azure = Color(0xFF0EA5E9),
    Shadow = Color(0xFF000000),
)

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

/** 统一圆角尺度 —— 克制使用。 */
object ResonanceShapes {
    val Button = RoundedCornerShape(12.dp)
    val ArtworkSmall = RoundedCornerShape(8.dp)
    val Artwork = RoundedCornerShape(12.dp)
    val ArtworkLarge = RoundedCornerShape(20.dp)
    val Panel = RoundedCornerShape(14.dp)
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

/** 按压反馈：轻微的即时缩放。 */
fun Modifier.resonancePressable(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
    restingScale: Float = 1f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else restingScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
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
 * 平面面板（原玻璃拟态的替代品）：纯色背景 + 细分隔描边，无阴影、无渐变。
 * 保留原函数签名，所有既有调用点自动变为扁平风格。
 */
@Composable
fun Modifier.resonanceGlass(
    shape: Shape = ResonanceShapes.Panel,
    backgroundColor: Color = ResonanceColors.Glass,
    borderColors: List<Color> = listOf(ResonanceColors.GlassBorder, ResonanceColors.GlassBorderSubtle),
    shadowElevation: Dp = 0.dp,
    shadowColor: Color = ResonanceColors.Shadow,
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(Dp.Hairline, borderColors.first(), shape)

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
