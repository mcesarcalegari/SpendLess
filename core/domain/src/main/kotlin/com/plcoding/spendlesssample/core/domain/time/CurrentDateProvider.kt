package com.plcoding.spendlesssample.core.domain.time

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Emits today's local date now and on every date change (midnight, time or time-zone change). */
interface CurrentDateProvider {
    val today: Flow<LocalDate>
}
