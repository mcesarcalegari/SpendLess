package com.plcoding.spendlesssample.transactions.presentation.create

import androidx.compose.runtime.Immutable
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
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
    /** Long labels from today. Empty until User Story 5. */
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
 */
@Immutable
data class AmountPartsUi(
    val prefix: String = "-$",
    val number: String = "00.00",
    val suffix: String = "",
    val isPlaceholder: Boolean = true,
    val signTone: TransactionType = TransactionType.EXPENSE,
)

/** One repeat choice: the long list label ("Weekly on Monday"). Filled in User Story 5. */
@Immutable
data class RepeatOptionUi(
    val frequency: RepeatFrequency,
    val label: UiText,
)

/** Which Select list is open. */
enum class OpenSelect { CATEGORY, REPEAT }
