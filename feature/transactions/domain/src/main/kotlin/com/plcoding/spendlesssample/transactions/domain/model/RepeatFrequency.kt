package com.plcoding.spendlesssample.transactions.domain.model

/** How often a transaction repeats (FR-019). */
enum class RepeatFrequency {
    DOES_NOT_REPEAT,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ;

    companion object {
        val Default = DOES_NOT_REPEAT
    }
}
