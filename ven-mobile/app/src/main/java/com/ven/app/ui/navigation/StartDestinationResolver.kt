package com.ven.app.ui.navigation

/**
 * Menentukan tujuan setelah splash selesai.
 *
 * Aturan:
 * - Mode debug "show explore" selalu membuka Home.
 * - Pengguna yang sudah punya token langsung ke Home; intro pra-login dilewati
 *   agar pengguna lama tidak melihat pengenalan yang tidak relevan.
 * - Pengguna yang belum login melihat intro sekali, lalu Login.
 */
object StartDestinationResolver {

    fun resolve(
        showExplore: Boolean,
        isLoggedIn: Boolean,
        introDone: Boolean,
    ): Screen {
        return when {
            showExplore -> Screen.Home
            isLoggedIn -> Screen.Home
            !introDone -> Screen.Intro
            else -> Screen.Login
        }
    }
}
