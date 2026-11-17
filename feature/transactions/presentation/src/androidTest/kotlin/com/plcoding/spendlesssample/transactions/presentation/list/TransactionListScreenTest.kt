package com.plcoding.spendlesssample.transactions.presentation.list

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import assertk.assertions.isNull
import com.plcoding.spendlesssample.transactions.presentation.create.CreateTransactionRobot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveRunningSeriesIds
import com.plcoding.spendlesssample.transactions.domain.usecase.ObserveTransactions
import com.plcoding.spendlesssample.transactions.domain.usecase.StopRecurringSeries
import com.plcoding.spendlesssample.transactions.presentation.R
import com.plcoding.spendlesssample.transactions.presentation.format.DayHeaderFormatter
import com.plcoding.spendlesssample.transactions.presentation.format.RepeatLabelFormatter
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
            observeRunningSeriesIds = ObserveRunningSeriesIds(dataSource),
            stopRecurringSeries = StopRecurringSeries(dataSource),
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
            currentDateProvider = FakeCurrentDateProvider(today),
            repeatLabelFormatter = RepeatLabelFormatter(),
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
        note: String? = null,
    ) = runBlocking {
        dataSource.insert(
            Transaction(
                id = 0,
                type = type,
                counterparty = name,
                amountMinor = amountMinor,
                note = note,
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

    // region Notes (US3)

    private val lampNote = "Two lamps for the hallway and one for the bedroom, plus spare bulbs."

    @Test
    fun showNoteRevealsTheFullNoteAndHideNoteHidesItAgain() {
        seed("Amazon", today.atTime(10, 0), 4_599, note = lampNote)
        setContent()
        composeTestRule.onNodeWithText(lampNote).assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription(string(R.string.cd_show_note)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(lampNote).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.cd_hide_note)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.cd_show_note)).assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription(string(R.string.cd_hide_note)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(lampNote).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(string(R.string.cd_show_note)).assertIsDisplayed()
    }

    @Test
    fun onlyRowsWithANoteHaveABadge() {
        seed("Amazon", today.atTime(10, 0), 4_599, note = lampNote)
        seed("Starbucks", today.atTime(9, 0), 750)
        seed("Rick", today.atTime(8, 0), 2_000, TransactionType.INCOME)
        setContent()

        composeTestRule.onAllNodesWithContentDescription(string(R.string.cd_show_note)).assertCountEquals(1)
        // The one badge belongs to the noted row: it sits in Amazon's row, above Starbucks.
        val badgeTop = composeTestRule.onNodeWithContentDescription(string(R.string.cd_show_note))
            .fetchSemanticsNode().boundsInRoot.top
        assertThat(badgeTop).isLessThan(top("Starbucks"))
    }

    @Test
    fun tappingTheRowOutsideTheBadgeDoesNotExpandIt() {
        seed("Amazon", today.atTime(10, 0), 4_599, note = lampNote)
        setContent()

        composeTestRule.onNodeWithText("Amazon").performClick()
        composeTestRule.onNodeWithText("-$45.99").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(lampNote).assertDoesNotExist()
        assertThat(viewModel.state.value.expandedIds.isEmpty()).isTrue()
    }

    @Test
    fun rowsExpandIndependently() {
        seed("Amazon", today.atTime(10, 0), 4_599, note = lampNote)
        seed("Rick", today.atTime(9, 0), 2_000, TransactionType.INCOME, note = "Birthday share")
        setContent()

        composeTestRule.onAllNodesWithContentDescription(string(R.string.cd_show_note))[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(string(R.string.cd_show_note)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(lampNote).assertIsDisplayed()
        composeTestRule.onNodeWithText("Birthday share").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription(string(R.string.cd_hide_note)).assertCountEquals(2)
    }

    @Test
    fun expandingKeepsTheNameInPlace() {
        seed("Amazon", today.atTime(10, 0), 4_599, note = lampNote)
        setContent()
        val nameTopBefore = top("Amazon")

        composeTestRule.onNodeWithContentDescription(string(R.string.cd_show_note)).performClick()
        composeTestRule.waitForIdle()

        // The card is drawn outside the row, so the name does not move (US3-1).
        assertThat(top("Amazon")).isEqualTo(nameTopBefore)
    }

    @Test
    fun anExpandedNoteStaysExpandedAfterScrollingAwayAndBack() {
        seed("Amazon", today.atTime(23, 30), 4_599, note = lampNote)
        repeat(40) { index -> seed("Older $index", today.minusDays(1).atTime(23, 0).minusMinutes(index.toLong()), 100) }
        setContent()

        composeTestRule.onNodeWithContentDescription(string(R.string.cd_show_note)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(lampNote).assertIsDisplayed()

        // Header + 1 row today, header + 40 rows yesterday: index 40 is far off screen.
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(40)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(lampNote).assertDoesNotExist()

        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(lampNote).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.cd_hide_note)).assertIsDisplayed()
    }

    // endregion

    // region Stop repeating (User Story 6, FR-037 – FR-040)

    /** A running daily "Gym" series created yesterday, with today's occurrence. Returns the series id. */
    private fun seedGymSeries(): Long = runBlocking {
        val original = Transaction(
            id = 0,
            type = TransactionType.EXPENSE,
            counterparty = "Gym",
            amountMinor = 4_599,
            note = null,
            category = ExpenseCategory.HEALTH_AND_WELLNESS,
            repeat = RepeatFrequency.DAILY,
            recordedAt = today.minusDays(1).atTime(9, 0).toInstant(ZoneOffset.UTC),
            zoneId = ZoneOffset.UTC,
            seriesId = null,
            occurrenceDate = null,
        )
        val seriesId = (dataSource.insertRecurring(original, RepeatFrequency.DAILY)
            as com.plcoding.spendlesssample.core.domain.util.Result.Success).data
        val occurrence = original.copy(
            repeat = RepeatFrequency.DOES_NOT_REPEAT,
            recordedAt = today.atTime(9, 0).toInstant(ZoneOffset.UTC),
            seriesId = seriesId,
            occurrenceDate = today,
        )
        dataSource.materialize(seriesId, listOf(occurrence), lastMaterialized = today)
        seriesId
    }

    /** Index 0 is today's occurrence (newest first), index 1 the original under YESTERDAY. */
    private fun longPressGym(index: Int = 0) {
        composeTestRule.onAllNodesWithText("Gym")[index].performTouchInput { longClick() }
        composeTestRule.waitForIdle()
    }

    private fun stopRepeatingTitle(name: String) =
        composeTestRule.activity.getString(R.string.stop_repeating_title, name)

    /** A real touch at the centre of [text]'s node, delivered by the system like a finger tap. */
    private fun tapOnScreenAt(text: String) {
        val bounds = composeTestRule.onNodeWithText(text).fetchSemanticsNode().boundsInWindow
        val window = IntArray(2)
        composeTestRule.activity.window.decorView.getLocationOnScreen(window)
        val x = (window[0] + bounds.center.x).toInt()
        val y = (window[1] + bounds.center.y).toInt()
        CreateTransactionRobot.shell("input tap $x $y")
        composeTestRule.waitForIdle()
    }

    @Test
    fun longPressOnARunningSeriesRowShowsStopRepeating() {
        seedGymSeries()
        setContent()

        longPressGym()

        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).assertIsDisplayed()
    }

    @Test
    fun longPressOnTheOriginalAlsoShowsTheMenu() {
        seedGymSeries()
        setContent()

        longPressGym(index = 1)

        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).assertIsDisplayed()
    }

    @Test
    fun longPressOnAOneTimeRowShowsNoMenu() {
        seed("Netflix", today.atTime(10, 0), 1_599)
        setContent()

        composeTestRule.onNodeWithText("Netflix").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).assertDoesNotExist()
    }

    @Test
    fun tappingOutsideTheMenuClosesItAndChangesNothing() {
        val seriesId = seedGymSeries()
        setContent()
        longPressGym()

        tapOnScreenAt(string(R.string.all_transactions))

        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).assertDoesNotExist()
        composeTestRule.onNodeWithText(stopRepeatingTitle("Gym")).assertDoesNotExist()
        assertThat(dataSource.storedSeries.single { it.seriesId == seriesId }.stoppedOn).isNull()
    }

    @Test
    fun choosingStopRepeatingClosesTheMenuAndAsksToConfirm() {
        seedGymSeries()
        setContent()
        longPressGym()

        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).assertDoesNotExist()
        composeTestRule.onNodeWithText(stopRepeatingTitle("Gym")).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.stop)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.cancel)).assertIsDisplayed()
    }

    @Test
    fun cancelKeepsTheSeriesRunning() {
        val seriesId = seedGymSeries()
        setContent()
        longPressGym()
        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).performClick()

        composeTestRule.onNodeWithText(string(R.string.cancel)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stopRepeatingTitle("Gym")).assertDoesNotExist()
        assertThat(dataSource.storedSeries.single { it.seriesId == seriesId }.stoppedOn).isNull()
        longPressGym()
        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).assertIsDisplayed()
    }

    @Test
    fun dismissingTheDialogWithBackKeepsTheSeriesRunning() {
        val seriesId = seedGymSeries()
        setContent()
        longPressGym()
        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).performClick()
        composeTestRule.waitForIdle()

        CreateTransactionRobot.shell("input keyevent KEYCODE_BACK")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stopRepeatingTitle("Gym")).assertDoesNotExist()
        assertThat(dataSource.storedSeries.single { it.seriesId == seriesId }.stoppedOn).isNull()
        assertThat(backCalls).isEqualTo(0)
    }

    @Test
    fun stopStopsTheSeriesAndKeepsItsRows() {
        val seriesId = seedGymSeries()
        setContent()
        longPressGym()
        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).performClick()

        composeTestRule.onNodeWithText(string(R.string.stop)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stopRepeatingTitle("Gym")).assertDoesNotExist()
        assertThat(dataSource.storedSeries.single { it.seriesId == seriesId }.stoppedOn).isEqualTo(today)
        composeTestRule.onAllNodesWithText("Gym").assertCountEquals(2)
        // A stopped series is no longer long-pressable (FR-037).
        longPressGym()
        composeTestRule.onNodeWithText(string(R.string.stop_repeating)).assertDoesNotExist()
    }

    @Test
    fun onlyRunningSeriesRowsOfferTheStopRepeatingAccessibilityActionAndItOpensTheMenu() {
        seedGymSeries()
        seed("Netflix", today.atTime(10, 0), 1_599)
        setContent()
        val label = string(R.string.stop_repeating)
        val hasStopAction = SemanticsMatcher("offers the $label custom action") { node ->
            node.config.getOrElseNullable(SemanticsActions.CustomActions) { null }
                ?.any { it.label == label } == true
        }

        // The two Gym rows; the one-time Netflix row has no such action.
        val rows = composeTestRule.onAllNodes(hasStopAction, useUnmergedTree = true)
        rows.assertCountEquals(2)
        val action = rows[0].fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == label }
        composeTestRule.runOnUiThread { action.action() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(label).assertIsDisplayed()
    }

    // endregion
}
