package com.t4kash.app.ui.session

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureTokenStore(
    private val preferences: SharedPreferences
) {
    fun save(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_ENCRYPTED_TOKEN, encrypted.toBase64())
            .putString(KEY_TOKEN_IV, cipher.iv.toBase64())
            .apply()
    }

    fun read(): String? {
        val encrypted = preferences.getString(KEY_ENCRYPTED_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val iv = preferences.getString(KEY_TOKEN_IV, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv.fromBase64())
            )
            cipher.doFinal(encrypted.fromBase64()).toString(Charsets.UTF_8)
        }.getOrElse {
            clear()
            null
        }
    }

    fun hasEncryptedToken(): Boolean {
        return preferences.contains(KEY_ENCRYPTED_TOKEN)
                && preferences.contains(KEY_TOKEN_IV)
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_ENCRYPTED_TOKEN)
            .remove(KEY_TOKEN_IV)
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private fun ByteArray.toBase64(): String {
        return Base64.encodeToString(this, Base64.NO_WRAP)
    }

    private fun String.fromBase64(): ByteArray {
        return Base64.decode(this, Base64.NO_WRAP)
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "t4kash_session_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val KEY_ENCRYPTED_TOKEN = "encrypted_token"
        const val KEY_TOKEN_IV = "token_iv"
    }
}
