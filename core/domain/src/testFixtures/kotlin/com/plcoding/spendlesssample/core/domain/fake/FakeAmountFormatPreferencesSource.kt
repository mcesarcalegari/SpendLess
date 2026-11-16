package com.plcoding.spendlesssample.core.domain.fake

import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferencesSource
import kotlinx.coroutines.flow.MutableStateFlow

/** Test double: set [current] to simulate the user changing their preferences. */
class FakeAmountFormatPreferencesSource(
    initial: AmountFormatPreferences = AmountFormatPreferences.Default,
) : AmountFormatPreferencesSource {
    val current = MutableStateFlow(initial)
    override val preferences = current
}
