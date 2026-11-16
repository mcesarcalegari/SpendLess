package com.plcoding.spendlesssample.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme

/** Button styles from the Figma `Style` property. */
enum class SpendLessButtonStyle { Filled, Text }

private val ButtonShape = RoundedCornerShape(16.dp)
private val ButtonMinHeight = 48.dp
private val ButtonIconSize = 18.dp
private val ButtonIconGap = 8.dp

/**
 * The SpendLess button: Filled (high emphasis) or Text (low emphasis), with an optional 18dp icon
 * before or after the label. Built on M3 [Button] / [TextButton] with every visual taken from Figma.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-110
 * Figma (Filled, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-283
 * Figma (Filled, Pressed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-274
 * Figma (Filled, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-277
 * Figma (Filled, Disabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-271
 * Figma (Filled, icon left): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-267
 * Figma (Filled, icon right): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-467
 * Figma (Text, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-213
 * Figma (Text, Pressed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-204
 * Figma (Text, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-207
 * Figma (Text, Disabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-201
 * Figma (Text, icon left): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-197
 * Figma (Text, icon right): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-427
 */
@Composable
fun SpendLessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SpendLessButtonStyle = SpendLessButtonStyle.Filled,
    enabled: Boolean = true,
    leadingIcon: Painter? = null,
    trailingIcon: Painter? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MaterialTheme.colorScheme
    val disabledContent = colors.onSurface.copy(alpha = 0.38f)
    val contentPadding = contentPadding(style, hasLeading = leadingIcon != null, hasTrailing = trailingIcon != null)
    val content: @Composable () -> Unit = {
        ButtonContent(text = text, leadingIcon = leadingIcon, trailingIcon = trailingIcon)
    }

    when (style) {
        SpendLessButtonStyle.Filled -> ProvideStateLayer(color = colors.onPrimary) {
            Button(
                onClick = onClick,
                modifier = modifier
                    .heightIn(min = ButtonMinHeight)
                    .previewStateLayer(colors.onPrimary, ButtonShape),
                enabled = enabled,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = disabledContent,
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                contentPadding = contentPadding,
                interactionSource = interactionSource,
            ) { content() }
        }
        SpendLessButtonStyle.Text -> ProvideStateLayer(color = colors.primary) {
            TextButton(
                onClick = onClick,
                modifier = modifier
                    .heightIn(min = ButtonMinHeight)
                    .previewStateLayer(colors.primary, ButtonShape),
                enabled = enabled,
                shape = ButtonShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.primary,
                    disabledContentColor = disabledContent,
                ),
                contentPadding = contentPadding,
                interactionSource = interactionSource,
            ) { content() }
        }
    }
}

@Composable
private fun ButtonContent(text: String, leadingIcon: Painter?, trailingIcon: Painter?) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        if (leadingIcon != null) {
            Icon(painter = leadingIcon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
            Spacer(Modifier.width(ButtonIconGap))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(ButtonIconGap))
            Icon(painter = trailingIcon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
        }
    }
}

/** Figma paddings per style and icon position; vertical is always 10dp. */
private fun contentPadding(style: SpendLessButtonStyle, hasLeading: Boolean, hasTrailing: Boolean): PaddingValues {
    val (outer, iconSide) = when (style) {
        SpendLessButtonStyle.Filled -> 24.dp to 16.dp
        SpendLessButtonStyle.Text -> 12.dp to 16.dp
    }
    return when {
        // Text style: icon side 12, text side 16 (inverted relative to Filled).
        style == SpendLessButtonStyle.Text && hasLeading -> PaddingValues(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
        style == SpendLessButtonStyle.Text && hasTrailing -> PaddingValues(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
        hasLeading -> PaddingValues(start = iconSide, end = outer, top = 10.dp, bottom = 10.dp)
        hasTrailing -> PaddingValues(start = outer, end = iconSide, top = 10.dp, bottom = 10.dp)
        else -> PaddingValues(horizontal = outer, vertical = 10.dp)
    }
}

// region Previews

@Composable
private fun ButtonPreview(
    style: SpendLessButtonStyle,
    enabled: Boolean = true,
    leading: Boolean = false,
    trailing: Boolean = false,
    interaction: PreviewInteraction? = null,
    text: String = "Label",
) {
    SpendLessTheme {
        WithPreviewInteraction(interaction) {
            SpendLessButton(
                text = text,
                onClick = {},
                style = style,
                enabled = enabled,
                leadingIcon = if (leading) SpendLessIcons.Add else null,
                trailingIcon = if (trailing) SpendLessIcons.Add else null,
            )
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-283 */
@Preview
@Composable
private fun SpendLessButtonFilledEnabledPreview() = ButtonPreview(SpendLessButtonStyle.Filled)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-274 */
@Preview
@Composable
private fun SpendLessButtonFilledPressedPreview() =
    ButtonPreview(SpendLessButtonStyle.Filled, interaction = PreviewInteraction.Pressed)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-277 */
@Preview
@Composable
private fun SpendLessButtonFilledFocusedPreview() =
    ButtonPreview(SpendLessButtonStyle.Filled, interaction = PreviewInteraction.Focused)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-271 */
@Preview
@Composable
private fun SpendLessButtonFilledDisabledPreview() = ButtonPreview(SpendLessButtonStyle.Filled, enabled = false)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-267 */
@Preview
@Composable
private fun SpendLessButtonFilledIconLeftPreview() = ButtonPreview(SpendLessButtonStyle.Filled, leading = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-467 */
@Preview
@Composable
private fun SpendLessButtonFilledIconRightPreview() = ButtonPreview(SpendLessButtonStyle.Filled, trailing = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-251 */
@Preview
@Composable
private fun SpendLessButtonFilledDisabledIconLeftPreview() =
    ButtonPreview(SpendLessButtonStyle.Filled, enabled = false, leading = true)

@Preview(widthDp = 220)
@Composable
private fun SpendLessButtonFilledLongTextPreview() =
    ButtonPreview(SpendLessButtonStyle.Filled, text = "Create a very long transaction label")

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-213 */
@Preview
@Composable
private fun SpendLessButtonTextEnabledPreview() = ButtonPreview(SpendLessButtonStyle.Text)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-204 */
@Preview
@Composable
private fun SpendLessButtonTextPressedPreview() =
    ButtonPreview(SpendLessButtonStyle.Text, interaction = PreviewInteraction.Pressed)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-207 */
@Preview
@Composable
private fun SpendLessButtonTextFocusedPreview() =
    ButtonPreview(SpendLessButtonStyle.Text, interaction = PreviewInteraction.Focused)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-201 */
@Preview
@Composable
private fun SpendLessButtonTextDisabledPreview() = ButtonPreview(SpendLessButtonStyle.Text, enabled = false)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-197 */
@Preview
@Composable
private fun SpendLessButtonTextIconLeftPreview() = ButtonPreview(SpendLessButtonStyle.Text, leading = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-427 */
@Preview
@Composable
private fun SpendLessButtonTextIconRightPreview() = ButtonPreview(SpendLessButtonStyle.Text, trailing = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-181 */
@Preview
@Composable
private fun SpendLessButtonTextDisabledIconLeftPreview() =
    ButtonPreview(SpendLessButtonStyle.Text, enabled = false, leading = true)

@Preview(widthDp = 160)
@Composable
private fun SpendLessButtonTextLongTextPreview() =
    ButtonPreview(SpendLessButtonStyle.Text, text = "A very long text action")

@Composable
private fun ButtonContentPreview(leading: Boolean = false, trailing: Boolean = false) {
    SpendLessTheme {
        ButtonContent(
            text = "Label",
            leadingIcon = if (leading) SpendLessIcons.Add else null,
            trailingIcon = if (trailing) SpendLessIcons.Add else null,
        )
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-283 */
@Preview(showBackground = true)
@Composable
private fun ButtonContentLabelOnlyPreview() = ButtonContentPreview()

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-267 */
@Preview(showBackground = true)
@Composable
private fun ButtonContentLeadingIconPreview() = ButtonContentPreview(leading = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-467 */
@Preview(showBackground = true)
@Composable
private fun ButtonContentTrailingIconPreview() = ButtonContentPreview(trailing = true)

// endregion
