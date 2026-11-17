package com.plcoding.spendlesssample.core.database.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Supplies the SQLCipher passphrase (research R13).
 *
 * The first call generates 32 random bytes and wraps them with an AndroidKeyStore AES-256-GCM key.
 * Only the IV and ciphertext are written, to a file in `noBackupFilesDir`. Later calls unwrap the
 * same bytes. Nothing here is ever logged.
 */
class DatabasePassphraseProvider internal constructor(
    private val blobFile: File,
    private val keyAlias: String,
) {

    constructor(context: Context) : this(
        blobFile = File(context.noBackupFilesDir, BLOB_FILE_NAME),
        keyAlias = KEY_ALIAS,
    )

    @Synchronized
    fun passphrase(): ByteArray {
        val key = getOrCreateKey()
        if (blobFile.exists()) return unwrap(key, blobFile.readBytes())

        val passphrase = ByteArray(PASSPHRASE_BYTES).also(SecureRandom()::nextBytes)
        val tmp = File(blobFile.parentFile, "${blobFile.name}.tmp")
        tmp.writeBytes(wrap(key, passphrase))
        check(tmp.renameTo(blobFile)) { "Could not store the database key" }
        return passphrase
    }

    private fun wrap(key: SecretKey, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        return ByteBuffer.allocate(1 + iv.size + ciphertext.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(ciphertext)
            .array()
    }

    private fun unwrap(key: SecretKey, blob: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(blob)
        val iv = ByteArray(buffer.get().toInt()).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    companion object {
        const val BLOB_FILE_NAME = "spendless_db_key.bin"
        private const val KEY_ALIAS = "spendless_db_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PASSPHRASE_BYTES = 32
        private const val KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
    }
}
