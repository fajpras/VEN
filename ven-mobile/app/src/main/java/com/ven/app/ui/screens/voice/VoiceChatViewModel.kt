package com.ven.app.ui.screens.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.data.api.ApiUndangan
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Orkestrator sisi UI untuk voice chat ruangan privat: mengambil token dari
 * backend lalu memberi tahu layar untuk menyalakan [VoiceChatService] dan
 * membuka ruangan 3D. Koneksi voice sendiri dimiliki service agar tetap
 * hidup ketika pengguna masuk ke proses Unity.
 */
class VoiceChatViewModel(
    private val penyediaToken: PenyediaTokenVoice = PenyediaTokenVoice { ApiUndangan.voiceToken(it) },
) : ViewModel() {

    private val _acara = MutableSharedFlow<VoiceAcara>(extraBufferCapacity = 1)
    val acara: SharedFlow<VoiceAcara> = _acara

    /** Status koneksi yang diperbarui oleh VoiceChatService. */
    val uiState: StateFlow<VoiceUiState> = VoiceSesiStatus.state

    fun mulaiSesi(idRuangan: Int, exhibitId: String? = null) {
        VoiceSesiStatus.menyiapkan()

        viewModelScope.launch {
            val hasil = penyediaToken.token(idRuangan)

            val peristiwa = hasil.fold(
                onSuccess = { VoiceAcara.SiapGabung(it, idRuangan, exhibitId) },
                onFailure = { VoiceAcara.Gagal(it.message ?: PESAN_GAGAL_BAWAAN) },
            )

            if (peristiwa is VoiceAcara.Gagal) {
                VoiceSesiStatus.gagal(peristiwa.pesan)
            }

            _acara.emit(peristiwa)
        }
    }

    private companion object {
        const val PESAN_GAGAL_BAWAAN = "Gagal menyiapkan voice chat"
    }
}
