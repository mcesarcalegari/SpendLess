package com.plcoding.spendlesssample

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import com.plcoding.spendlesssample.core.database.SpendLessDatabase
import com.plcoding.spendlesssample.core.domain.time.CurrentDateProvider
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.data.RoomTransactionDataSource
import com.plcoding.spendlesssample.transactions.domain.RecurrenceSchedule
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.domain.usecase.MaterializeDueOccurrences
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app-scoped trigger against the real use case and an in-memory Room data source. Only the
 * date source and the clock are fakes.
 */
@RunWith(AndroidJUnit4::class)
class RecurringOccurrencesTriggerTest {

    private val today = LocalDate.of(2026, 1, 15)

    private lateinit var db: SpendLessDatabase
    private lateinit var dataSource: RoomTransactionDataSource
    private lateinit var scope: CoroutineScope
    private lateinit var lifecycleOwner: TestLifecycleOwner
    private lateinit var trigger: RecurringOccurrencesTrigger
    private lateinit var job: Job

    /** Replays the latest date and, unlike a StateFlow, re-emits a date equal to the last one. */
    private val dates = MutableSharedFlow<LocalDate>(replay = 1)
    private val dateProvider = object : CurrentDateProvider {
        override val today = dates
    }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SpendLessDatabase::class.java,
        ).build()
        dataSource = RoomTransactionDataSource(db.transactionDao())
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        trigger = RecurringOccurrencesTrigger(
            currentDateProvider = dateProvider,
            materializeDueOccurrences = MaterializeDueOccurrences(
                dataSource = dataSource,
                schedule = RecurrenceSchedule,
                // Late in the day, so every occurrence keeps the original's 10:00.
                clock = Clock.fixed(today.plusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC),
            ),
            scope = scope,
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    private suspend fun insertDailyGymCreated(daysAgo: Long) {
        val original = Transaction(
            id = 0,
            type = TransactionType.EXPENSE,
            counterparty = "Gym",
            amountMinor = 4_599,
            note = null,
            category = ExpenseCategory.HEALTH_AND_WELLNESS,
            repeat = RepeatFrequency.DAILY,
            recordedAt = today.minusDays(daysAgo).atTime(10, 0).toInstant(ZoneOffset.UTC),
            zoneId = ZoneOffset.UTC,
            seriesId = null,
            occurrenceDate = null,
        )
        check(dataSource.insertRecurring(original, RepeatFrequency.DAILY) is Result.Success)
    }

    /** Waits until exactly [count] occurrences are stored, and returns their dates. */
    private suspend fun awaitOccurrenceDates(count: Int): List<LocalDate> = withTimeout(5_000) {
        dataSource.observeTransactions()
            .first { list -> list.count { it.occurrenceDate != null } >= count }
            .mapNotNull { it.occurrenceDate }
            .sorted()
    }

    private fun start() {
        job = trigger.start(lifecycleOwner.lifecycle)
        lifecycleOwner.currentState = Lifecycle.State.STARTED
    }

    @Test
    fun aDailySeriesCreatedThreeDaysAgoIsCaughtUpOnStart() = runBlocking {
        insertDailyGymCreated(daysAgo = 3)
        dates.emit(today)

        start()

        assertThat(awaitOccurrenceDates(3)).containsExactly(
            today.minusDays(2),
            today.minusDays(1),
            today,
        )
    }

    @Test
    fun theNextDateWhileStartedAddsExactlyOneMore() = runBlocking {
        insertDailyGymCreated(daysAgo = 3)
        dates.emit(today)
        start()
        awaitOccurrenceDates(3)

        dates.emit(today.plusDays(1))

        assertThat(awaitOccurrenceDates(4)).containsExactly(
            today.minusDays(2),
            today.minusDays(1),
            today,
            today.plusDays(1),
        )
    }

    @Test
    fun reEmittingTheSameDateAddsNone() = runBlocking {
        insertDailyGymCreated(daysAgo = 3)
        dates.emit(today)
        start()
        awaitOccurrenceDates(3)

        dates.emit(today)
        // The next date is processed after the repeat, so a duplicate would already be stored.
        dates.emit(today.plusDays(1))

        assertThat(awaitOccurrenceDates(4)).containsExactly(
            today.minusDays(2),
            today.minusDays(1),
            today,
            today.plusDays(1),
        )
    }

    @Test
    fun datesMissedWhileStoppedAreCaughtUpOnTheNextStart() = runBlocking {
        insertDailyGymCreated(daysAgo = 1)
        dates.emit(today)
        start()
        awaitOccurrenceDates(1)
        // The app goes to the background; two days pass.
        lifecycleOwner.currentState = Lifecycle.State.CREATED

        dates.emit(today.plusDays(2))
        lifecycleOwner.currentState = Lifecycle.State.STARTED

        assertThat(awaitOccurrenceDates(3)).containsExactly(
            today,
            today.plusDays(1),
            today.plusDays(2),
        )
    }
}
