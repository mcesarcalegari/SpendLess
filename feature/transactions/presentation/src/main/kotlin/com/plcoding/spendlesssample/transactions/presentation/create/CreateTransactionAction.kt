package com.plcoding.spendlesssample.transactions.presentation.create

import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType

/** User actions on the Create Transaction sheet (data-model §4). */
sealed interface CreateTransactionAction {
    data class OnTypeSelect(val type: TransactionType) : CreateTransactionAction
    data class OnNameChange(val text: String) : CreateTransactionAction
    data class OnAmountChange(val text: String) : CreateTransactionAction
    data object OnAddNoteClick : CreateTransactionAction
    data class OnNoteChange(val text: String) : CreateTransactionAction
    data object OnNoteFocusLost : CreateTransactionAction
    data object OnCategorySelectClick : CreateTransactionAction
    data class OnCategoryPick(val category: ExpenseCategory) : CreateTransactionAction
    data object OnRepeatSelectClick : CreateTransactionAction
    data class OnRepeatPick(val frequency: RepeatFrequency) : CreateTransactionAction
    data object OnSelectDismiss : CreateTransactionAction
    data object OnCreateClick : CreateTransactionAction
    data object OnCloseClick : CreateTransactionAction
}
