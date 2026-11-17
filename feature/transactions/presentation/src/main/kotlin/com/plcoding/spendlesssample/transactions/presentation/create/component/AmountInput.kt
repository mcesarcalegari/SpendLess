package com.plcoding.spendlesssample.transactions.presentation.create.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.designsystem.theme.spendLessColors
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.presentation.create.AmountPartsUi

/** Placeholder alpha from `2415:6640` (`opacity-38`). */
private const val PlaceholderAlpha = 0.38f

/**
 * The centred amount field of the create sheet (research R8): the sign [AmountPartsUi.prefix] and
 * [AmountPartsUi.suffix] in `error` (expense) or `success` (income) around the digits in
 * `onSurface`, all in `displayMedium`, with a 4dp gap, `py 10dp` and `px 8dp`. An empty field
 * shows the `00.00` placeholder (with the preferred separator) at 38%.
 *
 * The text field holds only the raw number ([value]); thousands separators are drawn by a
 * [VisualTransformation] that maps onto [AmountPartsUi.number], which the ViewModel builds with
 * `AmountFormatter.groupInput`. The phone's own keyboard opens in decimal-number mode; there is
 * no custom keypad (FR-021a), and the ViewModel's `AmountInputParser` filters anything it lets
 * through (FR-013).
 *
 * The row may grow past the 300dp inputs block, so `999,999,999.99` stays fully readable (edge
 * case "Very large amounts"); Figma draws no amount that wide.
 *
 * Figma (`-$`): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6638
 * Figma (`($ … )`): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-11064
 */
@Composable
internal fun AmountInput(
    value: String,
    parts: AmountPartsUi,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.displayMedium
    val signColor = when (parts.signTone) {
        TransactionType.EXPENSE -> colors.error
        TransactionType.INCOME -> MaterialTheme.spendLessColors.success
    }
    val transformation = remember(parts.number, parts.isPlaceholder) {
        if (parts.isPlaceholder) VisualTransformation.None else GroupedAmountTransformation(parts.number)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(unbounded = true)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = parts.prefix, style = style, color = signColor, maxLines = 1)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .focusRequester(focusRequester),
            textStyle = style.copy(color = colors.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = transformation,
            decorationBox = { innerTextField ->
                Box {
                    if (parts.isPlaceholder) {
                        Text(
                            text = parts.number,
                            style = style,
                            color = colors.onSurface.copy(alpha = PlaceholderAlpha),
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (parts.suffix.isNotEmpty()) {
            Text(text = parts.suffix, style = style, color = signColor, maxLines = 1)
        }
    }
}

/**
 * Shows [grouped] (the raw input with thousands separators inserted) in place of the raw text.
 * Offsets are mapped by aligning the raw characters onto [grouped] in order; the separators it
 * adds never occur in raw input. If [grouped] is stale and does not contain the raw text in order,
 * the raw text is shown unchanged rather than risking an out-of-range offset.
 */
internal class GroupedAmountTransformation(private val grouped: String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val originalToTransformed = IntArray(raw.length + 1)
        val transformedToOriginal = IntArray(grouped.length + 1)
        var matched = 0
        grouped.forEachIndexed { index, char ->
            transformedToOriginal[index] = matched
            if (matched < raw.length && char == raw[matched]) {
                originalToTransformed[matched] = index
                matched++
            }
        }
        if (matched != raw.length) return TransformedText(text, OffsetMapping.Identity)
        originalToTransformed[raw.length] = grouped.length
        transformedToOriginal[grouped.length] = raw.length

        return TransformedText(
            text = AnnotatedString(grouped),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    originalToTransformed[offset.coerceIn(0, raw.length)]

                override fun transformedToOriginal(offset: Int): Int =
                    transformedToOriginal[offset.coerceIn(0, grouped.length)]
            },
        )
    }

    override fun equals(other: Any?): Boolean = other is GroupedAmountTransformation && other.grouped == grouped

    override fun hashCode(): Int = grouped.hashCode()
}

// region Previews

@Composable
private fun AmountInputPreview(value: String, parts: AmountPartsUi) {
    SpendLessTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            AmountInput(
                value = value,
                parts = parts,
                onValueChange = {},
                modifier = Modifier.width(300.dp),
            )
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6638 */
@Preview(widthDp = 380)
@Composable
private fun AmountInputEmptyExpensePreview() = AmountInputPreview(value = "", parts = AmountPartsUi())

/** Figma (income sheet): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8638 */
@Preview(widthDp = 380)
@Composable
private fun AmountInputEmptyIncomePreview() = AmountInputPreview(
    value = "",
    parts = AmountPartsUi(prefix = "$", signTone = TransactionType.INCOME),
)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2443-9532 */
@Preview(widthDp = 380)
@Composable
private fun AmountInputFilledExpensePreview() = AmountInputPreview(
    value = "100.00",
    parts = AmountPartsUi(number = "100.00", isPlaceholder = false),
)

@Preview(widthDp = 380)
@Composable
private fun AmountInputFilledIncomePreview() = AmountInputPreview(
    value = "20",
    parts = AmountPartsUi(prefix = "$", number = "20", isPlaceholder = false, signTone = TransactionType.INCOME),
)

@Preview(widthDp = 380)
@Composable
private fun AmountInputMaxGroupedPreview() = AmountInputPreview(
    value = "999999999.99",
    parts = AmountPartsUi(number = "999,999,999.99", isPlaceholder = false),
)

@Preview(widthDp = 380)
@Composable
private fun AmountInputCommaDecimalPreview() = AmountInputPreview(
    value = "1234,5",
    parts = AmountPartsUi(number = "1.234,5", isPlaceholder = false),
)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-11064 */
@Preview(widthDp = 380)
@Composable
private fun AmountInputEmptyParenthesesPreview() = AmountInputPreview(
    value = "",
    parts = AmountPartsUi(prefix = "($", suffix = ")"),
)

@Preview(widthDp = 380)
@Composable
private fun AmountInputFilledParenthesesPreview() = AmountInputPreview(
    value = "1234.5",
    parts = AmountPartsUi(prefix = "($", number = "1,234.5", suffix = ")", isPlaceholder = false),
)

@Preview(widthDp = 380)
@Composable
private fun AmountInputCommaDecimalPlaceholderPreview() = AmountInputPreview(
    value = "",
    parts = AmountPartsUi(number = "00,00"),
)

// endregion
