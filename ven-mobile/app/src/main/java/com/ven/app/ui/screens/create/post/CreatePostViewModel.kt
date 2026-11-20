package com.ven.app.ui.screens.create.post

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.R
import com.ven.app.data.api.ApiKarya
import com.ven.app.data.api.ApiProfil
import com.ven.app.data.api.models.KaryaItemModel
import com.ven.app.data.api.models.KategoriModel
import com.ven.app.ui.components.ui.CreateContentType
import java.io.File
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Data yang dibutuhkan untuk mengunggah satu karya baru. */
data class UnggahanKarya(
    val file: File,
    val mimeType: String,
    val idKategori: Int,
    val judul: String,
    val jenis: String,
    val deskripsi: String,
)

sealed interface CreatePostEvent {
    data object PostSuccess : CreatePostEvent
    data object SwitchToExhibit : CreatePostEvent
}

class CreatePostViewModel(
    private val muatKategoriApi: suspend () -> Result<List<KategoriModel>> = { ApiProfil.kategori() },
    private val unggahKarya: suspend (UnggahanKarya) -> Result<KaryaItemModel> = { unggahan ->
        ApiKarya.unggah(
            file = unggahan.file,
            mimeType = unggahan.mimeType,
            idKategori = unggahan.idKategori,
            judul = unggahan.judul,
            jenis = unggahan.jenis,
            deskripsi = unggahan.deskripsi,
        )
    },
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState(isLoadingCategories = true))
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreatePostEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        muatKategori()
    }

    fun muatKategori() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true, errorMessageRes = null) }
            muatKategoriApi()
                .onSuccess { daftar ->
                    val kategoriPertama = daftar.firstOrNull()
                    _uiState.update {
                        it.copy(
                            categories = daftar,
                            categoryId = it.categoryId ?: kategoriPertama?.idKategori,
                            isLoadingCategories = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoadingCategories = false,
                            errorMessageRes = R.string.create_post_categories_error,
                        )
                    }
                }
        }
    }

    fun onMediaSelected(uriString: String, mimeType: String?) {
        val jenis = if (mimeType?.startsWith("video/") == true) {
            CreatePostJenis.Video
        } else {
            CreatePostJenis.Poster
        }

        _uiState.update {
            it.copy(
                mediaUriString = uriString,
                mediaMimeType = mimeType,
                mediaJenis = jenis,
                errorMessageRes = null,
                serverErrorMessage = null,
            )
        }
    }

    fun onMediaDitolak(@StringRes pesanRes: Int) {
        _uiState.update { it.copy(errorMessageRes = pesanRes) }
    }

    fun onNextClick() {
        if (!_uiState.value.isMediaSelected) {
            _uiState.update { it.copy(errorMessageRes = R.string.create_post_media_required) }
            return
        }

        _uiState.update {
            it.copy(step = CreatePostStep.Details, errorMessageRes = null)
        }
    }

    /** @return true jika tombol kembali ditangani di dalam ViewModel. */
    fun onBackClick(): Boolean {
        return if (_uiState.value.step == CreatePostStep.Details) {
            _uiState.update { it.copy(step = CreatePostStep.MediaPicker, errorMessageRes = null) }
            true
        } else {
            false
        }
    }

    fun onContentTypeSelected(type: CreateContentType) {
        _uiState.update { it.copy(selectedContentType = type) }
        if (type == CreateContentType.Exhibit) {
            viewModelScope.launch {
                _events.send(CreatePostEvent.SwitchToExhibit)
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title, errorMessageRes = null) }
    }

    fun onCategoryChange(idKategori: Int) {
        _uiState.update { it.copy(categoryId = idKategori, errorMessageRes = null) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description, errorMessageRes = null) }
    }

    fun submitPost(file: File, mimeType: String) {
        val state = _uiState.value
        if (state.isSubmitting) return

        if (!state.isMediaSelected || state.categoryId == null || state.title.isBlank() || state.description.isBlank()) {
            _uiState.update { it.copy(errorMessageRes = R.string.create_post_form_required) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessageRes = null, serverErrorMessage = null) }

            val hasil = unggahKarya(
                UnggahanKarya(
                    file = file,
                    mimeType = mimeType,
                    idKategori = state.categoryId,
                    judul = state.title.trim(),
                    jenis = state.mediaJenis.nilaiApi,
                    deskripsi = state.description.trim(),
                ),
            )

            hasil
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(CreatePostEvent.PostSuccess)
                }
                .onFailure { kesalahan ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            serverErrorMessage = kesalahan.message,
                        )
                    }
                }
        }
    }
}
