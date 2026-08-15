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
    val Canvas = Color(0xFF080A0F)
    val Raised = Color(0xFF10141D)
    val Soft = Color(0xFF171D29)
    val Glass = Color(0xE61A202C)
    val Divider = Color(0xFF2A3344)
    val DividerStrong = Color(0xFF3A465D)
    val Ivory = Color(0xFFF9F5EE)
    val Muted = Color(0xFFAEB7C7)
    val Dim = Color(0xFF788397)
    val Coral = Color(0xFFFF715B)
    val CoralGlow = Color(0xFFFFA08F)
    val CoralSoft = Color(0xFF351B21)
    val Mint = Color(0xFF67DBB3)
    val Amber = Color(0xFFF3BD63)
    val Violet = Color(0xFF8A7CFF)
    val Azure = Color(0xFF61C8FF)
    val Shadow = Color(0xFF020308)
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
