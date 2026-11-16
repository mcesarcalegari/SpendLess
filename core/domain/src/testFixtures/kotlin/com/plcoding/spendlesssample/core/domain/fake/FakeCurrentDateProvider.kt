package com.plcoding.spendlesssample.core.domain.fake

import com.plcoding.spendlesssample.core.domain.time.CurrentDateProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow

/** Test double: set [current] to simulate a date change. */
class FakeCurrentDateProvider(initial: LocalDate) : CurrentDateProvider {
    val current = MutableStateFlow(initial)
    override val today = current
}
