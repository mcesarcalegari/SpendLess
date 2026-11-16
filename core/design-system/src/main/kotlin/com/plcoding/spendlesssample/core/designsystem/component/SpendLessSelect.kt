package com.plcoding.spendlesssample.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessShadows
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.designsystem.theme.spendLessShadow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * One Select option. [label] is the list text (e.g. "Weekly on Monday"). The "Icon" list style shows
 * [leadingEmoji] in a tile; the "Text" style shows [leadingSymbol] (e.g. "$") followed by
 * [supportingText].
 */
@Immutable
data class SelectOption<T>(
    val value: T,
    val label: String,
    val leadingEmoji: String? = null,
    val leadingSymbol: String? = null,
    val supportingText: String? = null,
)

/** Row layout of the Select list: start padding 4 (Icon), 16 (Text) or 52 (TextIndented). */
enum class SelectListItemStyle(internal val startPadding: Dp) {
    Icon(4.dp),
    Text(16.dp),
    TextIndented(52.dp),
}

private val SelectShape = RoundedCornerShape(16.dp)
private val TileShape = RoundedCornerShape(12.dp)
private val AnchorHeight = 48.dp
private val RowHeight = 48.dp
private val TileSize = 40.dp
private val ContentGap = 8.dp
private val EndPadding = 12.dp
private val ArrowSize = 24.dp
private val CheckSize = 24.dp
private val ListGap = 4.dp
private const val MaxVisibleRows = 5
private val ScrollbarWidth = 4.dp
private val ScrollbarHeight = 100.dp
private val ScrollbarEndInset = 2.dp
private val ScrollbarTopInset = 14.dp

/** Space around the popup surface so its drop shadow is not clipped by the popup window. */
private val PopupShadowRoom = 16.dp

/**
 * The SpendLess Select (dropdown): a 48dp anchor with the `select` shadow and a list that opens 4dp
 * below it at the anchor's width. The arrow never flips (research F7). Figma draws no press state
 * for the anchor or rows, so neither has press feedback (research R15 D7).
 *
 * The anchor uses the Icon type (40dp emoji tile) when [fieldLeadingEmoji] is set, otherwise the
 * Text type, which shows [selectedText] and the selected option's supporting text (research R15 D9).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10171
 * Figma (Icon, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10170
 * Figma (Icon, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10169
 * Figma (Text, Enabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10277
 * Figma (Text, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10331
 * Figma (Select-list): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10118
 * Figma (list-item): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10072
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SpendLessSelect(
    options: ImmutableList<SelectOption<T>>,
    selected: T,
    selectedText: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fieldLeadingEmoji: String? = null,
    leadingTileColor: Color = MaterialTheme.colorScheme.primaryFixed,
    listItemStyle: SelectListItemStyle = SelectListItemStyle.Icon,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MaterialTheme.colorScheme
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val selectedSupporting = if (fieldLeadingEmoji == null) {
        options.firstOrNull { it.value == selected }?.supportingText
    } else {
        null
    }

    Box(modifier = modifier) {
        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            Surface(
                onClick = { onExpandedChange(!isExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AnchorHeight)
                    .onSizeChanged { anchorWidthPx = it.width }
                    .spendLessShadow(SpendLessShadows.select, SelectShape)
                    .semantics { role = Role.DropdownList },
                shape = SelectShape,
                color = colors.surfaceContainerLowest,
                contentColor = colors.onSurface,
                interactionSource = interactionSource,
            ) {
                SelectAnchorContent(
                    selectedText = selectedText,
                    supportingText = selectedSupporting,
                    leadingEmoji = fieldLeadingEmoji,
                    leadingTileColor = leadingTileColor,
                )
            }
        }

        if (isExpanded) {
            val density = LocalDensity.current
            val positionProvider = remember(density) {
                SelectPopupPositionProvider(
                    gapPx = with(density) { ListGap.roundToPx() },
                    shadowRoomPx = with(density) { PopupShadowRoom.roundToPx() },
                )
            }
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { onExpandedChange(false) },
                properties = PopupProperties(focusable = true),
            ) {
                Box(modifier = Modifier.padding(PopupShadowRoom)) {
                    SelectList(
                        options = options,
                        selected = selected,
                        style = listItemStyle,
                        leadingTileColor = leadingTileColor,
                        onPick = { value ->
                            onSelect(value)
                            onExpandedChange(false)
                        },
                        modifier = Modifier.width(with(density) { anchorWidthPx.toDp() }),
                    )
                }
            }
        }
    }
}

/** Places the list 4dp below the anchor, aligned to its start edge, leaving room for the shadow. */
private class SelectPopupPositionProvider(
    private val gapPx: Int,
    private val shadowRoomPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = anchorBounds.left - shadowRoomPx,
        y = anchorBounds.bottom + gapPx - shadowRoomPx,
    )
}

/**
 * The anchor's content row.
 *
 * Figma (Icon type): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10170
 * Figma (Text type): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10277
 */
@Composable
private fun SelectAnchorContent(
    selectedText: String,
    supportingText: String?,
    leadingEmoji: String?,
    leadingTileColor: Color,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val startPadding = if (leadingEmoji != null) SelectListItemStyle.Icon.startPadding else SelectListItemStyle.Text.startPadding
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AnchorHeight)
            .padding(start = startPadding, end = EndPadding),
        horizontalArrangement = Arrangement.spacedBy(ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingEmoji != null) {
            EmojiTile(emoji = leadingEmoji, color = leadingTileColor)
            Text(
                text = selectedText,
                style = typography.labelMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(text = selectedText, style = typography.labelMedium, color = colors.onSurface, maxLines = 1)
            Text(
                text = supportingText.orEmpty(),
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Icon(
            painter = SpendLessIcons.ArrowDropDown,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(ArrowSize),
        )
    }
}

/**
 * The open list: `surfaceContainerLowest`, radius 16, `select` shadow, no vertical padding, at most
 * five 48dp rows, then scroll. The scrollbar shows only when the list can scroll (research R15 D8).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10118
 */
@Composable
private fun <T> SelectList(
    options: ImmutableList<SelectOption<T>>,
    selected: T,
    style: SelectListItemStyle,
    leadingTileColor: Color,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val scrollbarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Surface(
        modifier = modifier.spendLessShadow(SpendLessShadows.select, SelectShape),
        shape = SelectShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .heightIn(max = RowHeight * MaxVisibleRows)
                .selectListScrollbar(listState, scrollbarColor),
        ) {
            itemsIndexed(options, key = { index, _ -> index }) { _, option ->
                SelectListRow(
                    option = option,
                    isSelected = option.value == selected,
                    style = style,
                    leadingTileColor = leadingTileColor,
                    onClick = { onPick(option.value) },
                )
            }
        }
    }
}

/**
 * Draws the fixed 4×100dp thumb, 2dp from the end edge, moving along the track with the scroll
 * position. Reads scroll state only in the draw phase.
 */
private fun Modifier.selectListScrollbar(state: LazyListState, color: Color): Modifier = drawWithContent {
    drawContent()
    val canScroll = state.canScrollForward || state.canScrollBackward
    if (!canScroll) return@drawWithContent
    val layout = state.layoutInfo
    val rowPx = RowHeight.toPx()
    val contentPx = layout.totalItemsCount * rowPx
    val viewportPx = size.height
    val scrolledPx = state.firstVisibleItemIndex * rowPx + state.firstVisibleItemScrollOffset
    val fraction = (scrolledPx / (contentPx - viewportPx)).coerceIn(0f, 1f)
    val thumbHeight = ScrollbarHeight.toPx()
    val inset = ScrollbarTopInset.toPx()
    val travel = (viewportPx - 2 * inset - thumbHeight).coerceAtLeast(0f)
    val width = ScrollbarWidth.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - ScrollbarEndInset.toPx() - width, inset + travel * fraction),
        size = Size(width, thumbHeight),
        cornerRadius = CornerRadius(width / 2, width / 2),
    )
}

/**
 * One list row: 48dp, end padding 12, gap 8, and a `primary` check on the selected row.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10072
 */
@Composable
private fun <T> SelectListRow(
    option: SelectOption<T>,
    isSelected: Boolean,
    style: SelectListItemStyle,
    leadingTileColor: Color,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .selectable(
                selected = isSelected,
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .padding(start = style.startPadding, end = EndPadding),
        horizontalArrangement = Arrangement.spacedBy(ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (style) {
            SelectListItemStyle.Icon -> {
                if (option.leadingEmoji != null) EmojiTile(emoji = option.leadingEmoji, color = leadingTileColor)
                RowText(option.label, Modifier.weight(1f))
            }
            SelectListItemStyle.Text -> {
                Text(
                    text = option.leadingSymbol ?: option.label,
                    style = typography.labelMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = option.supportingText.orEmpty(),
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            SelectListItemStyle.TextIndented -> RowText(option.label, Modifier.weight(1f))
        }
        if (isSelected) {
            Icon(
                painter = SpendLessIcons.Check,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(CheckSize),
            )
        }
    }
}

@Composable
private fun RowText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** 40dp, radius-12 tile with a centred emoji in `titleLarge`. */
@Composable
private fun EmojiTile(emoji: String, color: Color) {
    Box(
        modifier = Modifier
            .size(TileSize)
            .background(color, TileShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

// region Previews

private val PreviewCategories = persistentListOf(
    SelectOption("clothing", "Clothing & Accessories", leadingEmoji = "👔"),
    SelectOption("education", "Education", leadingEmoji = "🎓"),
    SelectOption("entertainment", "Entertainment", leadingEmoji = "💻"),
    SelectOption("food", "Food & Groceries", leadingEmoji = "🍕"),
    SelectOption("health", "Health & Wellness", leadingEmoji = "❤️"),
    SelectOption("home", "Home", leadingEmoji = "🏠"),
    SelectOption("other", "Other", leadingEmoji = "⚙️"),
)

private val PreviewCurrencies = persistentListOf(
    SelectOption("usd", "US Dollar (USD)", leadingSymbol = "$", supportingText = "US Dollar (USD)"),
    SelectOption("eur", "Euro (EUR)", leadingSymbol = "€", supportingText = "Euro (EUR)"),
    SelectOption("gbp", "British Pound Sterling (GBP)", leadingSymbol = "£", supportingText = "British Pound Sterling (GBP)"),
    SelectOption("jpy", "Japanese Yen (JPY)", leadingSymbol = "¥", supportingText = "Japanese Yen (JPY)"),
    SelectOption("chf", "Swiss Franc (CHF)", leadingSymbol = "CHF", supportingText = "Swiss Franc (CHF)"),
)

private val PreviewRepeat = persistentListOf(
    SelectOption("none", "Does not repeat"),
    SelectOption("daily", "Daily"),
    SelectOption("weekly", "Weekly on Monday"),
    SelectOption("monthly", "Monthly on the 14th"),
    SelectOption("yearly", "Yearly on Feb 14th"),
)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10170 */
@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessSelectIconEnabledPreview() {
    SpendLessTheme {
        Box(Modifier.padding(16.dp)) {
            SpendLessSelect(
                options = PreviewCategories,
                selected = "entertainment",
                selectedText = "Entertainment",
                isExpanded = false,
                onExpandedChange = {},
                onSelect = {},
                fieldLeadingEmoji = "💻",
            )
        }
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10277 */
@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessSelectTextEnabledPreview() {
    SpendLessTheme {
        Box(Modifier.padding(16.dp)) {
            SpendLessSelect(
                options = PreviewCurrencies,
                selected = "usd",
                selectedText = "$",
                isExpanded = false,
                onExpandedChange = {},
                onSelect = {},
                listItemStyle = SelectListItemStyle.Text,
            )
        }
    }
}

/** Repeat anchor. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-11244 */
@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessSelectRepeatAnchorPreview() {
    SpendLessTheme {
        Box(Modifier.padding(16.dp)) {
            SpendLessSelect(
                options = PreviewRepeat,
                selected = "none",
                selectedText = "Does not repeat",
                isExpanded = false,
                onExpandedChange = {},
                onSelect = {},
                fieldLeadingEmoji = "🔄",
                leadingTileColor = MaterialTheme.colorScheme.tertiaryContainer,
                listItemStyle = SelectListItemStyle.TextIndented,
            )
        }
    }
}

@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessSelectLongLabelPreview() {
    SpendLessTheme {
        Box(Modifier.padding(16.dp)) {
            SpendLessSelect(
                options = PreviewCategories,
                selected = "clothing",
                selectedText = "Clothing & Accessories and a much longer category name",
                isExpanded = false,
                onExpandedChange = {},
                onSelect = {},
                fieldLeadingEmoji = "👔",
            )
        }
    }
}

/** Open lists are rendered through their list composable; a Popup does not render in a static preview. */
@Composable
private fun SelectListPreview(
    options: ImmutableList<SelectOption<String>>,
    selected: String,
    style: SelectListItemStyle,
    tertiaryTile: Boolean = false,
) {
    SpendLessTheme {
        val colors = MaterialTheme.colorScheme
        Box(Modifier.padding(16.dp)) {
            SelectList(
                options = options,
                selected = selected,
                style = style,
                leadingTileColor = if (tertiaryTile) colors.tertiaryContainer else colors.primaryFixed,
                onPick = {},
                modifier = Modifier.width(380.dp),
            )
        }
    }
}

/** Icon type, more than five rows (scrollbar visible). Figma (Icon, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10169 */
@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessSelectListIconScrollingPreview() =
    SelectListPreview(PreviewCategories, selected = "entertainment", style = SelectListItemStyle.Icon)

/** Text type, five rows (no scrollbar). Figma (Text, Focused): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10331 */
@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessSelectListTextPreview() =
    SelectListPreview(PreviewCurrencies, selected = "usd", style = SelectListItemStyle.Text)

/** Repeat list (TextIndented). Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-11280 */
@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessSelectListTextIndentedPreview() =
    SelectListPreview(PreviewRepeat, selected = "weekly", style = SelectListItemStyle.TextIndented)

@Preview(widthDp = 412, showBackground = true)
@Composable
private fun SpendLessSelectListFewRowsPreview() =
    SelectListPreview(
        persistentListOf(PreviewCategories[0], PreviewCategories[1], PreviewCategories[2]),
        selected = "education",
        style = SelectListItemStyle.Icon,
    )

@Composable
private fun AnchorContentPreview(text: String, supporting: String?, emoji: String?, tertiaryTile: Boolean = false) {
    SpendLessTheme {
        val colors = MaterialTheme.colorScheme
        SelectAnchorContent(
            selectedText = text,
            supportingText = supporting,
            leadingEmoji = emoji,
            leadingTileColor = if (tertiaryTile) colors.tertiaryContainer else colors.primaryFixed,
        )
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10170 */
@Preview(widthDp = 380, showBackground = true)
@Composable
private fun SelectAnchorContentIconPreview() =
    AnchorContentPreview("Entertainment", null, "💻")

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10277 */
@Preview(widthDp = 380, showBackground = true)
@Composable
private fun SelectAnchorContentTextPreview() =
    AnchorContentPreview("$", "US Dollar (USD)", null)

@Preview(widthDp = 380, showBackground = true)
@Composable
private fun SelectAnchorContentTextWithoutSupportingPreview() =
    AnchorContentPreview("$", null, null)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-11244 */
@Preview(widthDp = 380, showBackground = true)
@Composable
private fun SelectAnchorContentRepeatTilePreview() =
    AnchorContentPreview("Does not repeat", null, "🔄", tertiaryTile = true)

@Preview(widthDp = 380, showBackground = true)
@Composable
private fun SelectAnchorContentLongTextPreview() =
    AnchorContentPreview("Clothing & Accessories and a much longer category name", null, "👔")

/** Category tile. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10126 */
@Preview(showBackground = true)
@Composable
private fun EmojiTilePrimaryFixedPreview() {
    SpendLessTheme { EmojiTile(emoji = "💻", color = MaterialTheme.colorScheme.primaryFixed) }
}

/** Repeat tile. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-11244 */
@Preview(showBackground = true)
@Composable
private fun EmojiTileTertiaryContainerPreview() {
    SpendLessTheme { EmojiTile(emoji = "🔄", color = MaterialTheme.colorScheme.tertiaryContainer) }
}

@Preview(widthDp = 200, showBackground = true)
@Composable
private fun RowTextShortPreview() {
    SpendLessTheme { RowText(text = "Education") }
}

@Preview(widthDp = 200, showBackground = true)
@Composable
private fun RowTextLongPreview() {
    SpendLessTheme { RowText(text = "Clothing & Accessories and more", modifier = Modifier.width(160.dp)) }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10072 */
@Preview(widthDp = 380, showBackground = true)
@Composable
private fun SelectListRowUnselectedPreview() {
    SpendLessTheme {
        SelectListRow(PreviewCategories[0], isSelected = false, style = SelectListItemStyle.Icon, leadingTileColor = MaterialTheme.colorScheme.primaryFixed, onClick = {})
    }
}

@Preview(widthDp = 380, showBackground = true)
@Composable
private fun SelectListRowSelectedPreview() {
    SpendLessTheme {
        SelectListRow(PreviewCategories[2], isSelected = true, style = SelectListItemStyle.Icon, leadingTileColor = MaterialTheme.colorScheme.primaryFixed, onClick = {})
    }
}

@Preview(widthDp = 380, showBackground = true)
@Composable
private fun SelectListRowTextSelectedPreview() {
    SpendLessTheme {
        SelectListRow(PreviewCurrencies[0], isSelected = true, style = SelectListItemStyle.Text, leadingTileColor = MaterialTheme.colorScheme.primaryFixed, onClick = {})
    }
}

@Preview(widthDp = 380, showBackground = true)
@Composable
private fun SelectListRowTextIndentedPreview() {
    SpendLessTheme {
        SelectListRow(PreviewRepeat[1], isSelected = false, style = SelectListItemStyle.TextIndented, leadingTileColor = MaterialTheme.colorScheme.tertiaryContainer, onClick = {})
    }
}

@Preview(widthDp = 380, showBackground = true)
@Composable
private fun SelectListRowLongLabelPreview() {
    SpendLessTheme {
        SelectListRow(
            SelectOption("x", "An exceptionally long option label that cannot fit on one row", leadingEmoji = "⚙️"),
            isSelected = true,
            style = SelectListItemStyle.Icon,
            leadingTileColor = MaterialTheme.colorScheme.primaryFixed,
            onClick = {},
        )
    }
}

// endregion
