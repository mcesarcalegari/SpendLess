package com.plcoding.spendlesssample.transactions.presentation.create

/**
 * One-time results only the ViewModel knows: the async insert succeeded and the sheet may close
 * (FR-022). Close (✕) needs no event; the parent handles it directly.
 */
sealed interface CreateTransactionEvent {
    data object Created : CreateTransactionEvent
}
