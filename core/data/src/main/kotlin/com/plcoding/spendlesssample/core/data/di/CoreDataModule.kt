package com.plcoding.spendlesssample.core.data.di

import com.plcoding.spendlesssample.core.data.format.DefaultAmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.data.time.SystemCurrentDateProvider
import com.plcoding.spendlesssample.core.data.time.SystemDefaultZoneClock
import com.plcoding.spendlesssample.core.domain.format.AmountFormatPreferencesSource
import com.plcoding.spendlesssample.core.domain.time.CurrentDateProvider
import java.time.Clock
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreDataModule = module {
    // Not Clock.systemDefaultZone(): that captures the zone once and would go stale on a
    // time-zone change (research R10).
    single<Clock> { SystemDefaultZoneClock() }
    singleOf(::DefaultAmountFormatPreferencesSource) { bind<AmountFormatPreferencesSource>() }
    // Lambda form: the class has a second, internal test constructor.
    single<CurrentDateProvider> { SystemCurrentDateProvider(androidContext(), get()) }
}
