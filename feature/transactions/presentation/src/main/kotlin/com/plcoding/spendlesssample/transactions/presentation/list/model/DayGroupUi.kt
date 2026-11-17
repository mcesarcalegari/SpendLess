package com.plcoding.spendlesssample.transactions.presentation.list.model

import androidx.compose.runtime.Immutable
import com.plcoding.spendlesssample.core.presentation.UiText
import kotlinx.collections.immutable.ImmutableList

/**
 * One day of transactions (FR-001).
 *
 * @property dateKey The local date as an epoch day; stable across relabelling at midnight.
 * @property header "TODAY", "YESTERDAY", or for example "JANUARY 9".
 * @property items Newest first (FR-002).
 */
@Immutable
data class DayGroupUi(
    val dateKey: Long,
    val header: UiText,
    val items: ImmutableList<TransactionUi>,
)
