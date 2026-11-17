package com.plcoding.spendlesssample.transactions.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.time.CurrentDateProvider
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveTransactions
import com.plcoding.spendlesssample.transactions.presentation.mapper.TransactionUiMapper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Drives the All Transactions screen. Groups update live on every insert (FR-010), relabel at
 * midnight, and follow the amount format preferences. No events: everything the UI needs is state.
 */
class TransactionListViewModel(
    observeTransactions: ObserveTransactions,
    currentDateProvider: CurrentDateProvider,
    preferencesSource: AmountFormatPreferencesSource,
    mapper: TransactionUiMapper,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groups = combine(
        observeTransactions(),
        currentDateProvider.today,
        preferencesSource.preferences,
    ) { transactions, today, prefs ->
        mapper.toGroups(transactions, today, prefs)
    }

    val state: StateFlow<TransactionListState> = combine(
        groups,
        savedStateHandle.getStateFlow(KEY_CREATE_SHEET_OPEN, false),
    ) { groups, isCreateSheetOpen ->
        TransactionListState(groups = groups, isCreateSheetOpen = isCreateSheetOpen)
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
        }
    }

    private companion object {
        const val KEY_CREATE_SHEET_OPEN = "isCreateSheetOpen"
    }
}
