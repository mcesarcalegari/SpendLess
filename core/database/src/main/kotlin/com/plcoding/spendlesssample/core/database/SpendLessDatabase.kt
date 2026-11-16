package com.plcoding.spendlesssample.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.plcoding.spendlesssample.core.database.dao.TransactionDao
import com.plcoding.spendlesssample.core.database.entity.RecurringSeriesEntity
import com.plcoding.spendlesssample.core.database.entity.TransactionEntity

/** The app's single database. In production it is always opened through SQLCipher. */
@Database(
    entities = [TransactionEntity::class, RecurringSeriesEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SpendLessDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val NAME = "spendless.db"
    }
}
