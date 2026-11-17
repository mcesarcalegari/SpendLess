package com.plcoding.spendlesssample.transactions.domain

import com.plcoding.spendlesssample.core.domain.util.Result
import com.plcoding.spendlesssample.transactions.domain.model.Transaction
import com.plcoding.spendlesssample.transactions.domain.model.TransactionDraft
import com.plcoding.spendlesssample.transactions.domain.model.TransactionType
import com.plcoding.spendlesssample.transactions.domain.model.TransactionValidationError
import java.time.Clock

/**
 * Checks a [TransactionDraft] against the rules of data-model §1 and stamps the result with the
 * current instant and zone (FR-002, FR-022). Pure and synchronous.
 */
class TransactionValidator(private val clock: Clock) {

    fun validate(draft: TransactionDraft): Result<Transaction, TransactionValidationError> {
        val name = draft.name.trim()
        val note = draft.note?.trim()?.takeIf { it.isNotEmpty() }
        val error = when {
            name.isEmpty() -> TransactionValidationError.NAME_BLANK
            name.length > MAX_NAME_LENGTH -> TransactionValidationError.NAME_TOO_LONG
            draft.amountMinor < MIN_AMOUNT_MINOR -> TransactionValidationError.AMOUNT_NOT_POSITIVE
            draft.amountMinor > MAX_AMOUNT_MINOR -> TransactionValidationError.AMOUNT_TOO_LARGE
            note != null && note.length > MAX_NOTE_LENGTH -> TransactionValidationError.NOTE_TOO_LONG
            draft.type == TransactionType.EXPENSE && draft.category == null ->
                TransactionValidationError.CATEGORY_MISSING_FOR_EXPENSE
            else -> null
        }
        if (error != null) return Result.Error(error)

        return Result.Success(
            Transaction(
                id = 0,
                type = draft.type,
                counterparty = name,
                amountMinor = draft.amountMinor,
                note = note,
                category = draft.category,
                repeat = draft.repeat,
                recordedAt = clock.instant(),
                zoneId = clock.zone,
                seriesId = null,
                occurrenceDate = null,
            ),
        )
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_NOTE_LENGTH = 500
        const val MIN_AMOUNT_MINOR = 1L
        const val MAX_AMOUNT_MINOR = 99_999_999_999L
    }
}
