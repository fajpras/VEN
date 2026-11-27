package com.ven.app.ui.screens.create.post

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.R
import com.ven.app.data.api.ApiKarya
import com.ven.app.data.api.ApiPameran
import com.ven.app.data.api.ApiProfil
import com.ven.app.data.api.models.KaryaItemModel
import com.ven.app.data.api.models.KategoriModel
import com.ven.app.data.api.models.PameranItemModel
import com.ven.app.ui.components.ui.CreateContentType
import com.ven.app.util.PendingArtworkPlacement
import com.ven.app.util.UnityLauncher
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
    val idPameran: String? = null,
)

/** Data yang dibutuhkan untuk memperbarui karya yang sudah ada. */
data class PembaruanKarya(
    val idKarya: String,
    val idKategori: Int,
    val judul: String,
    val deskripsi: String,
    val idPameran: String? = null,
    val jenis: String? = null,
    val file: File? = null,
    val mimeType: String? = null,
)

sealed interface CreatePostEvent {
    data object PostSuccess : CreatePostEvent
    data class PlaceInExhibition(
        val exhibitId: String,
        val templateId: String,
        val exhibitTitle: String,
        val tipe: String,
        val placement: PendingArtworkPlacement,
    ) : CreatePostEvent
    data object SwitchToExhibit : CreatePostEvent
}

class CreatePostViewModel(
    private val muatKategoriApi: suspend () -> Result<List<KategoriModel>> = { ApiProfil.kategori() },
    private val muatPameranApi: suspend () -> Result<List<PameranItemModel>> = { ApiPameran.getMyExhibitions() },
    private val muatDetailKaryaApi: suspend (String) -> Result<KaryaItemModel> = { id -> ApiPameran.getKarya(id) },
    private val unggahKarya: suspend (UnggahanKarya) -> Result<KaryaItemModel> = { unggahan ->
        ApiKarya.unggah(
            file = unggahan.file,
            mimeType = unggahan.mimeType,
            idKategori = unggahan.idKategori,
            judul = unggahan.judul,
            jenis = unggahan.jenis,
            deskripsi = unggahan.deskripsi,
            idPameran = unggahan.idPameran,
        )
    },
    private val perbaruiKarya: suspend (PembaruanKarya) -> Result<KaryaItemModel> = { pembaruan ->
        ApiKarya.perbarui(
            idKarya = pembaruan.idKarya,
            idKategori = pembaruan.idKategori,
            judul = pembaruan.judul,
            deskripsi = pembaruan.deskripsi,
            idPameran = pembaruan.idPameran,
            jenis = pembaruan.jenis,
            file = pembaruan.file,
            mimeType = pembaruan.mimeType,
        )
    },
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CreatePostUiState(isLoadingCategories = true, isLoadingExhibitions = true),
    )
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreatePostEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        muatKategori()
        muatPameran()
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

    fun muatPameran() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExhibitions = true) }
            muatPameranApi()
                .onSuccess { daftar ->
                    _uiState.update {
                        val defaultSelection = if (!it.isEditMode && it.selectedExhibitionId == null) {
                            daftar.firstOrNull()?.idPameran
                        } else {
                            it.selectedExhibitionId
                        }
                        it.copy(
                            exhibitions = daftar,
                            selectedExhibitionId = defaultSelection,
                            isLoadingExhibitions = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoadingExhibitions = false)
                    }
                }
        }
    }

    fun muatKaryaUntukEdit(postId: String) {
        if (postId.isBlank()) return
        if (_uiState.value.editingPostId == postId && !_uiState.value.isLoadingPost) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    editingPostId = postId,
                    step = CreatePostStep.Details,
                    isLoadingPost = true,
                    errorMessageRes = null,
                    serverErrorMessage = null,
                )
            }

            muatDetailKaryaApi(postId)
                .onSuccess { karya ->
                    val jenis = if (karya.jenis.equals("video", ignoreCase = true)) {
                        CreatePostJenis.Video
                    } else {
                        CreatePostJenis.Poster
                    }
                    _uiState.update {
                        it.copy(
                            title = karya.judul,
                            categoryId = karya.idKategori ?: karya.kategori?.idKategori ?: it.categoryId,
                            selectedExhibitionId = karya.idPameran ?: karya.pameran?.idPameran,
                            description = karya.deskripsi.orEmpty(),
                            existingMediaUrl = karya.fileUrl ?: karya.file,
                            mediaJenis = jenis,
                            isLoadingPost = false,
                        )
                    }
                }
                .onFailure { kesalahan ->
                    _uiState.update {
                        it.copy(
                            isLoadingPost = false,
                            errorMessageRes = R.string.edit_post_load_error,
                            serverErrorMessage = kesalahan.message,
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
        val state = _uiState.value
        if (state.isEditMode) {
            return false
        }
        return if (state.step == CreatePostStep.Details) {
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

    fun onExhibitionChange(idPameran: String?) {
        _uiState.update { it.copy(selectedExhibitionId = idPameran, errorMessageRes = null) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description, errorMessageRes = null) }
    }

    fun submitPost(file: File?, mimeType: String?) {
        val state = _uiState.value
        if (state.isSubmitting) return

        val categoryId = state.categoryId
        if (state.isEditMode) {
            if (!state.hasMedia || state.title.isBlank()) {
                _uiState.update { it.copy(errorMessageRes = R.string.create_post_form_required) }
                return
            }
        } else {
            if (!state.hasMedia || categoryId == null || state.title.isBlank() || state.description.isBlank()) {
                _uiState.update { it.copy(errorMessageRes = R.string.create_post_form_required) }
                return
            }
            if (file == null || mimeType == null) {
                _uiState.update { it.copy(errorMessageRes = R.string.create_post_media_required) }
                return
            }
            if (state.selectedExhibitionId == null) {
                _uiState.update { it.copy(errorMessageRes = R.string.create_post_exhibition_required) }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessageRes = null, serverErrorMessage = null) }

            val hasil = if (state.isEditMode) {
                perbaruiKarya(
                    PembaruanKarya(
                        idKarya = state.editingPostId!!,
                        idKategori = categoryId ?: state.categories.firstOrNull()?.idKategori ?: 1,
                        judul = state.title.trim(),
                        deskripsi = state.description.trim(),
                        idPameran = state.selectedExhibitionId,
                        jenis = if (file != null) state.mediaJenis.nilaiApi else null,
                        file = file,
                        mimeType = mimeType,
                    ),
                )
            } else {
                unggahKarya(
                    UnggahanKarya(
                        file = file!!,
                        mimeType = mimeType!!,
                        idKategori = categoryId!!,
                        judul = state.title.trim(),
                        jenis = state.mediaJenis.nilaiApi,
                        deskripsi = state.description.trim(),
                        idPameran = state.selectedExhibitionId,
                    ),
                )
            }

            hasil
                .onSuccess { karya ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    val selectedExhibit = state.selectedExhibition
                    if (!state.isEditMode && selectedExhibit != null) {
                        val tipe = selectedExhibit.tipe?.ifBlank { "kecil" } ?: "kecil"
                        _events.send(
                            CreatePostEvent.PlaceInExhibition(
                                exhibitId = selectedExhibit.idPameran,
                                templateId = UnityLauncher.mapTemplateId(tipe),
                                exhibitTitle = selectedExhibit.judul,
                                tipe = tipe,
                                placement = PendingArtworkPlacement(
                                    karyaId = karya.idKarya,
                                    title = karya.judul,
                                    description = karya.deskripsi.orEmpty(),
                                    contentUrl = karya.fileUrl ?: karya.file.orEmpty(),
                                    categoryId = karya.idKategori ?: categoryId ?: 1,
                                    isVideo = state.mediaJenis == CreatePostJenis.Video,
                                ),
                            ),
                        )
                    } else {
                        _events.send(CreatePostEvent.PostSuccess)
                    }
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
