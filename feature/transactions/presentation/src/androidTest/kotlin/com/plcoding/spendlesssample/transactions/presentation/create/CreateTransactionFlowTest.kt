package com.plcoding.spendlesssample.transactions.presentation.create

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.single
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.domain.fake.FakeAmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.fake.FakeCurrentDateProvider
import com.plcoding.spendlesssample.core.presentation.format.AmountFormatter
import com.plcoding.spendlesssample.core.presentation.format.AmountInputParser
import com.plcoding.spendlesssample.transactions.domain.TransactionValidator
import com.plcoding.spendlesssample.transactions.domain.fake.FakeTransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.domain.usecase.CreateTransaction
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveRunningSeriesIds
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveTransactions
import com.plcoding.spendlesssample.transactions.domain.usecase.StopRecurringSeries
import com.plcoding.spendlesssample.transactions.presentation.R
import com.plcoding.spendlesssample.transactions.presentation.format.DayHeaderFormatter
import com.plcoding.spendlesssample.transactions.presentation.format.RepeatLabelFormatter
import com.plcoding.spendlesssample.transactions.presentation.list.TransactionListRoot
import com.plcoding.spendlesssample.transactions.presentation.list.TransactionListViewModel
import com.plcoding.spendlesssample.transactions.presentation.mapper.TransactionUiMapper
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The create flow from the list (User Story 1): real list and create ViewModels, real validator
 * and use cases; storage, today's date and preferences are faked at the device boundary.
 */
@RunWith(AndroidJUnit4::class)
class CreateTransactionFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 1, 15)
    private val clock = Clock.fixed(today.atTime(10, 30).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private lateinit var dataSource: FakeTransactionLocalDataSource
    private lateinit var robot: CreateTransactionRobot
    private var previousShowImeWithHardKeyboard: String = "0"

    @Before
    fun setUp() {
        // An emulator with a hardware keyboard hides the soft keyboard unless this is on; FR-021 is
        // about the soft keyboard, so the test makes it visible regardless of the emulator profile.
        previousShowImeWithHardKeyboard =
            CreateTransactionRobot.shell("settings get secure show_ime_with_hard_keyboard").trim()
        CreateTransactionRobot.shell("settings put secure show_ime_with_hard_keyboard 1")

        dataSource = FakeTransactionLocalDataSource()
        val preferences = FakeAmountFormatPreferencesSource()
        val listViewModel = TransactionListViewModel(
            observeTransactions = ObserveTransactions(dataSource),
            observeRunningSeriesIds = ObserveRunningSeriesIds(dataSource),
            stopRecurringSeries = StopRecurringSeries(dataSource),
            currentDateProvider = FakeCurrentDateProvider(today),
            preferencesSource = preferences,
            mapper = TransactionUiMapper(AmountFormatter(), DayHeaderFormatter()),
            savedStateHandle = SavedStateHandle(),
        )
        val createViewModel = CreateTransactionViewModel(
            validator = TransactionValidator(clock),
            createTransaction = CreateTransaction(dataSource),
            preferencesSource = preferences,
            currentDateProvider = FakeCurrentDateProvider(today),
            repeatLabelFormatter = RepeatLabelFormatter(),
            amountInputParser = AmountInputParser(),
            amountFormatter = AmountFormatter(),
            savedStateHandle = SavedStateHandle(),
        )
        composeTestRule.setContent {
            SpendLessTheme {
                TransactionListRoot(
                    onNavigateBack = {},
                    viewModel = listViewModel,
                    createViewModel = createViewModel,
                )
            }
        }
        robot = CreateTransactionRobot(composeTestRule)
    }

    @After
    fun tearDown() {
        val restore = previousShowImeWithHardKeyboard.takeIf { it == "0" || it == "1" } ?: "0"
        CreateTransactionRobot.shell("settings put secure show_ime_with_hard_keyboard $restore")
    }

    @Test
    fun sheetOpensWithDefaultsAmountFocusedAndKeyboardShown() {
        robot.openSheet()
            .assertDefaults()
            .assertAmountFocused()
            .assertKeyboardShown()
    }

    @Test
    fun createStaysDisabledUntilNameAndAmountAreEntered() {
        robot.openSheet()
            .assertCreateDisabled()
            .enterName("Netflix")
            .assertCreateDisabled()
            .enterAmount("15.99")
            .assertCreateEnabled()
    }

    @Test
    fun creatingAnExpenseClosesTheSheetAndShowsTheRowUnderToday() {
        robot.openSheet()
            .enterName("Netflix")
            .enterAmount("15.99")
            .pickCategory(R.string.category_entertainment)
            .tapCreate()
            .assertRowAppearsWithin1s("Netflix")
            .assertSheetClosed()
            .assertRowUnderToday(
                name = "Netflix",
                label = composeTestRule.activity.getString(R.string.category_entertainment),
                amount = "-$15.99",
            )

        val stored = dataSource.storedTransactions.single()
        assertThat(stored.category).isEqualTo(ExpenseCategory.ENTERTAINMENT)
        assertThat(stored.amountMinor).isEqualTo(1_599L)
    }

    @Test
    fun creatingWithANoteStoresTheNote() {
        robot.openSheet()
            .enterName("Netflix")
            .enterAmount("15.99")
            .addNote("Family plan")
            .tapCreate()
            .assertRowAppearsWithin1s("Netflix")
            .assertRowShowsNote("Family plan")

        assertThat(dataSource.storedTransactions).single().transform { it.note }.isEqualTo("Family plan")
    }

    @Test
    fun switchingToIncomeShowsSenderHidesTheCategoryAndKeepsCreateInPlace() {
        robot.openSheet()
        val expenseCreateTop = robot.createButtonTop()

        robot.selectIncome()
            .assertIncomeForm()
        assertThat(robot.createButtonTop()).isEqualTo(expenseCreateTop)
    }

    @Test
    fun switchingBackToExpenseRestoresTheCategoryAndKeepsTheInput() {
        robot.openSheet()
            .enterName("Rick")
            .enterAmount("20")
            .pickCategory(R.string.category_entertainment)
            .selectIncome()
            .assertIncomeForm(isNameEmpty = false)
            .selectExpense()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.category_entertainment)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Rick").assertIsDisplayed()
        composeTestRule.onNodeWithText("-$").assertIsDisplayed()
        robot.assertCreateEnabled()
    }

    @Test
    fun creatingAnIncomeShowsAnIncomeRowUnderToday() {
        robot.openSheet()
            .selectIncome()
            .enterName("Rick")
            .enterAmount("20")
            .tapCreate()
            .assertRowAppearsWithin1s("Rick")
            .assertSheetClosed()
            .assertRowUnderToday(
                name = "Rick",
                label = composeTestRule.activity.getString(R.string.income),
                amount = "$20.00",
            )

        val stored = dataSource.storedTransactions.single()
        assertThat(stored.type).isEqualTo(TransactionType.INCOME)
        assertThat(stored.category).isNull()
        assertThat(stored.amountMinor).isEqualTo(2_000L)
    }

    @Test
    fun closeDiscardsTheInputWithoutCreatingARow() {
        robot.openSheet()
            .enterName("Netflix")
            .enterAmount("15.99")
            .tapClose()
            .assertSheetClosed()

        assertThat(dataSource.storedTransactions).isEmpty()
    }

    @Test
    fun reopeningAfterCloseShowsAnEmptyForm() {
        robot.openSheet()
            .enterName("Netflix")
            .enterAmount("15.99")
            .tapClose()
            .assertSheetClosed()
            .openSheet()
            .assertDefaults()
            .assertCreateDisabled()
    }

    // region Repeat (User Story 5, FR-019, FR-020)

    @Test
    fun theRepeatListOffersTodaysOptionsAndTheFieldShowsTheShortName() {
        val activity = composeTestRule.activity
        val doesNotRepeat = activity.getString(R.string.repeat_does_not_repeat)
        // Today is Thursday Jan 15.
        val options = listOf(
            activity.getString(R.string.repeat_daily),
            activity.getString(R.string.repeat_weekly_on, "Thursday"),
            activity.getString(R.string.repeat_monthly_on_the, "15th"),
            activity.getString(R.string.repeat_yearly_on, "Jan 15th"),
        )

        robot.openSheet().openRepeatList(fieldLabel = doesNotRepeat)
        options.forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }

        composeTestRule.onNodeWithText(options[1]).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(activity.getString(R.string.repeat_weekly)).assertIsDisplayed()
        composeTestRule.onNodeWithText(options[1]).assertDoesNotExist()
    }

    @Test
    fun creatingAMonthlyExpenseStartsASeries() {
        val activity = composeTestRule.activity
        robot.openSheet()
            .enterName("Gym")
            .enterAmount("45.99")
            .pickRepeat(
                fieldLabel = activity.getString(R.string.repeat_does_not_repeat),
                option = activity.getString(R.string.repeat_monthly_on_the, "15th"),
            )
            .tapCreate()
            .assertRowAppearsWithin1s("Gym")
            .assertSheetClosed()

        val stored = dataSource.storedTransactions.single()
        assertThat(stored.repeat).isEqualTo(RepeatFrequency.MONTHLY)
        assertThat(stored.seriesId).isEqualTo(stored.id)
        assertThat(dataSource.storedSeries.single().frequency).isEqualTo(RepeatFrequency.MONTHLY)
    }

    // endregion
}
