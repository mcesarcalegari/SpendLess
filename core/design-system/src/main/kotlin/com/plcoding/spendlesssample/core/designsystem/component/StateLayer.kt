package com.plcoding.spendlesssample.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** Figma state-layer alphas: pressed and focused 12%, hovered 8% (research R3). */
internal val SpendLessRippleAlpha = RippleAlpha(
    pressedAlpha = 0.12f,
    focusedAlpha = 0.12f,
    draggedAlpha = 0.12f,
    hoveredAlpha = 0.08f,
)

/** Segmented-button alphas: the Figma state layer is 8% in every state (research R3). */
internal val SpendLessSegmentRippleAlpha = RippleAlpha(
    pressedAlpha = 0.08f,
    focusedAlpha = 0.08f,
    draggedAlpha = 0.08f,
    hoveredAlpha = 0.08f,
)

/**
 * Provides the Figma flat state layer in [color] to M3 components inside [content]. The colour is
 * the opaque role; [alpha] applies the Figma opacity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProvideStateLayer(
    color: Color,
    alpha: RippleAlpha = SpendLessRippleAlpha,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = color, rippleAlpha = alpha),
        content = content,
    )
}
