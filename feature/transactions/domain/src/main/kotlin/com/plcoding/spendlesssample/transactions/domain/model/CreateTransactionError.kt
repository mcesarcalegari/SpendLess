package com.plcoding.spendlesssample.transactions.domain.model

import com.plcoding.spendlesssample.core.domain.util.Error

/** Why a [TransactionDraft] is not a valid [Transaction]. Returned only by `TransactionValidator`. */
enum class TransactionValidationError : Error {
    NAME_BLANK,
    NAME_TOO_LONG,
    AMOUNT_NOT_POSITIVE,
    AMOUNT_TOO_LARGE,
    NOTE_TOO_LONG,
    CATEGORY_MISSING_FOR_EXPENSE,
    CATEGORY_NOT_ALLOWED_FOR_INCOME,
}
