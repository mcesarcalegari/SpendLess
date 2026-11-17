package com.plcoding.spendlesssample.transactions.domain.usecase

import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.EmptyResult
import com.plcoding.spendlesssample.transactions.domain.TransactionLocalDataSource
import java.time.LocalDate

/**
 * Stops a recurring series as of [today] (FR-038). No transaction is changed or removed, and a
 * series that is already stopped keeps its first stop date; it cannot be restarted (FR-039).
 */
class StopRecurringSeries(private val dataSource: TransactionLocalDataSource) {

    suspend operator fun invoke(seriesId: Long, today: LocalDate): EmptyResult<DataError.Local> =
        dataSource.stopSeries(seriesId = seriesId, stoppedOn = today)
}
