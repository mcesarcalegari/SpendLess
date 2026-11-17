package com.plcoding.spendlesssample.transactions.data.di

import com.plcoding.spendlesssample.transactions.data.RoomTransactionDataSource
import com.plcoding.spendlesssample.transactions.domain.TransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.TransactionValidator
import com.plcoding.spendlesssample.transactions.domain.usecase.CreateTransaction
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveTransactions
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val transactionsDataModule = module {
    singleOf(::RoomTransactionDataSource) { bind<TransactionLocalDataSource>() }
}

/** Use cases of the transactions domain. The domain module itself stays Koin-free. */
val transactionsDomainModule = module {
    factoryOf(::ObserveTransactions)
    factoryOf(::TransactionValidator)
    factoryOf(::CreateTransaction)
}
