package com.plcoding.spendlesssample.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.single
import com.plcoding.spendlesssample.core.database.SpendLessDatabase
import com.plcoding.spendlesssample.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var db: SpendLessDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpendLessDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        recordedAt: Long,
        name: String = "Tx $recordedAt",
        seriesId: Long? = null,
        occurrenceDay: Long? = null,
    ) = TransactionEntity(
        type = "EXPENSE",
        counterparty = name,
        amountMinor = 1_000,
        note = null,
        category = "OTHER",
        repeat = "DOES_NOT_REPEAT",
        recordedAtEpochMs = recordedAt,
        zoneId = "UTC",
        seriesId = seriesId,
        occurrenceDateEpochDay = occurrenceDay,
    )

    private suspend fun insertOriginal(recordedAt: Long = 1_000, anchorDay: Long = 100): Long =
        dao.insertWithSeries(
            transaction = entity(recordedAt, name = "Rent").copy(repeat = "MONTHLY"),
            frequency = "MONTHLY",
            anchorEpochDay = anchorDay,
            anchorSecondsOfDay = 3_600,
            zoneId = "UTC",
        )

    @Test
    fun observeAll_isOrderedNewestFirst() = runTest {
        dao.insert(entity(recordedAt = 2_000, name = "middle"))
        dao.insert(entity(recordedAt = 3_000, name = "newest"))
        dao.insert(entity(recordedAt = 1_000, name = "oldest"))

        val names = dao.observeAll().first().map { it.counterparty }

        assertThat(names).containsExactly("newest", "middle", "oldest")
    }

    @Test
    fun observeAll_reEmitsAfterInsert() = runTest {
        dao.observeAll().test {
            assertThat(awaitItem()).isEmpty()
            dao.insert(entity(recordedAt = 1_000))
            assertThat(awaitItem()).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertOccurrences_duplicateSeriesAndDate_keepsExactlyOneRow() = runTest {
        val seriesId = insertOriginal()

        dao.insertOccurrences(listOf(entity(2_000, seriesId = seriesId, occurrenceDay = 131)))
        dao.insertOccurrences(
            listOf(
                entity(2_500, name = "duplicate", seriesId = seriesId, occurrenceDay = 131),
                entity(3_000, seriesId = seriesId, occurrenceDay = 159),
            ),
        )

        val occurrences = dao.observeAll().first().filter { it.occurrenceDateEpochDay != null }
        assertThat(occurrences.map { it.occurrenceDateEpochDay })
            .containsExactlyInAnyOrder(131L, 159L)
        assertThat(occurrences.map { it.counterparty }).containsExactlyInAnyOrder("Tx 2000", "Tx 3000")
    }

    @Test
    fun oneTimeRows_withNullSeriesId_neverCollide() = runTest {
        dao.insert(entity(recordedAt = 1_000))
        dao.insert(entity(recordedAt = 1_000))
        dao.insertOccurrences(listOf(entity(recordedAt = 1_000), entity(recordedAt = 1_000)))

        assertThat(dao.observeAll().first()).hasSize(4)
    }

    @Test
    fun insertWithSeries_setsSeriesIdToOwnId_andCreatesRunningSeries() = runTest {
        val id = insertOriginal(anchorDay = 100)

        val original = dao.observeAll().first().single()
        assertThat(original.id).isEqualTo(id)
        assertThat(original.seriesId).isEqualTo(id)

        val series = dao.runningSeries()
        assertThat(series).single().transform { it.seriesId }.isEqualTo(id)
        assertThat(series.single().frequency).isEqualTo("MONTHLY")
        assertThat(series.single().anchorEpochDay).isEqualTo(100L)
        assertThat(series.single().anchorSecondsOfDay).isEqualTo(3_600)
        assertThat(series.single().lastMaterializedEpochDay).isEqualTo(100L)
        assertThat(series.single().stoppedOnEpochDay).isNull()
    }

    @Test
    fun markStopped_isIdempotent() = runTest {
        val id = insertOriginal()

        dao.markStopped(seriesId = id, epochDay = 120)
        dao.markStopped(seriesId = id, epochDay = 150)

        val stopped = db.query("SELECT stopped_on_epoch_day FROM recurring_series", null)
            .use { cursor -> cursor.moveToFirst(); cursor.getLong(0) }
        assertThat(stopped).isEqualTo(120L)
    }

    @Test
    fun runningSeries_excludesStoppedSeries() = runTest {
        val running = insertOriginal(recordedAt = 1_000)
        val stopped = insertOriginal(recordedAt = 2_000)

        dao.markStopped(seriesId = stopped, epochDay = 120)

        assertThat(dao.runningSeries().map { it.seriesId }).containsExactly(running)
        assertThat(dao.observeRunningSeriesIds().first()).containsExactly(running)
    }

    @Test
    fun materialize_insertsOccurrences_andAdvancesLastMaterialized() = runTest {
        val id = insertOriginal(anchorDay = 100)

        dao.materialize(
            seriesId = id,
            occurrences = listOf(
                entity(2_000, seriesId = id, occurrenceDay = 131),
                entity(3_000, seriesId = id, occurrenceDay = 159),
            ),
            newLastEpochDay = 159,
        )

        assertThat(dao.observeAll().first()).hasSize(3)
        assertThat(dao.runningSeries().single().lastMaterializedEpochDay).isEqualTo(159L)
    }

    @Test
    fun materialize_rollsBackOccurrences_whenAdvancingFails() = runTest {
        val seriesId = insertOriginal(anchorDay = 100)
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_update BEFORE UPDATE OF last_materialized_epoch_day " +
                "ON recurring_series BEGIN SELECT RAISE(ABORT, 'boom'); END",
        )

        val result = runCatching {
            dao.materialize(
                seriesId = seriesId,
                occurrences = listOf(entity(2_000, seriesId = seriesId, occurrenceDay = 131)),
                newLastEpochDay = 131,
            )
        }

        assertThat(result.isFailure).isTrue()
        assertThat(dao.observeAll().first()).hasSize(1)
        assertThat(dao.runningSeries().single().lastMaterializedEpochDay).isEqualTo(100L)
    }
}
