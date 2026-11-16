package com.plcoding.spendlesssample.transactions.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A recorded expense or income (data-model §1).
 *
 * @property id Assigned by storage; `0` before insert.
 * @property counterparty Trimmed. Must not be blank after trimming. 1–50 chars.
 * @property amountMinor Always positive: 1 ≤ value ≤ 99_999_999_999. The sign comes from [type].
 * @property note `null` when empty after trimming. ≤ 500 chars.
 * @property category Required (non-null) when `type == EXPENSE`; must be `null` when `type == INCOME`.
 * @property repeat `DOES_NOT_REPEAT` for every occurrence.
 * @property recordedAt Creation instant. For an occurrence: `min(due date + the original's local
 *   time of day, the instant it is materialised)`.
 * @property zoneId Zone at recording. Used to compute the local date and time-of-day anchor.
 * @property seriesId `null` for a one-time transaction. Equals its own `id` for a recurring
 *   original. The original's `id` for an occurrence.
 * @property occurrenceDate Due date for occurrences only; `null` otherwise.
 */
data class Transaction(
    val id: Long,
    val type: TransactionType,
    val counterparty: String,
    val amountMinor: Long,
    val note: String?,
    val category: ExpenseCategory?,
    val repeat: RepeatFrequency,
    val recordedAt: Instant,
    val zoneId: ZoneId,
    val seriesId: Long?,
    val occurrenceDate: LocalDate?,
)
