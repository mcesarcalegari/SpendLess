package com.plcoding.spendlesssample.transactions.presentation.list

import androidx.compose.runtime.Immutable
import com.plcoding.spendlesssample.transactions.presentation.list.model.DayGroupUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/** State of the All Transactions screen (data-model §4). */
@Immutable
data class TransactionListState(
    /** Newest day first. */
    val groups: ImmutableList<DayGroupUi> = persistentListOf(),
    /** Rows whose note is expanded. Not saved across visits (FR-031). */
    val expandedIds: ImmutableSet<Long> = persistentSetOf(),
    /** Persisted in `SavedStateHandle`, so the sheet survives process death. */
    val isCreateSheetOpen: Boolean = false,
    /** Row id whose long-press menu is open (FR-037). */
    val contextMenuFor: Long? = null,
    /** Non-null shows the stop-repeating dialog (FR-038). */
    val stopConfirmationFor: StopConfirmationUi? = null,
)

@Immutable
data class StopConfirmationUi(
    val seriesId: Long,
    val name: String,
)
