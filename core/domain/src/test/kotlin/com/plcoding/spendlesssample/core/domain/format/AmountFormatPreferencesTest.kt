package com.plcoding.spendlesssample.core.domain.format

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test

class AmountFormatPreferencesTest {

    @Test
    fun `default uses dollar, minus prefix, dot decimal and comma thousands`() {
        val default = AmountFormatPreferences.Default

        assertThat(default.currencySymbol).isEqualTo("$")
        assertThat(default.negativeStyle).isEqualTo(NegativeStyle.MINUS_PREFIX)
        assertThat(default.decimalSeparator).isEqualTo('.')
        assertThat(default.thousandsSeparator).isEqualTo(',')
    }

    @Test
    fun `same decimal and thousands separator is rejected`() {
        assertFailure {
            AmountFormatPreferences("$", NegativeStyle.MINUS_PREFIX, '.', '.')
        }.isInstanceOf(IllegalArgumentException::class)
    }

    @Test
    fun `space is accepted as thousands separator`() {
        val prefs = AmountFormatPreferences("$", NegativeStyle.PARENTHESES, '.', ' ')

        assertThat(prefs.thousandsSeparator).isEqualTo(' ')
    }

    @Test
    fun `comma decimal with dot thousands is accepted`() {
        val prefs = AmountFormatPreferences("€", NegativeStyle.MINUS_PREFIX, ',', '.')

        assertThat(prefs.decimalSeparator).isEqualTo(',')
        assertThat(prefs.thousandsSeparator).isEqualTo('.')
    }
}
