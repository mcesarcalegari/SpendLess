package com.plcoding.spendlesssample.transactions.presentation.list

/** User actions on the All Transactions screen. Back is not an action; the Root handles it. */
sealed interface TransactionListAction {
    data object OnCreateClick : TransactionListAction
    data object OnCreateSheetDismiss : TransactionListAction
}
