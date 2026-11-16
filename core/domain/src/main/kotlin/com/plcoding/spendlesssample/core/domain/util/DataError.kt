package com.plcoding.spendlesssample.core.domain.util

/** Errors raised by data sources. SpendLess is offline-first, so only local errors exist. */
sealed interface DataError : Error {
    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN,
    }
}
