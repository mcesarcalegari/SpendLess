package com.plcoding.spendlesssample.transactions.domain.usecase

import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.TransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction

/**
 * Persists an already-validated transaction (FR-022). Returns its id, or a storage error. A
 * repeating transaction becomes the original of a new series, whose id is its own (FR-025b).
 */
class CreateTransaction(private val dataSource: TransactionLocalDataSource) {

    suspend operator fun invoke(transaction: Transaction): Result<Long, DataError.Local> =
        if (transaction.repeat == RepeatFrequency.DOES_NOT_REPEAT) {
            dataSource.insert(transaction)
        } else {
            dataSource.insertRecurring(original = transaction, frequency = transaction.repeat)
        }
}
