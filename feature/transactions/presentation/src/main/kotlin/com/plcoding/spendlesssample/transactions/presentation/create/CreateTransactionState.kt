package com.plcoding.spendlesssample.transactions.presentation.create

import androidx.compose.runtime.Immutable
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.core.presentation.format.AmountFormatter
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** State of the Create Transaction sheet (data-model §4). */
@Immutable
data class CreateTransactionState(
    /** FR-011. */
    val type: TransactionType = TransactionType.EXPENSE,
    /** Raw; trimmed on save. Input capped at 50. */
    val name: String = "",
    /** Raw digits plus at most one preferred separator, at most 2 decimals and 9 whole digits (FR-013). */
    val amountInput: String = "",
    val amountParts: AmountPartsUi = AmountPartsUi(),
    /** Capped at 500. */
    val note: String = "",
    /** Shows the note input (approved deviation, research R8). */
    val isNoteEditing: Boolean = false,
    /** Kept while the type is `INCOME` (edge case). */
    val category: ExpenseCategory = ExpenseCategory.Default,
    val repeat: RepeatFrequency = RepeatFrequency.Default,
    /** Derived: the short name of [repeat] shown in the closed field ("Weekly", FR-020). */
    val repeatFieldLabel: UiText = UiText.StringResource(R.string.repeat_does_not_repeat),
    /** Long labels from today ("Weekly on Monday"), always English (FR-019). */
    val repeatOptions: ImmutableList<RepeatOptionUi> = persistentListOf(),
    val openSelect: OpenSelect? = null,
    /** Derived: name not blank **and** amount valid (FR-015). */
    val isCreateEnabled: Boolean = false,
    /** Storage failure on create. The sheet stays open (FR-022a). */
    val error: UiText? = null,
)

/**
 * The amount as the sheet draws it: [prefix] (`-$`, `($` or `$`) and [suffix] (`)` or `""`) in the
 * [signTone] colour around [number], which is the grouped input or the `00.00` placeholder.
 *
 * The defaults are the empty expense amount under [AmountFormatPreferences.Default], built by
 * [AmountFormatter], so no format literal lives here (FR-033). The ViewModel always replaces them
 * with the parts for the current preferences.
 */
@Immutable
data class AmountPartsUi(
    val prefix: String = DefaultEmptyExpense.prefix,
    val number: String = DefaultEmptyExpense.number,
    val suffix: String = DefaultEmptyExpense.suffix,
    val isPlaceholder: Boolean = true,
    val signTone: TransactionType = TransactionType.EXPENSE,
)

private val DefaultEmptyExpense = AmountFormatter().parts(
    amountMinor = null,
    isNegative = true,
    prefs = AmountFormatPreferences.Default,
)

/** One repeat choice and its long list label ("Weekly on Monday", FR-019). */
@Immutable
data class RepeatOptionUi(
    val frequency: RepeatFrequency,
    val label: UiText,
)

/** Which Select list is open. */
enum class OpenSelect { CATEGORY, REPEAT }
