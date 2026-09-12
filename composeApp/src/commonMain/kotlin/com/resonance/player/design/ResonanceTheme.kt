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
 * Resonance Design System — 现代声学磨砂玻璃 & 水润流体动效 (Acoustic Glassmorphism & Juicy Motion).
 *
 * 界面呈现通透光学折射感、双重物理高光倒角描边、微光环境阴影与带有生命力呼吸律动的果冻级弹性物理动效。
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
    Canvas = Color(0xFF0C0D14),
    CanvasElevated = Color(0xFF131522),
    Raised = Color(0xFF1B1E30),
    Soft = Color(0x33282E47),
    SurfaceSubtle = Color(0x38333D5E),
    Glass = Color(0x401D2136),
    GlassLight = Color(0x59272D4A),
    GlassUltra = Color(0x73323B61),
    GlassBorder = Color(0x47FFFFFF),
    GlassBorderSubtle = Color(0x1AFFFFFF),
    GlassBorderGlow = Color(0x73FF6B55),
    Divider = Color(0x1FFFFFFF),
    DividerStrong = Color(0x33FFFFFF),
    Ivory = Color(0xFFF6F8FC),
    Muted = Color(0xFFAAB2C8),
    Dim = Color(0xFF78829C),
    Dimmer = Color(0xFF555D74),
    Coral = Color(0xFFFF6B55),
    CoralGlow = Color(0xFFFF8E7C),
    CoralSoft = Color(0x33FF6B55),
    Mint = Color(0xFF4EE3B0),
    MintGlow = Color(0xFF88F2CE),
    MintSoft = Color(0x334EE3B0),
    Amber = Color(0xFFFFB84D),
    Violet = Color(0xFF9E8DFF),
    VioletGlow = Color(0xFFC3B8FF),
    VioletSoft = Color(0x339E8DFF),
    Azure = Color(0xFF38BDF8),
    Shadow = Color(0xFF000000),
)

val LightResonanceColors = ResonanceColorTokens(
    isDark = false,
    Canvas = Color(0xFFF3F5FA),
    CanvasElevated = Color(0xFFFFFFFF),
    Raised = Color(0xFFFFFFFF),
    Soft = Color(0xFFE8EDF7),
    SurfaceSubtle = Color(0xFFDEE5F5),
    Glass = Color(0xD9FFFFFF),
    GlassLight = Color(0xE6FFFFFF),
    GlassUltra = Color(0xF2FFFFFF),
    GlassBorder = Color(0x66FFFFFF),
    GlassBorderSubtle = Color(0x14000000),
    GlassBorderGlow = Color(0x4DFF5238),
    Divider = Color(0x14000000),
    DividerStrong = Color(0x24000000),
    Ivory = Color(0xFF111420),
    Muted = Color(0xFF535B70),
    Dim = Color(0xFF7E879E),
    Dimmer = Color(0xFFA8B2C8),
    Coral = Color(0xFFFF5238),
    CoralGlow = Color(0xFFFF6E57),
    CoralSoft = Color(0x1FFF5238),
    Mint = Color(0xFF0FB883),
    MintGlow = Color(0xFF24D49D),
    MintSoft = Color(0x1F0FB883),
    Amber = Color(0xFFE68A00),
    Violet = Color(0xFF735BF2),
    VioletGlow = Color(0xFF8F7BFA),
    VioletSoft = Color(0x1F735BF2),
    Azure = Color(0xFF0284C7),
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
    pressedScale: Float = 0.94f,
    restingScale: Float = 1f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else restingScale,
        animationSpec = spring(
            dampingRatio = 0.65f, // 果冻级弹性阻尼，松手过冲回弹
            stiffness = Spring.StiffnessMediumLow,
        ),
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
    shadowElevation: Dp = 10.dp,
    shadowColor: Color = ResonanceColors.Shadow.copy(alpha = if (ResonanceColors.isDark) 0.35f else 0.08f),
): Modifier {
    val gradient = backgroundGradient ?: if (ResonanceColors.isDark) {
        listOf(
            backgroundColor.copy(alpha = (backgroundColor.alpha * 1.22f).coerceAtMost(0.95f)),
            backgroundColor.copy(alpha = (backgroundColor.alpha * 0.72f).coerceAtLeast(0.12f)),
        )
    } else {
        listOf(
            backgroundColor.copy(alpha = 0.95f),
            backgroundColor.copy(alpha = 0.78f),
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
        .background(Brush.verticalGradient(gradient))
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
