package com.plcoding.spendlesssample.transactions.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteFullException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.plcoding.spendlesssample.core.database.SpendLessDatabase
import com.plcoding.spendlesssample.core.database.dao.TransactionDao
import com.plcoding.spendlesssample.core.database.entity.RecurringSeriesEntity
import com.plcoding.spendlesssample.core.database.entity.TransactionEntity
import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTransactionDataSourceTest {

    private lateinit var db: SpendLessDatabase
    private lateinit var dataSource: RoomTransactionDataSource
    private val zone = ZoneId.of("Europe/Berlin")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SpendLessDatabase::class.java,
        ).allowMainThreadQueries().build()
        dataSource = RoomTransactionDataSource(db.transactionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tx(
        name: String,
        recordedAt: String,
        repeat: RepeatFrequency = RepeatFrequency.DOES_NOT_REPEAT,
        seriesId: Long? = null,
        occurrenceDate: LocalDate? = null,
    ) = Transaction(
        id = 0,
        type = TransactionType.EXPENSE,
        counterparty = name,
        amountMinor = 1_000,
        note = null,
        category = ExpenseCategory.OTHER,
        repeat = repeat,
        recordedAt = Instant.parse(recordedAt),
        zoneId = zone,
        seriesId = seriesId,
        occurrenceDate = occurrenceDate,
    )

    @Test
    fun insert_thenObserve_isNewestFirst() = runTest {
        dataSource.observeTransactions().test {
            assertThat(awaitItem()).isEmpty()
            dataSource.insert(tx("older", "2026-03-10T08:00:00Z"))
            assertThat(awaitItem().map { it.counterparty }).containsExactly("older")
            dataSource.insert(tx("newer", "2026-03-11T08:00:00Z"))
            assertThat(awaitItem().map { it.counterparty }).containsExactly("newer", "older")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertRecurring_createsOriginalAndSeriesAtomically() = runTest {
        val result = dataSource.insertRecurring(tx("Rent", "2026-03-10T08:15:00Z", RepeatFrequency.MONTHLY), RepeatFrequency.MONTHLY)

        val id = (result as Result.Success).data
        val original = dataSource.observeTransactions().first().single()
        assertThat(original.seriesId).isEqualTo(id)
        val series = (dataSource.runningSeries() as Result.Success).data.single()
        assertThat(series.seriesId).isEqualTo(id)
        assertThat(series.frequency).isEqualTo(RepeatFrequency.MONTHLY)
        // 08:15 UTC is 09:15 in Berlin on March 10.
        assertThat(series.anchorDate).isEqualTo(LocalDate.of(2026, 3, 10))
        assertThat(series.anchorTime).isEqualTo(LocalTime.of(9, 15))
        assertThat(series.zoneId).isEqualTo(zone)
        assertThat(series.lastMaterializedDate).isEqualTo(LocalDate.of(2026, 3, 10))
    }

    @Test
    fun materialize_ignoresDuplicates_andAdvancesLastMaterialized() = runTest {
        val id = (dataSource.insertRecurring(tx("Rent", "2026-03-10T08:00:00Z", RepeatFrequency.MONTHLY), RepeatFrequency.MONTHLY) as Result.Success).data
        val april = tx("Rent", "2026-04-10T08:00:00Z", seriesId = id, occurrenceDate = LocalDate.of(2026, 4, 10))

        dataSource.materialize(id, listOf(april), LocalDate.of(2026, 4, 10))
        val second = dataSource.materialize(
            id,
            listOf(april, tx("Rent", "2026-05-10T08:00:00Z", seriesId = id, occurrenceDate = LocalDate.of(2026, 5, 10))),
            LocalDate.of(2026, 5, 10),
        )

        assertThat(second).isEqualTo(Result.Success(Unit))
        val occurrences = dataSource.observeTransactions().first().mapNotNull { it.occurrenceDate }
        assertThat(occurrences).containsExactlyInAnyOrder(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 5, 10))
        assertThat((dataSource.runningSeries() as Result.Success).data.single().lastMaterializedDate)
            .isEqualTo(LocalDate.of(2026, 5, 10))
    }

    @Test
    fun stopSeries_removesItFromRunningIds() = runTest {
        val first = (dataSource.insertRecurring(tx("Rent", "2026-03-10T08:00:00Z", RepeatFrequency.MONTHLY), RepeatFrequency.MONTHLY) as Result.Success).data
        val second = (dataSource.insertRecurring(tx("Gym", "2026-03-11T08:00:00Z", RepeatFrequency.WEEKLY), RepeatFrequency.WEEKLY) as Result.Success).data

        dataSource.observeRunningSeriesIds().test {
            assertThat(awaitItem()).isEqualTo(setOf(first, second))
            dataSource.stopSeries(first, LocalDate.of(2026, 3, 20))
            assertThat(awaitItem()).isEqualTo(setOf(second))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat((dataSource.runningSeries() as Result.Success).data).hasSize(1)
    }

    @Test
    fun diskFull_mapsToDiskFull() = runTest {
        val failing = RoomTransactionDataSource(ThrowingDao(SQLiteFullException("full")))

        assertThat(failing.insert(tx("x", "2026-03-10T08:00:00Z"))).isEqualTo(Result.Error(DataError.Local.DISK_FULL))
        assertThat(failing.runningSeries()).isEqualTo(Result.Error(DataError.Local.DISK_FULL))
    }

    @Test
    fun otherSqliteError_mapsToUnknown() = runTest {
        val failing = RoomTransactionDataSource(ThrowingDao(SQLiteConstraintException("constraint")))

        assertThat(failing.insert(tx("x", "2026-03-10T08:00:00Z"))).isEqualTo(Result.Error(DataError.Local.UNKNOWN))
        assertThat(failing.stopSeries(1, LocalDate.of(2026, 1, 1))).isEqualTo(Result.Error(DataError.Local.UNKNOWN))
        assertThat(failing.materialize(1, emptyList(), LocalDate.of(2026, 1, 1)))
            .isEqualTo(Result.Error(DataError.Local.UNKNOWN))
        assertThat(failing.insertRecurring(tx("x", "2026-03-10T08:00:00Z"), RepeatFrequency.DAILY))
            .isEqualTo(Result.Error(DataError.Local.UNKNOWN))
    }

    @Test
    fun observeFlows_endInsteadOfThrowing_onStorageFailure() = runTest {
        val failing = RoomTransactionDataSource(ThrowingDao(SQLiteFullException("full")))

        failing.observeTransactions().test { awaitComplete() }
        failing.observeRunningSeriesIds().test { awaitComplete() }
    }

    @Test
    fun observeFlows_rethrowNonStorageFailures() = runTest {
        val failing = RoomTransactionDataSource(ThrowingDao(IllegalStateException("bug")))

        failing.observeTransactions().test {
            assertThat(awaitError()).isInstanceOf(IllegalStateException::class)
        }
    }

    @Test
    fun cancellation_isRethrown_notMapped() = runTest {
        val failing = RoomTransactionDataSource(ThrowingDao(CancellationException("cancelled")))

        assertFailure { failing.insert(tx("x", "2026-03-10T08:00:00Z")) }
            .isInstanceOf(CancellationException::class)
    }

    /** DAO whose every suspend call throws [error]. */
    private class ThrowingDao(private val error: Throwable) : TransactionDao() {
        override fun observeAll(): Flow<List<TransactionEntity>> = flow { throw error }
        override suspend fun insert(entity: TransactionEntity): Long = throw error
        override suspend fun insertOccurrences(occurrences: List<TransactionEntity>) = throw error
        override suspend fun insertSeries(entity: RecurringSeriesEntity) = throw error
        override suspend fun runningSeries(): List<RecurringSeriesEntity> = throw error
        override fun observeRunningSeriesIds(): Flow<List<Long>> = flow { throw error }
        override suspend fun markStopped(seriesId: Long, epochDay: Long) = throw error
        override suspend fun updateLastMaterialized(seriesId: Long, epochDay: Long) = throw error
        override suspend fun setSeriesId(id: Long, seriesId: Long) = throw error
    }
}
