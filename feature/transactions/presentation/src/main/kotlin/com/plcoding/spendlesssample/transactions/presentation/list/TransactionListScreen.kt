package com.plcoding.spendlesssample.transactions.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import com.plcoding.spendlesssample.transactions.presentation.create.CreateTransactionAction
import com.plcoding.spendlesssample.transactions.presentation.create.CreateTransactionRoot
import com.plcoding.spendlesssample.transactions.presentation.create.CreateTransactionViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plcoding.spendlesssample.core.designsystem.component.SpendLessFloatingActionButton
import com.plcoding.spendlesssample.core.designsystem.component.SpendLessTopAppBar
import com.plcoding.spendlesssample.core.designsystem.component.SpendLessTopAppBarIconButton
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.presentation.R
import com.plcoding.spendlesssample.transactions.presentation.list.component.DayHeader
import com.plcoding.spendlesssample.transactions.presentation.list.component.StopRepeatingDialog
import com.plcoding.spendlesssample.transactions.presentation.list.component.StopRepeatingMenu
import com.plcoding.spendlesssample.transactions.presentation.list.component.TransactionRow
import com.plcoding.spendlesssample.transactions.presentation.list.model.DayGroupUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import org.koin.androidx.compose.koinViewModel

/** Space between rows, and between a header and its first row (`2417:12043`). */
private val ItemSpacing = 12.dp

/** Extra space under the last row of each day (`2417:12047`, `pb-8`). */
private val GroupBottomPadding = 8.dp

/** Scaffold places the FAB 16dp from the end; Figma has it at 24dp (`2417:12067`). */
private val FabExtraEndPadding = 8.dp

/** Create sheet tokens (`2415:6631`, research R7). */
private val SheetTopGap = 8.dp
private val SheetCornerRadius = 28.dp
private const val SheetScrimAlpha = 0.32f

/**
 * Hosts [TransactionListScreen] with its ViewModel. Back goes straight to [onNavigateBack].
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12042
 */
@Composable
fun TransactionListRoot(
    onNavigateBack: () -> Unit,
    viewModel: TransactionListViewModel = koinViewModel(),
    createViewModel: CreateTransactionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionListScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        createSheetContent = {
            CreateTransactionSheet(
                createViewModel = createViewModel,
                onDismissed = { viewModel.onAction(TransactionListAction.OnCreateSheetDismiss) },
            )
        },
    )
}

/**
 * The create sheet (research R7): an M3 modal sheet in `surfaceContainerLow` with 28dp top
 * corners, no drag handle, an `onBackground`@32% scrim, never half-expanded, and 8dp below the
 * status bar. Its insets are zero and it has no IME padding, so the keyboard overlays its lower
 * part instead of reflowing it (`2415:6533`).
 *
 * [createViewModel] belongs to the list's nav entry, so every way of closing resets its form:
 * ✕ (in [CreateTransactionRoot]), scrim tap, swipe and back here, and `Created` in the ViewModel.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6629
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTransactionSheet(
    createViewModel: CreateTransactionViewModel,
    onDismissed: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThenDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissed() }
    }
    ModalBottomSheet(
        onDismissRequest = {
            createViewModel.onAction(CreateTransactionAction.OnCloseClick)
            onDismissed()
        },
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = SheetTopGap),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = SheetScrimAlpha),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        CreateTransactionRoot(
            onClose = hideThenDismiss,
            onCreated = hideThenDismiss,
            viewModel = createViewModel,
        )
    }
}

/**
 * The All Transactions screen: a top bar with back, every transaction grouped by day with pinned
 * headers (FR-001 to FR-008), and the add FAB that opens the create sheet (FR-009). The export
 * action drawn in Figma is omitted until the export feature ships (FR-009, F8).
 *
 * [listState] is exposed only so a preview can start scrolled (`2417:12068`).
 *
 * Figma (default): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12042
 * Figma (scrolled, pinned header): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12068
 */
@Composable
fun TransactionListScreen(
    state: TransactionListState,
    onAction: (TransactionListAction) -> Unit,
    onNavigateBack: () -> Unit,
    createSheetContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SpendLessTopAppBar(
                title = stringResource(R.string.all_transactions),
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    SpendLessTopAppBarIconButton(
                        icon = SpendLessIcons.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        onClick = onNavigateBack,
                    )
                },
            )
        },
        floatingActionButton = {
            SpendLessFloatingActionButton(
                icon = SpendLessIcons.Add,
                contentDescription = stringResource(R.string.cd_add_transaction),
                onClick = { onAction(TransactionListAction.OnCreateClick) },
                modifier = Modifier.padding(end = FabExtraEndPadding),
            )
        },
    ) { innerPadding ->
        TransactionList(
            groups = state.groups,
            expandedIds = state.expandedIds,
            onNoteBadgeClick = { id -> onAction(TransactionListAction.OnNoteBadgeClick(id)) },
            contextMenuFor = state.contextMenuFor,
            onRowLongPress = { id -> onAction(TransactionListAction.OnRowLongPress(id)) },
            onStopRepeatingClick = { onAction(TransactionListAction.OnStopRepeatingClick) },
            onContextMenuDismiss = { onAction(TransactionListAction.OnContextMenuDismiss) },
            listState = listState,
            modifier = Modifier
                .fillMaxSize()
                // Top as a modifier, so pinned headers stick right under the top bar.
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding(),
            ),
        )
    }
    if (state.isCreateSheetOpen) {
        createSheetContent()
    }
    state.stopConfirmationFor?.let { confirmation ->
        StopRepeatingDialog(
            name = confirmation.name,
            onStopClick = { onAction(TransactionListAction.OnStopConfirm) },
            onCancel = { onAction(TransactionListAction.OnStopCancel) },
        )
    }
}

/**
 * The grouped list. Each day is a sticky header followed by its rows (FR-008). Rows of a running
 * recurring series take a long press, which opens the stop-repeating menu under the row whose id
 * is [contextMenuFor] (FR-037); other rows ignore it.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12043
 */
@Composable
private fun TransactionList(
    groups: ImmutableList<DayGroupUi>,
    expandedIds: ImmutableSet<Long>,
    onNoteBadgeClick: (Long) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    contextMenuFor: Long? = null,
    onRowLongPress: (Long) -> Unit = {},
    onStopRepeatingClick: () -> Unit = {},
    onContextMenuDismiss: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        groups.forEach { group ->
            // Typed differently from row ids, so a header key never collides with a row key.
            stickyHeader(key = "day_${group.dateKey}", contentType = ContentTypeHeader) {
                DayHeader(text = group.header.asString())
            }
            val lastId = group.items.last().id
            items(
                items = group.items,
                key = { it.id },
                contentType = { ContentTypeRow },
            ) { item ->
                // The outer box holds the group's bottom padding; the menu anchors to the inner
                // one, which is exactly the row.
                Box(modifier = if (item.id == lastId) Modifier.padding(bottom = GroupBottomPadding) else Modifier) {
                    Box {
                        TransactionRow(
                            item = item,
                            isExpanded = item.id in expandedIds,
                            onNoteBadgeClick = { onNoteBadgeClick(item.id) },
                            onLongPress = if (item.isInRunningSeries) {
                                { onRowLongPress(item.id) }
                            } else {
                                null
                            },
                        )
                        if (item.id == contextMenuFor) {
                            StopRepeatingMenu(
                                onStopRepeatingClick = onStopRepeatingClick,
                                onDismiss = onContextMenuDismiss,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val ContentTypeHeader = "day_header"
private const val ContentTypeRow = "transaction"

// region Previews

@Composable
private fun ScreenPreview(
    groups: List<DayGroupUi> = TransactionListPreviewData.groups,
    expandedIds: Set<Long> = emptySet(),
    listState: LazyListState = rememberLazyListState(),
    contextMenuFor: Long? = null,
    stopConfirmationFor: StopConfirmationUi? = null,
) {
    SpendLessTheme {
        TransactionListScreen(
            state = TransactionListState(
                groups = groups.toImmutableList(),
                expandedIds = expandedIds.toImmutableSet(),
                contextMenuFor = contextMenuFor,
                stopConfirmationFor = stopConfirmationFor,
            ),
            onAction = {},
            onNavigateBack = {},
            createSheetContent = {},
            listState = listState,
        )
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12043 */
@Preview(widthDp = 412, heightDp = 768)
@Composable
private fun TransactionListPopulatedPreview() {
    SpendLessTheme {
        TransactionList(
            groups = TransactionListPreviewData.groups,
            expandedIds = persistentSetOf(),
            onNoteBadgeClick = {},
            listState = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        )
    }
}

/** The TODAY expense and YESTERDAY income notes expanded. */
@Preview(widthDp = 412, heightDp = 768)
@Composable
private fun TransactionListExpandedNotesPreview() {
    SpendLessTheme {
        TransactionList(
            groups = TransactionListPreviewData.groups,
            expandedIds = persistentSetOf(
                TransactionListPreviewData.starbucksWithNote.id,
                TransactionListPreviewData.ricksShareWithNote.id,
            ),
            onNoteBadgeClick = {},
            listState = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        )
    }
}

@Preview(widthDp = 412, heightDp = 200)
@Composable
private fun TransactionListEmptyPreview() {
    SpendLessTheme {
        TransactionList(
            groups = persistentListOf(),
            expandedIds = persistentSetOf(),
            onNoteBadgeClick = {},
            listState = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
    }
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12042 */
@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TransactionListScreenPopulatedPreview() {
    ScreenPreview()
}

/**
 * Scrolled past the TODAY header, which stays pinned over the first row. The static preview
 * render was observed to come out unscrolled (cause not confirmed), so the scrolled state may
 * show only in the interactive preview or on a device; `TransactionListScreenTest` checks the
 * pinning itself.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12068
 */
@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TransactionListScreenPinnedHeaderPreview() {
    ScreenPreview(listState = rememberLazyListState(initialFirstVisibleItemIndex = 1))
}

/**
 * One expanded expense and one expanded income, between collapsed rows with and without a note.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5142
 */
@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TransactionListScreenExpandedNotesPreview() {
    val expense = TransactionListPreviewData.starbucksWithNote
    val income = TransactionListPreviewData.ricksShareWithNote
    ScreenPreview(
        groups = listOf(
            DayGroupUi(
                dateKey = 2,
                header = UiText.DynamicString("TODAY"),
                items = persistentListOf(TransactionListPreviewData.amazon, expense, income),
            ),
            DayGroupUi(
                dateKey = 1,
                header = UiText.DynamicString("YESTERDAY"),
                items = persistentListOf(
                    TransactionListPreviewData.expense(
                        title = "Netflix",
                        category = ExpenseCategory.ENTERTAINMENT,
                        amount = "-$15.99",
                        note = "Family plan",
                    ),
                    TransactionListPreviewData.gym,
                ),
            ),
        ),
        expandedIds = setOf(expense.id, income.id),
    )
}

/**
 * The stop-repeating menu open under the Gym row. The menu is a popup, which a static preview may
 * not draw; `StopRepeatingMenu` previews its surface. No Figma node (research R11).
 */
@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TransactionListScreenStopMenuOpenPreview() {
    ScreenPreview(contextMenuFor = TransactionListPreviewData.gym.id)
}

/**
 * The confirmation dialog over the list. A dialog is a separate window, which a static preview may
 * not draw; `StopRepeatingDialog` previews it. No Figma node (research R11).
 */
@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TransactionListScreenStopDialogPreview() {
    ScreenPreview(
        stopConfirmationFor = StopConfirmationUi(
            seriesId = TransactionListPreviewData.gym.id,
            name = TransactionListPreviewData.gym.title,
        ),
    )
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TransactionListScreenEmptyPreview() {
    ScreenPreview(groups = emptyList())
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TransactionListScreenLongNamePreview() {
    ScreenPreview(
        groups = listOf(
            DayGroupUi(
                dateKey = 1,
                header = UiText.DynamicString("TODAY"),
                items = persistentListOf(
                    TransactionListPreviewData.expense(
                        title = "A very long receiver name that cannot fit in one line",
                        category = ExpenseCategory.EDUCATION,
                        amount = "-$1,234.50",
                    ),
                    TransactionListPreviewData.income(
                        title = "A very long sender name that cannot fit in one line either",
                        amount = "$20.00",
                    ),
                ),
            ),
        ),
    )
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TransactionListScreenMaxAmountPreview() {
    ScreenPreview(
        groups = listOf(
            DayGroupUi(
                dateKey = 1,
                header = UiText.DynamicString("TODAY"),
                items = persistentListOf(
                    TransactionListPreviewData.expense(
                        title = "Gym - Monthly Membership",
                        category = ExpenseCategory.HEALTH_AND_WELLNESS,
                        amount = "-$999,999,999.99",
                    ),
                    TransactionListPreviewData.income(title = "Rick’s share - Birthday M.", amount = "$999,999,999.99"),
                ),
            ),
        ),
    )
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TransactionListScreenParenthesesPreview() {
    ScreenPreview(
        groups = listOf(
            DayGroupUi(
                dateKey = 1,
                header = UiText.DynamicString("TODAY"),
                items = persistentListOf(
                    TransactionListPreviewData.expense("Amazon", ExpenseCategory.HOME, "($45.99)"),
                    TransactionListPreviewData.expense("Starbucks", ExpenseCategory.FOOD_AND_GROCERIES, "($7.50)"),
                    TransactionListPreviewData.income("Rick’s share - Birthday M.", "$20.00"),
                ),
            ),
        ),
    )
}

// endregion
