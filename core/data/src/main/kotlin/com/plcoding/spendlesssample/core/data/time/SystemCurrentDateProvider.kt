package com.plcoding.spendlesssample.core.data.time

import android.content.Context
import com.plcoding.spendlesssample.core.domain.time.CurrentDateProvider
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * Today's local date, re-read on the system date/time/time-zone broadcasts and on a timer that
 * fires at the next local midnight from [clock]. The timer covers devices that deliver
 * `ACTION_DATE_CHANGED` late (research R10).
 */
class SystemCurrentDateProvider internal constructor(
    private val clock: Clock,
    private val signals: DateChangeSignals,
) : CurrentDateProvider {

    constructor(context: Context, clock: Clock) : this(clock, BroadcastDateChangeSignals(context))

    override val today: Flow<LocalDate> =
        merge(flowOf(Unit), signals.signals(), midnightTicks())
            .map { LocalDate.now(clock) }
            .distinctUntilChanged()

    private fun midnightTicks(): Flow<Unit> = flow {
        while (true) {
            val now = ZonedDateTime.now(clock)
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1))
            emit(Unit)
        }
    }
}
