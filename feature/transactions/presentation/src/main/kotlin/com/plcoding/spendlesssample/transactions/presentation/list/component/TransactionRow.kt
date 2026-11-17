package com.plcoding.spendlesssample.transactions.presentation.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessShadows
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.designsystem.theme.bodyXSmall
import com.plcoding.spendlesssample.core.designsystem.theme.spendLessColors
import com.plcoding.spendlesssample.core.designsystem.theme.spendLessShadow
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.presentation.R
import com.plcoding.spendlesssample.transactions.presentation.list.TransactionListPreviewData
import com.plcoding.spendlesssample.transactions.presentation.list.model.TransactionUi

private val TileSize = 44.dp
private val TileShape = RoundedCornerShape(12.dp)

/** Figma fixes the amount column at 100dp; it may grow so the amount never truncates (F3). */
private val AmountMinWidth = 100.dp

/** Income tile: `secondaryFixed` at 40% over `surfaceContainerLowest` (`2417:12050`, F1). */
private const val IncomeTileAlpha = 0.4f

/** Note badge (`423:4883`, research R9): 20dp chip at (26, 26) inside the tile, 14dp icon. */
private val BadgeSize = 20.dp
private val BadgeOffset = 26.dp
private val BadgeShape = RoundedCornerShape(6.dp)
private val BadgeIconSize = 14.dp

/** The badge's touch target: 48dp, centred on the badge, without changing layout (R9). */
private val BadgeTouchSize = 48.dp
private val BadgeTouchOffset = BadgeOffset + BadgeSize / 2 - BadgeTouchSize / 2

/** Expanded card (`2414:5065`, `2414:5106`, research R9). */
private val CardShape = RoundedCornerShape(16.dp)
private val CardPadding = 4.dp
private val CardBottomPadding = 10.dp
private val CardContentGap = 6.dp

/** The expanded card's inner row ends 4dp further in, so the amount shifts 4dp (`2414:5066`, F9). */
private val ExpandedRowEndPadding = 4.dp

/** The note starts under the name: 44dp tile + 8dp gap (`2414:5078`). */
private val NoteStartPadding = 52.dp
private val NoteEndPadding = 12.dp

/**
 * One transaction in the list: a 44dp emoji tile, the counterparty and its category (or "Income"),
 * and the amount. The name takes the remaining width and ellipsizes first, so the amount is never
 * cut off (FR-007). Income amounts use `success` (FR-005).
 *
 * A row with a note carries a note badge on its tile (FR-006). Tapping the badge, and nothing else
 * on the row, toggles [isExpanded] through [onNoteBadgeClick]. Expanded, the row becomes a raised
 * card that also shows the full note (FR-028, FR-029). The card is drawn 4dp outside the row on
 * the start, top and end, so the tile, name and label stay where they were (US3-1); the amount
 * moves 4dp inward, as in Figma (F9).
 *
 * When [onLongPress] is set (a row of a running recurring series), a long press anywhere on the row
 * gives haptic feedback and calls it, and screen readers get a "Stop repeating" action (FR-037).
 * The row looks the same either way; there is no repeat marker.
 *
 * Figma (master): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=423-4852
 * Figma (expense collapsed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5064
 * Figma (income collapsed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5093
 * Figma (expense expanded): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5065
 * Figma (income expanded): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5106
 * Figma (all four states): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5142
 * Figma (income in list): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12050
 */
@Composable
internal fun TransactionRow(
    item: TransactionUi,
    isExpanded: Boolean,
    onNoteBadgeClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isIncome = item.type == TransactionType.INCOME
    val note = item.note
    val showsNote = isExpanded && note != null
    val haptic = LocalHapticFeedback.current
    val stopRepeatingLabel = stringResource(R.string.stop_repeating)
    // The gesture detector is not restarted when the callback changes; it always calls the latest.
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongPress != null) {
                    Modifier.longPressAction(
                        label = stopRepeatingLabel,
                        onLongPress = { currentOnLongPress?.invoke() },
                        onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    )
                } else {
                    Modifier
                },
            )
            .then(
                if (showsNote) Modifier.expandedCard(MaterialTheme.colorScheme.surfaceContainerLowest) else Modifier,
            ),
        verticalArrangement = Arrangement.spacedBy(CardContentGap),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (showsNote) ExpandedRowEndPadding else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransactionIconTile(
                emoji = item.iconEmoji,
                isIncome = isIncome,
                hasNote = note != null,
                isNoteExpanded = showsNote,
                onNoteBadgeClick = onNoteBadgeClick,
            )
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
                    // titleLarge 26/0, not the 28/0.1 drawn only in `2414:5076` (F2).
                    text = item.formattedAmount,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isIncome) MaterialTheme.spendLessColors.success else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        if (showsNote) {
            // Wraps freely and is never ellipsized (FR-028).
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = NoteStartPadding, end = NoteEndPadding),
            )
        }
    }
}

/**
 * A long press with [onHaptic] feedback calls [onLongPress]; screen readers get the same as a
 * custom action named [label]. Taps are left to the children (the note badge).
 */
private fun Modifier.longPressAction(
    label: String,
    onLongPress: () -> Unit,
    onHaptic: () -> Unit,
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                onHaptic()
                onLongPress()
            },
        )
    }
    .semantics {
        customActions = listOf(
            CustomAccessibilityAction(label) {
                onLongPress()
                true
            },
        )
    }

/**
 * The expanded card (`2414:5065`): `surfaceContainerLowest`, radius 16, shadow `raised`, padding
 * 4/4/4/10. It is laid out [CardPadding] wider on each side and [CardPadding] higher than the row's
 * slot, so the content inside keeps the collapsed row's position (research R9).
 */
private fun Modifier.expandedCard(containerColor: Color): Modifier = this
    .overhang(horizontal = CardPadding, top = CardPadding)
    .spendLessShadow(SpendLessShadows.raised, CardShape)
    .background(containerColor, CardShape)
    .padding(start = CardPadding, top = CardPadding, end = CardPadding, bottom = CardBottomPadding)

/**
 * Lays the content out [horizontal] wider on each side and moves it up by [top], while reporting
 * the original slot to the parent. The extra area is drawn outside the slot.
 */
private fun Modifier.overhang(horizontal: Dp, top: Dp): Modifier = layout { measurable, constraints ->
    val side = horizontal.roundToPx()
    val up = top.roundToPx()
    val widened = if (constraints.hasBoundedWidth) {
        constraints.copy(
            minWidth = constraints.minWidth + side * 2,
            maxWidth = constraints.maxWidth + side * 2,
        )
    } else {
        constraints
    }
    val placeable = measurable.measure(widened)
    val width = (placeable.width - side * 2).coerceAtLeast(0)
    val height = (placeable.height - up).coerceAtLeast(0)
    layout(width, height) { placeable.place(-side, -up) }
}

/**
 * The 44dp, radius-12 category tile. Expense: `primaryFixed`. Income: `secondaryFixed` at 40%
 * layered over `surfaceContainerLowest`, so the page background does not show through (F1).
 * Only the background is clipped, so the note badge can overhang the tile by 2dp with its shadow.
 *
 * Figma (expense): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=422-4329
 * Figma (income): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12050
 */
@Composable
private fun TransactionIconTile(
    emoji: String,
    isIncome: Boolean,
    hasNote: Boolean,
    isNoteExpanded: Boolean,
    onNoteBadgeClick: () -> Unit,
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
    Box(modifier = modifier.size(TileSize)) {
        Box(
            modifier = Modifier
                .matchParentSize()
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
        if (hasNote) {
            NoteBadge(
                isIncome = isIncome,
                isActive = isNoteExpanded,
                modifier = Modifier.offset(x = BadgeOffset, y = BadgeOffset),
            )
            // A transparent 48dp target centred on the badge, so the 20dp badge is easy to tap
            // without changing layout (research R9). Not an IconButton: that would draw a state
            // layer and take layout space the tile does not have. Expanding the row is the
            // feedback, so no ripple is drawn.
            Box(
                modifier = Modifier
                    .offset(x = BadgeTouchOffset, y = BadgeTouchOffset)
                    .size(BadgeTouchSize)
                    .noteBadgeToggle(
                        description = stringResource(
                            if (isNoteExpanded) R.string.cd_hide_note else R.string.cd_show_note,
                        ),
                        onClick = onNoteBadgeClick,
                    ),
            )
        }
    }
}

/** A button that says "Show note" or "Hide note" (FR-036). */
private fun Modifier.noteBadgeToggle(description: String, onClick: () -> Unit): Modifier = this
    .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
    .semantics { contentDescription = description }

/**
 * The 20dp note badge: `surfaceContainerLowest`, radius 6, shadow `raised`, with a 14dp `Notes`
 * icon. Subdued while the row is collapsed and active while it is expanded (FR-029, research R9):
 * expense `primaryFixedDim` / `primaryContainer`, income `secondaryFixedDim` / `secondary`.
 * Decorative: the touch target in [TransactionIconTile] carries the spoken label.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=423-4883
 * Figma (instances, all four tints): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5142
 */
@Composable
private fun NoteBadge(
    isIncome: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tint = when {
        isIncome && isActive -> colors.secondary
        isIncome -> colors.secondaryFixedDim
        isActive -> colors.primaryContainer
        else -> colors.primaryFixedDim
    }
    Box(
        modifier = modifier
            .size(BadgeSize)
            .spendLessShadow(SpendLessShadows.raised, BadgeShape)
            .background(colors.surfaceContainerLowest, BadgeShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = SpendLessIcons.Notes,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(BadgeIconSize),
        )
    }
}

// region Previews

/** Room around the row so the expanded card's 4dp overhang and its shadow are not cut off. */
private val PreviewPadding = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 24.dp)

@Composable
private fun RowPreview(item: TransactionUi, isExpanded: Boolean = false) {
    SpendLessTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .then(PreviewPadding),
        ) {
            TransactionRow(item = item, isExpanded = isExpanded, onNoteBadgeClick = {}, onLongPress = null)
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5064 */
@Preview(widthDp = 396)
@Composable
private fun TransactionRowExpenseCollapsedPreview() {
    RowPreview(TransactionListPreviewData.starbucks)
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12050 */
@Preview(widthDp = 396)
@Composable
private fun TransactionRowIncomeCollapsedPreview() {
    RowPreview(TransactionListPreviewData.ricksShare)
}

/** Subdued badge. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5064 */
@Preview(widthDp = 396)
@Composable
private fun TransactionRowExpenseWithNoteCollapsedPreview() {
    RowPreview(TransactionListPreviewData.starbucksWithNote)
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5065 */
@Preview(widthDp = 396)
@Composable
private fun TransactionRowExpenseExpandedPreview() {
    RowPreview(TransactionListPreviewData.starbucksWithNote, isExpanded = true)
}

/** Subdued badge. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5093 */
@Preview(widthDp = 396)
@Composable
private fun TransactionRowIncomeWithNoteCollapsedPreview() {
    RowPreview(TransactionListPreviewData.ricksShareWithNote)
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5106 */
@Preview(widthDp = 396)
@Composable
private fun TransactionRowIncomeExpandedPreview() {
    RowPreview(TransactionListPreviewData.ricksShareWithNote, isExpanded = true)
}

/** The longest note (500 characters) wraps in full and is never ellipsized (FR-028). */
@Preview(widthDp = 396)
@Composable
private fun TransactionRowLongNoteExpandedPreview() {
    RowPreview(
        TransactionListPreviewData.expense(
            title = "Home Depot",
            category = ExpenseCategory.HOME,
            amount = "-$35.60",
            note = TransactionListPreviewData.longNote,
        ),
        isExpanded = true,
    )
}

/**
 * All four states side by side, as in the Figma frame.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5142
 */
@Preview(widthDp = 396)
@Composable
private fun TransactionRowNoteStatesPreview() {
    SpendLessTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .then(PreviewPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            listOf(TransactionListPreviewData.starbucksWithNote, TransactionListPreviewData.ricksShareWithNote)
                .forEach { item ->
                    TransactionRow(item = item, isExpanded = false, onNoteBadgeClick = {}, onLongPress = null)
                    TransactionRow(item = item, isExpanded = true, onNoteBadgeClick = {}, onLongPress = null)
                }
        }
    }
}

/** Every category tile, in FR-026 order. */
@Preview(widthDp = 396)
@Composable
private fun TransactionRowAllCategoriesPreview() {
    SpendLessTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .then(PreviewPadding),
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

@Preview(widthDp = 396)
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
@Preview(widthDp = 396)
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

@Preview(widthDp = 396)
@Composable
private fun TransactionRowMaxIncomePreview() {
    RowPreview(TransactionListPreviewData.income("Salary", "$999,999,999.99"))
}

/**
 * Parentheses negative style (FR-032): the expense amount reads `($45.99)` in the normal text colour.
 * The list behind the Figma sheet still draws `-$45.99`; this follows FR-032 instead.
 *
 * Figma (screen): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10959
 */
@Preview(widthDp = 396)
@Composable
private fun TransactionRowParenthesesPreview() {
    RowPreview(TransactionListPreviewData.expense("Amazon", ExpenseCategory.HOME, "($45.99)"))
}

@Composable
private fun TilePreview(isIncome: Boolean, hasNote: Boolean = false, isNoteExpanded: Boolean = false) {
    SpendLessTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .then(PreviewPadding),
        ) {
            TransactionIconTile(
                emoji = if (isIncome) "💰" else "🍕",
                isIncome = isIncome,
                hasNote = hasNote,
                isNoteExpanded = isNoteExpanded,
                onNoteBadgeClick = {},
            )
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=422-4329 */
@Preview
@Composable
private fun TransactionIconTileExpensePreview() = TilePreview(isIncome = false)

/**
 * `secondaryFixed` at 40% over `surfaceContainerLowest` (F1).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12050
 */
@Preview
@Composable
private fun TransactionIconTileIncomePreview() = TilePreview(isIncome = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5064 */
@Preview
@Composable
private fun TransactionIconTileExpenseWithNotePreview() = TilePreview(isIncome = false, hasNote = true)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5093 */
@Preview
@Composable
private fun TransactionIconTileIncomeWithNotePreview() = TilePreview(isIncome = true, hasNote = true)

/** Active badge. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5067 */
@Preview
@Composable
private fun TransactionIconTileExpenseNoteExpandedPreview() =
    TilePreview(isIncome = false, hasNote = true, isNoteExpanded = true)

/** Active badge. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5124 */
@Preview
@Composable
private fun TransactionIconTileIncomeNoteExpandedPreview() =
    TilePreview(isIncome = true, hasNote = true, isNoteExpanded = true)

@Composable
private fun BadgePreview(isIncome: Boolean, isActive: Boolean) {
    SpendLessTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .then(PreviewPadding),
        ) {
            NoteBadge(isIncome = isIncome, isActive = isActive)
        }
    }
}

/** `primaryFixedDim`. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5064 */
@Preview
@Composable
private fun NoteBadgeExpenseSubduedPreview() = BadgePreview(isIncome = false, isActive = false)

/** `primaryContainer`. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5069 */
@Preview
@Composable
private fun NoteBadgeExpenseActivePreview() = BadgePreview(isIncome = false, isActive = true)

/** `secondaryFixedDim`. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5093 */
@Preview
@Composable
private fun NoteBadgeIncomeSubduedPreview() = BadgePreview(isIncome = true, isActive = false)

/** `secondary`. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5126 */
@Preview
@Composable
private fun NoteBadgeIncomeActivePreview() = BadgePreview(isIncome = true, isActive = true)

// endregion
