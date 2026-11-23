package com.ven.app.data.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Menyimpan status pengenalan aplikasi (intro pra-login).
 *
 * Flag ini bersifat per perangkat dan sengaja terpisah dari
 * [com.ven.app.data.api.models.UserModel.onboardingSelesai] yang merupakan
 * status pemilihan minat milik akun.
 */
class OnboardingPreferences private constructor(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _introDone = MutableStateFlow(isIntroDone())
    val introDone: StateFlow<Boolean> = _introDone.asStateFlow()

    fun isIntroDone(): Boolean = preferences.getBoolean(KEY_INTRO_DONE, false)

    fun markIntroDone() {
        if (isIntroDone()) return
        preferences.edit().putBoolean(KEY_INTRO_DONE, true).apply()
        _introDone.value = true
    }

    companion object {
        private const val PREF_NAME = "ven_onboarding_pref"
        private const val KEY_INTRO_DONE = "key_intro_done"

        @Volatile
        private var INSTANCE: OnboardingPreferences? = null

        fun getInstance(context: Context): OnboardingPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnboardingPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
