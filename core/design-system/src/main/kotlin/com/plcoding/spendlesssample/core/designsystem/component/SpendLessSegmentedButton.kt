package com.plcoding.spendlesssample.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** One segment: its [value], visible [label] and an optional 18dp [icon] before the label. */
@Immutable
data class SegmentedOption<T>(val value: T, val label: String, val icon: Painter? = null)

private val TrackShape = RoundedCornerShape(16.dp)
private val SegmentShape = RoundedCornerShape(12.dp)
private val TrackHeight = 48.dp
private val TrackHorizontalPadding = 4.dp
private val SegmentVerticalPadding = 4.dp
private val SegmentContentHorizontalPadding = 12.dp
private val SegmentIconSize = 18.dp
private val SegmentIconGap = 6.dp
private const val UnselectedContentAlpha = 0.7f

/**
 * The SpendLess segmented button: a tinted 48dp track with equal-width segments; the selected one
 * is a raised `surfaceContainerLowest` segment. Built from a selectable-group [Row] of M3
 * [Surface] segments; the M3 SegmentedButton cannot model this design (research R3).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5441
 * Figma (building blocks): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5462
 * Figma (Selected, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5583
 * Figma (Unselected, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5564
 * Figma (Selected, Pressed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5499
 * Figma (Unselected, Pressed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5480
 * Figma (Selected, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5527
 * Figma (Unselected, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5508
 * Figma (icon + label, Create sheet instance): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12817
 */
@Composable
fun <T> SpendLessSegmentedButton(
    options: ImmutableList<SegmentedOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(TrackHeight)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f), TrackShape)
            .padding(horizontal = TrackHorizontalPadding)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            Segment(
                label = option.label,
                icon = option.icon,
                isSelected = option.value == selected,
                onClick = { onSelect(option.value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One segment. The 48dp slot has 4dp vertical padding, so the container is 40dp with radius 12 and
 * the content is centred in it (research R15 D3).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5462
 */
@Composable
private fun Segment(
    label: String,
    icon: Painter?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (isSelected) colors.primary else colors.onPrimaryFixed.copy(alpha = UnselectedContentAlpha)
    Box(modifier = modifier.padding(vertical = SegmentVerticalPadding)) {
        ProvideStateLayer(color = colors.primaryContainer, alpha = SpendLessSegmentRippleAlpha) {
            Surface(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier
                    .fillMaxSize()
                    .previewStateLayer(colors.primaryContainer, SegmentShape, alpha = 0.08f)
                    .semantics { role = Role.Tab },
                shape = SegmentShape,
                color = if (isSelected) colors.surfaceContainerLowest else Color.Transparent,
                contentColor = contentColor,
                interactionSource = interactionSource,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = SegmentContentHorizontalPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) {
                        Icon(painter = icon, contentDescription = null, modifier = Modifier.size(SegmentIconSize))
                        Spacer(Modifier.width(SegmentIconGap))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// region Previews

private enum class PreviewType { Expense, Income }

@Composable
private fun SegmentedPreview(selected: PreviewType, withIcons: Boolean) {
    SpendLessTheme {
        SpendLessSegmentedButton(
            options = persistentListOf(
                SegmentedOption(PreviewType.Expense, "Expense", if (withIcons) SpendLessIcons.TrendingDown else null),
                SegmentedOption(PreviewType.Income, "Income", if (withIcons) SpendLessIcons.TrendingUp else null),
            ),
            selected = selected,
            onSelect = {},
            modifier = Modifier.width(380.dp),
        )
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12817 */
@Preview
@Composable
private fun SpendLessSegmentedButtonFirstSelectedWithIconsPreview() =
    SegmentedPreview(PreviewType.Expense, withIcons = true)

@Preview
@Composable
private fun SpendLessSegmentedButtonSecondSelectedWithIconsPreview() =
    SegmentedPreview(PreviewType.Income, withIcons = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5441 */
@Preview
@Composable
private fun SpendLessSegmentedButtonFirstSelectedNoIconsPreview() =
    SegmentedPreview(PreviewType.Expense, withIcons = false)

@Preview
@Composable
private fun SpendLessSegmentedButtonSecondSelectedNoIconsPreview() =
    SegmentedPreview(PreviewType.Income, withIcons = false)

@Preview
@Composable
private fun SpendLessSegmentedButtonFiveSegmentsPreview() {
    SpendLessTheme {
        SpendLessSegmentedButton(
            options = persistentListOf(
                SegmentedOption(1, "Label"),
                SegmentedOption(2, "Label"),
                SegmentedOption(3, "Label"),
                SegmentedOption(4, "Label"),
                SegmentedOption(5, "Label"),
            ),
            selected = 1,
            onSelect = {},
            modifier = Modifier.width(516.dp),
        )
    }
}

@Composable
private fun SegmentPreview(isSelected: Boolean, interaction: PreviewInteraction? = null, withIcon: Boolean = false) {
    SpendLessTheme {
        Box(
            modifier = Modifier
                .width(108.dp)
                .height(TrackHeight)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)),
        ) {
            WithPreviewInteraction(interaction) {
                Segment(
                    label = "Label",
                    icon = if (withIcon) SpendLessIcons.Add else null,
                    isSelected = isSelected,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5583 */
@Preview
@Composable
private fun SegmentSelectedEnabledPreview() = SegmentPreview(isSelected = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5564 */
@Preview
@Composable
private fun SegmentUnselectedEnabledPreview() = SegmentPreview(isSelected = false)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5499 */
@Preview
@Composable
private fun SegmentSelectedPressedPreview() = SegmentPreview(isSelected = true, interaction = PreviewInteraction.Pressed)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5480 */
@Preview
@Composable
private fun SegmentUnselectedPressedPreview() = SegmentPreview(isSelected = false, interaction = PreviewInteraction.Pressed)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5527 */
@Preview
@Composable
private fun SegmentSelectedFocusedPreview() = SegmentPreview(isSelected = true, interaction = PreviewInteraction.Focused)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-5508 */
@Preview
@Composable
private fun SegmentUnselectedFocusedPreview() = SegmentPreview(isSelected = false, interaction = PreviewInteraction.Focused)

@Preview
@Composable
private fun SegmentSelectedWithIconPreview() = SegmentPreview(isSelected = true, withIcon = true)

@Preview
@Composable
private fun SegmentUnselectedWithIconPreview() = SegmentPreview(isSelected = false, withIcon = true)

// endregion
