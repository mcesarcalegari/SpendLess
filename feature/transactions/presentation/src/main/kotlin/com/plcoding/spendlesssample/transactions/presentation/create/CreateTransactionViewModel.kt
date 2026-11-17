package com.plcoding.spendlesssample.transactions.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.time.CurrentDateProvider
import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.core.domain.util.onFailure
import com.plcoding.spendlesssample.core.domain.util.onSuccess
import com.plcoding.spendlesssample.core.presentation.format.AmountFormatter
import com.plcoding.spendlesssample.core.presentation.format.AmountInputParser
import com.plcoding.spendlesssample.core.presentation.toUiText
import com.plcoding.spendlesssample.transactions.domain.TransactionValidator
import com.plcoding.spendlesssample.transactions.domain.model.ExpenseCategory
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.domain.model.TransactionDraft
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.domain.usecase.CreateTransaction
import com.plcoding.spendlesssample.transactions.presentation.format.RepeatLabelFormatter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the Create Transaction sheet. Every keystroke updates [state] synchronously so text fields
 * never lose characters. The form fields are saved in [SavedStateHandle] and survive process death;
 * the form is reset on close and after [CreateTransactionEvent.Created], never on open (research R7).
 */
class CreateTransactionViewModel(
    private val validator: TransactionValidator,
    private val createTransaction: CreateTransaction,
    preferencesSource: AmountFormatPreferencesSource,
    currentDateProvider: CurrentDateProvider,
    private val repeatLabelFormatter: RepeatLabelFormatter,
    private val amountInputParser: AmountInputParser,
    private val amountFormatter: AmountFormatter,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var preferences = AmountFormatPreferences.Default

    /** Set while an insert is in flight, so a double tap on Create cannot save twice. */
    private var isCreating = false

    private val _state = MutableStateFlow(restoredState().withDerivedFields())
    val state: StateFlow<CreateTransactionState> = _state.asStateFlow()

    private val _events = Channel<CreateTransactionEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            preferencesSource.preferences.collect { prefs ->
                preferences = prefs
                _state.update { it.withDerivedFields() }
            }
        }
        // The long labels name today's weekday and date, and follow a date change (FR-019).
        viewModelScope.launch {
            currentDateProvider.today.collect { today ->
                _state.update { it.copy(repeatOptions = repeatLabelFormatter.options(today)) }
            }
        }
    }

    fun onAction(action: CreateTransactionAction) {
        when (action) {
            // The category stays in state while Income (it is hidden, not cleared) so it comes back
            // on switching to Expense; every other field is kept too (FR-024).
            is CreateTransactionAction.OnTypeSelect ->
                updateForm { it.copy(type = action.type, openSelect = null) }
            is CreateTransactionAction.OnNameChange -> {
                if (action.text.length > TransactionValidator.MAX_NAME_LENGTH) return
                updateForm { it.copy(name = action.text) }
            }
            is CreateTransactionAction.OnAmountChange -> {
                val accepted = amountInputParser.accept(
                    current = _state.value.amountInput,
                    proposed = action.text,
                    prefs = preferences,
                ) ?: return
                updateForm { it.copy(amountInput = accepted) }
            }
            CreateTransactionAction.OnAddNoteClick -> _state.update { it.copy(isNoteEditing = true) }
            is CreateTransactionAction.OnNoteChange -> {
                if (action.text.length > TransactionValidator.MAX_NOTE_LENGTH) return
                updateForm { it.copy(note = action.text) }
            }
            CreateTransactionAction.OnNoteFocusLost -> {
                if (_state.value.note.isBlank()) {
                    updateForm { it.copy(note = "", isNoteEditing = false) }
                }
            }
            CreateTransactionAction.OnCategorySelectClick ->
                _state.update { it.copy(openSelect = OpenSelect.CATEGORY) }
            is CreateTransactionAction.OnCategoryPick ->
                updateForm { it.copy(category = action.category, openSelect = null) }
            CreateTransactionAction.OnSelectDismiss -> _state.update { it.copy(openSelect = null) }
            CreateTransactionAction.OnRepeatSelectClick ->
                _state.update { it.copy(openSelect = OpenSelect.REPEAT) }
            is CreateTransactionAction.OnRepeatPick ->
                updateForm { it.copy(repeat = action.frequency, openSelect = null) }
            CreateTransactionAction.OnCreateClick -> create()
            CreateTransactionAction.OnCloseClick -> resetForm()
        }
    }

    private fun create() {
        val current = _state.value
        if (isCreating || !current.isCreateEnabled) return
        val amountMinor = amountInputParser.toMinorUnits(current.amountInput, preferences) ?: return
        val draft = TransactionDraft(
            type = current.type,
            name = current.name,
            amountMinor = amountMinor,
            note = current.note,
            category = current.category.takeIf { current.type == TransactionType.EXPENSE },
            repeat = current.repeat,
        )
        // Create is enabled only for valid input and the inputs are capped, so a validation error
        // here is a programming error; there is nothing the user could correct.
        val transaction = when (val result = validator.validate(draft)) {
            is Result.Success -> result.data
            is Result.Error -> return
        }

        isCreating = true
        _state.update { it.copy(error = null) }
        viewModelScope.launch {
            createTransaction(transaction)
                .onSuccess {
                    resetForm()
                    _events.send(CreateTransactionEvent.Created)
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.toUiText()) }
                }
            isCreating = false
        }
    }

    /** Applies [transform], saves the form fields and recomputes the derived ones. */
    private fun updateForm(transform: (CreateTransactionState) -> CreateTransactionState) {
        _state.update { transform(it).withDerivedFields() }
        save(_state.value)
    }

    /** Clears the form; the repeat options depend on today, not on the form, so they are kept. */
    private fun resetForm() {
        SAVED_KEYS.forEach { savedStateHandle.remove<Any>(it) }
        _state.update { CreateTransactionState(repeatOptions = it.repeatOptions).withDerivedFields() }
    }

    private fun save(state: CreateTransactionState) {
        savedStateHandle[KEY_TYPE] = state.type.name
        savedStateHandle[KEY_NAME] = state.name
        savedStateHandle[KEY_AMOUNT] = state.amountInput
        savedStateHandle[KEY_NOTE] = state.note
        savedStateHandle[KEY_CATEGORY] = state.category.name
        savedStateHandle[KEY_REPEAT] = state.repeat.name
    }

    private fun restoredState(): CreateTransactionState {
        val note = savedStateHandle.get<String>(KEY_NOTE).orEmpty()
        return CreateTransactionState(
            type = savedStateHandle.enumOrNull<TransactionType>(KEY_TYPE) ?: TransactionType.EXPENSE,
            name = savedStateHandle.get<String>(KEY_NAME).orEmpty(),
            amountInput = savedStateHandle.get<String>(KEY_AMOUNT).orEmpty(),
            note = note,
            isNoteEditing = note.isNotEmpty(),
            category = savedStateHandle.enumOrNull<ExpenseCategory>(KEY_CATEGORY) ?: ExpenseCategory.Default,
            repeat = savedStateHandle.enumOrNull<RepeatFrequency>(KEY_REPEAT) ?: RepeatFrequency.Default,
        )
    }

    private fun CreateTransactionState.withDerivedFields(): CreateTransactionState {
        val amountMinor = amountInputParser.toMinorUnits(amountInput, preferences)
        val parts = amountFormatter.parts(
            amountMinor = null,
            isNegative = type == TransactionType.EXPENSE,
            prefs = preferences,
        )
        return copy(
            amountParts = AmountPartsUi(
                prefix = parts.prefix,
                number = if (amountInput.isEmpty()) parts.number else amountFormatter.groupInput(amountInput, preferences),
                suffix = parts.suffix,
                isPlaceholder = amountInput.isEmpty(),
                signTone = type,
            ),
            repeatFieldLabel = repeatLabelFormatter.shortLabel(repeat),
            isCreateEnabled = name.isNotBlank() && amountMinor != null,
        )
    }

    private companion object {
        const val KEY_TYPE = "create_type"
        const val KEY_NAME = "create_name"
        const val KEY_AMOUNT = "create_amount"
        const val KEY_NOTE = "create_note"
        const val KEY_CATEGORY = "create_category"
        const val KEY_REPEAT = "create_repeat"
        val SAVED_KEYS = listOf(KEY_TYPE, KEY_NAME, KEY_AMOUNT, KEY_NOTE, KEY_CATEGORY, KEY_REPEAT)
    }
}

private inline fun <reified E : Enum<E>> SavedStateHandle.enumOrNull(key: String): E? =
    get<String>(key)?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
