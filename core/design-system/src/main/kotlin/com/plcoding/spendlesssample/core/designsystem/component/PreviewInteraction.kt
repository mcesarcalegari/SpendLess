package com.plcoding.spendlesssample.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Interaction states that parameters cannot reach (Constitution VIII).
 *
 * The preview renderer captures a single frame before any coroutine runs, so a state driven through
 * an `interactionSource` (the M3 ripple, or any collector) never appears in a rendered preview.
 * Previews therefore force the state through [LocalPreviewInteraction], and components draw the
 * Figma flat state layer for it. At runtime the local is `null` and the M3 ripple is used.
 */
internal enum class PreviewInteraction { Pressed, Focused }

/** Set only by design-system previews. Not reachable from feature modules. */
internal val LocalPreviewInteraction = staticCompositionLocalOf<PreviewInteraction?> { null }

/** Renders [content] as if its component were in [interaction]. Previews only. */
@Composable
internal fun WithPreviewInteraction(interaction: PreviewInteraction?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPreviewInteraction provides interaction, content = content)
}

/**
 * Draws the Figma flat state layer ([color] at [alpha], clipped to [shape] inset by [inset]) when a
 * preview forces [interaction]. Callers pass `LocalPreviewInteraction.current`, which is `null` at
 * runtime, so this is a no-op outside previews.
 */
internal fun Modifier.previewStateLayer(
    interaction: PreviewInteraction?,
    color: Color,
    shape: Shape,
    alpha: Float = 0.12f,
    inset: Dp = 0.dp,
): Modifier {
    if (interaction == null) return this
    return drawWithContent {
        drawContent()
        val insetPx = inset.toPx()
        val outlineSize = size.copy(width = size.width - 2 * insetPx, height = size.height - 2 * insetPx)
        val outline = shape.createOutline(outlineSize, layoutDirection, this)
        drawContext.transform.translate(insetPx, insetPx)
        drawOutline(outline, color.copy(alpha = alpha))
        drawContext.transform.translate(-insetPx, -insetPx)
    }
}
