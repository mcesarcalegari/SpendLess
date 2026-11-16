package com.plcoding.spendlesssample.core.domain.format

import kotlinx.coroutines.flow.Flow

/** The one shared place for amount formatting preferences (FR-033). */
interface AmountFormatPreferencesSource {
    val preferences: Flow<AmountFormatPreferences>
}
