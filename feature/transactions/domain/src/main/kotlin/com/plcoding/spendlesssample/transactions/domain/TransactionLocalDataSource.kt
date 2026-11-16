package com.plcoding.spendlesssample.transactions.domain

import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.EmptyResult
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.model.RecurringSeries
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Local storage of transactions and recurring series (contracts/domain-interfaces.md). */
interface TransactionLocalDataSource {

    /** Every transaction, newest first (FR-001, FR-002). */
    fun observeTransactions(): Flow<List<Transaction>>

    suspend fun insert(transaction: Transaction): Result<Long, DataError.Local>

    /** Inserts the original and its series row atomically; the original's series id is its own id. */
    suspend fun insertRecurring(original: Transaction, frequency: RepeatFrequency): Result<Long, DataError.Local>

    suspend fun runningSeries(): Result<List<RecurringSeries>, DataError.Local>

    /** Inserts due occurrences (duplicates ignored) and advances the series atomically. */
    suspend fun materialize(
        seriesId: Long,
        occurrences: List<Transaction>,
        lastMaterialized: LocalDate,
    ): EmptyResult<DataError.Local>

    suspend fun stopSeries(seriesId: Long, stoppedOn: LocalDate): EmptyResult<DataError.Local>

    /** Ids of series that are still running (FR-037). */
    fun observeRunningSeriesIds(): Flow<Set<Long>>
}
