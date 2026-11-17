package com.plcoding.spendlesssample.transactions.domain.usecase

import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.TransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.Transaction

/** Persists an already-validated transaction (FR-022). Returns its id, or a storage error. */
class CreateTransaction(private val dataSource: TransactionLocalDataSource) {

    // The recurring path (repeat != DOES_NOT_REPEAT → insertRecurring) is added in User Story 5.
    suspend operator fun invoke(transaction: Transaction): Result<Long, DataError.Local> =
        dataSource.insert(transaction)
}
