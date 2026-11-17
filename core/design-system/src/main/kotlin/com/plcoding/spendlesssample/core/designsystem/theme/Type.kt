package com.plcoding.spendlesssample.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.plcoding.spendlesssample.core.designsystem.R

private val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
)

/**
 * Every style starts from the M3 default of its slot so it keeps M3's `LineHeightStyle(Center,
 * Trim.None)`: a single line then takes its full Figma line height (16sp text in a 24dp line box).
 * A bare `TextStyle` falls back to Compose's trimming default and renders shorter lines.
 */
private val DefaultTypography = Typography()

private fun TextStyle.withFigtree(): TextStyle = copy(fontFamily = Figtree)

val SpendLessTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displayMedium = DefaultTypography.displayMedium.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = DefaultTypography.displaySmall.withFigtree(),
    headlineLarge = DefaultTypography.headlineLarge.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.1.sp,
    ),
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.1.sp,
    ),
    headlineSmall = DefaultTypography.headlineSmall.withFigtree(),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = DefaultTypography.titleSmall.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = DefaultTypography.bodyLarge.withFigtree(),
    bodyMedium = DefaultTypography.bodyMedium.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = DefaultTypography.bodySmall.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = DefaultTypography.labelLarge.withFigtree(),
    labelMedium = DefaultTypography.labelMedium.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = DefaultTypography.labelSmall.copy(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
)

internal val BodyXSmall = DefaultTypography.bodySmall.copy(
    fontFamily = Figtree,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp,
)

internal val LocalBodyXSmall = staticCompositionLocalOf { BodyXSmall }

val Typography.bodyXSmall: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = LocalBodyXSmall.current
