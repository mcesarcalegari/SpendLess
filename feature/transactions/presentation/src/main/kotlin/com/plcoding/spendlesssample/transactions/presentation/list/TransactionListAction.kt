package com.plcoding.spendlesssample.transactions.presentation.list

/** User actions on the All Transactions screen. Back is not an action; the Root handles it. */
sealed interface TransactionListAction {
    data object OnCreateClick : TransactionListAction
    data object OnCreateSheetDismiss : TransactionListAction

    /** Expands a collapsed row's note, or collapses an expanded one (FR-028, FR-030). */
    data class OnNoteBadgeClick(val id: Long) : TransactionListAction

    /** Opens the stop-repeating menu, only for a row of a running series (FR-037). */
    data class OnRowLongPress(val id: Long) : TransactionListAction
    data object OnContextMenuDismiss : TransactionListAction
    data object OnStopRepeatingClick : TransactionListAction
    data object OnStopConfirm : TransactionListAction
    data object OnStopCancel : TransactionListAction
}
