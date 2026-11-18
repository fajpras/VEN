package com.ven.app.data.api

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.ven.app.data.api.models.UserModel
import com.ven.app.util.SecurePrefs

class SessionManager(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveToken(token: String?) {
        preferences.edit().putString(KEY_TOKEN, encryptForStorage(token)).apply()
    }

    fun getToken(): String? {
        val stored = preferences.getString(KEY_TOKEN, null) ?: return null
        val decrypted = SecurePrefs.decrypt(stored)
        if (decrypted != null) return decrypted

        // Nilai legacy tersimpan plaintext sebelum enkripsi diaktifkan:
        // kembalikan dan migrasi diam-diam ke bentuk terenkripsi.
        val migrated = SecurePrefs.encrypt(stored)
        if (migrated != null) {
            preferences.edit().putString(KEY_TOKEN, migrated).apply()
            return stored
        }
        return null
    }

    fun saveUser(user: UserModel?) {
        val userJson = if (user != null) gson.toJson(user) else null
        preferences.edit().putString(KEY_USER, encryptForStorage(userJson)).apply()
    }

    /**
     * Menyimpan data pengguna terbaru tanpa menghilangkan field yang tidak
     * dikirim oleh endpoint profil (mis. role dan status onboarding).
     */
    fun updateUser(user: UserModel?) {
        if (user == null) return

        val lama = getUser()
        saveUser(
            user.copy(
                role = user.role ?: lama?.role,
                onboardingSelesai = user.onboardingSelesai ?: lama?.onboardingSelesai,
            ),
        )
    }

    fun getUser(): UserModel? {
        val stored = preferences.getString(KEY_USER, null) ?: return null
        val userJson = SecurePrefs.decrypt(stored) ?: stored
        return try {
            gson.fromJson(userJson, UserModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clearSession() {
        preferences.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    /**
     * Mengenkripsi nilai sebelum disimpan. Enkripsi bersifat fail-closed:
     * bila keystore gagal, nilai tidak disimpan agar token tidak pernah
     * muncul sebagai plaintext di disk.
     */
    private fun encryptForStorage(value: String?): String? {
        if (value.isNullOrBlank()) return value
        return SecurePrefs.encrypt(value)
    }

    companion object {
        private const val PREF_NAME = "ven_auth_pref"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "auth_user"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
