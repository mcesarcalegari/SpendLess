package com.plcoding.spendlesssample.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.dropShadow

/**
 * Tint shared by every Figma drop shadow. It is not a Figma variable, so it stays internal to the
 * design system (research R4).
 */
internal val ShadowTint = Color(0xFF180040)

/** A Figma drop shadow: offset, blur, spread, and the [ShadowTint] alpha. */
@Immutable
data class SpendLessShadow(
    val offsetX: Dp,
    val offsetY: Dp,
    val blur: Dp,
    val spread: Dp,
    val alpha: Float,
)

/** The three drop-shadow tokens used in Figma (research R4). Elevation is never used for looks. */
object SpendLessShadows {
    /** Select anchor and Select-list (`2415:10171`, `2415:10118`): 0/6, blur 10, spread 0, 8%. */
    val select = SpendLessShadow(offsetX = 0.dp, offsetY = 6.dp, blur = 10.dp, spread = 0.dp, alpha = 0.08f)

    /** Note badge and expanded row card (`423:4883`, `2414:5065`): 0/8, blur 12, spread 0, 10%. */
    val raised = SpendLessShadow(offsetX = 0.dp, offsetY = 8.dp, blur = 12.dp, spread = 0.dp, alpha = 0.10f)

    /** FAB (`410:2338`): 0/8, blur 24, spread 4, 16%. */
    val fab = SpendLessShadow(offsetX = 0.dp, offsetY = 8.dp, blur = 24.dp, spread = 4.dp, alpha = 0.16f)
}

/** Draws [token] behind the content, outside [shape]. */
fun Modifier.spendLessShadow(token: SpendLessShadow, shape: Shape): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(
        radius = token.blur,
        color = ShadowTint,
        spread = token.spread,
        offset = DpOffset(token.offsetX, token.offsetY),
        alpha = token.alpha,
    ),
)
