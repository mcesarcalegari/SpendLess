package com.plcoding.spendlesssample.core.presentation.format

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import com.plcoding.spendlesssample.core.domain.format.NegativeStyle
import org.junit.jupiter.api.Test

class AmountFormatterTest {

    private val formatter = AmountFormatter()
    private val default = AmountFormatPreferences.Default
    private val parentheses = default.copy(negativeStyle = NegativeStyle.PARENTHESES)
    private val european = default.copy(decimalSeparator = ',', thousandsSeparator = '.')
    private val spaced = default.copy(thousandsSeparator = ' ')

    @Test
    fun `expense with default preferences`() {
        assertThat(formatter.format(123_450, isNegative = true, prefs = default)).isEqualTo("-$1,234.50")
    }

    @Test
    fun `expense with comma decimal and dot thousands`() {
        assertThat(formatter.format(123_450, isNegative = true, prefs = european)).isEqualTo("-$1.234,50")
    }

    @Test
    fun `income with space thousands separator`() {
        assertThat(formatter.format(1_200_000, isNegative = false, prefs = spaced)).isEqualTo("$12 000.00")
    }

    @Test
    fun `expense in parentheses`() {
        assertThat(formatter.format(4_599, isNegative = true, prefs = parentheses)).isEqualTo("($45.99)")
    }

    @Test
    fun `income has no sign even with parentheses style`() {
        assertThat(formatter.format(2_000, isNegative = false, prefs = default)).isEqualTo("$20.00")
        assertThat(formatter.format(2_000, isNegative = false, prefs = parentheses)).isEqualTo("$20.00")
    }

    @Test
    fun `maximum amount is fully grouped`() {
        assertThat(formatter.format(99_999_999_999, isNegative = true, prefs = default))
            .isEqualTo("-$999,999,999.99")
    }

    @Test
    fun `small amounts keep a leading zero and two decimals`() {
        assertThat(formatter.format(1, isNegative = false, prefs = default)).isEqualTo("$0.01")
        assertThat(formatter.format(10, isNegative = false, prefs = default)).isEqualTo("$0.10")
        assertThat(formatter.format(99_999, isNegative = false, prefs = default)).isEqualTo("$999.99")
        assertThat(formatter.format(100_000, isNegative = false, prefs = default)).isEqualTo("$1,000.00")
    }

    @Test
    fun `currency symbol is always before the number`() {
        val euro = default.copy(currencySymbol = "€")
        assertThat(formatter.format(4_599, isNegative = true, prefs = euro)).isEqualTo("-€45.99")
    }

    @Test
    fun `placeholder parts for an expense with default preferences`() {
        assertThat(formatter.parts(null, isNegative = true, prefs = default))
            .isEqualTo(AmountParts(prefix = "-$", number = "00.00", suffix = "", isPlaceholder = true))
    }

    @Test
    fun `placeholder parts for an expense in parentheses`() {
        assertThat(formatter.parts(null, isNegative = true, prefs = parentheses))
            .isEqualTo(AmountParts(prefix = "($", number = "00.00", suffix = ")", isPlaceholder = true))
    }

    @Test
    fun `placeholder parts for income`() {
        assertThat(formatter.parts(null, isNegative = false, prefs = parentheses))
            .isEqualTo(AmountParts(prefix = "$", number = "00.00", suffix = "", isPlaceholder = true))
    }

    @Test
    fun `placeholder uses the preferred decimal separator`() {
        assertThat(formatter.parts(null, isNegative = true, prefs = european).number).isEqualTo("00,00")
    }

    @Test
    fun `parts for a value split the formatted amount`() {
        assertThat(formatter.parts(123_450, isNegative = true, prefs = parentheses))
            .isEqualTo(AmountParts(prefix = "($", number = "1,234.50", suffix = ")", isPlaceholder = false))
    }

    @Test
    fun `groupInput groups the whole part and keeps the typed decimals`() {
        assertThat(formatter.groupInput("1234567.5", default)).isEqualTo("1,234,567.5")
    }

    @Test
    fun `groupInput keeps a trailing separator and short inputs`() {
        assertThat(formatter.groupInput("1234.", default)).isEqualTo("1,234.")
        assertThat(formatter.groupInput("999", default)).isEqualTo("999")
        assertThat(formatter.groupInput("", default)).isEqualTo("")
    }

    @Test
    fun `groupInput uses the preferred separators`() {
        assertThat(formatter.groupInput("1234567,89", european)).isEqualTo("1.234.567,89")
        assertThat(formatter.groupInput("12000", spaced)).isEqualTo("12 000")
    }
}
