package com.plcoding.spendlesssample.transactions.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Read model of a recurring series, derived from the original transaction plus its series row.
 *
 * @property seriesId Equals the original transaction's `id`.
 * @property frequency Never `DOES_NOT_REPEAT`.
 * @property anchorDate The original's local date. Fixes the weekday, day-of-month and month-day.
 * @property anchorTime The original's local time of day.
 * @property stoppedOn `null` while running. Once set it is never cleared (FR-039).
 * @property lastMaterializedDate Highest due date that has been processed. Starts at `anchorDate`.
 */
data class RecurringSeries(
    val seriesId: Long,
    val frequency: RepeatFrequency,
    val anchorDate: LocalDate,
    val anchorTime: LocalTime,
    val zoneId: ZoneId,
    val stoppedOn: LocalDate?,
    val lastMaterializedDate: LocalDate,
)
