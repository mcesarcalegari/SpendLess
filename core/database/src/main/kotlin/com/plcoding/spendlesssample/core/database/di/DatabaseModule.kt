package com.plcoding.spendlesssample.core.database.di

import android.content.Context
import androidx.room.Room
import com.plcoding.spendlesssample.core.database.SpendLessDatabase
import com.plcoding.spendlesssample.core.database.security.DatabasePassphraseProvider
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Builds the SQLCipher-encrypted database.
 *
 * The passphrase is deliberately **not** zeroed: [SupportOpenHelperFactory] keeps a reference to
 * the array (no copy) and SQLCipher reads it on every lazy open, so zeroing it would key the
 * database with 32 zero bytes (research R13). The array lives only inside this singleton.
 */
fun buildEncryptedDatabase(
    context: Context,
    passphrase: ByteArray,
    name: String = SpendLessDatabase.NAME,
): SpendLessDatabase {
    System.loadLibrary("sqlcipher")
    return Room.databaseBuilder(context, SpendLessDatabase::class.java, name)
        .openHelperFactory(SupportOpenHelperFactory(passphrase))
        .build()
}

val databaseModule = module {
    single { DatabasePassphraseProvider(androidContext()) }
    single { buildEncryptedDatabase(androidContext(), get<DatabasePassphraseProvider>().passphrase()) }
    single { get<SpendLessDatabase>().transactionDao() }
}
