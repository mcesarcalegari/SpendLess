package com.plcoding.spendlesssample.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme

/** Icon button styles from the Figma `Style` property. */
enum class SpendLessIconButtonStyle { Filled, Standard, Error, OnPrimary }

private val IconButtonShape = RoundedCornerShape(16.dp)
private val IconButtonOuterSize = 48.dp
private val IconButtonInset = 2.dp
private val IconButtonIconSize = 24.dp
private const val DisabledAlpha = 0.38f

/**
 * The SpendLess icon button: a 48dp box with 2dp padding around a 44dp, radius-16 container and a
 * 24dp icon. Built on M3 [FilledIconButton] (Filled, Error, On Primary) and [IconButton] (Standard).
 *
 * Error and On Primary have no disabled variant in Figma, so [enabled] only blocks clicks for them.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3390
 * Figma (Filled, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3447
 * Figma (Filled, Pressed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3435
 * Figma (Filled, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3439
 * Figma (Filled, Disabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3431
 * Figma (Standard, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3467
 * Figma (Standard, Pressed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3455
 * Figma (Standard, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3459
 * Figma (Standard, Disabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3451
 * Figma (Error, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3427
 * Figma (Error, Pressed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2416-30521
 * Figma (Error, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2416-30505
 * Figma (On Primary, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2416-30246
 * Figma (On Primary, Pressed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2416-30234
 * Figma (On Primary, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2416-30527
 */
@Composable
fun SpendLessIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SpendLessIconButtonStyle = SpendLessIconButtonStyle.Standard,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MaterialTheme.colorScheme
    val sized = modifier.size(IconButtonOuterSize).padding(IconButtonInset)
    val iconContent: @Composable () -> Unit = {
        Icon(painter = icon, contentDescription = contentDescription, modifier = Modifier.size(IconButtonIconSize))
    }

    when (style) {
        SpendLessIconButtonStyle.Standard -> ProvideStateLayer(color = colors.onSurfaceVariant) {
            IconButton(
                onClick = onClick,
                // Figma Standard Disabled: the whole button at 38% with an onSurface icon (research R15 D1).
                modifier = sized
                    .previewStateLayer(LocalPreviewInteraction.current, colors.onSurfaceVariant, IconButtonShape)
                    .alpha(if (enabled) 1f else DisabledAlpha),
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = colors.onSurfaceVariant,
                    disabledContentColor = colors.onSurface,
                ),
                interactionSource = interactionSource,
                shape = IconButtonShape,
                content = iconContent,
            )
        }
        else -> {
            val container = when (style) {
                SpendLessIconButtonStyle.Filled -> colors.primary
                SpendLessIconButtonStyle.Error -> colors.error.copy(alpha = 0.08f)
                else -> colors.onPrimary.copy(alpha = 0.12f)
            }
            val content = when (style) {
                SpendLessIconButtonStyle.Filled -> colors.onPrimary
                SpendLessIconButtonStyle.Error -> colors.error
                else -> colors.onPrimary
            }
            val stateLayer = when (style) {
                SpendLessIconButtonStyle.Filled -> colors.onPrimary
                SpendLessIconButtonStyle.Error -> colors.error
                else -> colors.onPrimary
            }
            val isFilled = style == SpendLessIconButtonStyle.Filled
            ProvideStateLayer(color = stateLayer) {
                FilledIconButton(
                    onClick = onClick,
                    modifier = sized.previewStateLayer(LocalPreviewInteraction.current, stateLayer, IconButtonShape),
                    enabled = enabled,
                    shape = IconButtonShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = container,
                        contentColor = content,
                        disabledContainerColor = if (isFilled) colors.onSurface.copy(alpha = 0.12f) else container,
                        disabledContentColor = if (isFilled) colors.onSurface.copy(alpha = DisabledAlpha) else content,
                    ),
                    interactionSource = interactionSource,
                    content = iconContent,
                )
            }
        }
    }
}

// region Previews

@Composable
private fun IconButtonPreview(
    style: SpendLessIconButtonStyle,
    enabled: Boolean = true,
    interaction: PreviewInteraction? = null,
) {
    SpendLessTheme {
        val onPrimaryBackdrop = style == SpendLessIconButtonStyle.OnPrimary
        androidx.compose.foundation.layout.Box(
            modifier = if (onPrimaryBackdrop) {
                Modifier.background(MaterialTheme.colorScheme.primary)
            } else {
                Modifier
            },
        ) {
            WithPreviewInteraction(interaction) {
                SpendLessIconButton(
                    icon = SpendLessIcons.Add,
                    contentDescription = "Add",
                    onClick = {},
                    style = style,
                    enabled = enabled,
                )
            }
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3447 */
@Preview
@Composable
private fun SpendLessIconButtonFilledEnabledPreview() = IconButtonPreview(SpendLessIconButtonStyle.Filled)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3435 */
@Preview
@Composable
private fun SpendLessIconButtonFilledPressedPreview() =
    IconButtonPreview(SpendLessIconButtonStyle.Filled, interaction = PreviewInteraction.Pressed)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3439 */
@Preview
@Composable
private fun SpendLessIconButtonFilledFocusedPreview() =
    IconButtonPreview(SpendLessIconButtonStyle.Filled, interaction = PreviewInteraction.Focused)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3431 */
@Preview
@Composable
private fun SpendLessIconButtonFilledDisabledPreview() =
    IconButtonPreview(SpendLessIconButtonStyle.Filled, enabled = false)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3467 */
@Preview
@Composable
private fun SpendLessIconButtonStandardEnabledPreview() = IconButtonPreview(SpendLessIconButtonStyle.Standard)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3455 */
@Preview
@Composable
private fun SpendLessIconButtonStandardPressedPreview() =
    IconButtonPreview(SpendLessIconButtonStyle.Standard, interaction = PreviewInteraction.Pressed)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3459 */
@Preview
@Composable
private fun SpendLessIconButtonStandardFocusedPreview() =
    IconButtonPreview(SpendLessIconButtonStyle.Standard, interaction = PreviewInteraction.Focused)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3451 */
@Preview
@Composable
private fun SpendLessIconButtonStandardDisabledPreview() =
    IconButtonPreview(SpendLessIconButtonStyle.Standard, enabled = false)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-3427 */
@Preview
@Composable
private fun SpendLessIconButtonErrorEnabledPreview() = IconButtonPreview(SpendLessIconButtonStyle.Error)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2416-30521 */
@Preview
@Composable
private fun SpendLessIconButtonErrorPressedPreview() =
    IconButtonPreview(SpendLessIconButtonStyle.Error, interaction = PreviewInteraction.Pressed)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2416-30505 */
@Preview
@Composable
private fun SpendLessIconButtonErrorFocusedPreview() =
    IconButtonPreview(SpendLessIconButtonStyle.Error, interaction = PreviewInteraction.Focused)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2416-30246 */
@Preview
@Composable
private fun SpendLessIconButtonOnPrimaryEnabledPreview() = IconButtonPreview(SpendLessIconButtonStyle.OnPrimary)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2416-30234 */
@Preview
@Composable
private fun SpendLessIconButtonOnPrimaryPressedPreview() =
    IconButtonPreview(SpendLessIconButtonStyle.OnPrimary, interaction = PreviewInteraction.Pressed)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2416-30527 */
@Preview
@Composable
private fun SpendLessIconButtonOnPrimaryFocusedPreview() =
    IconButtonPreview(SpendLessIconButtonStyle.OnPrimary, interaction = PreviewInteraction.Focused)

// endregion
