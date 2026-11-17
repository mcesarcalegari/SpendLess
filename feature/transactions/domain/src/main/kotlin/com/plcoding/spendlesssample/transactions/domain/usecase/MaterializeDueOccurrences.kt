package com.plcoding.spendlesssample.transactions.domain.usecase

import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.EmptyResult
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.RecurrenceSchedule
import com.plcoding.spendlesssample.transactions.domain.TransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.RecurringSeries
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.firstOrNull

/**
 * Adds the due occurrences of every running series up to [invoke]'s `today` (FR-025, FR-025a,
 * FR-025c). Each occurrence is a one-time copy of the original, linked to it by `seriesId`
 * (FR-025b). Occurrences are never created ahead of their due date, and the data source ignores a
 * second occurrence for the same `(series, date)`, so a run can be repeated safely (exactly once).
 *
 * An occurrence is stamped at the original's local time of day on its due date, or at the moment
 * it is added when that is earlier (FR-002). A failing series does not stop the others; the first
 * storage error is returned.
 */
class MaterializeDueOccurrences(
    private val dataSource: TransactionLocalDataSource,
    private val schedule: RecurrenceSchedule,
    private val clock: Clock,
) {

    suspend operator fun invoke(today: LocalDate): EmptyResult<DataError.Local> {
        val running = when (val result = dataSource.runningSeries()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        if (running.isEmpty()) return Result.Success(Unit)

        // The read flow completes without emitting on a storage failure.
        val originals = dataSource.observeTransactions().firstOrNull()
            ?.associateBy { it.id }
            ?: return Result.Error(DataError.Local.UNKNOWN)

        var firstError: DataError.Local? = null
        running.forEach { series ->
            val original = originals[series.seriesId] ?: return@forEach
            val result = materializeSeries(series, original, today)
            if (result is Result.Error && firstError == null) firstError = result.error
        }
        return firstError?.let { Result.Error(it) } ?: Result.Success(Unit)
    }

    private suspend fun materializeSeries(
        series: RecurringSeries,
        original: Transaction,
        today: LocalDate,
    ): EmptyResult<DataError.Local> {
        // runningSeries() already excludes stopped series; the bound is kept as a guard (FR-038).
        val upTo = series.stoppedOn?.let { minOf(it, today) } ?: today
        // Nothing new, or the date moved back: never lower the high-water mark.
        if (upTo <= series.lastMaterializedDate) return Result.Success(Unit)

        val now = clock.instant()
        val occurrences = schedule
            .dueDatesBetween(
                anchor = series.anchorDate,
                frequency = series.frequency,
                fromExclusive = series.lastMaterializedDate,
                toInclusive = upTo,
            )
            .map { due ->
                val dueAt = due.atTime(series.anchorTime).atZone(series.zoneId).toInstant()
                original.copy(
                    id = 0,
                    repeat = RepeatFrequency.DOES_NOT_REPEAT,
                    recordedAt = minOf(dueAt, now),
                    zoneId = series.zoneId,
                    seriesId = series.seriesId,
                    occurrenceDate = due,
                )
            }
        return dataSource.materialize(
            seriesId = series.seriesId,
            occurrences = occurrences,
            lastMaterialized = upTo,
        )
    }
}
