package com.resonance.player.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

object ResonanceColors {
    val Canvas = Color(0xFF07090E)
    val CanvasElevated = Color(0xFF0C1017)
    val Raised = Color(0xFF111622)
    val Soft = Color(0xFF161D2C)
    val SurfaceSubtle = Color(0xFF1B2335)
    val Glass = Color(0xD9151C28)
    val GlassLight = Color(0x99202A3D)
    val GlassUltra = Color(0x66182130)
    
    val GlassBorder = Color(0x33FFFFFF)
    val GlassBorderSubtle = Color(0x1AFFFFFF)
    val GlassBorderGlow = Color(0x4DFF715B)
    val Divider = Color(0xFF232D40)
    val DividerStrong = Color(0xFF33415C)
    
    val Ivory = Color(0xFFF9F6F0)
    val Muted = Color(0xFFB0BACB)
    val Dim = Color(0xFF75839C)
    val Dimmer = Color(0xFF4C5870)
    
    val Coral = Color(0xFFFF715B)
    val CoralGlow = Color(0xFFFFA796)
    val CoralSoft = Color(0xFF381C23)
    val Mint = Color(0xFF5CE5B8)
    val MintSoft = Color(0xFF16382D)
    val Amber = Color(0xFFFFBA52)
    val Violet = Color(0xFF8F80FF)
    val VioletSoft = Color(0xFF262046)
    val Azure = Color(0xFF56CCF2)
    val Shadow = Color(0xFF020306)
}

/**
 * Emil Kowalski's Spring-physics pressable modifier for instantaneous (zero-delay)
 * and tactile tactile active interaction feedback.
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

private val resonanceColorScheme: ColorScheme = darkColorScheme(
    primary = ResonanceColors.Coral,
    onPrimary = Color(0xFF2B0C08),
    primaryContainer = ResonanceColors.CoralSoft,
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = ResonanceColors.Mint,
    background = ResonanceColors.Canvas,
    onBackground = ResonanceColors.Ivory,
    surface = ResonanceColors.Raised,
    onSurface = ResonanceColors.Ivory,
    surfaceVariant = ResonanceColors.Soft,
    onSurfaceVariant = ResonanceColors.Muted,
    outline = ResonanceColors.Divider,
    outlineVariant = ResonanceColors.DividerStrong,
    error = Color(0xFFFFB4AB),
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
fun ResonanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = resonanceColorScheme,
        typography = resonanceTypography,
        content = content,
    )
}
