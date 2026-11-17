package com.plcoding.spendlesssample.transactions.presentation.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.plcoding.spendlesssample.core.designsystem.component.SelectListItemStyle
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
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.presentation.R
import com.plcoding.spendlesssample.transactions.presentation.create.component.AmountInput
import com.plcoding.spendlesssample.transactions.presentation.create.component.CounterpartyInput
import com.plcoding.spendlesssample.transactions.presentation.create.component.NoteInput
import com.plcoding.spendlesssample.transactions.presentation.format.RepeatLabelFormatter
import com.plcoding.spendlesssample.transactions.presentation.mapper.CategoryUiMapper
import java.time.LocalDate
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
private val FiltersToCreate = 12.dp

/** Repeat Select (`2415:11244`): 🔄 in a `tertiaryContainer` tile. */
private const val RepeatEmoji = "🔄"

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
 * Figma (filled, income, sheet): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8638
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
                repeat = state.repeat,
                repeatFieldLabel = state.repeatFieldLabel.asString(),
                repeatOptions = state.repeatOptions,
                isRepeatOpen = state.openSelect == OpenSelect.REPEAT,
                onRepeatExpandedChange = { expand ->
                    if (expand) {
                        focusManager.clearFocus()
                        onAction(CreateTransactionAction.OnRepeatSelectClick)
                    } else {
                        onAction(CreateTransactionAction.OnSelectDismiss)
                    }
                },
                onRepeatPick = { onAction(CreateTransactionAction.OnRepeatPick(it)) },
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
 * Expense (`TrendingDown`) / Income (`TrendingUp`) toggle (FR-011). Income switches the name
 * placeholder to "Sender", the amount sign to `$` in `success` and hides the category (User Story 4).
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
 * Select on top and the repeat Select below it. For income the category is hidden and the block
 * keeps its height, so the repeat field and Create stay put when the type changes (`2415:8638`).
 *
 * The repeat field shows the short frequency name ("Weekly", FR-020); its list shows the long
 * labels for today ("Weekly on Monday", FR-019) with the current one ticked.
 *
 * Figma (closed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10578
 * Figma (category list open): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-9786
 * Figma (repeat, closed): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-11244
 * Figma (repeat, open): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-11280
 */
@Composable
private fun FiltersBlock(
    category: ExpenseCategory,
    isCategoryVisible: Boolean,
    isCategoryOpen: Boolean,
    onCategoryExpandedChange: (Boolean) -> Unit,
    onCategoryPick: (ExpenseCategory) -> Unit,
    repeat: RepeatFrequency,
    repeatFieldLabel: String,
    repeatOptions: ImmutableList<RepeatOptionUi>,
    isRepeatOpen: Boolean,
    onRepeatExpandedChange: (Boolean) -> Unit,
    onRepeatPick: (RepeatFrequency) -> Unit,
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
        SpendLessSelect(
            options = repeatSelectOptions(repeatOptions),
            selected = repeat,
            selectedText = repeatFieldLabel,
            isExpanded = isRepeatOpen,
            onExpandedChange = onRepeatExpandedChange,
            onSelect = onRepeatPick,
            fieldLeadingEmoji = RepeatEmoji,
            leadingTileColor = MaterialTheme.colorScheme.tertiaryContainer,
            listItemStyle = SelectListItemStyle.TextIndented,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun repeatSelectOptions(
    options: ImmutableList<RepeatOptionUi>,
): ImmutableList<SelectOption<RepeatFrequency>> =
    options.map { SelectOption(value = it.frequency, label = it.label.asString()) }.toImmutableList()

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

/** Monday Feb 14 (2028): the date behind the Figma list labels ("Monthly on the 14th"). */
private val PreviewFormatter = RepeatLabelFormatter()
private val PreviewRepeatOptions = PreviewFormatter.options(LocalDate.of(2028, 2, 14))

/** Fresh sheet state as the ViewModel builds it: repeat options for today, field "Does not repeat". */
private val EmptyState = CreateTransactionState(repeatOptions = PreviewRepeatOptions)

private fun CreateTransactionState.withRepeat(frequency: RepeatFrequency) = copy(
    repeat = frequency,
    repeatFieldLabel = PreviewFormatter.shortLabel(frequency),
)

/** The filled frames (`2443:9532`, `2415:7969`) show Entertainment and Weekly. */
private val FilledState = EmptyState.copy(
    name = "Netflix",
    amountInput = "100.00",
    amountParts = AmountPartsUi(number = "100.00", isPlaceholder = false),
    category = ExpenseCategory.ENTERTAINMENT,
    isCreateEnabled = true,
).withRepeat(RepeatFrequency.WEEKLY)

/**
 * Create is disabled on the empty form (F6).
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6629
 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenEmptyPreview() = ScreenPreview(EmptyState)

/**
 * Empty, with the parentheses negative style (FR-032): `($` and `)` in `error` around the `00.00`
 * placeholder; everything else as the empty sheet, Create disabled (F6). The parts are what
 * `AmountFormatter.parts` builds for these preferences; preview state is static.
 *
 * Figma (sheet): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-11055
 * Figma (screen): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10959
 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenEmptyParenthesesPreview() = ScreenPreview(
    EmptyState.copy(amountParts = AmountPartsUi(prefix = "($", suffix = ")")),
)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-7314 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenTypingNamePreview() =
    ScreenPreview(EmptyState.copy(name = "Netflix"))

/**
 * The list opens in a popup, which a static preview may not draw; `SpendLessSelect` previews the
 * list itself.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-9770
 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenCategoryOpenPreview() = ScreenPreview(
    EmptyState.copy(
        name = "Netflix",
        category = ExpenseCategory.ENTERTAINMENT,
        openSelect = OpenSelect.CATEGORY,
    ),
)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2443-9532 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenFilledPreview() = ScreenPreview(FilledState)

/**
 * Filled, with the repeat list open under the Weekly field and "Weekly on Monday" ticked. A static
 * render does not draw the list's popup (checked 2026-11-17); the DS
 * `SpendLessSelectListTextIndentedPreview` shows the list, and the open state is verified on a device.
 *
 * Figma (screen): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-7873
 * Figma (sheet): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-7969
 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenRepeatOpenPreview() =
    ScreenPreview(FilledState.copy(openSelect = OpenSelect.REPEAT))

/**
 * Income with nothing typed: "Sender" placeholder, `$` in `success`, the category hidden while the
 * repeat slot and Create keep their place. Figma has no empty-income frame; the filled-income sheet
 * below fixes the layout.
 *
 * Figma (filled, income): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8638
 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenEmptyIncomePreview() = ScreenPreview(
    EmptyState.copy(
        type = TransactionType.INCOME,
        amountParts = AmountPartsUi(prefix = "$", signTone = TransactionType.INCOME),
    ),
)

/**
 * Rick, $100.00, Weekly; the category is hidden.
 *
 * Figma (sheet): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8638
 * Figma (screen): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8542
 */
@Preview(widthDp = 412, heightDp = 832)
@Composable
private fun CreateTransactionScreenFilledIncomePreview() = ScreenPreview(
    EmptyState.copy(
        type = TransactionType.INCOME,
        name = "Rick",
        amountInput = "100.00",
        amountParts = AmountPartsUi(
            prefix = "$",
            number = "100.00",
            isPlaceholder = false,
            signTone = TransactionType.INCOME,
        ),
        isCreateEnabled = true,
    ).withRepeat(RepeatFrequency.WEEKLY),
)

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

@Composable
private fun FiltersBlockPreview(
    category: ExpenseCategory = ExpenseCategory.OTHER,
    isCategoryVisible: Boolean = true,
    repeat: RepeatFrequency = RepeatFrequency.DOES_NOT_REPEAT,
    isRepeatOpen: Boolean = false,
) = SheetPreview {
    FiltersBlock(
        category = category,
        isCategoryVisible = isCategoryVisible,
        isCategoryOpen = false,
        onCategoryExpandedChange = {},
        onCategoryPick = {},
        repeat = repeat,
        repeatFieldLabel = PreviewFormatter.shortLabel(repeat).asString(),
        repeatOptions = PreviewRepeatOptions,
        isRepeatOpen = isRepeatOpen,
        onRepeatExpandedChange = {},
        onRepeatPick = {},
        modifier = Modifier.padding(16.dp),
    )
}

/** Defaults: Other and Does not repeat. Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10578 */
@Preview(widthDp = 412)
@Composable
private fun FiltersBlockClosedPreview() = FiltersBlockPreview()

/** Figma (income sheet): https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8638 */
@Preview(widthDp = 412)
@Composable
private fun FiltersBlockCategoryHiddenPreview() =
    FiltersBlockPreview(isCategoryVisible = false, repeat = RepeatFrequency.WEEKLY)

/** No Figma frame for this label; the field shows the short name (FR-020). */
@Preview(widthDp = 412)
@Composable
private fun FiltersBlockRepeatDailyPreview() = FiltersBlockPreview(repeat = RepeatFrequency.DAILY)

/** Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2443-9532 */
@Preview(widthDp = 412)
@Composable
private fun FiltersBlockRepeatWeeklyPreview() =
    FiltersBlockPreview(category = ExpenseCategory.ENTERTAINMENT, repeat = RepeatFrequency.WEEKLY)

/** No Figma frame for this label; the field shows the short name (FR-020). */
@Preview(widthDp = 412)
@Composable
private fun FiltersBlockRepeatMonthlyPreview() = FiltersBlockPreview(repeat = RepeatFrequency.MONTHLY)

/** No Figma frame for this label; the field shows the short name (FR-020). */
@Preview(widthDp = 412)
@Composable
private fun FiltersBlockRepeatYearlyPreview() = FiltersBlockPreview(repeat = RepeatFrequency.YEARLY)

/**
 * The repeat list open with Weekly ticked. A static render does not draw the popup (checked
 * 2026-11-17), so this shows the anchors only; the DS `SpendLessSelectListTextIndentedPreview`
 * shows the list itself, and the open state is verified on a device.
 *
 * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-11280
 */
@Preview(widthDp = 412)
@Composable
private fun FiltersBlockRepeatListPreview() = FiltersBlockPreview(
    category = ExpenseCategory.ENTERTAINMENT,
    repeat = RepeatFrequency.WEEKLY,
    isRepeatOpen = true,
)

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
