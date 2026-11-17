package com.plcoding.spendlesssample.transactions.presentation.list.model

import androidx.compose.runtime.Immutable
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType

/** One list row, ready to display (data-model §4). */
@Immutable
data class TransactionUi(
    val id: Long,
    val type: TransactionType,
    /** Category emoji, or 💰 for income. */
    val iconEmoji: String,
    /** The counterparty. */
    val title: String,
    /** Category name, or "Income". */
    val label: UiText,
    /** For example `-$45.99` or `($45.99)`. */
    val formattedAmount: String,
    val note: String?,
    val seriesId: Long?,
    val isInRunningSeries: Boolean,
)
