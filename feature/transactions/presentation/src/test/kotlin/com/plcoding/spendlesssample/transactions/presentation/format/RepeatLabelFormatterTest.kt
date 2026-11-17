package com.plcoding.spendlesssample.transactions.presentation.format

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.presentation.R
import java.time.LocalDate
import java.util.Locale
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RepeatLabelFormatterTest {

    private val formatter = RepeatLabelFormatter()

    /** Monday. */
    private val jan13 = LocalDate.of(2025, 1, 13)

    private lateinit var defaultLocale: Locale

    @BeforeEach
    fun setUp() {
        defaultLocale = Locale.getDefault()
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    private fun resource(id: Int, vararg args: Any) = UiText.StringResource(id, arrayOf(*args))

    @Test
    fun `long labels for Monday Jan 13`() {
        assertThat(formatter.longLabel(RepeatFrequency.DOES_NOT_REPEAT, jan13))
            .isEqualTo(resource(R.string.repeat_does_not_repeat))
        assertThat(formatter.longLabel(RepeatFrequency.DAILY, jan13)).isEqualTo(resource(R.string.repeat_daily))
        assertThat(formatter.longLabel(RepeatFrequency.WEEKLY, jan13))
            .isEqualTo(resource(R.string.repeat_weekly_on, "Monday"))
        assertThat(formatter.longLabel(RepeatFrequency.MONTHLY, jan13))
            .isEqualTo(resource(R.string.repeat_monthly_on_the, "13th"))
        assertThat(formatter.longLabel(RepeatFrequency.YEARLY, jan13))
            .isEqualTo(resource(R.string.repeat_yearly_on, "Jan 13th"))
    }

    @Test
    fun `the weekday and month follow the date`() {
        val saturdayFeb22 = LocalDate.of(2025, 2, 22)

        assertThat(formatter.longLabel(RepeatFrequency.WEEKLY, saturdayFeb22))
            .isEqualTo(resource(R.string.repeat_weekly_on, "Saturday"))
        assertThat(formatter.longLabel(RepeatFrequency.MONTHLY, saturdayFeb22))
            .isEqualTo(resource(R.string.repeat_monthly_on_the, "22nd"))
        assertThat(formatter.longLabel(RepeatFrequency.YEARLY, saturdayFeb22))
            .isEqualTo(resource(R.string.repeat_yearly_on, "Feb 22nd"))
    }

    @ParameterizedTest
    @CsvSource(
        "1, 1st", "2, 2nd", "3, 3rd", "4, 4th", "10, 10th",
        "11, 11th", "12, 12th", "13, 13th", "14, 14th",
        "20, 20th", "21, 21st", "22, 22nd", "23, 23rd", "24, 24th",
        "30, 30th", "31, 31st",
    )
    fun `English ordinals`(day: Int, expected: String) {
        assertThat(formatter.ordinal(day)).isEqualTo(expected)
    }

    @Test
    fun `short labels`() {
        assertThat(RepeatFrequency.entries.map { formatter.shortLabel(it) }).containsExactly(
            resource(R.string.repeat_does_not_repeat),
            resource(R.string.repeat_daily),
            resource(R.string.repeat_weekly),
            resource(R.string.repeat_monthly),
            resource(R.string.repeat_yearly),
        )
    }

    @Test
    fun `options list every frequency in order with its long label`() {
        val options = formatter.options(jan13)

        assertThat(options.map { it.frequency }).containsExactly(*RepeatFrequency.entries.toTypedArray())
        assertThat(options.map { it.label }).containsExactly(
            resource(R.string.repeat_does_not_repeat),
            resource(R.string.repeat_daily),
            resource(R.string.repeat_weekly_on, "Monday"),
            resource(R.string.repeat_monthly_on_the, "13th"),
            resource(R.string.repeat_yearly_on, "Jan 13th"),
        )
    }

    @Test
    fun `labels stay English under a German default locale`() {
        Locale.setDefault(Locale.GERMAN)
        val fresh = RepeatLabelFormatter()

        assertThat(fresh.longLabel(RepeatFrequency.WEEKLY, jan13))
            .isEqualTo(resource(R.string.repeat_weekly_on, "Monday"))
        assertThat(fresh.longLabel(RepeatFrequency.YEARLY, LocalDate.of(2025, 3, 3)))
            .isEqualTo(resource(R.string.repeat_yearly_on, "Mar 3rd"))
    }
}
