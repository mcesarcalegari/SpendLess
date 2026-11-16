package com.plcoding.spendlesssample.transactions.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.spendlesssample.core.database.entity.RecurringSeriesEntity
import com.plcoding.spendlesssample.core.database.entity.TransactionEntity
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RecurringSeries
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.jupiter.api.Test

class TransactionMappersTest {

    private val zone = ZoneId.of("Europe/Berlin")

    private val expense = Transaction(
        id = 7,
        type = TransactionType.EXPENSE,
        counterparty = "Groceries",
        amountMinor = 4_599,
        note = "Weekly shop",
        category = ExpenseCategory.FOOD_AND_GROCERIES,
        repeat = RepeatFrequency.DOES_NOT_REPEAT,
        recordedAt = Instant.parse("2026-03-10T08:15:30.123Z"),
        zoneId = zone,
        seriesId = null,
        occurrenceDate = null,
    )

    @Test
    fun `expense maps to the entity column values`() {
        assertThat(expense.toEntity()).isEqualTo(
            TransactionEntity(
                id = 7,
                type = "EXPENSE",
                counterparty = "Groceries",
                amountMinor = 4_599,
                note = "Weekly shop",
                category = "FOOD_AND_GROCERIES",
                repeat = "DOES_NOT_REPEAT",
                recordedAtEpochMs = Instant.parse("2026-03-10T08:15:30.123Z").toEpochMilli(),
                zoneId = "Europe/Berlin",
                seriesId = null,
                occurrenceDateEpochDay = null,
            ),
        )
    }

    @Test
    fun `expense round-trips`() {
        assertThat(expense.toEntity().toTransaction()).isEqualTo(expense)
    }

    @Test
    fun `income without category or note round-trips`() {
        val income = expense.copy(type = TransactionType.INCOME, category = null, note = null, counterparty = "Salary")

        assertThat(income.toEntity().category).isEqualTo(null)
        assertThat(income.toEntity().toTransaction()).isEqualTo(income)
    }

    @Test
    fun `recurring original keeps its series id`() {
        val original = expense.copy(repeat = RepeatFrequency.MONTHLY, seriesId = 7)

        assertThat(original.toEntity().repeat).isEqualTo("MONTHLY")
        assertThat(original.toEntity().toTransaction()).isEqualTo(original)
    }

    @Test
    fun `occurrence maps its date to an epoch day`() {
        val occurrence = expense.copy(id = 9, seriesId = 7, occurrenceDate = LocalDate.of(2026, 4, 10))

        assertThat(occurrence.toEntity().occurrenceDateEpochDay).isEqualTo(LocalDate.of(2026, 4, 10).toEpochDay())
        assertThat(occurrence.toEntity().toTransaction()).isEqualTo(occurrence)
    }

    @Test
    fun `every category and frequency round-trips`() {
        ExpenseCategory.entries.forEach { category ->
            val tx = expense.copy(category = category)
            assertThat(tx.toEntity().toTransaction()).isEqualTo(tx)
        }
        RepeatFrequency.entries.forEach { frequency ->
            val tx = expense.copy(repeat = frequency)
            assertThat(tx.toEntity().toTransaction()).isEqualTo(tx)
        }
    }

    @Test
    fun `running series maps epoch day, seconds of day and zone`() {
        val entity = RecurringSeriesEntity(
            seriesId = 7,
            frequency = "WEEKLY",
            anchorEpochDay = LocalDate.of(2026, 3, 10).toEpochDay(),
            anchorSecondsOfDay = LocalTime.of(9, 15, 30).toSecondOfDay(),
            zoneId = "Europe/Berlin",
            stoppedOnEpochDay = null,
            lastMaterializedEpochDay = LocalDate.of(2026, 3, 17).toEpochDay(),
        )

        assertThat(entity.toRecurringSeries()).isEqualTo(
            RecurringSeries(
                seriesId = 7,
                frequency = RepeatFrequency.WEEKLY,
                anchorDate = LocalDate.of(2026, 3, 10),
                anchorTime = LocalTime.of(9, 15, 30),
                zoneId = zone,
                stoppedOn = null,
                lastMaterializedDate = LocalDate.of(2026, 3, 17),
            ),
        )
    }

    @Test
    fun `stopped series maps its stop date`() {
        val entity = RecurringSeriesEntity(
            seriesId = 7,
            frequency = "YEARLY",
            anchorEpochDay = LocalDate.of(2024, 2, 29).toEpochDay(),
            anchorSecondsOfDay = 0,
            zoneId = "UTC",
            stoppedOnEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            lastMaterializedEpochDay = LocalDate.of(2025, 2, 28).toEpochDay(),
        )

        assertThat(entity.toRecurringSeries().stoppedOn).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(entity.toRecurringSeries().frequency).isEqualTo(RepeatFrequency.YEARLY)
    }
}
