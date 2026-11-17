package com.plcoding.spendlesssample.transactions.presentation.format

import com.plcoding.spendlesssample.core.presentation.UiText
import com.plcoding.spendlesssample.transactions.presentation.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Day-group header text (FR-003): "TODAY", "YESTERDAY", or the uppercased English month name and
 * day number, for example "JANUARY 9". English regardless of the device locale.
 */
class DayHeaderFormatter {

    private val monthDay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH)

    fun format(date: LocalDate, today: LocalDate): UiText = when (date) {
        today -> UiText.StringResource(R.string.today)
        today.minusDays(1) -> UiText.StringResource(R.string.yesterday)
        else -> UiText.DynamicString(monthDay.format(date).uppercase(Locale.ENGLISH))
    }
}
