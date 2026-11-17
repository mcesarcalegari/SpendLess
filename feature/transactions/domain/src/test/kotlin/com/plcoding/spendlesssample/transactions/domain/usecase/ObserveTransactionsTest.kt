package com.plcoding.spendlesssample.transactions.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.plcoding.spendlesssample.transactions.domain.fake.FakeTransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObserveTransactionsTest {

    private val dataSource = FakeTransactionLocalDataSource()
    private val observeTransactions = ObserveTransactions(dataSource)

    private fun tx(name: String, epochSecond: Long) = Transaction(
        id = 0,
        type = TransactionType.EXPENSE,
        counterparty = name,
        amountMinor = 1_000,
        note = null,
        category = ExpenseCategory.OTHER,
        repeat = RepeatFrequency.DOES_NOT_REPEAT,
        recordedAt = Instant.ofEpochSecond(epochSecond),
        zoneId = ZoneOffset.UTC,
        seriesId = null,
        occurrenceDate = null,
    )

    @Test
    fun `emits the data source list newest first`() = runTest {
        dataSource.insert(tx("middle", 200))
        dataSource.insert(tx("newest", 300))
        dataSource.insert(tx("oldest", 100))

        observeTransactions().test {
            assertThat(awaitItem().map { it.counterparty }).containsExactly("newest", "middle", "oldest")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `re-emits when a transaction is inserted`() = runTest {
        observeTransactions().test {
            assertThat(awaitItem()).isEmpty()
            dataSource.insert(tx("coffee", 100))
            assertThat(awaitItem().map { it.counterparty }).containsExactly("coffee")
            dataSource.insert(tx("lunch", 200))
            assertThat(awaitItem().map { it.counterparty }).containsExactly("lunch", "coffee")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
