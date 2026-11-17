package com.plcoding.spendlesssample.transactions.presentation.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.designsystem.theme.bodyXSmall
import com.plcoding.spendlesssample.core.designsystem.theme.spendLessColors
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.presentation.list.TransactionListPreviewData
import com.plcoding.spendlesssample.transactions.presentation.list.model.TransactionUi

private val TileSize = 44.dp
private val TileShape = RoundedCornerShape(12.dp)

/** Figma fixes the amount column at 100dp; it may grow so the amount never truncates (F3). */
private val AmountMinWidth = 100.dp

/** Income tile: `secondaryFixed` at 40% over `surfaceContainerLowest` (`2417:12050`, F1). */
private const val IncomeTileAlpha = 0.4f

/**
 * One transaction in the list: a 44dp emoji tile, the counterparty and its category (or "Income"),
 * and the amount. The name takes the remaining width and ellipsizes first, so the amount is never
 * cut off (FR-007). Income amounts use `success` (FR-005).
 *
 * This is the collapsed row. The note badge, note expansion (US3) and the long-press menu (US6)
 * are added by their stories; until then [isExpanded], [onNoteBadgeClick] and [onLongPress] are
 * accepted for the contract signature but have no effect.
 *
 * Figma (master): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=423-4852
 * Figma (expense collapsed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5064
 * Figma (income collapsed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5093
 * Figma (income in list): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12050
 */
@Suppress("UNUSED_PARAMETER") // isExpanded, onNoteBadgeClick (US3) and onLongPress (US6).
@Composable
internal fun TransactionRow(
    item: TransactionUi,
    isExpanded: Boolean,
    onNoteBadgeClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isIncome = item.type == TransactionType.INCOME
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransactionIconTile(emoji = item.iconEmoji, isIncome = isIncome)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.label.asString(),
                style = MaterialTheme.typography.bodyXSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier.widthIn(min = AmountMinWidth),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = item.formattedAmount,
                style = MaterialTheme.typography.titleLarge,
                color = if (isIncome) MaterialTheme.spendLessColors.success else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/**
 * The 44dp, radius-12 category tile. Expense: `primaryFixed`. Income: `secondaryFixed` at 40%
 * layered over `surfaceContainerLowest`, so the page background does not show through (F1).
 *
 * Figma (expense): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=422-4329
 * Figma (income): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12050
 */
@Composable
private fun TransactionIconTile(
    emoji: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tileBackground = if (isIncome) {
        Modifier
            .background(colors.surfaceContainerLowest)
            .background(colors.secondaryFixed.copy(alpha = IncomeTileAlpha))
    } else {
        Modifier.background(colors.primaryFixed)
    }
    Box(
        modifier = modifier
            .size(TileSize)
            .clip(TileShape)
            .then(tileBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
        )
    }
}

// region Previews

@Composable
private fun RowPreview(item: TransactionUi) {
    SpendLessTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            TransactionRow(item = item, isExpanded = false, onNoteBadgeClick = {}, onLongPress = null)
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5064 */
@Preview(widthDp = 380)
@Composable
private fun TransactionRowExpenseCollapsedPreview() {
    RowPreview(TransactionListPreviewData.starbucks)
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12050 */
@Preview(widthDp = 380)
@Composable
private fun TransactionRowIncomeCollapsedPreview() {
    RowPreview(TransactionListPreviewData.ricksShare)
}

/** Every category tile, in FR-026 order. */
@Preview(widthDp = 380)
@Composable
private fun TransactionRowAllCategoriesPreview() {
    SpendLessTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExpenseCategory.entries.forEach { category ->
                TransactionRow(
                    item = TransactionListPreviewData.expense("Receiver", category, "-$10.00"),
                    isExpanded = false,
                    onNoteBadgeClick = {},
                    onLongPress = null,
                )
            }
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun TransactionRowLongNamePreview() {
    RowPreview(
        TransactionListPreviewData.expense(
            title = "A very long receiver name that cannot fit in one line",
            category = ExpenseCategory.EDUCATION,
            amount = "-$1,234.50",
        ),
    )
}

/** The amount grows past the 100dp column and the name yields (F3). */
@Preview(widthDp = 380)
@Composable
private fun TransactionRowMaxAmountPreview() {
    RowPreview(
        TransactionListPreviewData.expense(
            title = "Gym - Monthly Membership",
            category = ExpenseCategory.HEALTH_AND_WELLNESS,
            amount = "-$999,999,999.99",
        ),
    )
}

@Preview(widthDp = 380)
@Composable
private fun TransactionRowMaxIncomePreview() {
    RowPreview(TransactionListPreviewData.income("Salary", "$999,999,999.99"))
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=422-4329 */
@Preview
@Composable
private fun TransactionIconTileExpensePreview() {
    SpendLessTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            TransactionIconTile(emoji = "🍕", isIncome = false)
        }
    }
}

/**
 * `secondaryFixed` at 40% over `surfaceContainerLowest` (F1).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12050
 */
@Preview
@Composable
private fun TransactionIconTileIncomePreview() {
    SpendLessTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            TransactionIconTile(emoji = "💰", isIncome = true)
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun TransactionRowParenthesesPreview() {
    RowPreview(TransactionListPreviewData.expense("Netflix", ExpenseCategory.ENTERTAINMENT, "($15.99)"))
}

// endregion
