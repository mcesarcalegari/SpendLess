package com.plcoding.spendlesssample.transactions.presentation.create.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme

/** Placeholder alpha from `2415:6637` (`opacity-60`). */
private const val PlaceholderAlpha = 0.6f

/**
 * The borderless, centred receiver / sender name field of the create sheet (research R8): one line
 * in `titleMedium` `onSurface` with `px 8dp`, the placeholder in the same style at 60%, and a
 * `primary` caret. The 50-character cap is enforced by the ViewModel.
 *
 * Figma (empty): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6636
 * Figma (typing): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-7321
 */
@Composable
internal fun CounterpartyInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.titleMedium.copy(
        color = colors.onSurface,
        textAlign = TextAlign.Center,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next,
        ),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        modifier = Modifier.fillMaxWidth(),
                        style = textStyle.copy(color = colors.onSurface.copy(alpha = PlaceholderAlpha)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            }
        },
    )
}

// region Previews

@Composable
private fun CounterpartyInputPreview(value: String, placeholder: String = "Receiver") {
    SpendLessTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            CounterpartyInput(
                value = value,
                placeholder = placeholder,
                onValueChange = {},
                modifier = Modifier.width(300.dp),
            )
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6636 */
@Preview
@Composable
private fun CounterpartyInputReceiverPlaceholderPreview() = CounterpartyInputPreview(value = "")

/** Figma (income sheet): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8638 */
@Preview
@Composable
private fun CounterpartyInputSenderPlaceholderPreview() = CounterpartyInputPreview(value = "", placeholder = "Sender")

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-7321 */
@Preview
@Composable
private fun CounterpartyInputTypingPreview() = CounterpartyInputPreview(value = "Netflix")

@Preview
@Composable
private fun CounterpartyInputMaxLengthPreview() = CounterpartyInputPreview(value = "Gym - Monthly Membership at the Downtown Club, Ltd.".take(50))

// endregion
