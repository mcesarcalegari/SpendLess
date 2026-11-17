package com.plcoding.spendlesssample.transactions.presentation.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.plcoding.spendlesssample.core.domain.fake.FakeAmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.fake.FakeCurrentDateProvider
import com.plcoding.spendlesssample.core.domain.format.NegativeStyle
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.core.presentation.format.AmountFormatter
import com.plcoding.spendlesssample.transactions.domain.fake.FakeTransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveTransactions
import com.plcoding.spendlesssample.transactions.presentation.R
import com.plcoding.spendlesssample.transactions.presentation.format.DayHeaderFormatter
import com.plcoding.spendlesssample.transactions.presentation.mapper.TransactionUiMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModelTest {

    private val today = LocalDate.of(2026, 1, 15)
    private val dataSource = FakeTransactionLocalDataSource()
    private val dateProvider = FakeCurrentDateProvider(today)
    private val preferences = FakeAmountFormatPreferencesSource()
    private val savedStateHandle = SavedStateHandle()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = savedStateHandle) = TransactionListViewModel(
        observeTransactions = ObserveTransactions(dataSource),
        currentDateProvider = dateProvider,
        preferencesSource = preferences,
        mapper = TransactionUiMapper(AmountFormatter(), DayHeaderFormatter()),
        savedStateHandle = handle,
    )

    private fun tx(name: String, at: LocalDateTime, type: TransactionType = TransactionType.EXPENSE) = Transaction(
        id = 0,
        type = type,
        counterparty = name,
        amountMinor = 4_599,
        note = null,
        category = if (type == TransactionType.EXPENSE) ExpenseCategory.HOME else null,
        repeat = RepeatFrequency.DOES_NOT_REPEAT,
        recordedAt = at.toInstant(ZoneOffset.UTC),
        zoneId = ZoneOffset.UTC,
        seriesId = null,
        occurrenceDate = null,
    )

    /** Skips states until one satisfies [predicate]; the initial `stateIn` value may come first. */
    private suspend fun ReceiveTurbine<TransactionListState>.awaitState(
        predicate: (TransactionListState) -> Boolean,
    ): TransactionListState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    private fun TransactionListState.titles() = groups.map { group -> group.items.map { it.title } }

    @Test
    fun `groups reflect the data source, newest day first`() = runTest {
        dataSource.insert(tx("Older", today.minusDays(3).atTime(9, 0)))
        dataSource.insert(tx("Morning", today.atTime(8, 0)))
        dataSource.insert(tx("Evening", today.atTime(20, 0)))

        viewModel().state.test {
            val state = awaitState { it.groups.isNotEmpty() }

            assertThat(state.titles()).containsExactly(listOf("Evening", "Morning"), listOf("Older"))
            assertThat(state.groups.map { it.header }).containsExactly(
                UiText.StringResource(R.string.today),
                UiText.DynamicString("JANUARY 12"),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no transactions give no groups and a closed sheet`() = runTest {
        viewModel().state.test {
            val state = awaitItem()

            assertThat(state.groups).isEmpty()
            assertThat(state.isCreateSheetOpen).isFalse()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a newly inserted transaction appears without a refresh`() = runTest {
        viewModel().state.test {
            awaitItem()

            dataSource.insert(tx("Netflix", today.atTime(10, 0)))

            assertThat(awaitState { it.groups.isNotEmpty() }.titles()).containsExactly(listOf("Netflix"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a new date relabels TODAY and YESTERDAY`() = runTest {
        dataSource.insert(tx("Late", today.atTime(23, 59)))

        viewModel().state.test {
            awaitState { it.groups.isNotEmpty() }

            dateProvider.current.value = today.plusDays(1)

            val relabelled = awaitState { state ->
                state.groups.single().header == UiText.StringResource(R.string.yesterday)
            }
            assertThat(relabelled.titles()).containsExactly(listOf("Late"))

            dateProvider.current.value = today.plusDays(2)

            awaitState { state -> state.groups.single().header == UiText.DynamicString("JANUARY 15") }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a preference change reformats the amounts`() = runTest {
        dataSource.insert(tx("Gym", today.atTime(9, 0)))

        viewModel().state.test {
            val before = awaitState { it.groups.isNotEmpty() }
            assertThat(before.groups.single().items.map { it.formattedAmount }).containsExactly("-$45.99")

            preferences.current.value = preferences.current.value.copy(negativeStyle = NegativeStyle.PARENTHESES)

            awaitState { state -> state.groups.single().items.single().formattedAmount == "($45.99)" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `create click opens the sheet and dismiss closes it`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitItem()

            viewModel.onAction(TransactionListAction.OnCreateClick)
            assertThat(awaitState { it.isCreateSheetOpen }.isCreateSheetOpen).isTrue()

            viewModel.onAction(TransactionListAction.OnCreateSheetDismiss)
            assertThat(awaitState { !it.isCreateSheetOpen }.isCreateSheetOpen).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an open sheet survives process death`() = runTest {
        viewModel().onAction(TransactionListAction.OnCreateClick)

        val restored = viewModel(
            handle = SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) }),
        )

        // collectAsStateWithLifecycle renders state.value before collecting, so the restored sheet
        // must already be open there, or it would flash closed for one frame.
        assertThat(restored.state.value.isCreateSheetOpen).isTrue()
        restored.state.test {
            assertThat(awaitItem().isCreateSheetOpen).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a dismissed sheet stays closed after process death`() = runTest {
        val first = viewModel()
        first.onAction(TransactionListAction.OnCreateClick)
        first.onAction(TransactionListAction.OnCreateSheetDismiss)

        viewModel(handle = SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) }))
            .state.test {
                assertThat(awaitItem().isCreateSheetOpen).isFalse()
                // The restored state equals the initial one, so StateFlow emits nothing further.
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
    }
}
