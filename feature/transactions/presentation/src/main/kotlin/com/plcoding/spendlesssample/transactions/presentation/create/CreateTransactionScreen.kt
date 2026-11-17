package com.plcoding.spendlesssample.transactions.presentation.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plcoding.spendlesssample.core.designsystem.component.SegmentedOption
import com.plcoding.spendlesssample.core.designsystem.component.SelectOption
import com.plcoding.spendlesssample.core.designsystem.component.SpendLessButton
import com.plcoding.spendlesssample.core.designsystem.component.SpendLessSegmentedButton
import com.plcoding.spendlesssample.core.designsystem.component.SpendLessSelect
import com.plcoding.spendlesssample.core.designsystem.component.SpendLessTopAppBar
import com.plcoding.spendlesssample.core.designsystem.component.SpendLessTopAppBarIconButton
import com.plcoding.spendlesssample.core.designsystem.icon.SpendLessIcons
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.presentation.ObserveAsEvents
import com.plcoding.spendlesssample.core.presentation.R as CoreR
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.presentation.R
import com.plcoding.spendlesssample.transactions.presentation.create.component.AmountInput
import com.plcoding.spendlesssample.transactions.presentation.create.component.CounterpartyInput
import com.plcoding.spendlesssample.transactions.presentation.create.component.NoteInput
import com.plcoding.spendlesssample.transactions.presentation.mapper.CategoryUiMapper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel

/** Sheet content: `py 4dp`, `gap 4dp` (`2415:6631`). */
private val SheetVerticalPadding = 4.dp
private val SheetGap = 4.dp

/** Offsets inside the content area, from the frame's absolute positions (research R7). */
private val SegmentedToInputs = 34.dp
private val InputsWidth = 300.dp
private val InputsGap = 12.dp
private val InputsToFilters = 32.dp
private val FiltersHeight = 104.dp
private val FiltersGap = 8.dp
private val SelectHeight = 48.dp
private val FiltersToCreate = 12.dp

/** Same gap as the DS Text field's supporting text. */
private val ErrorTopPadding = 7.dp

/**
 * Hosts [CreateTransactionScreen] with its ViewModel. ✕ resets the form and calls [onClose];
 * a successful create (the ViewModel has already reset the form) calls [onCreated].
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6629
 */
@Composable
fun CreateTransactionRoot(
    onClose: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateTransactionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CreateTransactionEvent.Created -> onCreated()
        }
    }
    CreateTransactionScreen(
        state = state,
        onAction = viewModel::onAction,
        onCloseClick = {
            viewModel.onAction(CreateTransactionAction.OnCloseClick)
            onClose()
        },
    )
}

/**
 * Content of the Create Transaction sheet (research R7): the title bar with ✕, the Expense /
 * Income toggle, the centred name, amount and note inputs, the bottom-aligned 104dp filters block
 * and the full-width Create button. A storage failure is shown under Create in the DS Text field's
 * supporting-text style, as Figma has no error design (FR-022a). The sheet surface itself is drawn
 * by the host.
 *
 * On open the amount field takes focus and the keyboard shows (FR-021). Opening a Select clears
 * focus, which hides the keyboard (`2415:9770`).
 *
 * Figma (sheet): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6629
 * Figma (empty, `-$10`): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6533
 * Figma (empty, `($10)`): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10959
 * Figma (typing name): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-7218
 * Figma (category list open): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-9674
 * Figma (filled, repeat list open): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-7873
 * Figma (filled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2443-9436
 * Figma (filled, income): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8542
 * Figma (title bar): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6632
 * Figma (type toggle): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12817
 * Figma (Create, disabled): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=406-271
 */
@Composable
fun CreateTransactionScreen(
    state: CreateTransactionState,
    onAction: (CreateTransactionAction) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isPreview = LocalInspectionMode.current

    // Compose-internal: focus the amount and show the keyboard when the sheet opens (FR-021).
    // Waits one frame so the field is attached inside the sheet's window.
    LaunchedEffect(Unit) {
        if (isPreview) return@LaunchedEffect
        withFrameNanos { }
        amountFocusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = SheetVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(SheetGap),
    ) {
        SpendLessTopAppBar(
            title = stringResource(R.string.create_transaction),
            actions = {
                SpendLessTopAppBarIconButton(
                    icon = SpendLessIcons.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    onClick = onCloseClick,
                )
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TypeToggle(
                selected = state.type,
                onSelect = { onAction(CreateTransactionAction.OnTypeSelect(it)) },
            )
            Spacer(modifier = Modifier.height(SegmentedToInputs))
            Column(
                modifier = Modifier.width(InputsWidth),
                verticalArrangement = Arrangement.spacedBy(InputsGap),
            ) {
                CounterpartyInput(
                    value = state.name,
                    placeholder = stringResource(
                        if (state.type == TransactionType.EXPENSE) R.string.receiver else R.string.sender,
                    ),
                    onValueChange = { onAction(CreateTransactionAction.OnNameChange(it)) },
                )
                AmountInput(
                    value = state.amountInput,
                    parts = state.amountParts,
                    onValueChange = { onAction(CreateTransactionAction.OnAmountChange(it)) },
                    focusRequester = amountFocusRequester,
                )
                NoteInput(
                    value = state.note,
                    isEditing = state.isNoteEditing,
                    onAddNoteClick = { onAction(CreateTransactionAction.OnAddNoteClick) },
                    onValueChange = { onAction(CreateTransactionAction.OnNoteChange(it)) },
                    onFocusLost = { onAction(CreateTransactionAction.OnNoteFocusLost) },
                )
            }
            Spacer(modifier = Modifier.height(InputsToFilters))
            FiltersBlock(
                category = state.category,
                isCategoryVisible = state.type == TransactionType.EXPENSE,
                isCategoryOpen = state.openSelect == OpenSelect.CATEGORY,
                onCategoryExpandedChange = { expand ->
                    if (expand) {
                        focusManager.clearFocus()
                        onAction(CreateTransactionAction.OnCategorySelectClick)
                    } else {
                        onAction(CreateTransactionAction.OnSelectDismiss)
                    }
                },
                onCategoryPick = { onAction(CreateTransactionAction.OnCategoryPick(it)) },
            )
            Spacer(modifier = Modifier.height(FiltersToCreate))
            SpendLessButton(
                text = stringResource(R.string.create),
                onClick = { onAction(CreateTransactionAction.OnCreateClick) },
                enabled = state.isCreateEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { error ->
                Text(
                    text = error.asString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = ErrorTopPadding),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Expense (`TrendingDown`) / Income (`TrendingUp`) toggle (FR-011).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12817
 */
@Composable
private fun TypeToggle(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = persistentListOf(
        SegmentedOption(TransactionType.EXPENSE, stringResource(R.string.expense), SpendLessIcons.TrendingDown),
        SegmentedOption(TransactionType.INCOME, stringResource(R.string.income), SpendLessIcons.TrendingUp),
    )
    SpendLessSegmentedButton(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The 104dp filters block, content bottom-aligned with an 8dp gap (research R7): the category
 * Select on top and the 48dp repeat slot below it. The repeat Select fills that slot in User
 * Story 5; until then the slot is reserved so Create keeps its position. For income the category
 * is hidden and the block keeps its height, so nothing jumps when the type changes (`2415:8638`).
 *
 * Figma (closed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10578
 * Figma (category list open): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-9786
 */
@Composable
private fun FiltersBlock(
    category: ExpenseCategory,
    isCategoryVisible: Boolean,
    isCategoryOpen: Boolean,
    onCategoryExpandedChange: (Boolean) -> Unit,
    onCategoryPick: (ExpenseCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(FiltersHeight),
        verticalArrangement = Arrangement.spacedBy(FiltersGap, Alignment.Bottom),
    ) {
        if (isCategoryVisible) {
            SpendLessSelect(
                options = categoryOptions(),
                selected = category,
                selectedText = CategoryUiMapper.label(category).asString(),
                isExpanded = isCategoryOpen,
                onExpandedChange = onCategoryExpandedChange,
                onSelect = onCategoryPick,
                fieldLeadingEmoji = CategoryUiMapper.emoji(category),
                leadingTileColor = MaterialTheme.colorScheme.primaryFixed,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Repeat slot, filled by the repeat Select in User Story 5.
        Spacer(modifier = Modifier.size(width = 0.dp, height = SelectHeight))
    }
}

@Composable
private fun categoryOptions(): ImmutableList<SelectOption<ExpenseCategory>> =
    ExpenseCategory.entries.map { category ->
        SelectOption(
            value = category,
            label = CategoryUiMapper.label(category).asString(),
            leadingEmoji = CategoryUiMapper.emoji(category),
        )
    }.toImmutableList()

// region Previews

/** The sheet surface the host draws (`2415:6631`): `surfaceContainerLow`, top corners 28dp, 832dp tall. */
@Composable
private fun SheetPreview(content: @Composable () -> Unit) {
    SpendLessTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ScreenPreview(state: CreateTransactionState) {
    SheetPreview {
        CreateTransactionScreen(state = state, onAction = {}, onCloseClick = {})
    }
}

private val FilledState = CreateTransactionState(
    name = "Netflix",
    amountInput = "100.00",
    amountParts = AmountPartsUi(number = "100.00", isPlaceholder = false),
    category = ExpenseCategory.ENTERTAINMENT,
    isCreateEnabled = true,
)

/**
 * Create is disabled on the empty form (F6).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6629
 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenEmptyPreview() = ScreenPreview(CreateTransactionState())

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-7314 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenTypingNamePreview() =
    ScreenPreview(CreateTransactionState(name = "Netflix"))

/**
 * The list opens in a popup, which a static preview may not draw; `SpendLessSelect` previews the
 * list itself.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-9770
 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenCategoryOpenPreview() = ScreenPreview(
    CreateTransactionState(
        name = "Netflix",
        category = ExpenseCategory.ENTERTAINMENT,
        openSelect = OpenSelect.CATEGORY,
    ),
)

/**
 * The repeat field is added in User Story 5, so its slot is empty here.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2443-9532
 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenFilledPreview() = ScreenPreview(FilledState)

/** No Figma frame (research R8). */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenNoteEditingPreview() = ScreenPreview(
    FilledState.copy(note = "Family plan, shared with Rick", isNoteEditing = true),
)

/** No Figma frame; owner-approved copy and style (FR-022a). */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenStorageErrorPreview() = ScreenPreview(
    FilledState.copy(error = UiText.StringResource(CoreR.string.error_disk_full)),
)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10578 */
@Preview(widthDp = 412)
@Composable
private fun FiltersBlockClosedPreview() = SheetPreview {
    FiltersBlock(
        category = ExpenseCategory.OTHER,
        isCategoryVisible = true,
        isCategoryOpen = false,
        onCategoryExpandedChange = {},
        onCategoryPick = {},
        modifier = Modifier.padding(16.dp),
    )
}

/** Figma (income sheet): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8638 */
@Preview(widthDp = 412)
@Composable
private fun FiltersBlockCategoryHiddenPreview() = SheetPreview {
    FiltersBlock(
        category = ExpenseCategory.OTHER,
        isCategoryVisible = false,
        isCategoryOpen = false,
        onCategoryExpandedChange = {},
        onCategoryPick = {},
        modifier = Modifier.padding(16.dp),
    )
}

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12817 */
@Preview(widthDp = 412)
@Composable
private fun TypeToggleExpensePreview() = SheetPreview {
    TypeToggle(selected = TransactionType.EXPENSE, onSelect = {}, modifier = Modifier.padding(16.dp))
}

/** Figma (income sheet): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8638 */
@Preview(widthDp = 412)
@Composable
private fun TypeToggleIncomePreview() = SheetPreview {
    TypeToggle(selected = TransactionType.INCOME, onSelect = {}, modifier = Modifier.padding(16.dp))
}

// endregion
