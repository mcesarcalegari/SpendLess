package com.plcoding.spendlesssample.transactions.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.single
import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.EmptyResult
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.RecurrenceSchedule
import com.plcoding.spendlesssample.transactions.domain.TransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.fake.FakeTransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class MaterializeDueOccurrencesTest {

    private val dataSource = FakeTransactionLocalDataSource()

    private fun materializeAt(now: String) = MaterializeDueOccurrences(
        dataSource = dataSource,
        schedule = RecurrenceSchedule,
        clock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC),
    )

    private fun original(
        recordedAt: String,
        repeat: RepeatFrequency = RepeatFrequency.DAILY,
        zoneId: ZoneId = ZoneOffset.UTC,
        counterparty: String = "Gym",
        type: TransactionType = TransactionType.EXPENSE,
        category: ExpenseCategory? = ExpenseCategory.HEALTH_AND_WELLNESS,
        note: String? = "Monthly membership",
    ) = Transaction(
        id = 0,
        type = type,
        counterparty = counterparty,
        amountMinor = 4_599,
        note = note,
        category = category,
        repeat = repeat,
        recordedAt = Instant.parse(recordedAt),
        zoneId = zoneId,
        seriesId = null,
        occurrenceDate = null,
    )

    private suspend fun insertSeries(original: Transaction): Long {
        val result = dataSource.insertRecurring(original, original.repeat)
        return (result as Result.Success).data
    }

    private fun occurrences() = dataSource.storedTransactions.filter { it.occurrenceDate != null }

    private fun date(text: String) = LocalDate.parse(text)

    @Test
    fun `adds one occurrence per missed due date that copies the original`() = runTest {
        val id = insertSeries(original("2025-01-13T10:30:00Z"))

        val result = materializeAt("2025-01-16T12:00:00Z")(today = date("2025-01-16"))

        assertThat(result).isEqualTo(Result.Success(Unit))
        // Ids are assigned by storage; compared separately below.
        assertThat(occurrences().map { it.copy(id = 0) }).containsExactly(
            occurrence(seriesId = id, due = "2025-01-14", recordedAt = "2025-01-14T10:30:00Z"),
            occurrence(seriesId = id, due = "2025-01-15", recordedAt = "2025-01-15T10:30:00Z"),
            occurrence(seriesId = id, due = "2025-01-16", recordedAt = "2025-01-16T10:30:00Z"),
        )
        assertThat(occurrences().map { it.id }.toSet()).hasSize(3)
    }

    private fun occurrence(seriesId: Long, due: String, recordedAt: String) = Transaction(
        id = 0,
        type = TransactionType.EXPENSE,
        counterparty = "Gym",
        amountMinor = 4_599,
        note = "Monthly membership",
        category = ExpenseCategory.HEALTH_AND_WELLNESS,
        repeat = RepeatFrequency.DOES_NOT_REPEAT,
        recordedAt = Instant.parse(recordedAt),
        zoneId = ZoneOffset.UTC,
        seriesId = seriesId,
        occurrenceDate = date(due),
    )

    @Test
    fun `occurrences are one-time copies linked to the original`() = runTest {
        val id = insertSeries(original("2025-01-13T10:30:00Z"))

        materializeAt("2025-01-14T12:00:00Z")(today = date("2025-01-14"))

        assertThat(occurrences()).single().transform { Triple(it.repeat, it.seriesId, it.occurrenceDate) }
            .isEqualTo(Triple(RepeatFrequency.DOES_NOT_REPEAT, id, date("2025-01-14")))
    }

    @Test
    fun `an income occurrence copies the type and keeps no category`() = runTest {
        insertSeries(
            original(
                recordedAt = "2025-01-13T10:30:00Z",
                counterparty = "Rick",
                type = TransactionType.INCOME,
                category = null,
                note = null,
            ),
        )

        materializeAt("2025-01-14T12:00:00Z")(today = date("2025-01-14"))

        assertThat(occurrences()).single().transform { listOf(it.type, it.counterparty, it.category, it.note) }
            .isEqualTo(listOf(TransactionType.INCOME, "Rick", null, null))
    }

    @Test
    fun `an occurrence added at midnight for a 23 00 original is stamped with the materialisation instant`() =
        runTest {
            insertSeries(original("2025-01-13T23:00:00Z"))

            materializeAt("2025-01-14T00:00:05Z")(today = date("2025-01-14"))

            assertThat(occurrences()).single().transform { it.recordedAt }
                .isEqualTo(Instant.parse("2025-01-14T00:00:05Z"))
        }

    @Test
    fun `caught-up past occurrences keep the original's time of day`() = runTest {
        insertSeries(original("2025-01-13T23:00:00Z"))

        materializeAt("2025-01-16T00:00:05Z")(today = date("2025-01-16"))

        assertThat(occurrences().map { it.recordedAt }).containsExactly(
            Instant.parse("2025-01-14T23:00:00Z"),
            Instant.parse("2025-01-15T23:00:00Z"),
            Instant.parse("2025-01-16T00:00:05Z"),
        )
    }

    @Test
    fun `the time of day is taken in the series zone`() = runTest {
        val newYork = ZoneId.of("America/New_York")
        // 2025-01-13 22:00 in New York (UTC-5).
        insertSeries(original("2025-01-14T03:00:00Z", zoneId = newYork))

        materializeAt("2025-01-16T12:00:00Z")(today = date("2025-01-14"))

        assertThat(occurrences()).single().transform { Triple(it.occurrenceDate, it.recordedAt, it.zoneId) }
            .isEqualTo(Triple(date("2025-01-14"), Instant.parse("2025-01-15T03:00:00Z"), newYork))
    }

    @Test
    fun `running twice for the same day adds nothing the second time`() = runTest {
        insertSeries(original("2025-01-13T10:30:00Z"))
        val materialize = materializeAt("2025-01-16T12:00:00Z")

        materialize(today = date("2025-01-16"))
        val second = materialize(today = date("2025-01-16"))

        assertThat(second).isEqualTo(Result.Success(Unit))
        assertThat(occurrences()).hasSize(3)
    }

    @Test
    fun `a later run adds only the new due dates`() = runTest {
        insertSeries(original("2025-01-13T10:30:00Z"))

        materializeAt("2025-01-14T12:00:00Z")(today = date("2025-01-14"))
        materializeAt("2025-01-16T12:00:00Z")(today = date("2025-01-16"))

        assertThat(occurrences().map { it.occurrenceDate }).containsExactly(
            date("2025-01-14"),
            date("2025-01-15"),
            date("2025-01-16"),
        )
    }

    @Test
    fun `nothing is added on the original's own day`() = runTest {
        insertSeries(original("2025-01-13T10:30:00Z"))

        materializeAt("2025-01-13T12:00:00Z")(today = date("2025-01-13"))

        assertThat(occurrences()).isEmpty()
    }

    @Test
    fun `never creates a future-dated occurrence`() = runTest {
        insertSeries(original("2025-01-13T10:30:00Z", repeat = RepeatFrequency.WEEKLY))

        materializeAt("2025-01-19T12:00:00Z")(today = date("2025-01-19"))

        assertThat(occurrences()).isEmpty()
        assertThat(dataSource.storedSeries.single().lastMaterializedDate).isEqualTo(date("2025-01-19"))
    }

    @Test
    fun `the series high-water mark is advanced to today`() = runTest {
        insertSeries(original("2025-01-13T10:30:00Z"))

        materializeAt("2025-01-16T12:00:00Z")(today = date("2025-01-16"))

        assertThat(dataSource.storedSeries.single().lastMaterializedDate).isEqualTo(date("2025-01-16"))
    }

    @Test
    fun `a date that moved back adds nothing and never lowers the high-water mark`() = runTest {
        insertSeries(original("2025-01-13T10:30:00Z"))
        materializeAt("2025-01-16T12:00:00Z")(today = date("2025-01-16"))

        val result = materializeAt("2025-01-14T12:00:00Z")(today = date("2025-01-14"))

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(occurrences()).hasSize(3)
        assertThat(dataSource.storedSeries.single().lastMaterializedDate).isEqualTo(date("2025-01-16"))
    }

    @Test
    fun `a stopped series adds nothing, including a catch-up`() = runTest {
        val id = insertSeries(original("2025-01-13T10:30:00Z"))
        dataSource.stopSeries(id, stoppedOn = date("2025-01-13"))

        materializeAt("2025-01-20T12:00:00Z")(today = date("2025-01-20"))

        assertThat(occurrences()).isEmpty()
    }

    @Test
    fun `every running series is materialised`() = runTest {
        val gym = insertSeries(original("2025-01-13T10:30:00Z", counterparty = "Gym"))
        val rent = insertSeries(original("2025-01-13T09:00:00Z", counterparty = "Rent"))

        materializeAt("2025-01-14T12:00:00Z")(today = date("2025-01-14"))

        assertThat(occurrences().map { it.seriesId to it.counterparty })
            .containsExactlyInAnyOrder(gym to "Gym", rent to "Rent")
    }

    @Test
    fun `one-time transactions never produce occurrences`() = runTest {
        dataSource.insert(original("2025-01-13T10:30:00Z", repeat = RepeatFrequency.DOES_NOT_REPEAT))

        materializeAt("2025-01-20T12:00:00Z")(today = date("2025-01-20"))

        assertThat(occurrences()).isEmpty()
    }

    @Test
    fun `occurrences never produce occurrences of their own`() = runTest {
        insertSeries(original("2025-01-13T10:30:00Z"))

        materializeAt("2025-01-15T12:00:00Z")(today = date("2025-01-15"))

        // Two occurrences for one series; none of them started a series.
        assertThat(occurrences()).hasSize(2)
        assertThat(dataSource.storedSeries).hasSize(1)
    }

    @Test
    fun `a storage error is returned`() = runTest {
        insertSeries(original("2025-01-13T10:30:00Z"))
        dataSource.error = DataError.Local.DISK_FULL

        val result = materializeAt("2025-01-16T12:00:00Z")(today = date("2025-01-16"))

        assertThat(result).isEqualTo(Result.Error(DataError.Local.DISK_FULL))
        assertThat(occurrences()).isEmpty()
    }

    @Test
    fun `a failing series returns its error and the other series are still materialised`() = runTest {
        val gym = insertSeries(original("2025-01-13T10:30:00Z", counterparty = "Gym"))
        val rent = insertSeries(original("2025-01-13T09:00:00Z", counterparty = "Rent"))
        val failingForGym = object : TransactionLocalDataSource by dataSource {
            override suspend fun materialize(
                seriesId: Long,
                occurrences: List<Transaction>,
                lastMaterialized: LocalDate,
            ): EmptyResult<DataError.Local> =
                if (seriesId == gym) Result.Error(DataError.Local.DISK_FULL)
                else dataSource.materialize(seriesId, occurrences, lastMaterialized)
        }
        val materialize = MaterializeDueOccurrences(
            dataSource = failingForGym,
            schedule = RecurrenceSchedule,
            clock = Clock.fixed(Instant.parse("2025-01-14T12:00:00Z"), ZoneOffset.UTC),
        )

        val result = materialize(today = date("2025-01-14"))

        assertThat(result).isEqualTo(Result.Error(DataError.Local.DISK_FULL))
        assertThat(occurrences().map { it.seriesId }).containsExactly(rent)
    }

    @Test
    fun `no series means success and no writes`() = runTest {
        val result = materializeAt("2025-01-16T12:00:00Z")(today = date("2025-01-16"))

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(dataSource.storedTransactions).isEmpty()
    }
}
