package com.plcoding.spendlesssample.transactions.domain.model

/**
 * Fixed expense categories, in alphabetical order (FR-026). Emoji and display names are mapped in
 * presentation; the domain holds only the enum.
 */
enum class ExpenseCategory {
    CLOTHING_AND_ACCESSORIES,
    EDUCATION,
    ENTERTAINMENT,
    FOOD_AND_GROCERIES,
    HEALTH_AND_WELLNESS,
    HOME,
    OTHER,
    ;

    companion object {
        val Default = OTHER
    }
}
