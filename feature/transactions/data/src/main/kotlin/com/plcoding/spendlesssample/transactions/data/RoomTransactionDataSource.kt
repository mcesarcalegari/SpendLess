package com.plcoding.spendlesssample.transactions.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import com.plcoding.spendlesssample.core.database.dao.TransactionDao
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * [TransactionLocalDataSource] over the encrypted Room database. Storage exceptions become typed
 * errors here and nothing leaks upward: a full disk is [DataError.Local.DISK_FULL], any other SQLite
 * failure is [DataError.Local.UNKNOWN]. Cancellation is never caught. Nothing is logged.
 */
class RoomTransactionDataSource(private val dao: TransactionDao) : TransactionLocalDataSource {

    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeAll()
            .map { entities -> entities.map { it.toTransaction() } }
            .endOnStorageFailure()

    override suspend fun insert(transaction: Transaction): Result<Long, DataError.Local> =
        safeDbCall { dao.insert(transaction.toEntity()) }

    override suspend fun insertRecurring(
        original: Transaction,
        frequency: RepeatFrequency,
    ): Result<Long, DataError.Local> = safeDbCall {
        val local = ZonedDateTime.ofInstant(original.recordedAt, original.zoneId)
        dao.insertWithSeries(
            transaction = original.toEntity(),
            frequency = frequency.name,
            anchorEpochDay = local.toLocalDate().toEpochDay(),
            anchorSecondsOfDay = local.toLocalTime().toSecondOfDay(),
            zoneId = original.zoneId.id,
        )
    }

    override suspend fun runningSeries(): Result<List<RecurringSeries>, DataError.Local> =
        safeDbCall { dao.runningSeries().map { it.toRecurringSeries() } }

    override suspend fun materialize(
        seriesId: Long,
        occurrences: List<Transaction>,
        lastMaterialized: LocalDate,
    ): EmptyResult<DataError.Local> = safeDbCall {
        dao.materialize(
            seriesId = seriesId,
            occurrences = occurrences.map { it.toEntity() },
            newLastEpochDay = lastMaterialized.toEpochDay(),
        )
    }

    override suspend fun stopSeries(seriesId: Long, stoppedOn: LocalDate): EmptyResult<DataError.Local> =
        safeDbCall { dao.markStopped(seriesId = seriesId, epochDay = stoppedOn.toEpochDay()) }

    override fun observeRunningSeriesIds(): Flow<Set<Long>> =
        dao.observeRunningSeriesIds()
            .map { it.toSet() }
            .endOnStorageFailure()

    /**
     * A read flow has no error channel, so a storage failure ends the flow instead of crashing the
     * collector; nothing leaks upward. Cancellation is not a [SQLiteException] and passes through.
     */
    private fun <T> Flow<T>.endOnStorageFailure(): Flow<T> = catch { e ->
        if (e !is SQLiteException) throw e
    }

    private inline fun <T> safeDbCall(block: () -> T): Result<T, DataError.Local> = try {
        Result.Success(block())
    } catch (e: SQLiteFullException) {
        Result.Error(DataError.Local.DISK_FULL)
    } catch (e: SQLiteException) {
        Result.Error(DataError.Local.UNKNOWN)
    }
}
