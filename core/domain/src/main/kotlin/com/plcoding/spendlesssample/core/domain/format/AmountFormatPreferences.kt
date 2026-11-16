package com.plcoding.spendlesssample.core.domain.format

/** How a negative (expense) amount is written. */
enum class NegativeStyle {
    /** `-$10.00` */
    MINUS_PREFIX,

    /** `($10.00)` */
    PARENTHESES,
}

/**
 * The user's amount formatting preferences (FR-032).
 *
 * The decimal and thousands separators must differ, otherwise an amount could not be read back.
 */
data class AmountFormatPreferences(
    val currencySymbol: String,
    val negativeStyle: NegativeStyle,
    val decimalSeparator: Char,
    val thousandsSeparator: Char,
) {
    init {
        require(decimalSeparator != thousandsSeparator) {
            "Decimal and thousands separators must differ"
        }
    }

    companion object {
        /** Used until the user sets their own preferences (FR-034). */
        val Default = AmountFormatPreferences(
            currencySymbol = "$",
            negativeStyle = NegativeStyle.MINUS_PREFIX,
            decimalSeparator = '.',
            thousandsSeparator = ',',
        )
    }
}
