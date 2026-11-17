package com.plcoding.spendlesssample.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme

private val FieldShape = RoundedCornerShape(16.dp)
private val FieldHeight = 48.dp
private val FieldGap = 7.dp
private val FieldBorderWidth = 1.dp
private val TrailingIconSize = 20.dp
private const val DisabledAlpha = 0.38f
private const val FocusedPlaceholderAlpha = 0.7f

/**
 * The SpendLess text field: label above, a 48dp radius-16 field with a 1dp border, and optional
 * supporting text below. Built on [BasicTextField] with a decoration box; OutlinedTextField cannot
 * model the label-above, 48dp layout (research R3).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-1793
 * Figma (Input, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2445
 * Figma (Input, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2433
 * Figma (Input, Error): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2410
 * Figma (Input, Disabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2398
 * Figma (Placeholder, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2338
 * Figma (Placeholder, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2326
 * Figma (Placeholder, Error): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2304
 * Figma (Placeholder, Disabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2292
 */
@Composable
fun SpendLessTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    showDropDownIcon: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSourceFocused by source.collectIsFocusedAsState()
    val isFocused = isSourceFocused || LocalPreviewInteraction.current == PreviewInteraction.Focused

    val borderColor = when {
        !enabled -> colors.outline
        isError -> colors.error
        isFocused -> colors.primary
        else -> colors.outline
    }
    val placeholderColor = when {
        !enabled -> colors.onSurface
        isFocused && !isError -> colors.onSurfaceVariant.copy(alpha = FocusedPlaceholderAlpha)
        else -> colors.onSurfaceVariant
    }
    val supportingColor = when {
        !enabled -> colors.onSurface.copy(alpha = DisabledAlpha)
        isError -> colors.error
        else -> colors.onSurfaceVariant
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FieldGap)) {
        if (label != null) {
            Text(
                text = label,
                style = typography.labelSmall,
                color = colors.onSurface,
                modifier = Modifier.alpha(if (enabled) 1f else DisabledAlpha),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            textStyle = typography.bodyMedium.copy(color = colors.onSurface),
            cursorBrush = SolidColor(if (isError) colors.error else colors.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = source,
            decorationBox = { innerTextField ->
                // The background stays solid when disabled; only the border and content fade
                // (research R15 D6).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FieldHeight)
                        .background(colors.surfaceContainerLowest, FieldShape),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FieldHeight)
                            .alpha(if (enabled) 1f else DisabledAlpha)
                            .border(FieldBorderWidth, borderColor, FieldShape)
                            .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(
                                    text = placeholder,
                                    style = typography.bodyMedium,
                                    color = placeholderColor,
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                        if (showDropDownIcon) {
                            Icon(
                                painter = SpendLessIcons.ArrowDropDown,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(TrailingIconSize),
                            )
                        }
                    }
                }
            },
        )
        if (supportingText != null) {
            Text(text = supportingText, style = typography.bodySmall, color = supportingColor)
        }
    }
}

// region Previews

@Composable
private fun TextFieldPreview(
    value: String,
    isError: Boolean = false,
    enabled: Boolean = true,
    focused: Boolean = false,
    showDropDownIcon: Boolean = false,
) {
    SpendLessTheme {
        WithPreviewInteraction(if (focused) PreviewInteraction.Focused else null) {
            SpendLessTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.width(210.dp),
                label = "Label",
                placeholder = "Placeholder",
                supportingText = "Supporting text",
                isError = isError,
                enabled = enabled,
                showDropDownIcon = showDropDownIcon,
            )
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2445 */
@Preview(showBackground = true)
@Composable
private fun SpendLessTextFieldInputEnabledPreview() = TextFieldPreview(value = "Input")

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2433 */
@Preview(showBackground = true)
@Composable
private fun SpendLessTextFieldInputFocusedPreview() = TextFieldPreview(value = "Input", focused = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2410 */
@Preview(showBackground = true)
@Composable
private fun SpendLessTextFieldInputErrorPreview() = TextFieldPreview(value = "Input", isError = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2398 */
@Preview(showBackground = true)
@Composable
private fun SpendLessTextFieldInputDisabledPreview() = TextFieldPreview(value = "Input", enabled = false)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2338 */
@Preview(showBackground = true)
@Composable
private fun SpendLessTextFieldPlaceholderEnabledPreview() = TextFieldPreview(value = "")

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2326 */
@Preview(showBackground = true)
@Composable
private fun SpendLessTextFieldPlaceholderFocusedPreview() = TextFieldPreview(value = "", focused = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2304 */
@Preview(showBackground = true)
@Composable
private fun SpendLessTextFieldPlaceholderErrorPreview() = TextFieldPreview(value = "", isError = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=407-2292 */
@Preview(showBackground = true)
@Composable
private fun SpendLessTextFieldPlaceholderDisabledPreview() = TextFieldPreview(value = "", enabled = false)

@Preview(showBackground = true)
@Composable
private fun SpendLessTextFieldWithDropDownIconPreview() = TextFieldPreview(value = "Input", showDropDownIcon = true)

// endregion
