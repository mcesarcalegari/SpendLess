package com.plcoding.spendlesssample.transactions.presentation.list

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isTrue
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.domain.fake.FakeAmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.fake.FakeCurrentDateProvider
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
import com.plcoding.spendlesssample.core.presentation.format.AmountInputParser
import com.plcoding.spendlesssample.transactions.domain.TransactionValidator
import com.plcoding.spendlesssample.transactions.domain.usecase.CreateTransaction
import com.plcoding.spendlesssample.transactions.presentation.create.CreateTransactionViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** All Transactions screen with its real ViewModel; storage, date and preferences are faked. */
@RunWith(AndroidJUnit4::class)
class TransactionListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 1, 15)
    private lateinit var dataSource: FakeTransactionLocalDataSource
    private lateinit var viewModel: TransactionListViewModel
    private lateinit var createViewModel: CreateTransactionViewModel
    private var backCalls = 0

    @Before
    fun setUp() {
        dataSource = FakeTransactionLocalDataSource()
        viewModel = TransactionListViewModel(
            observeTransactions = ObserveTransactions(dataSource),
            currentDateProvider = FakeCurrentDateProvider(today),
            preferencesSource = FakeAmountFormatPreferencesSource(),
            mapper = TransactionUiMapper(AmountFormatter(), DayHeaderFormatter()),
            savedStateHandle = SavedStateHandle(),
        )
        // The list Root hosts the create sheet; give it a real ViewModel so no Koin is needed.
        createViewModel = CreateTransactionViewModel(
            validator = TransactionValidator(Clock.systemUTC()),
            createTransaction = CreateTransaction(dataSource),
            preferencesSource = FakeAmountFormatPreferencesSource(),
            amountInputParser = AmountInputParser(),
            amountFormatter = AmountFormatter(),
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun string(@StringRes id: Int) = composeTestRule.activity.getString(id)

    private fun seed(
        name: String,
        at: LocalDateTime,
        amountMinor: Long,
        type: TransactionType = TransactionType.EXPENSE,
    ) = runBlocking {
        dataSource.insert(
            Transaction(
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
            ),
        )
    }

    private fun setContent() {
        composeTestRule.setContent {
            SpendLessTheme {
                TransactionListRoot(
                    onNavigateBack = { backCalls++ },
                    viewModel = viewModel,
                    createViewModel = createViewModel,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun top(text: String) =
        composeTestRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top

    private fun seedThreeDays() {
        seed("Amazon", today.atTime(10, 0), 4_599)
        seed("Rick", today.minusDays(1).atTime(10, 0), 2_000, TransactionType.INCOME)
        seed("Home Depot", LocalDate.of(2026, 1, 9).atTime(10, 0), 3_560)
    }

    @Test
    fun headersAppearNewestDayFirst() {
        seedThreeDays()
        setContent()

        val todayHeader = string(R.string.today)
        val yesterdayHeader = string(R.string.yesterday)
        listOf(todayHeader, yesterdayHeader, "JANUARY 9").forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
        assertThat(top(todayHeader)).isLessThan(top(yesterdayHeader))
        assertThat(top(yesterdayHeader)).isLessThan(top("JANUARY 9"))
    }

    @Test
    fun expenseAndIncomeRowsShowTheirAmountsAndLabels() {
        seedThreeDays()
        setContent()

        composeTestRule.onNodeWithText("Amazon").assertIsDisplayed()
        composeTestRule.onNodeWithText("-$45.99").assertIsDisplayed()
        // Both seeded expenses (Amazon, Home Depot) are in Home.
        composeTestRule.onAllNodesWithText(string(R.string.category_home)).assertCountEquals(2)
        composeTestRule.onNodeWithText("Rick").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.income)).assertIsDisplayed()
        composeTestRule.onNodeWithText("$20.00").assertIsDisplayed()
    }

    @Test
    fun topGroupHeaderStaysPinnedAfterScrolling() {
        repeat(30) { index -> seed("Today $index", today.atTime(23, 0).minusMinutes(index.toLong()), 100) }
        setContent()
        val todayHeader = string(R.string.today)
        val headerTopBefore = top(todayHeader)

        // Index 0 is the header; scroll so a later row is first.
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(15)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(todayHeader).assertIsDisplayed()
        assertThat(top(todayHeader)).isEqualTo(headerTopBefore)
        composeTestRule.onNodeWithText("Today 0").assertDoesNotExist()
    }

    @Test
    fun newTransactionAppearsWithoutRefresh() {
        setContent()
        composeTestRule.onAllNodesWithText("Netflix").assertCountEquals(0)

        seed("Netflix", today.atTime(9, 0), 1_599)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Netflix").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Netflix").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.today)).assertIsDisplayed()
    }

    @Test
    fun backInvokesOnNavigateBack() {
        setContent()

        composeTestRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backCalls).isEqualTo(1)
    }

    @Test
    fun addButtonOpensTheCreateSheet() {
        setContent()

        composeTestRule.onNodeWithContentDescription(string(R.string.cd_add_transaction)).performClick()
        composeTestRule.waitForIdle()

        assertThat(viewModel.state.value.isCreateSheetOpen).isTrue()
    }

    @Test
    fun onlyBackAndAddAreActionableSoNoExportActionIsShown() {
        seedThreeDays()
        setContent()

        // FR-009, F8: the export/download action drawn in Figma must not be shown.
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(2)
        composeTestRule.onNodeWithContentDescription(string(R.string.cd_back)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.cd_add_transaction)).assertIsDisplayed()
    }
}
