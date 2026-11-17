package com.plcoding.spendlesssample.transactions.presentation.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.designsystem.theme.bodyXSmall

/** Figma's Medium 12/16 header lacks its own token; `bodyXSmall` is Regular (research R9, F4). */
private val DayHeaderStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.typography.bodyXSmall.copy(fontWeight = FontWeight.Medium)

/**
 * Day-group header, for example "TODAY" or "JANUARY 9". Full width on an opaque `background` fill,
 * so it hides the rows scrolling under it when pinned (FR-008). Text is `onSurface` at 70% in a
 * feature-private Medium 12/16 style, since no text style token matches it (research R9, F4).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12045
 * Figma (pinned): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12088
 */
@Composable
internal fun DayHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = DayHeaderStyle,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    )
}

// region Previews

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12045 */
@Preview(widthDp = 380)
@Composable
private fun DayHeaderTodayPreview() {
    SpendLessTheme { DayHeader(text = "TODAY") }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12049 */
@Preview(widthDp = 380)
@Composable
private fun DayHeaderYesterdayPreview() {
    SpendLessTheme { DayHeader(text = "YESTERDAY") }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12054 */
@Preview(widthDp = 380)
@Composable
private fun DayHeaderOlderDatePreview() {
    SpendLessTheme { DayHeader(text = "JANUARY 9") }
}

// endregion
