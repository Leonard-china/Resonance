package com.resonance.player.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ResonanceColors {
    val Canvas = Color(0xFF0D1117)
    val Raised = Color(0xFF151B23)
    val Soft = Color(0xFF1C2430)
    val Divider = Color(0xFF29313D)
    val Ivory = Color(0xFFF7F2E8)
    val Muted = Color(0xFFA9B1BD)
    val Dim = Color(0xFF747E8D)
    val Coral = Color(0xFFFF735C)
    val CoralSoft = Color(0xFF38201F)
    val Mint = Color(0xFF5FD19B)
    val Amber = Color(0xFFF2BD5B)
}

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
