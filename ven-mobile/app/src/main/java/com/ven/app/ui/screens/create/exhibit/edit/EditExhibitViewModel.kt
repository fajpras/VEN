package com.ven.app.ui.screens.create.exhibit.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.R
import com.ven.app.data.api.ApiPameran
import com.ven.app.data.api.models.PameranItemModel
import java.io.File
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PembaruanPameran(
    val id: String,
    val judul: String,
    val tipe: String,
    val warna: String,
    val bannerFile: File? = null,
    val bannerMimeType: String = "image/jpeg",
)

sealed interface EditExhibitEvent {
    data object SaveSuccess : EditExhibitEvent
}

class EditExhibitViewModel(
    private val muatDetailApi: suspend (String) -> Result<PameranItemModel> = { id -> ApiPameran.getPameran(id) },
    private val perbaruiPameranApi: suspend (PembaruanPameran) -> Result<PameranItemModel> = { data ->
        ApiPameran.updatePameran(
            id = data.id,
            judul = data.judul,
            tipe = data.tipe,
            warna = data.warna,
            bannerFile = data.bannerFile,
            bannerMimeType = data.bannerMimeType,
        )
    },
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditExhibitUiState())
    val uiState: StateFlow<EditExhibitUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditExhibitEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun muat(exhibitId: String) {
        if (exhibitId.isBlank()) return
        if (_uiState.value.exhibitId == exhibitId && !_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    exhibitId = exhibitId,
                    isLoading = true,
                    errorMessageRes = null,
                    serverErrorMessage = null,
                )
            }

            muatDetailApi(exhibitId)
                .onSuccess { pameran ->
                    _uiState.update {
                        it.copy(
                            exhibitId = pameran.idPameran,
                            title = pameran.judul,
                            tipe = pameran.tipe?.ifBlank { "kecil" } ?: "kecil",
                            wallColorHex = pameran.warna?.ifBlank { "#FFFFFF" } ?: "#FFFFFF",
                            existingBannerUrl = pameran.banner,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { kesalahan ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = R.string.edit_exhibit_load_error,
                            serverErrorMessage = kesalahan.message,
                        )
                    }
                }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title.take(100), errorMessageRes = null, serverErrorMessage = null) }
    }

    fun onTipeChange(tipe: String) {
        _uiState.update { it.copy(tipe = tipe, errorMessageRes = null) }
    }

    fun onWallColorChange(hex: String) {
        _uiState.update { it.copy(wallColorHex = hex, errorMessageRes = null) }
    }

    fun onBannerSelected(uriString: String?) {
        _uiState.update { it.copy(newBannerUriString = uriString, errorMessageRes = null) }
    }

    fun simpan(bannerFile: File? = null, bannerMimeType: String = "image/jpeg") {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return

        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessageRes = R.string.edit_exhibit_title_required) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessageRes = null, serverErrorMessage = null) }
            perbaruiPameranApi(
                PembaruanPameran(
                    id = state.exhibitId,
                    judul = state.title.trim(),
                    tipe = state.tipe,
                    warna = state.wallColorHex,
                    bannerFile = bannerFile,
                    bannerMimeType = bannerMimeType,
                ),
            )
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(EditExhibitEvent.SaveSuccess)
                }
                .onFailure { kesalahan ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            serverErrorMessage = kesalahan.message,
                        )
                    }
                }
        }
    }
}
