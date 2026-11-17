package com.plcoding.spendlesssample.transactions.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.plcoding.spendlesssample.transactions.domain.model.RepeatFrequency
import java.time.LocalDate
import org.junit.jupiter.api.Test

class RecurrenceScheduleTest {

    private fun date(text: String) = LocalDate.parse(text)

    private fun due(
        anchor: String,
        frequency: RepeatFrequency,
        fromExclusive: String,
        toInclusive: String,
    ) = RecurrenceSchedule.dueDatesBetween(
        anchor = date(anchor),
        frequency = frequency,
        fromExclusive = date(fromExclusive),
        toInclusive = date(toInclusive),
    )

    @Test
    fun `monthly on Jan 31 clamps to each month's end and returns to the 31st`() {
        assertThat(due("2025-01-31", RepeatFrequency.MONTHLY, "2025-01-31", "2025-05-31")).containsExactly(
            date("2025-02-28"),
            date("2025-03-31"),
            date("2025-04-30"),
            date("2025-05-31"),
        )
    }

    @Test
    fun `monthly on Jan 31 lands on Feb 29 in a leap year`() {
        assertThat(due("2024-01-31", RepeatFrequency.MONTHLY, "2024-01-31", "2024-03-31")).containsExactly(
            date("2024-02-29"),
            date("2024-03-31"),
        )
    }

    @Test
    fun `monthly after a short month is computed from the anchor, not the previous date`() {
        // From the previous date, Feb 28 + 1 month would be Mar 28.
        assertThat(due("2025-01-31", RepeatFrequency.MONTHLY, "2025-02-28", "2025-03-31"))
            .containsExactly(date("2025-03-31"))
    }

    @Test
    fun `monthly on the 13th repeats on the 13th`() {
        assertThat(due("2025-01-13", RepeatFrequency.MONTHLY, "2025-01-13", "2025-03-13")).containsExactly(
            date("2025-02-13"),
            date("2025-03-13"),
        )
    }

    @Test
    fun `yearly on Feb 29 is Feb 28 in non-leap years and Feb 29 in leap years`() {
        assertThat(due("2024-02-29", RepeatFrequency.YEARLY, "2024-02-29", "2028-02-29")).containsExactly(
            date("2025-02-28"),
            date("2026-02-28"),
            date("2027-02-28"),
            date("2028-02-29"),
        )
    }

    @Test
    fun `yearly repeats on the same month and day`() {
        assertThat(due("2025-01-13", RepeatFrequency.YEARLY, "2025-01-13", "2027-01-13")).containsExactly(
            date("2026-01-13"),
            date("2027-01-13"),
        )
    }

    @Test
    fun `weekly is the anchor plus n weeks`() {
        assertThat(due("2025-01-13", RepeatFrequency.WEEKLY, "2025-01-13", "2025-02-03")).containsExactly(
            date("2025-01-20"),
            date("2025-01-27"),
            date("2025-02-03"),
        )
    }

    @Test
    fun `daily is the anchor plus n days`() {
        assertThat(due("2025-01-13", RepeatFrequency.DAILY, "2025-01-13", "2025-01-16")).containsExactly(
            date("2025-01-14"),
            date("2025-01-15"),
            date("2025-01-16"),
        )
    }

    @Test
    fun `daily crosses a month and year boundary`() {
        assertThat(due("2024-12-30", RepeatFrequency.DAILY, "2024-12-30", "2025-01-01")).containsExactly(
            date("2024-12-31"),
            date("2025-01-01"),
        )
    }

    @Test
    fun `fromExclusive is excluded and toInclusive is included`() {
        assertThat(due("2025-01-01", RepeatFrequency.DAILY, "2025-01-03", "2025-01-05")).containsExactly(
            date("2025-01-04"),
            date("2025-01-05"),
        )
    }

    @Test
    fun `a window between two due dates is empty`() {
        assertThat(due("2025-01-13", RepeatFrequency.WEEKLY, "2025-01-14", "2025-01-19")).isEmpty()
    }

    @Test
    fun `a window starting before the anchor never returns the anchor or earlier dates`() {
        assertThat(due("2025-01-13", RepeatFrequency.DAILY, "2025-01-10", "2025-01-14"))
            .containsExactly(date("2025-01-14"))
    }

    @Test
    fun `empty when toInclusive equals fromExclusive`() {
        assertThat(due("2025-01-13", RepeatFrequency.DAILY, "2025-01-15", "2025-01-15")).isEmpty()
    }

    @Test
    fun `empty when toInclusive is before fromExclusive`() {
        assertThat(due("2025-01-13", RepeatFrequency.DAILY, "2025-01-15", "2025-01-14")).isEmpty()
    }

    @Test
    fun `DOES_NOT_REPEAT has no due dates`() {
        assertThat(due("2025-01-13", RepeatFrequency.DOES_NOT_REPEAT, "2025-01-13", "2026-01-13")).isEmpty()
    }
}
