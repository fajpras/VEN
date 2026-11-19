package com.ven.app.ui.screens.profile

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.data.api.ApiClient
import com.ven.app.data.api.ApiProfil
import com.ven.app.ui.components.feature.MediaGridItem
import com.ven.app.ui.screens.explore.PostsFeedItem
import com.ven.app.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Memuat data profil pemilik akun beserta minat dan jumlah karyanya.
 */
class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    fun muat() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = it.profile == null, errorMessage = null)
            }

            val profil = ApiProfil.profil()
            if (profil.isFailure) {
                tampilkanGagal(profil.exceptionOrNull()?.message ?: "Gagal memuat profil")
                return@launch
            }

            val minat = ApiProfil.minat()
            if (minat.isFailure) {
                tampilkanGagal(minat.exceptionOrNull()?.message ?: "Gagal memuat minat")
                return@launch
            }

            val pengguna = profil.getOrThrow()
            val profile = Profile(
                username = pengguna.namaPanggilan.orEmpty(),
                fullName = pengguna.nama.orEmpty(),
                postCount = ApiProfil.jumlahKarya().getOrDefault(0),
                exhibitCount = ApiProfil.jumlahPameran().getOrDefault(0),
                skills = minat.getOrThrow().map { Skill(it.nama) },
                bio = pengguna.deskripsi.orEmpty(),
                email = pengguna.email.orEmpty(),
                phone = pengguna.nomorHp.orEmpty(),
            )

            val rawKarya = ApiProfil.listKaryaMilikku().getOrDefault(emptyList())
            val rawPameran = ApiProfil.listPameranMilikku().getOrDefault(emptyList())

            val posts = rawKarya.map { k ->
                MediaGridItem(
                    id = k.idKarya,
                    title = k.judul,
                    imageUrl = k.fileUrl ?: k.file,
                )
            }
            val exhibits = rawPameran.map { p ->
                MediaGridItem(
                    id = p.idPameran.toString(),
                    title = p.judul,
                    imageUrl = p.banner,
                )
            }
            val authorName = pengguna.namaPanggilan.orEmpty().ifBlank { pengguna.nama.orEmpty() }
            val feedPosts = rawKarya.map { k ->
                PostsFeedItem(
                    id = k.idKarya,
                    username = if (authorName.startsWith("@")) authorName else "@$authorName",
                    userId = pengguna.idPengguna,
                    userAvatarUrl = pengguna.fotoProfil,
                    title = k.judul,
                    category = k.kategori?.nama,
                    timestampText = DateFormatter.formatRelativeTime(k.createdAt),
                    description = k.deskripsi ?: "",
                    imageUrl = k.fileUrl ?: k.file,
                    pameranId = k.idPameran?.toString(),
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    profile = profile,
                    posts = posts,
                    exhibits = exhibits,
                    feedPosts = feedPosts,
                    errorMessage = null,
                )
            }

            muatAvatar(pengguna.fotoProfil)
        }
    }

    fun pilihTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun aturSkillSheet(buka: Boolean) {
        _uiState.update { it.copy(isSkillSheetOpen = buka) }
    }

    private fun tampilkanGagal(pesan: String) {
        _uiState.update {
            if (it.profile != null) {
                it.copy(isLoading = false, errorMessage = null)
            } else {
                it.copy(isLoading = false, errorMessage = pesan)
            }
        }
    }

    private suspend fun muatAvatar(url: String?) {
        if (url.isNullOrBlank()) return

        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val koneksi = URL(normalisasiUrl(url)).openConnection() as HttpURLConnection
                koneksi.connectTimeout = 5000
                koneksi.readTimeout = 5000
                koneksi.inputStream.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()
        }

        _uiState.update { it.copy(avatarBitmap = bitmap) }
    }

    /**
     * URL foto bisa dibuat dari host request (mis. 127.0.0.1 saat diunggah
     * lewat curl). Dari emulator/perangkat, host tersebut harus mengikuti
     * alamat API yang sedang dipakai aplikasi.
     */
    private fun normalisasiUrl(url: String): String {
        val asal = runCatching { URI(url) }.getOrNull() ?: return url
        if (asal.host != "127.0.0.1" && asal.host != "localhost") return url

        val dasar = runCatching { URI(ApiClient.getBaseUrl()) }.getOrNull() ?: return url
        val host = dasar.host ?: return url

        return runCatching {
            URI(
                asal.scheme ?: dasar.scheme,
                null,
                host,
                if (dasar.port > 0) dasar.port else -1,
                asal.path,
                asal.query,
                asal.fragment,
            ).toString()
        }.getOrDefault(url)
    }
}
