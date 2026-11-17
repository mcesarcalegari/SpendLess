package com.plcoding.spendlesssample.transactions.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.fake.FakeTransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObserveRunningSeriesIdsTest {

    private val dataSource = FakeTransactionLocalDataSource()
    private val observeRunningSeriesIds = ObserveRunningSeriesIds(dataSource)

    private val gym = Transaction(
        id = 0,
        type = TransactionType.EXPENSE,
        counterparty = "Gym",
        amountMinor = 4_599,
        note = null,
        category = ExpenseCategory.HEALTH_AND_WELLNESS,
        repeat = RepeatFrequency.DAILY,
        recordedAt = Instant.parse("2025-01-13T10:30:00Z"),
        zoneId = ZoneOffset.UTC,
        seriesId = null,
        occurrenceDate = null,
    )

    @Test
    fun `emits running series and drops one when it is stopped`() = runTest {
        val id = (dataSource.insertRecurring(gym, RepeatFrequency.DAILY) as Result.Success).data

        observeRunningSeriesIds().test {
            assertThat(awaitItem()).containsOnly(id)

            dataSource.stopSeries(id, LocalDate.parse("2025-01-14"))

            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `one-time transactions are not series`() = runTest {
        dataSource.insert(gym.copy(repeat = RepeatFrequency.DOES_NOT_REPEAT))

        observeRunningSeriesIds().test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
