package com.plcoding.spendlesssample.core.data.format

import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferences
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferencesSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Serves the default preferences (FR-034) until a preferences feature replaces this binding.
 * Every amount on screen reads from this one source (FR-033).
 */
class DefaultAmountFormatPreferencesSource : AmountFormatPreferencesSource {
    override val preferences: Flow<AmountFormatPreferences> = flowOf(AmountFormatPreferences.Default)
}
