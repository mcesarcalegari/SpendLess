package com.plcoding.spendlesssample.transactions.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.plcoding.spendlesssample.transactions.presentation.list.component.TransactionRow
import com.plcoding.spendlesssample.transactions.presentation.list.model.DayGroupUi
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel

/** Space between rows, and between a header and its first row (`2417:12043`). */
private val ItemSpacing = 12.dp

/** Extra space under the last row of each day (`2417:12047`, `pb-8`). */
private val GroupBottomPadding = 8.dp

/** Scaffold places the FAB 16dp from the end; Figma has it at 24dp (`2417:12067`). */
private val FabExtraEndPadding = 8.dp

/**
 * Hosts [TransactionListScreen] with its ViewModel. Back goes straight to [onNavigateBack].
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12042
 */
@Composable
fun TransactionListRoot(
    onNavigateBack: () -> Unit,
    viewModel: TransactionListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionListScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        // The create sheet is hosted here once User Story 1 builds it.
        createSheetContent = {},
    )
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
}

/**
 * The grouped list. Each day is a sticky header followed by its rows (FR-008).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12043
 */
@Composable
private fun TransactionList(
    groups: List<DayGroupUi>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
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
                TransactionRow(
                    item = item,
                    isExpanded = false,
                    onNoteBadgeClick = {},
                    onLongPress = null,
                    modifier = if (item.id == lastId) Modifier.padding(bottom = GroupBottomPadding) else Modifier,
                )
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
    listState: LazyListState = rememberLazyListState(),
) {
    SpendLessTheme {
        TransactionListScreen(
            state = TransactionListState(groups = groups.toImmutableList()),
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
