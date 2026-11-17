package com.plcoding.spendlesssample.transactions.presentation.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.plcoding.spendlesssample.core.domain.fake.FakeAmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.fake.FakeCurrentDateProvider
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import com.plcoding.spendlesssample.core.domain.format.NegativeStyle
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.core.presentation.format.AmountFormatter
import com.plcoding.spendlesssample.transactions.domain.fake.FakeTransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveRunningSeriesIds
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveTransactions
import com.plcoding.spendlesssample.transactions.domain.usecase.StopRecurringSeries
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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
        observeRunningSeriesIds = ObserveRunningSeriesIds(dataSource),
        stopRecurringSeries = StopRecurringSeries(dataSource),
        currentDateProvider = dateProvider,
        preferencesSource = preferences,
        mapper = TransactionUiMapper(AmountFormatter(), DayHeaderFormatter()),
        savedStateHandle = handle,
    )

    private fun tx(
        name: String,
        at: LocalDateTime,
        type: TransactionType = TransactionType.EXPENSE,
        amountMinor: Long = 4_599,
    ) = Transaction(
        id = 0,
        type = type,
        counterparty = name,
        amountMinor = amountMinor,
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

    // region Amount format preferences (User Story 7, FR-032 – FR-035)

    @ParameterizedTest(name = "{0}")
    @MethodSource("formatCases")
    fun `every row follows the preferences from the shared source`(case: FormatCase) = runTest {
        preferences.current.value = case.prefs
        dataSource.insert(tx("Rent", today.atTime(9, 0), amountMinor = 123_450))
        dataSource.insert(tx("Salary", today.atTime(10, 0), TransactionType.INCOME, amountMinor = 1_200_000))
        dataSource.insert(tx("Gym", today.atTime(11, 0), amountMinor = 4_599))

        viewModel().state.test {
            val amounts = awaitState { it.groups.isNotEmpty() }.groups.single().items
                .associate { it.title to it.formattedAmount }

            assertThat(amounts).isEqualTo(
                mapOf("Rent" to case.rent, "Salary" to case.salary, "Gym" to case.gym),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing the separators reformats existing rows without a new transaction`() = runTest {
        dataSource.insert(tx("Rent", today.atTime(9, 0), amountMinor = 123_450))
        dataSource.insert(tx("Salary", today.atTime(10, 0), TransactionType.INCOME, amountMinor = 1_200_000))

        viewModel().state.test {
            awaitState { it.groups.isNotEmpty() }

            preferences.current.value = CommaDecimal

            val amounts = awaitState { state -> state.groups.single().items.any { it.formattedAmount == "-$1.234,50" } }
                .groups.single().items.associate { it.title to it.formattedAmount }
            assertThat(amounts).isEqualTo(mapOf("Rent" to "-$1.234,50", "Salary" to "$12.000,00"))
            assertThat(dataSource.storedTransactions.size).isEqualTo(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

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

    /** Inserts [transaction] and returns its storage id. */
    private suspend fun insert(transaction: Transaction): Long =
        (dataSource.insert(transaction) as Result.Success).data

    @Test
    fun `no row is expanded at first`() = runTest {
        insert(tx("Netflix", today.atTime(9, 0)))

        viewModel().state.test {
            assertThat(awaitState { it.groups.isNotEmpty() }.expandedIds).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a note badge click expands the row and a second click collapses it`() = runTest {
        val id = insert(tx("Netflix", today.atTime(9, 0)))
        val viewModel = viewModel()

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }

            viewModel.onAction(TransactionListAction.OnNoteBadgeClick(id))
            assertThat(awaitState { id in it.expandedIds }.expandedIds).containsOnly(id)

            viewModel.onAction(TransactionListAction.OnNoteBadgeClick(id))
            assertThat(awaitState { it.expandedIds.isEmpty() }.expandedIds).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rows expand and collapse independently`() = runTest {
        val first = insert(tx("Netflix", today.atTime(9, 0)))
        val second = insert(tx("Rick", today.atTime(10, 0), type = TransactionType.INCOME))
        val viewModel = viewModel()

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }

            viewModel.onAction(TransactionListAction.OnNoteBadgeClick(first))
            viewModel.onAction(TransactionListAction.OnNoteBadgeClick(second))
            assertThat(awaitState { it.expandedIds.size == 2 }.expandedIds).containsOnly(first, second)

            viewModel.onAction(TransactionListAction.OnNoteBadgeClick(first))
            assertThat(awaitState { it.expandedIds.size == 1 }.expandedIds).containsOnly(second)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a new transaction starts collapsed and keeps existing expansions`() = runTest {
        val expanded = insert(tx("Netflix", today.atTime(9, 0)))
        val viewModel = viewModel()

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }
            viewModel.onAction(TransactionListAction.OnNoteBadgeClick(expanded))
            awaitState { expanded in it.expandedIds }

            val added = insert(tx("Gym", today.atTime(11, 0)))

            val state = awaitState { s -> s.groups.flatMap { it.items }.any { it.id == added } }
            assertThat(state.expandedIds).containsOnly(expanded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expansions are not restored after process death`() = runTest {
        val id = insert(tx("Netflix", today.atTime(9, 0)))
        val first = viewModel()
        first.state.test {
            awaitState { it.groups.isNotEmpty() }
            first.onAction(TransactionListAction.OnNoteBadgeClick(id))
            awaitState { id in it.expandedIds }
            cancelAndIgnoreRemainingEvents()
        }

        val restored = viewModel(
            handle = SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) }),
        )

        assertThat(restored.state.value.expandedIds).isEmpty()
        restored.state.test {
            assertThat(awaitState { it.groups.isNotEmpty() }.expandedIds).isEmpty()
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

    // region Stop repeating (User Story 6, FR-037 – FR-040)

    /** A running daily "Gym" series created yesterday with today's occurrence; returns (seriesId, occurrenceId). */
    private suspend fun insertGymSeries(): Pair<Long, Long> {
        val original = tx("Gym", today.minusDays(1).atTime(9, 0)).copy(repeat = RepeatFrequency.DAILY)
        val seriesId = (dataSource.insertRecurring(original, RepeatFrequency.DAILY) as Result.Success).data
        val occurrence = original.copy(
            repeat = RepeatFrequency.DOES_NOT_REPEAT,
            recordedAt = today.atTime(9, 0).toInstant(ZoneOffset.UTC),
            seriesId = seriesId,
            occurrenceDate = today,
        )
        dataSource.materialize(seriesId, listOf(occurrence), lastMaterialized = today)
        val occurrenceId = dataSource.storedTransactions.single { it.occurrenceDate == today }.id
        return seriesId to occurrenceId
    }

    private fun TransactionListState.row(id: Long) = groups.flatMap { it.items }.single { it.id == id }

    @Test
    fun `the original and occurrences of a running series are marked, one-time rows are not`() = runTest {
        val (seriesId, occurrenceId) = insertGymSeries()
        val oneTime = insert(tx("Netflix", today.atTime(8, 0)))

        viewModel().state.test {
            val state = awaitState { it.groups.flatMap { g -> g.items }.size == 3 }

            assertThat(state.row(seriesId).isInRunningSeries).isTrue()
            assertThat(state.row(occurrenceId).isInRunningSeries).isTrue()
            assertThat(state.row(oneTime).isInRunningSeries).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rows of a stopped series are no longer marked`() = runTest {
        val (seriesId, occurrenceId) = insertGymSeries()

        viewModel().state.test {
            awaitState { it.groups.isNotEmpty() && it.row(seriesId).isInRunningSeries }

            dataSource.stopSeries(seriesId, today)

            val state = awaitState { !it.row(seriesId).isInRunningSeries }
            assertThat(state.row(occurrenceId).isInRunningSeries).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `long-press on a running-series row opens the menu for that row`() = runTest {
        val (_, occurrenceId) = insertGymSeries()
        val viewModel = viewModel()

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }

            viewModel.onAction(TransactionListAction.OnRowLongPress(occurrenceId))

            assertThat(awaitState { it.contextMenuFor != null }.contextMenuFor).isEqualTo(occurrenceId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `long-press on a one-time row or an unknown id does nothing`() = runTest {
        val oneTime = insert(tx("Netflix", today.atTime(8, 0)))
        val viewModel = viewModel()

        viewModel.state.test {
            val before = awaitState { it.groups.isNotEmpty() }

            viewModel.onAction(TransactionListAction.OnRowLongPress(oneTime))
            viewModel.onAction(TransactionListAction.OnRowLongPress(999))

            expectNoEvents()
            assertThat(viewModel.state.value).isEqualTo(before)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `long-press on a stopped series row does nothing`() = runTest {
        val (seriesId, _) = insertGymSeries()
        dataSource.stopSeries(seriesId, today)
        val viewModel = viewModel()

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }

            viewModel.onAction(TransactionListAction.OnRowLongPress(seriesId))

            expectNoEvents()
            assertThat(viewModel.state.value.contextMenuFor).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissing the menu closes it without changes`() = runTest {
        val (seriesId, _) = insertGymSeries()
        val viewModel = viewModel()

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }
            viewModel.onAction(TransactionListAction.OnRowLongPress(seriesId))
            awaitState { it.contextMenuFor != null }

            viewModel.onAction(TransactionListAction.OnContextMenuDismiss)

            val state = awaitState { it.contextMenuFor == null }
            assertThat(state.stopConfirmationFor).isNull()
            assertThat(state.row(seriesId).isInRunningSeries).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Stop repeating closes the menu and asks to confirm with the row's name and series`() = runTest {
        val (seriesId, occurrenceId) = insertGymSeries()
        val viewModel = viewModel()

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }
            viewModel.onAction(TransactionListAction.OnRowLongPress(occurrenceId))
            awaitState { it.contextMenuFor != null }

            viewModel.onAction(TransactionListAction.OnStopRepeatingClick)

            val state = awaitState { it.stopConfirmationFor != null }
            assertThat(state.contextMenuFor).isNull()
            assertThat(state.stopConfirmationFor).isEqualTo(StopConfirmationUi(seriesId = seriesId, name = "Gym"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Stop repeating without an open menu does nothing`() = runTest {
        insertGymSeries()
        val viewModel = viewModel()

        viewModel.state.test {
            val before = awaitState { it.groups.isNotEmpty() }

            viewModel.onAction(TransactionListAction.OnStopRepeatingClick)

            expectNoEvents()
            assertThat(viewModel.state.value).isEqualTo(before)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirming stops the whole series today and closes the dialog`() = runTest {
        val (seriesId, occurrenceId) = insertGymSeries()
        val viewModel = viewModel()

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }
            viewModel.onAction(TransactionListAction.OnRowLongPress(occurrenceId))
            viewModel.onAction(TransactionListAction.OnStopRepeatingClick)
            awaitState { it.stopConfirmationFor != null }

            viewModel.onAction(TransactionListAction.OnStopConfirm)

            val state = awaitState { it.stopConfirmationFor == null && !it.row(seriesId).isInRunningSeries }
            assertThat(state.row(occurrenceId).isInRunningSeries).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(dataSource.storedSeries.single().stoppedOn).isEqualTo(today)
        // Stopping changes no transaction (FR-039).
        assertThat(dataSource.storedTransactions.map { it.counterparty }).containsExactly("Gym", "Gym")
    }

    @Test
    fun `confirming uses the current date after a date change`() = runTest {
        val (_, occurrenceId) = insertGymSeries()
        val viewModel = viewModel()
        dateProvider.current.value = today.plusDays(1)

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }
            viewModel.onAction(TransactionListAction.OnRowLongPress(occurrenceId))
            viewModel.onAction(TransactionListAction.OnStopRepeatingClick)
            viewModel.onAction(TransactionListAction.OnStopConfirm)
            awaitState { it.stopConfirmationFor == null && it.contextMenuFor == null }
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(dataSource.storedSeries.single().stoppedOn).isEqualTo(today.plusDays(1))
    }

    @Test
    fun `cancelling closes the dialog and the series keeps running`() = runTest {
        val (seriesId, _) = insertGymSeries()
        val viewModel = viewModel()

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }
            viewModel.onAction(TransactionListAction.OnRowLongPress(seriesId))
            viewModel.onAction(TransactionListAction.OnStopRepeatingClick)
            awaitState { it.stopConfirmationFor != null }

            viewModel.onAction(TransactionListAction.OnStopCancel)

            val state = awaitState { it.stopConfirmationFor == null }
            assertThat(state.row(seriesId).isInRunningSeries).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(dataSource.storedSeries.single().stoppedOn).isNull()
    }

    @Test
    fun `Stop confirm without a dialog does nothing`() = runTest {
        insertGymSeries()
        val viewModel = viewModel()

        viewModel.state.test {
            awaitState { it.groups.isNotEmpty() }
            viewModel.onAction(TransactionListAction.OnStopConfirm)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(dataSource.storedSeries.single().stoppedOn).isNull()
    }

    // endregion

    /** One preference combination and the rows it must produce: Rent -1,234.50, Salary +12,000.00, Gym -45.99. */
    data class FormatCase(
        val label: String,
        val prefs: AmountFormatPreferences,
        val rent: String,
        val salary: String,
        val gym: String,
    ) {
        override fun toString() = label
    }

    companion object {
        private val CommaDecimal = AmountFormatPreferences.Default.copy(decimalSeparator = ',', thousandsSeparator = '.')

        @JvmStatic
        fun formatCases() = listOf(
            FormatCase("defaults", AmountFormatPreferences.Default, "-$1,234.50", "$12,000.00", "-$45.99"),
            FormatCase("comma decimal, dot thousands", CommaDecimal, "-$1.234,50", "$12.000,00", "-$45,99"),
            FormatCase(
                "space thousands",
                AmountFormatPreferences.Default.copy(thousandsSeparator = ' '),
                "-$1 234.50",
                "$12 000.00",
                "-$45.99",
            ),
            FormatCase(
                "parentheses",
                AmountFormatPreferences.Default.copy(negativeStyle = NegativeStyle.PARENTHESES),
                "($1,234.50)",
                "$12,000.00",
                "($45.99)",
            ),
            FormatCase(
                "parentheses, comma decimal, euro",
                CommaDecimal.copy(currencySymbol = "€", negativeStyle = NegativeStyle.PARENTHESES),
                "(€1.234,50)",
                "€12.000,00",
                "(€45,99)",
            ),
        )
    }
}
