package com.plcoding.spendlesssample.transactions.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.spendlesssample.core.designsystem.theme.SpendLessTheme
import com.plcoding.spendlesssample.core.domain.fake.FakeAmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.fake.FakeCurrentDateProvider
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.format.NegativeStyle
import com.plcoding.spendlesssample.core.domain.time.CurrentDateProvider
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
import com.plcoding.spendlesssample.transactions.presentation.create.CreateTransactionRobot
import com.plcoding.spendlesssample.transactions.presentation.di.transactionsPresentationModule
import com.plcoding.spendlesssample.transactions.presentation.list.TransactionListRoot
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * SC-010 / FR-035: swapping the single `AmountFormatPreferencesSource` binding changes every amount
 * in the feature, with no other change. Koin loads the production `transactionsPresentationModule`
 * unchanged; the test module only replaces what sits at the device boundary (storage, today's date)
 * plus the preferences binding under test. Both ViewModels come from `koinViewModel()`.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AmountFormatPreferencesSwapTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 1, 15)
    private val clock = Clock.fixed(today.atTime(10, 30).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val dataSource = FakeTransactionLocalDataSource()

    @After
    fun tearDown() {
        stopKoin()
    }

    /** Seeds Rent (expense 1,234.50) and Salary (income 12,000.00) and shows the list through Koin. */
    private fun launchWith(prefs: AmountFormatPreferences): CreateTransactionRobot {
        runBlocking {
            dataSource.insert(transaction("Rent", TransactionType.EXPENSE, 123_450, hour = 9))
            dataSource.insert(transaction("Salary", TransactionType.INCOME, 1_200_000, hour = 10))
        }
        startKoin {
            modules(
                transactionsPresentationModule,
                module {
                    single<AmountFormatPreferencesSource> { FakeAmountFormatPreferencesSource(prefs) }
                    single<CurrentDateProvider> { FakeCurrentDateProvider(today) }
                    single { TransactionValidator(clock) }
                    single { CreateTransaction(dataSource) }
                    single { ObserveTransactions(dataSource) }
                    single { ObserveRunningSeriesIds(dataSource) }
                    single { StopRecurringSeries(dataSource) }
                },
            )
        }
        composeTestRule.setContent {
            SpendLessTheme {
                TransactionListRoot(onNavigateBack = {})
            }
        }
        composeTestRule.waitUntilExactlyOneExists(hasText("Rent"))
        return CreateTransactionRobot(composeTestRule)
    }

    private fun transaction(name: String, type: TransactionType, amountMinor: Long, hour: Int) = Transaction(
        id = 0,
        type = type,
        counterparty = name,
        amountMinor = amountMinor,
        note = null,
        category = if (type == TransactionType.EXPENSE) ExpenseCategory.HOME else null,
        repeat = RepeatFrequency.DOES_NOT_REPEAT,
        recordedAt = today.atTime(hour, 0).toInstant(ZoneOffset.UTC),
        zoneId = ZoneOffset.UTC,
        seriesId = null,
        occurrenceDate = null,
    )

    private fun assertShown(vararg texts: String) {
        texts.forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun defaultsFormatRowsAndSheetAsMinusDollarDotDecimal() {
        launchWith(AmountFormatPreferences.Default).openSheet()

        assertShown("-$1,234.50", "$12,000.00", "-$", "00.00")
        composeTestRule.onNodeWithText(")").assertDoesNotExist()
    }

    @Test
    fun parenthesesWithCommaDecimalFormatRowsAndSheet() {
        launchWith(ParenthesesCommaDecimal).openSheet()

        // The sheet draws `($ 00,00 )` as three nodes: sign, placeholder, closing parenthesis.
        assertShown("($1.234,50)", "$12.000,00", "($", "00,00", ")")
        composeTestRule.onNodeWithText("-$").assertDoesNotExist()
    }

    @Test
    fun spaceThousandsGroupsRowsWithASpace() {
        launchWith(AmountFormatPreferences.Default.copy(thousandsSeparator = ' '))

        assertShown("-$1 234.50", "$12 000.00")
    }

    @Test
    fun anAmountTypedWithTheCommaDecimalIsStoredAndShownInThePreferredFormat() {
        launchWith(ParenthesesCommaDecimal)
            .openSheet()
            .enterName("Netflix")
            .enterAmount("15,99")
            .assertCreateEnabled()
            .tapCreate()
            .assertRowAppearsWithin1s("Netflix")
            .assertSheetClosed()

        assertShown("($15,99)")
        assertThat(dataSource.storedTransactions.single { it.counterparty == "Netflix" }.amountMinor)
            .isEqualTo(1_599L)
    }

    private companion object {
        val ParenthesesCommaDecimal = AmountFormatPreferences(
            currencySymbol = "$",
            negativeStyle = NegativeStyle.PARENTHESES,
            decimalSeparator = ',',
            thousandsSeparator = '.',
        )
    }
}
