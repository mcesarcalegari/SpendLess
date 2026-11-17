package com.plcoding.spendlesssample.transactions.presentation.format

import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import com.plcoding.spendlesssample.transactions.presentation.R
import com.plcoding.spendlesssample.transactions.presentation.create.RepeatOptionUi
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Repeat labels (FR-019, FR-020). The long labels name the weekday, ordinal day and month of the
 * transaction's date: "Weekly on Monday", "Monthly on the 13th", "Yearly on Jan 13th". The short
 * labels are the frequency names shown in the closed field. English regardless of the device
 * locale.
 */
class RepeatLabelFormatter {

    /** Every frequency in [RepeatFrequency] order, each with its long label for [date]. */
    fun options(date: LocalDate): ImmutableList<RepeatOptionUi> =
        RepeatFrequency.entries
            .map { RepeatOptionUi(frequency = it, label = longLabel(it, date)) }
            .toImmutableList()

    fun longLabel(frequency: RepeatFrequency, date: LocalDate): UiText = when (frequency) {
        RepeatFrequency.DOES_NOT_REPEAT,
        RepeatFrequency.DAILY,
        -> shortLabel(frequency)
        RepeatFrequency.WEEKLY -> UiText.StringResource(
            R.string.repeat_weekly_on,
            arrayOf(date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
        )
        RepeatFrequency.MONTHLY -> UiText.StringResource(
            R.string.repeat_monthly_on_the,
            arrayOf(ordinal(date.dayOfMonth)),
        )
        RepeatFrequency.YEARLY -> UiText.StringResource(
            R.string.repeat_yearly_on,
            arrayOf("${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${ordinal(date.dayOfMonth)}"),
        )
    }

    fun shortLabel(frequency: RepeatFrequency): UiText = UiText.StringResource(
        when (frequency) {
            RepeatFrequency.DOES_NOT_REPEAT -> R.string.repeat_does_not_repeat
            RepeatFrequency.DAILY -> R.string.repeat_daily
            RepeatFrequency.WEEKLY -> R.string.repeat_weekly
            RepeatFrequency.MONTHLY -> R.string.repeat_monthly
            RepeatFrequency.YEARLY -> R.string.repeat_yearly
        },
    )

    /** English ordinal of a day of the month: 1st, 2nd, 3rd, 4th, 11th, 12th, 13th, 21st, … */
    fun ordinal(day: Int): String {
        val suffix = if (day % 100 in 11..13) {
            "th"
        } else {
            when (day % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
        return "$day$suffix"
    }
}
