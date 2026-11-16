package com.plcoding.spendlesssample.transactions.domain.usecase

import com.plcoding.spendlesssample.transactions.domain.TransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/** Every transaction, newest first (FR-001, FR-002). */
class ObserveTransactions(private val dataSource: TransactionLocalDataSource) {
    operator fun invoke(): Flow<List<Transaction>> = dataSource.observeTransactions()
}
