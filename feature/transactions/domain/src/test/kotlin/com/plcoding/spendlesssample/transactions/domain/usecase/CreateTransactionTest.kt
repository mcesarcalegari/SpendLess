package com.plcoding.spendlesssample.transactions.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.fake.FakeTransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CreateTransactionTest {

    private val dataSource = FakeTransactionLocalDataSource()
    private val createTransaction = CreateTransaction(dataSource)

    private val transaction = Transaction(
        id = 0,
        type = TransactionType.EXPENSE,
        counterparty = "Netflix",
        amountMinor = 1_599,
        note = null,
        category = ExpenseCategory.ENTERTAINMENT,
        repeat = RepeatFrequency.DOES_NOT_REPEAT,
        recordedAt = Instant.parse("2026-01-15T10:30:00Z"),
        zoneId = ZoneOffset.UTC,
        seriesId = null,
        occurrenceDate = null,
    )

    @Test
    fun `one-time transaction is inserted and its id returned`() = runTest {
        val result = createTransaction(transaction)

        assertThat(result).isEqualTo(Result.Success(1L))
        assertThat(dataSource.storedTransactions).containsExactly(transaction.copy(id = 1))
        assertThat(dataSource.storedSeries).isEmpty()
    }

    @Test
    fun `each call inserts a new row with its own id`() = runTest {
        createTransaction(transaction)
        val second = createTransaction(transaction.copy(counterparty = "Spotify"))

        assertThat(second).isEqualTo(Result.Success(2L))
        assertThat(dataSource.storedTransactions.map { it.counterparty }).containsExactly("Netflix", "Spotify")
    }

    @Test
    fun `DISK_FULL from the data source is returned unchanged`() = runTest {
        dataSource.error = DataError.Local.DISK_FULL

        assertThat(createTransaction(transaction)).isEqualTo(Result.Error(DataError.Local.DISK_FULL))
        assertThat(dataSource.storedTransactions).isEmpty()
    }

    @Test
    fun `UNKNOWN from the data source is returned unchanged`() = runTest {
        dataSource.error = DataError.Local.UNKNOWN

        assertThat(createTransaction(transaction)).isEqualTo(Result.Error(DataError.Local.UNKNOWN))
    }
}
