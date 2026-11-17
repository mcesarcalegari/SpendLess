package com.plcoding.spendlesssample.transactions.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.time.CurrentDateProvider
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveRunningSeriesIds
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveTransactions
import com.plcoding.spendlesssample.transactions.domain.usecase.StopRecurringSeries
import com.plcoding.spendlesssample.transactions.presentation.mapper.TransactionUiMapper
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the All Transactions screen. Groups update live on every insert (FR-010), relabel at
 * midnight, and follow the amount format preferences. Rows of a running recurring series can be
 * long-pressed to stop the series after a confirmation (FR-037 – FR-040). No events: everything
 * the UI needs is state.
 */
class TransactionListViewModel(
    observeTransactions: ObserveTransactions,
    observeRunningSeriesIds: ObserveRunningSeriesIds,
    private val stopRecurringSeries: StopRecurringSeries,
    private val currentDateProvider: CurrentDateProvider,
    preferencesSource: AmountFormatPreferencesSource,
    mapper: TransactionUiMapper,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groups = combine(
        observeTransactions(),
        observeRunningSeriesIds(),
        currentDateProvider.today,
        preferencesSource.preferences,
    ) { transactions, runningSeriesIds, today, prefs ->
        mapper.toGroups(transactions, today, prefs, runningSeriesIds)
    }

    /**
     * Rows whose note is expanded. Kept in memory only, never in [savedStateHandle], so every row
     * starts collapsed when the screen is opened again (FR-031).
     */
    private val expandedIds = MutableStateFlow<PersistentSet<Long>>(persistentSetOf())

    /** The long-press menu and the confirmation dialog; at most one is open. Not saved. */
    private val stopRepeating = MutableStateFlow(StopRepeatingOverlay())

    val state: StateFlow<TransactionListState> = combine(
        groups,
        expandedIds,
        savedStateHandle.getStateFlow(KEY_CREATE_SHEET_OPEN, false),
        stopRepeating,
    ) { groups, expandedIds, isCreateSheetOpen, overlay ->
        TransactionListState(
            groups = groups,
            expandedIds = expandedIds,
            isCreateSheetOpen = isCreateSheetOpen,
            contextMenuFor = overlay.contextMenuFor,
            stopConfirmationFor = overlay.stopConfirmationFor,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TransactionListState(
            isCreateSheetOpen = savedStateHandle[KEY_CREATE_SHEET_OPEN] ?: false,
        ),
    )

    fun onAction(action: TransactionListAction) {
        when (action) {
            TransactionListAction.OnCreateClick -> savedStateHandle[KEY_CREATE_SHEET_OPEN] = true
            TransactionListAction.OnCreateSheetDismiss -> savedStateHandle[KEY_CREATE_SHEET_OPEN] = false
            is TransactionListAction.OnNoteBadgeClick -> expandedIds.update { ids ->
                if (action.id in ids) ids.remove(action.id) else ids.add(action.id)
            }
            is TransactionListAction.OnRowLongPress -> {
                // A one-time row or a stopped series does nothing (FR-037).
                if (rowOrNull(action.id)?.isInRunningSeries != true) return
                stopRepeating.value = StopRepeatingOverlay(contextMenuFor = action.id)
            }
            TransactionListAction.OnContextMenuDismiss -> stopRepeating.value = StopRepeatingOverlay()
            TransactionListAction.OnStopRepeatingClick -> {
                val row = stopRepeating.value.contextMenuFor?.let(::rowOrNull) ?: return
                val seriesId = row.seriesId ?: return
                stopRepeating.value = StopRepeatingOverlay(
                    stopConfirmationFor = StopConfirmationUi(seriesId = seriesId, name = row.title),
                )
            }
            TransactionListAction.OnStopConfirm -> {
                val target = stopRepeating.value.stopConfirmationFor ?: return
                stopRepeating.value = StopRepeatingOverlay()
                // Stops the whole series, whichever row was pressed (FR-038). There is no error
                // design; on a storage failure the rows stay marked and can be stopped again.
                viewModelScope.launch {
                    stopRecurringSeries(seriesId = target.seriesId, today = currentDateProvider.today.first())
                }
            }
            TransactionListAction.OnStopCancel -> stopRepeating.value = StopRepeatingOverlay()
        }
    }

    private fun rowOrNull(id: Long) = state.value.groups.firstNotNullOfOrNull { group ->
        group.items.firstOrNull { it.id == id }
    }

    private data class StopRepeatingOverlay(
        val contextMenuFor: Long? = null,
        val stopConfirmationFor: StopConfirmationUi? = null,
    )

    private companion object {
        const val KEY_CREATE_SHEET_OPEN = "isCreateSheetOpen"
    }
}
