package com.plcoding.spendlesssample

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.plcoding.spendlesssample.core.data.di.coreDataModule
import com.plcoding.spendlesssample.core.database.di.databaseModule
import com.plcoding.spendlesssample.transactions.data.di.transactionsDataModule
import com.plcoding.spendlesssample.transactions.data.di.transactionsDomainModule
import com.plcoding.spendlesssample.transactions.presentation.di.transactionsPresentationModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SpendLessApp : Application() {

    /** Lives as long as the process; one failing child does not cancel the others. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@SpendLessApp)
            modules(
                coreDataModule,
                databaseModule,
                transactionsDataModule,
                transactionsDomainModule,
                transactionsPresentationModule,
            )
        }.koin

        // Catch-up on every foreground start and live at the date change (FR-025a, FR-025c).
        RecurringOccurrencesTrigger(
            currentDateProvider = koin.get(),
            materializeDueOccurrences = koin.get(),
            scope = applicationScope,
        ).start(ProcessLifecycleOwner.get().lifecycle)
    }
}
