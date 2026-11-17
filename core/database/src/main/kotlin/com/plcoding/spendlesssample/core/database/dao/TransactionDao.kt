package com.plcoding.spendlesssample.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.plcoding.spendlesssample.core.database.entity.RecurringSeriesEntity
import com.plcoding.spendlesssample.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TransactionDao {

    /** Every transaction, newest first (FR-001, FR-002). */
    @Query("SELECT * FROM transactions ORDER BY recorded_at_epoch_ms DESC")
    abstract fun observeAll(): Flow<List<TransactionEntity>>

    @Insert
    abstract suspend fun insert(entity: TransactionEntity): Long

    /** Occurrences that already exist for the same `(series_id, date)` are skipped (FR-025a). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertOccurrences(occurrences: List<TransactionEntity>)

    @Insert
    abstract suspend fun insertSeries(entity: RecurringSeriesEntity)

    @Query("SELECT * FROM recurring_series WHERE stopped_on_epoch_day IS NULL")
    abstract suspend fun runningSeries(): List<RecurringSeriesEntity>

    @Query("SELECT series_id FROM recurring_series WHERE stopped_on_epoch_day IS NULL")
    abstract fun observeRunningSeriesIds(): Flow<List<Long>>

    /** Stops a running series. A series that is already stopped keeps its original stop date. */
    @Query(
        "UPDATE recurring_series SET stopped_on_epoch_day = :epochDay " +
            "WHERE series_id = :seriesId AND stopped_on_epoch_day IS NULL",
    )
    abstract suspend fun markStopped(seriesId: Long, epochDay: Long)

    @Query(
        "UPDATE recurring_series SET last_materialized_epoch_day = :epochDay " +
            "WHERE series_id = :seriesId",
    )
    abstract suspend fun updateLastMaterialized(seriesId: Long, epochDay: Long)

    @Query("UPDATE transactions SET series_id = :seriesId WHERE id = :id")
    protected abstract suspend fun setSeriesId(id: Long, seriesId: Long)

    /**
     * Inserts a recurring original and its series row atomically. The original's `series_id`
     * becomes its own id, and the series starts materialised up to its anchor date.
     */
    @Transaction
    open suspend fun insertWithSeries(
        transaction: TransactionEntity,
        frequency: String,
        anchorEpochDay: Long,
        anchorSecondsOfDay: Int,
        zoneId: String,
    ): Long {
        val id = insert(transaction.copy(seriesId = null))
        setSeriesId(id = id, seriesId = id)
        insertSeries(
            RecurringSeriesEntity(
                seriesId = id,
                frequency = frequency,
                anchorEpochDay = anchorEpochDay,
                anchorSecondsOfDay = anchorSecondsOfDay,
                zoneId = zoneId,
                stoppedOnEpochDay = null,
                lastMaterializedEpochDay = anchorEpochDay,
            ),
        )
        return id
    }

    /** Inserts due occurrences and advances the series' high-water mark in one transaction. */
    @Transaction
    open suspend fun materialize(
        seriesId: Long,
        occurrences: List<TransactionEntity>,
        newLastEpochDay: Long,
    ) {
        insertOccurrences(occurrences)
        updateLastMaterialized(seriesId = seriesId, epochDay = newLastEpochDay)
    }
}
