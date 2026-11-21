package com.ven.app.ui.screens.voice

import androidx.compose.runtime.Immutable
import com.ven.app.data.api.models.VoiceTokenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Kondisi koneksi voice chat ruangan privat. */
@Immutable
enum class VoiceSesi {
    /** Tidak ada sesi voice yang berjalan. */
    Jeda,

    /** Token diambil / koneksi LiveKit sedang dibuat. */
    Menyiapkan,

    /** Voice chat aktif dan mikrofon diterbitkan. */
    Tersambung,

    /** Koneksi gagal atau sesi dihentikan karena galat. */
    Gagal,
}

@Immutable
data class VoiceUiState(
    val sesi: VoiceSesi = VoiceSesi.Jeda,
    val micAktif: Boolean = true,
    val jumlahPeserta: Int = 1,
    val pesanGalat: String? = null,
)

/** Peristiwa voice chat yang dikonsumsi layar (UDF: satu arah, dari ViewModel ke UI). */
sealed interface VoiceAcara {
    data class SiapGabung(
        val token: VoiceTokenModel,
        val idRuangan: Int,
        val exhibitId: String?,
    ) : VoiceAcara

    data class Gagal(val pesan: String) : VoiceAcara
}

/** Penyedia token voice; diabstraksi agar ViewModel dapat diuji tanpa Retrofit. */
fun interface PenyediaTokenVoice {
    suspend fun token(idRuangan: Int): Result<VoiceTokenModel>
}

/**
 * Keadaan sesi voice yang dibagi antara [VoiceChatService] (pemilik koneksi)
 * dan UI. Disimpan sebagai singleton object karena service dan Compose UI
 * berada di siklus hidup yang berbeda.
 */
object VoiceSesiStatus {

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    fun menyiapkan() {
        _state.update { it.copy(sesi = VoiceSesi.Menyiapkan, pesanGalat = null) }
    }

    fun tersambung() {
        _state.update { it.copy(sesi = VoiceSesi.Tersambung, pesanGalat = null) }
    }

    fun gagal(pesan: String) {
        _state.update { it.copy(sesi = VoiceSesi.Gagal, pesanGalat = pesan) }
    }

    fun setMicAktif(aktif: Boolean) {
        _state.update { it.copy(micAktif = aktif) }
    }

    fun setJumlahPeserta(jumlah: Int) {
        _state.update { it.copy(jumlahPeserta = jumlah) }
    }

    fun reset() {
        _state.value = VoiceUiState()
    }
}
