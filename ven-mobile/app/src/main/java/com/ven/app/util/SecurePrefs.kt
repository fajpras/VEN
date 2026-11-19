package com.ven.app.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Enkripsi AES-256-GCM berbasis AndroidKeyStore untuk nilai sensitif yang
 * disimpan di SharedPreferences (token sesi, data pengguna).
 *
 * Format payload tersimpan: `base64(iv):base64(ciphertext)` sehingga nilai
 * plaintext lama mudah dibedakan dan dimigrasi saat pertama kali dibaca.
 * Kunci tidak pernah keluar dari Keystore dan tidak boleh dicatat ke log.
 */
object SecurePrefs {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ven_session_key"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SEPARATOR = ":"

    fun encrypt(value: String): String? {
        if (value.isBlank()) return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, obtainKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            listOf(
                Base64.encodeToString(iv, Base64.NO_WRAP),
                Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ).joinToString(SEPARATOR)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Mengembalikan null bila payload bukan ciphertext valid — pemanggil bisa
     * memperlakukan nilai tersebut sebagai data legacy yang belum terenkripsi.
     */
    fun decrypt(payload: String): String? {
        if (payload.isBlank()) return null
        val parts = payload.split(SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        return try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, obtainKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun obtainKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generator.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
}
