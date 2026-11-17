package com.plcoding.spendlesssample.transactions.domain.usecase

import com.plcoding.spendlesssample.transactions.domain.TransactionLocalDataSource
import kotlinx.coroutines.flow.Flow

/** Ids of the recurring series that are still running; updates when one is stopped (FR-037). */
class ObserveRunningSeriesIds(private val dataSource: TransactionLocalDataSource) {
    operator fun invoke(): Flow<Set<Long>> = dataSource.observeRunningSeriesIds()
}
