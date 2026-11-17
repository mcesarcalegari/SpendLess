package com.plcoding.spendlesssample.transactions.presentation.list

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.domain.fake.FakeAmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.fake.FakeCurrentDateProvider
import com.plcoding.spendlesssample.core.presentation.format.AmountFormatter
import com.plcoding.spendlesssample.core.presentation.format.AmountInputParser
import com.plcoding.spendlesssample.transactions.domain.TransactionValidator
import com.plcoding.spendlesssample.transactions.domain.fake.FakeTransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.domain.usecase.CreateTransaction
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveRunningSeriesIds
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveTransactions
import com.plcoding.spendlesssample.transactions.domain.usecase.StopRecurringSeries
import com.plcoding.spendlesssample.transactions.presentation.R
import com.plcoding.spendlesssample.transactions.presentation.create.CreateTransactionViewModel
import com.plcoding.spendlesssample.transactions.presentation.format.DayHeaderFormatter
import com.plcoding.spendlesssample.transactions.presentation.format.RepeatLabelFormatter
import com.plcoding.spendlesssample.transactions.presentation.mapper.TransactionUiMapper
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SC-005 scale check: 1,000 transactions over 12 months render and scroll end to end.
 *
 * The rows are seeded into the fake data source only. There is no seeder in production code
 * (owner decision 2026-11-16). Frame timing is not measured here (owner decision 2026-11-17).
 */
@RunWith(AndroidJUnit4::class)
class TransactionListScrollTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 1, 15)
    private val dataSource = FakeTransactionLocalDataSource()

    /** Newest first: row `i` is `i * 365 / 1000` days before today, so all 12 months are covered. */
    private val seededDates = (0 until ROW_COUNT).map { today.minusDays(it * DAY_SPAN / ROW_COUNT) }

    @Before
    fun setUp() = runBlocking {
        seededDates.forEachIndexed { index, date ->
            dataSource.insert(
                Transaction(
                    id = 0,
                    type = if (index % 10 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                    counterparty = "Row $index",
                    amountMinor = 100L + index,
                    note = if (index % 7 == 0) "Note $index" else null,
                    category = if (index % 10 == 0) null else ExpenseCategory.entries[index % ExpenseCategory.entries.size],
                    repeat = RepeatFrequency.DOES_NOT_REPEAT,
                    // Later index = older; minutes keep rows on the same day in seed order.
                    recordedAt = date.atTime(23, 0).minusMinutes(index.toLong()).toInstant(ZoneOffset.UTC),
                    zoneId = ZoneOffset.UTC,
                    seriesId = null,
                    occurrenceDate = null,
                ),
            )
        }
        Unit
    }

    private fun setContent() {
        val preferences = FakeAmountFormatPreferencesSource()
        val dateProvider = FakeCurrentDateProvider(today)
        val viewModel = TransactionListViewModel(
            observeTransactions = ObserveTransactions(dataSource),
            observeRunningSeriesIds = ObserveRunningSeriesIds(dataSource),
            stopRecurringSeries = StopRecurringSeries(dataSource),
            currentDateProvider = dateProvider,
            preferencesSource = preferences,
            mapper = TransactionUiMapper(AmountFormatter(), DayHeaderFormatter()),
            savedStateHandle = SavedStateHandle(),
        )
        val createViewModel = CreateTransactionViewModel(
            validator = TransactionValidator(Clock.systemUTC()),
            createTransaction = CreateTransaction(dataSource),
            preferencesSource = preferences,
            currentDateProvider = dateProvider,
            repeatLabelFormatter = RepeatLabelFormatter(),
            amountInputParser = AmountInputParser(),
            amountFormatter = AmountFormatter(),
            savedStateHandle = SavedStateHandle(),
        )
        composeTestRule.setContent {
            SpendLessTheme {
                TransactionListRoot(
                    onNavigateBack = {},
                    viewModel = viewModel,
                    createViewModel = createViewModel,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    /** One sticky header per distinct day plus one item per row. */
    private val lastIndex get() = seededDates.distinct().size + ROW_COUNT - 1

    @Test
    fun scrollsToTheOldestRowOfAThousand() {
        setContent()
        composeTestRule.onNodeWithText("Row 0").assertIsDisplayed()

        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(lastIndex)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Row ${ROW_COUNT - 1}").assertIsDisplayed()
        composeTestRule.onNodeWithText(header(seededDates.last())).assertIsDisplayed()
    }

    @Test
    fun scrollingBackToTheTopShowsTodayAgain() {
        setContent()
        val scrollable = composeTestRule.onNode(hasScrollAction())

        scrollable.performScrollToIndex(lastIndex)
        scrollable.performScrollToIndex(lastIndex / 2)
        scrollable.performScrollToIndex(0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.today)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Row 0").assertIsDisplayed()
    }

    private fun header(date: LocalDate) =
        DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH).format(date).uppercase(Locale.ENGLISH)

    private companion object {
        const val ROW_COUNT = 1_000
        const val DAY_SPAN = 365L
    }
}
