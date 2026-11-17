package com.plcoding.spendlesssample.transactions.domain

import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import java.time.LocalDate

/**
 * Due-date maths for recurring series (research R10). Pure and synchronous.
 *
 * Each due date is the anchor plus n periods, **always computed from the anchor**, never from the
 * previous due date. `plusMonths` / `plusYears` clamp to the month length, so monthly on Jan 31 is
 * Feb 28 (29 in a leap year) → Mar 31 → Apr 30, and yearly on Feb 29 is Feb 28 in non-leap years.
 */
object RecurrenceSchedule {

    /**
     * The due dates after [anchor] in the window ([fromExclusive], [toInclusive]], oldest first.
     * The anchor itself is the original transaction, so it is never a due date. Empty for
     * [RepeatFrequency.DOES_NOT_REPEAT] and for an empty window.
     */
    fun dueDatesBetween(
        anchor: LocalDate,
        frequency: RepeatFrequency,
        fromExclusive: LocalDate,
        toInclusive: LocalDate,
    ): List<LocalDate> {
        if (frequency == RepeatFrequency.DOES_NOT_REPEAT || toInclusive <= fromExclusive) return emptyList()
        return generateSequence(1L) { it + 1 }
            .map { n -> anchor.plusPeriods(frequency, n) }
            .takeWhile { it <= toInclusive }
            .filter { it > fromExclusive }
            .toList()
    }

    private fun LocalDate.plusPeriods(frequency: RepeatFrequency, n: Long): LocalDate = when (frequency) {
        RepeatFrequency.DAILY -> plusDays(n)
        RepeatFrequency.WEEKLY -> plusWeeks(n)
        RepeatFrequency.MONTHLY -> plusMonths(n)
        RepeatFrequency.YEARLY -> plusYears(n)
        RepeatFrequency.DOES_NOT_REPEAT -> error("DOES_NOT_REPEAT has no periods")
    }
}
