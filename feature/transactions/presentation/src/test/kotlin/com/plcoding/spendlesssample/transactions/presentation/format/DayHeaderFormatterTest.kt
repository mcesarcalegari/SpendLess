package com.plcoding.spendlesssample.transactions.presentation.format

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.presentation.R
import java.time.LocalDate
import java.util.Locale
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class DayHeaderFormatterTest {

    private val formatter = DayHeaderFormatter()
    private val today = LocalDate.of(2026, 1, 15)
    private val originalLocale = Locale.getDefault()

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `today reads TODAY`() {
        assertThat(formatter.format(today, today)).isEqualTo(UiText.StringResource(R.string.today))
    }

    @Test
    fun `the previous date reads YESTERDAY`() {
        assertThat(formatter.format(today.minusDays(1), today)).isEqualTo(UiText.StringResource(R.string.yesterday))
    }

    @Test
    fun `yesterday across a month boundary reads YESTERDAY`() {
        val firstOfMonth = LocalDate.of(2026, 3, 1)
        assertThat(formatter.format(LocalDate.of(2026, 2, 28), firstOfMonth))
            .isEqualTo(UiText.StringResource(R.string.yesterday))
    }

    @Test
    fun `two days ago reads the uppercased month and day`() {
        assertThat(formatter.format(today.minusDays(2), today)).isEqualTo(UiText.DynamicString("JANUARY 13"))
    }

    @Test
    fun `an older date reads the month and day without padding`() {
        assertThat(formatter.format(LocalDate.of(2026, 1, 9), today)).isEqualTo(UiText.DynamicString("JANUARY 9"))
    }

    @Test
    fun `a date in the previous year reads the month and day without a year`() {
        assertThat(formatter.format(LocalDate.of(2025, 12, 28), today)).isEqualTo(UiText.DynamicString("DECEMBER 28"))
    }

    @Test
    fun `yesterday across a year boundary reads YESTERDAY`() {
        assertThat(formatter.format(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1)))
            .isEqualTo(UiText.StringResource(R.string.yesterday))
    }

    @Test
    fun `month names stay English when the device locale is German`() {
        Locale.setDefault(Locale.GERMAN)
        // Built after the locale change, so a locale captured at construction would show here.
        assertThat(DayHeaderFormatter().format(LocalDate.of(2026, 3, 9), today)).isEqualTo(UiText.DynamicString("MARCH 9"))
    }

    @Test
    fun `uppercasing ignores a Turkish device locale`() {
        // uppercase(Locale.getDefault()) would turn the "i" in "april" into a dotted "İ".
        Locale.setDefault(Locale.forLanguageTag("tr"))
        assertThat(DayHeaderFormatter().format(LocalDate.of(2025, 4, 9), today)).isEqualTo(UiText.DynamicString("APRIL 9"))
    }
}
