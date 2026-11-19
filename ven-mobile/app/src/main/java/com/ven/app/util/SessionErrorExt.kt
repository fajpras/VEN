package com.ven.app.util

/**
 * Mendeteksi apakah pesan galat dari API menandakan sesi/login telah berakhir.
 * Dipakai bersama oleh ViewModel yang perlu mengarahkan pengguna ke layar login.
 */
fun String?.isSessionExpired(): Boolean {
    val teks = this.orEmpty().lowercase()
    return "unauthenticated" in teks || "tidak terautentikasi" in teks ||
        "tidak_terautentikasi" in teks
}
