package com.plcoding.spendlesssample.transactions.presentation.create

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.plcoding.spendlesssample.core.domain.fake.FakeAmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.util.DataError
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.core.presentation.R as CoreR
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.core.presentation.format.AmountFormatter
import com.plcoding.spendlesssample.core.presentation.format.AmountInputParser
import com.plcoding.spendlesssample.transactions.domain.TransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.TransactionValidator
import com.plcoding.spendlesssample.transactions.domain.fake.FakeTransactionLocalDataSource
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.domain.usecase.CreateTransaction
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
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
class CreateTransactionViewModelTest {

    private val now = Instant.parse("2026-01-15T10:30:00Z")
    private val dataSource = FakeTransactionLocalDataSource()
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

    private fun viewModel(
        handle: SavedStateHandle = savedStateHandle,
        createTransaction: CreateTransaction = CreateTransaction(dataSource),
    ) = CreateTransactionViewModel(
        validator = TransactionValidator(Clock.fixed(now, ZoneOffset.UTC)),
        createTransaction = createTransaction,
        preferencesSource = preferences,
        amountInputParser = AmountInputParser(),
        amountFormatter = AmountFormatter(),
        savedStateHandle = handle,
    )

    private fun CreateTransactionViewModel.fill(name: String = "Netflix", amount: String = "15.99") {
        onAction(CreateTransactionAction.OnNameChange(name))
        onAction(CreateTransactionAction.OnAmountChange(amount))
    }

    /** Types [text] one character at a time, as a keyboard would. */
    private fun CreateTransactionViewModel.typeAmount(text: String) {
        text.forEach { onAction(CreateTransactionAction.OnAmountChange(state.value.amountInput + it)) }
    }

    // region Defaults

    @Test
    fun `defaults are Expense, empty, Other, Does not repeat, Create disabled`() {
        val state = viewModel().state.value

        assertThat(state.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(state.name).isEqualTo("")
        assertThat(state.amountInput).isEqualTo("")
        assertThat(state.note).isEqualTo("")
        assertThat(state.isNoteEditing).isFalse()
        assertThat(state.category).isEqualTo(ExpenseCategory.OTHER)
        assertThat(state.repeat).isEqualTo(RepeatFrequency.DOES_NOT_REPEAT)
        assertThat(state.openSelect).isNull()
        assertThat(state.isCreateEnabled).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `empty amount shows the -$ 00_00 placeholder in the expense tone`() {
        assertThat(viewModel().state.value.amountParts).isEqualTo(
            AmountPartsUi(
                prefix = "-$",
                number = "00.00",
                suffix = "",
                isPlaceholder = true,
                signTone = TransactionType.EXPENSE,
            ),
        )
    }

    // endregion

    // region Create enabled (FR-015)

    @Test
    fun `name alone does not enable Create`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnNameChange("Netflix"))
        assertThat(vm.state.value.isCreateEnabled).isFalse()
    }

    @Test
    fun `amount alone does not enable Create`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnAmountChange("15.99"))
        assertThat(vm.state.value.isCreateEnabled).isFalse()
    }

    @Test
    fun `name and amount above zero enable Create`() {
        val vm = viewModel()
        vm.fill()
        assertThat(vm.state.value.isCreateEnabled).isTrue()
    }

    @Test
    fun `whitespace-only name keeps Create disabled`() {
        val vm = viewModel()
        vm.fill(name = "   ")
        assertThat(vm.state.value.isCreateEnabled).isFalse()
    }

    @Test
    fun `zero amounts keep Create disabled`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnNameChange("Netflix"))
        listOf("0", "0.", "0.00", ".", "000").forEach { amount ->
            vm.onAction(CreateTransactionAction.OnAmountChange(amount))
            assertThat(vm.state.value.isCreateEnabled, name = amount).isFalse()
        }
    }

    @Test
    fun `smallest amount 0_01 enables Create`() {
        val vm = viewModel()
        vm.fill(amount = "0.01")
        assertThat(vm.state.value.isCreateEnabled).isTrue()
    }

    @Test
    fun `clearing the name disables Create again`() {
        val vm = viewModel()
        vm.fill()
        vm.onAction(CreateTransactionAction.OnNameChange(""))
        assertThat(vm.state.value.isCreateEnabled).isFalse()
    }

    // endregion

    // region Input caps (FR-012, FR-016)

    @Test
    fun `name accepts 50 characters and ignores the 51st`() {
        val vm = viewModel()
        val fifty = "a".repeat(50)
        vm.onAction(CreateTransactionAction.OnNameChange(fifty))
        vm.onAction(CreateTransactionAction.OnNameChange(fifty + "b"))
        assertThat(vm.state.value.name).isEqualTo(fifty)
    }

    @Test
    fun `pasting a name longer than 50 characters is rejected`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnNameChange("Net"))
        vm.onAction(CreateTransactionAction.OnNameChange("x".repeat(60)))
        assertThat(vm.state.value.name).isEqualTo("Net")
    }

    @Test
    fun `note accepts 500 characters and ignores the 501st`() {
        val vm = viewModel()
        val max = "n".repeat(500)
        vm.onAction(CreateTransactionAction.OnNoteChange(max))
        vm.onAction(CreateTransactionAction.OnNoteChange(max + "x"))
        assertThat(vm.state.value.note).isEqualTo(max)
    }

    // endregion

    // region Amount filtering (FR-013, FR-014)

    @Test
    fun `amount keystrokes the parser rejects are ignored`() {
        val vm = viewModel()
        vm.typeAmount("12a-,3.456")
        assertThat(vm.state.value.amountInput).isEqualTo("123.45")
    }

    @Test
    fun `a tenth whole digit is rejected`() {
        val vm = viewModel()
        vm.typeAmount("1234567890")
        assertThat(vm.state.value.amountInput).isEqualTo("123456789")
    }

    @Test
    fun `a second decimal separator is rejected`() {
        val vm = viewModel()
        vm.typeAmount("1.2.")
        assertThat(vm.state.value.amountInput).isEqualTo("1.2")
    }

    @Test
    fun `typed amount is grouped and no longer a placeholder`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnAmountChange("999999999.99"))
        assertThat(vm.state.value.amountParts).isEqualTo(
            AmountPartsUi(
                prefix = "-$",
                number = "999,999,999.99",
                suffix = "",
                isPlaceholder = false,
                signTone = TransactionType.EXPENSE,
            ),
        )
    }

    @Test
    fun `deleting the whole amount brings the placeholder back`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnAmountChange("5"))
        vm.onAction(CreateTransactionAction.OnAmountChange(""))
        assertThat(vm.state.value.amountInput).isEqualTo("")
        assertThat(vm.state.value.amountParts.isPlaceholder).isTrue()
        assertThat(vm.state.value.amountParts.number).isEqualTo("00.00")
    }

    // endregion

    // region Category select (FR-017)

    @Test
    fun `category select click opens the category list`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnCategorySelectClick)
        assertThat(vm.state.value.openSelect).isEqualTo(OpenSelect.CATEGORY)
    }

    @Test
    fun `picking a category sets it and closes the list`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnCategorySelectClick)
        vm.onAction(CreateTransactionAction.OnCategoryPick(ExpenseCategory.ENTERTAINMENT))
        assertThat(vm.state.value.category).isEqualTo(ExpenseCategory.ENTERTAINMENT)
        assertThat(vm.state.value.openSelect).isNull()
    }

    @Test
    fun `dismissing the select closes it without changing the category`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnCategorySelectClick)
        vm.onAction(CreateTransactionAction.OnSelectDismiss)
        assertThat(vm.state.value.openSelect).isNull()
        assertThat(vm.state.value.category).isEqualTo(ExpenseCategory.OTHER)
    }

    // endregion

    // region Note (research R8)

    @Test
    fun `Add Note starts note editing`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnAddNoteClick)
        assertThat(vm.state.value.isNoteEditing).isTrue()
    }

    @Test
    fun `losing focus with an empty note returns to the idle row`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnAddNoteClick)
        vm.onAction(CreateTransactionAction.OnNoteFocusLost)
        assertThat(vm.state.value.isNoteEditing).isFalse()
    }

    @Test
    fun `losing focus with a whitespace-only note returns to the idle row and clears it`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnAddNoteClick)
        vm.onAction(CreateTransactionAction.OnNoteChange("   "))
        vm.onAction(CreateTransactionAction.OnNoteFocusLost)
        assertThat(vm.state.value.isNoteEditing).isFalse()
        assertThat(vm.state.value.note).isEqualTo("")
    }

    @Test
    fun `losing focus with a note keeps editing`() {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnAddNoteClick)
        vm.onAction(CreateTransactionAction.OnNoteChange("monthly plan"))
        vm.onAction(CreateTransactionAction.OnNoteFocusLost)
        assertThat(vm.state.value.isNoteEditing).isTrue()
        assertThat(vm.state.value.note).isEqualTo("monthly plan")
    }

    // endregion

    // region Create (FR-022, FR-022a)

    @Test
    fun `Create with valid input persists one transaction, emits Created and resets the form`() = runTest {
        val vm = viewModel()
        vm.fill(name = "  Netflix ", amount = "15.99")
        vm.onAction(CreateTransactionAction.OnCategoryPick(ExpenseCategory.ENTERTAINMENT))
        vm.onAction(CreateTransactionAction.OnAddNoteClick)
        vm.onAction(CreateTransactionAction.OnNoteChange("monthly plan"))

        vm.events.test {
            vm.onAction(CreateTransactionAction.OnCreateClick)
            assertThat(awaitItem()).isEqualTo(CreateTransactionEvent.Created)
        }

        assertThat(dataSource.storedTransactions).containsExactly(
            Transaction(
                id = 1,
                type = TransactionType.EXPENSE,
                counterparty = "Netflix",
                amountMinor = 1_599,
                note = "monthly plan",
                category = ExpenseCategory.ENTERTAINMENT,
                repeat = RepeatFrequency.DOES_NOT_REPEAT,
                recordedAt = now,
                zoneId = ZoneOffset.UTC,
                seriesId = null,
                occurrenceDate = null,
            ),
        )
        assertThat(vm.state.value).isEqualTo(viewModel(SavedStateHandle()).state.value)
    }

    @Test
    fun `Create with an empty note stores a null note`() = runTest {
        val vm = viewModel()
        vm.fill()
        vm.onAction(CreateTransactionAction.OnCreateClick)
        assertThat(dataSource.storedTransactions.single().note).isNull()
    }

    @Test
    fun `Create stores the default Other category when none was picked`() = runTest {
        val vm = viewModel()
        vm.fill()
        vm.onAction(CreateTransactionAction.OnCreateClick)
        assertThat(dataSource.storedTransactions.single().category).isEqualTo(ExpenseCategory.OTHER)
    }

    @Test
    fun `Create while disabled saves nothing and emits nothing`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateTransactionAction.OnNameChange("Netflix"))

        vm.events.test {
            vm.onAction(CreateTransactionAction.OnCreateClick)
            expectNoEvents()
        }
        assertThat(dataSource.storedTransactions).isEmpty()
    }

    @Test
    fun `tapping Create twice saves once`() = runTest {
        val vm = viewModel()
        vm.fill()
        vm.onAction(CreateTransactionAction.OnCreateClick)
        vm.onAction(CreateTransactionAction.OnCreateClick)
        assertThat(dataSource.storedTransactions).hasSize(1)
    }

    @Test
    fun `a second tap while the insert is still running does not save twice`() = runTest {
        val gated = GatedDataSource(dataSource)
        val vm = viewModel(createTransaction = CreateTransaction(gated))
        vm.fill()

        vm.onAction(CreateTransactionAction.OnCreateClick)
        vm.onAction(CreateTransactionAction.OnCreateClick)
        gated.gate.complete(Unit)

        assertThat(dataSource.storedTransactions).hasSize(1)
    }

    @Test
    fun `retrying clears the previous error while the insert runs`() = runTest {
        dataSource.error = DataError.Local.UNKNOWN
        val gated = GatedDataSource(dataSource).apply { gate.complete(Unit) }
        val vm = viewModel(createTransaction = CreateTransaction(gated))
        vm.fill()
        vm.onAction(CreateTransactionAction.OnCreateClick)
        assertThat(vm.state.value.error).isEqualTo(UiText.StringResource(CoreR.string.error_save_failed))

        gated.gate = CompletableDeferred()
        vm.onAction(CreateTransactionAction.OnCreateClick)

        assertThat(vm.state.value.error).isNull()
        gated.gate.complete(Unit)
    }

    @Test
    fun `disk full keeps the input, shows the storage message and emits nothing`() = runTest {
        dataSource.error = DataError.Local.DISK_FULL
        val vm = viewModel()
        vm.fill()
        vm.onAction(CreateTransactionAction.OnCategoryPick(ExpenseCategory.HOME))

        vm.events.test {
            vm.onAction(CreateTransactionAction.OnCreateClick)
            expectNoEvents()
        }

        val state = vm.state.value
        assertThat(state.error).isEqualTo(UiText.StringResource(CoreR.string.error_disk_full))
        assertThat(state.name).isEqualTo("Netflix")
        assertThat(state.amountInput).isEqualTo("15.99")
        assertThat(state.category).isEqualTo(ExpenseCategory.HOME)
        assertThat(state.isCreateEnabled).isTrue()
        assertThat(dataSource.storedTransactions).isEmpty()
    }

    @Test
    fun `other storage failure shows the generic message`() = runTest {
        dataSource.error = DataError.Local.UNKNOWN
        val vm = viewModel()
        vm.fill()
        vm.onAction(CreateTransactionAction.OnCreateClick)
        assertThat(vm.state.value.error).isEqualTo(UiText.StringResource(CoreR.string.error_save_failed))
    }

    @Test
    fun `retry after a failure succeeds and clears the error`() = runTest {
        dataSource.error = DataError.Local.UNKNOWN
        val vm = viewModel()
        vm.fill()
        vm.onAction(CreateTransactionAction.OnCreateClick)
        dataSource.error = null

        vm.events.test {
            vm.onAction(CreateTransactionAction.OnCreateClick)
            assertThat(awaitItem()).isEqualTo(CreateTransactionEvent.Created)
        }
        assertThat(vm.state.value.error).isNull()
        assertThat(dataSource.storedTransactions).hasSize(1)
    }

    // endregion

    // region Close

    @Test
    fun `Close resets the form without saving`() = runTest {
        val vm = viewModel()
        vm.fill()
        vm.onAction(CreateTransactionAction.OnCategoryPick(ExpenseCategory.HOME))
        vm.onAction(CreateTransactionAction.OnAddNoteClick)
        vm.onAction(CreateTransactionAction.OnNoteChange("note"))
        vm.onAction(CreateTransactionAction.OnCategorySelectClick)

        vm.events.test {
            vm.onAction(CreateTransactionAction.OnCloseClick)
            expectNoEvents()
        }

        assertThat(vm.state.value).isEqualTo(viewModel(SavedStateHandle()).state.value)
        assertThat(dataSource.storedTransactions).isEmpty()
    }

    @Test
    fun `Close clears the error`() = runTest {
        dataSource.error = DataError.Local.UNKNOWN
        val vm = viewModel()
        vm.fill()
        vm.onAction(CreateTransactionAction.OnCreateClick)
        vm.onAction(CreateTransactionAction.OnCloseClick)
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `Close clears the saved form, so a restored ViewModel starts empty`() {
        val vm = viewModel()
        vm.fill()
        vm.onAction(CreateTransactionAction.OnCloseClick)

        val restored = viewModel(savedStateHandle).state.value
        assertThat(restored.name).isEqualTo("")
        assertThat(restored.amountInput).isEqualTo("")
    }

    // endregion

    // region Process death

    @Test
    fun `form fields survive a new ViewModel built from the same SavedStateHandle`() {
        val vm = viewModel()
        vm.fill(name = "Netflix", amount = "1234.5")
        vm.onAction(CreateTransactionAction.OnCategoryPick(ExpenseCategory.EDUCATION))
        vm.onAction(CreateTransactionAction.OnAddNoteClick)
        vm.onAction(CreateTransactionAction.OnNoteChange("course"))

        val restored = viewModel(savedStateHandle).state.value

        assertThat(restored.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(restored.name).isEqualTo("Netflix")
        assertThat(restored.amountInput).isEqualTo("1234.5")
        assertThat(restored.amountParts.number).isEqualTo("1,234.5")
        assertThat(restored.note).isEqualTo("course")
        assertThat(restored.isNoteEditing).isTrue()
        assertThat(restored.category).isEqualTo(ExpenseCategory.EDUCATION)
        assertThat(restored.repeat).isEqualTo(RepeatFrequency.DOES_NOT_REPEAT)
        assertThat(restored.isCreateEnabled).isTrue()
    }

    @Test
    fun `a restored form without a note starts with the idle note row`() {
        val vm = viewModel()
        vm.fill()

        assertThat(viewModel(savedStateHandle).state.value.isNoteEditing).isFalse()
    }

    // endregion

    /** Holds every insert until [gate] completes, so a test can act while the insert is in flight. */
    private class GatedDataSource(
        private val delegate: FakeTransactionLocalDataSource,
    ) : TransactionLocalDataSource by delegate {
        var gate = CompletableDeferred<Unit>()

        override suspend fun insert(transaction: Transaction): Result<Long, DataError.Local> {
            gate.await()
            return delegate.insert(transaction)
        }
    }
}
