package com.plcoding.spendlesssample.core.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.plcoding.spendlesssample.core.database.di.buildEncryptedDatabase
import com.plcoding.spendlesssample.core.database.entity.TransactionEntity
import com.plcoding.spendlesssample.core.database.security.DatabasePassphraseProvider
import java.io.File
import java.security.KeyStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "encrypted_test.db"
    private val keyAlias = "spendless_db_key_test"
    private lateinit var blobFile: File

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        blobFile = File(context.noBackupFilesDir, "test_key.bin").also { it.delete() }
        deleteKey()
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
        blobFile.delete()
        deleteKey()
    }

    private fun deleteKey() {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(keyAlias)
    }

    private fun provider() = DatabasePassphraseProvider(blobFile = blobFile, keyAlias = keyAlias)

    private val row = TransactionEntity(
        type = "EXPENSE",
        counterparty = "Groceries",
        amountMinor = 4_599,
        note = null,
        category = "FOOD_AND_GROCERIES",
        repeat = "DOES_NOT_REPEAT",
        recordedAtEpochMs = 1_000,
        zoneId = "UTC",
        seriesId = null,
        occurrenceDateEpochDay = null,
    )

    private suspend fun createOnDisk(passphrase: ByteArray) {
        val db = buildEncryptedDatabase(context, passphrase, dbName)
        db.transactionDao().insert(row)
        db.close()
    }

    private fun readCount(passphrase: ByteArray): Int {
        val db = buildEncryptedDatabase(context, passphrase, dbName)
        try {
            return db.query("SELECT COUNT(*) FROM transactions", null)
                .use { it.moveToFirst(); it.getInt(0) }
        } finally {
            db.close()
        }
    }

    @Test
    fun fileOnDisk_doesNotStartWithThePlainSqliteHeader() = runTest {
        createOnDisk(provider().passphrase())

        val header = context.getDatabasePath(dbName).inputStream().use { it.readNBytes(16) }

        assertThat(String(header, Charsets.US_ASCII)).isNotEqualTo("SQLite format 3\u0000")
    }

    @Test
    fun reopeningWithTheSamePassphrase_readsTheRow() = runTest {
        val passphrase = provider().passphrase()
        createOnDisk(passphrase)

        assertThat(readCount(provider().passphrase())).isEqualTo(1)
    }

    @Test
    fun openingWithAWrongPassphrase_fails() = runTest {
        createOnDisk(provider().passphrase())

        assertFailure { readCount(ByteArray(32) { 7 }) }
    }

    @Test
    fun openingWithAnAllZeroPassphrase_fails() = runTest {
        createOnDisk(provider().passphrase())

        assertFailure { readCount(ByteArray(32)) }
    }

    @Test
    fun passphraseProvider_returnsTheSameBytesAcrossInstances() {
        val first = provider().passphrase()
        val second = provider().passphrase()

        assertThat(first).hasSize(32)
        assertThat(second.toList()).isEqualTo(first.toList())
    }

    @Test
    fun passphraseProvider_storesOnlyTheWrappedBlob() {
        val passphrase = provider().passphrase()

        val stored = blobFile.readBytes()
        assertThat(stored.toList().windowed(passphrase.size).none { it == passphrase.toList() })
            .isEqualTo(true)
    }

    @Test
    fun rowsSurviveARoundTrip() = runTest {
        val passphrase = provider().passphrase()
        createOnDisk(passphrase)

        val db = buildEncryptedDatabase(context, passphrase, dbName)
        val rows = db.transactionDao().observeAll().first()
        db.close()
        assertThat(rows.map { it.counterparty }).isEqualTo(listOf("Groceries"))
    }
}
