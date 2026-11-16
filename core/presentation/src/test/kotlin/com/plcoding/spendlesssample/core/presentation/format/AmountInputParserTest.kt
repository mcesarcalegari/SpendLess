package com.plcoding.spendlesssample.core.presentation.format

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class AmountInputParserTest {

    private val parser = AmountInputParser()
    private val default = AmountFormatPreferences.Default
    private val european = default.copy(decimalSeparator = ',', thousandsSeparator = '.')

    @ParameterizedTest
    @ValueSource(strings = ["", "0", "7", "15", "15.", "15.9", "15.99", "123456789", "123456789.99", ".5"])
    fun `accepts digits with at most one preferred separator`(proposed: String) {
        assertThat(parser.accept(current = "", proposed = proposed, prefs = default)).isEqualTo(proposed)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "15.9.", "1.2.3", "15,9", "1,000", "12a", "abc", "-5", "+5", "1 000", "15.999",
            "1234567890", "1234567890.5",
        ],
    )
    fun `rejects anything else`(proposed: String) {
        assertThat(parser.accept(current = "1", proposed = proposed, prefs = default)).isNull()
    }

    @Test
    fun `a rejected keystroke leaves the caller with the current text`() {
        assertThat(parser.accept(current = "15.99", proposed = "15.999", prefs = default)).isNull()
    }

    @Test
    fun `comma is the only separator when it is preferred`() {
        assertThat(parser.accept("", "15,99", european)).isEqualTo("15,99")
        assertThat(parser.accept("", "15.99", european)).isNull()
        assertThat(parser.accept("", "15,999", european)).isNull()
    }

    @ParameterizedTest
    @CsvSource(
        "15.99, 1599",
        "15.9, 1590",
        "15., 1500",
        "0.01, 1",
        "999999999.99, 99999999999",
        ".5, 50",
    )
    fun `converts to minor units`(raw: String, expected: Long) {
        assertThat(parser.toMinorUnits(raw, default)).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "0", "0.00", "0.", ".", "00"])
    fun `empty or zero has no amount`(raw: String) {
        assertThat(parser.toMinorUnits(raw, default)).isNull()
    }

    @Test
    fun `converts with comma as decimal separator`() {
        assertThat(parser.toMinorUnits("15,99", european)).isEqualTo(1599L)
        assertThat(parser.toMinorUnits("1,5", european)).isEqualTo(150L)
    }
}
