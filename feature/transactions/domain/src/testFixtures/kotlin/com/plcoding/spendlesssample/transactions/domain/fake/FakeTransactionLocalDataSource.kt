package com.plcoding.spendlesssample.transactions.domain.fake

import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.EmptyResult
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.TransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.RecurringSeries
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [TransactionLocalDataSource] that follows the same rules as the Room DAO: newest-first
 * ordering, `(seriesId, occurrenceDate)` de-duplication, and stopping only a running series.
 * Set [error] to make every write fail with that error.
 */
class FakeTransactionLocalDataSource : TransactionLocalDataSource {

    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val series = MutableStateFlow<Map<Long, RecurringSeries>>(emptyMap())
    private var nextId = 1L

    /** When non-null, every write returns this error and changes nothing. */
    var error: DataError.Local? = null

    val storedTransactions: List<Transaction> get() = transactions.value
    val storedSeries: List<RecurringSeries> get() = series.value.values.toList()

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactions.map { list -> list.sortedByDescending { it.recordedAt } }

    override suspend fun insert(transaction: Transaction): Result<Long, DataError.Local> {
        error?.let { return Result.Error(it) }
        val id = nextId++
        transactions.update { it + transaction.copy(id = id) }
        return Result.Success(id)
    }

    override suspend fun insertRecurring(
        original: Transaction,
        frequency: RepeatFrequency,
    ): Result<Long, DataError.Local> {
        error?.let { return Result.Error(it) }
        val id = nextId++
        val local = ZonedDateTime.ofInstant(original.recordedAt, original.zoneId)
        transactions.update { it + original.copy(id = id, seriesId = id) }
        series.update {
            it + (id to RecurringSeries(
                seriesId = id,
                frequency = frequency,
                anchorDate = local.toLocalDate(),
                anchorTime = local.toLocalTime(),
                zoneId = original.zoneId,
                stoppedOn = null,
                lastMaterializedDate = local.toLocalDate(),
            ))
        }
        return Result.Success(id)
    }

    override suspend fun runningSeries(): Result<List<RecurringSeries>, DataError.Local> {
        error?.let { return Result.Error(it) }
        return Result.Success(series.value.values.filter { it.stoppedOn == null })
    }

    override suspend fun materialize(
        seriesId: Long,
        occurrences: List<Transaction>,
        lastMaterialized: LocalDate,
    ): EmptyResult<DataError.Local> {
        error?.let { return Result.Error(it) }
        transactions.update { current ->
            val existing = current.mapNotNullTo(mutableSetOf()) { tx ->
                tx.occurrenceDate?.let { tx.seriesId to it }
            }
            val fresh = occurrences
                .filter { occurrence ->
                    val date = occurrence.occurrenceDate ?: return@filter true
                    existing.add(occurrence.seriesId to date)
                }
                .map { it.copy(id = nextId++) }
            current + fresh
        }
        series.update { all ->
            all[seriesId]?.let { all + (seriesId to it.copy(lastMaterializedDate = lastMaterialized)) } ?: all
        }
        return Result.Success(Unit)
    }

    override suspend fun stopSeries(seriesId: Long, stoppedOn: LocalDate): EmptyResult<DataError.Local> {
        error?.let { return Result.Error(it) }
        series.update { all ->
            val current = all[seriesId]
            if (current == null || current.stoppedOn != null) all else all + (seriesId to current.copy(stoppedOn = stoppedOn))
        }
        return Result.Success(Unit)
    }

    override fun observeRunningSeriesIds(): Flow<Set<Long>> =
        series.map { all -> all.values.filter { it.stoppedOn == null }.mapTo(mutableSetOf()) { it.seriesId } }
}
