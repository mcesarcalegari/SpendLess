package com.plcoding.spendlesssample.transactions.domain.usecase

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.single
import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.RecurrenceSchedule
import com.plcoding.spendlesssample.transactions.domain.fake.FakeTransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StopRecurringSeriesTest {

    private val dataSource = FakeTransactionLocalDataSource()
    private val stopRecurringSeries = StopRecurringSeries(dataSource)

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

    private fun date(text: String) = LocalDate.parse(text)

    private fun materializeAt(now: String) = MaterializeDueOccurrences(
        dataSource = dataSource,
        schedule = RecurrenceSchedule,
        clock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC),
    )

    private suspend fun insertGym(): Long =
        (dataSource.insertRecurring(gym, RepeatFrequency.DAILY) as Result.Success).data

    @Test
    fun `stopping sets stoppedOn to today`() = runTest {
        val id = insertGym()

        val result = stopRecurringSeries(seriesId = id, today = date("2025-01-15"))

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(dataSource.storedSeries.single().stoppedOn).isEqualTo(date("2025-01-15"))
        assertThat(dataSource.observeRunningSeriesIds().first()).isEmpty()
    }

    @Test
    fun `a second stop keeps the first stop date`() = runTest {
        val id = insertGym()

        stopRecurringSeries(seriesId = id, today = date("2025-01-15"))
        val second = stopRecurringSeries(seriesId = id, today = date("2025-01-20"))

        assertThat(second).isEqualTo(Result.Success(Unit))
        assertThat(dataSource.storedSeries.single().stoppedOn).isEqualTo(date("2025-01-15"))
    }

    @Test
    fun `no transaction is changed or removed`() = runTest {
        val id = insertGym()
        materializeAt("2025-01-15T12:00:00Z")(today = date("2025-01-15"))
        val before = dataSource.storedTransactions

        stopRecurringSeries(seriesId = id, today = date("2025-01-15"))

        assertThat(dataSource.storedTransactions).isEqualTo(before)
    }

    @Test
    fun `after stopping, a later day adds no occurrence`() = runTest {
        val id = insertGym()
        materializeAt("2025-01-15T12:00:00Z")(today = date("2025-01-15"))
        stopRecurringSeries(seriesId = id, today = date("2025-01-15"))
        val before = dataSource.storedTransactions

        materializeAt("2025-01-16T12:00:00Z")(today = date("2025-01-16"))

        assertThat(dataSource.storedTransactions).isEqualTo(before)
    }

    @Test
    fun `after stopping, a catch-up over many missed days adds nothing`() = runTest {
        val id = insertGym()
        stopRecurringSeries(seriesId = id, today = date("2025-01-13"))

        materializeAt("2025-02-13T12:00:00Z")(today = date("2025-02-13"))

        assertThat(dataSource.storedTransactions).single().transform { it.counterparty }.isEqualTo("Gym")
    }

    @Test
    fun `stopping one series leaves another running`() = runTest {
        val gymId = insertGym()
        val rentId = (dataSource.insertRecurring(gym.copy(counterparty = "Rent"), RepeatFrequency.MONTHLY)
            as Result.Success).data

        stopRecurringSeries(seriesId = gymId, today = date("2025-01-15"))

        assertThat(dataSource.observeRunningSeriesIds().first()).isEqualTo(setOf(rentId))
    }

    @Test
    fun `a storage error is returned`() = runTest {
        val id = insertGym()
        dataSource.error = DataError.Local.UNKNOWN

        val result = stopRecurringSeries(seriesId = id, today = date("2025-01-15"))

        assertThat(result).isEqualTo(Result.Error(DataError.Local.UNKNOWN))
        assertThat(dataSource.storedSeries.single().stoppedOn).isEqualTo(null)
    }
}
