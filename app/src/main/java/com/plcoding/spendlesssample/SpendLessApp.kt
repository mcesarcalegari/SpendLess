package com.plcoding.spendlesssample

import android.app.Application
import com.plcoding.spendlesssample.core.data.di.coreDataModule
import com.plcoding.spendlesssample.core.database.di.databaseModule
import com.plcoding.spendlesssample.transactions.data.di.transactionsDataModule
import com.plcoding.spendlesssample.transactions.data.di.transactionsDomainModule
import com.plcoding.spendlesssample.transactions.presentation.di.transactionsPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SpendLessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SpendLessApp)
            modules(
                coreDataModule,
                databaseModule,
                transactionsDataModule,
                transactionsDomainModule,
                transactionsPresentationModule,
            )
        }
    }
}
