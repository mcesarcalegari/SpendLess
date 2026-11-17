package com.plcoding.spendlesssample.transactions.presentation.di

import com.plcoding.spendlesssample.core.presentation.format.AmountFormatter
import com.plcoding.spendlesssample.transactions.presentation.format.DayHeaderFormatter
import com.plcoding.spendlesssample.transactions.presentation.list.TransactionListViewModel
import com.plcoding.spendlesssample.transactions.presentation.mapper.TransactionUiMapper
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** ViewModels of the transactions feature, plus the stateless presentation helpers they share (singletons). */
val transactionsPresentationModule = module {
    singleOf(::AmountFormatter)
    singleOf(::DayHeaderFormatter)
    singleOf(::TransactionUiMapper)

    viewModelOf(::TransactionListViewModel)
}
