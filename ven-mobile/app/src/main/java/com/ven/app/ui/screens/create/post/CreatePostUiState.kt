package com.ven.app.ui.screens.create.post

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ven.app.data.api.models.KategoriModel
import com.ven.app.data.api.models.PameranItemModel
import com.ven.app.ui.components.ui.CreateContentType

enum class CreatePostStep {
    MediaPicker, // Langkah 1: pilih media dari galeri & pratinjau
    Details,     // Langkah 2: formulir detail & pemilihan pameran wajib (untuk penempatan 3D)
}

enum class CreatePostJenis(val nilaiApi: String) {
    Poster("poster"),
    Video("video"),
}

/**
 * State UI untuk alur Create Post (wajib pilih Pameran → lanjut ke penempatan 3D) & Edit Post.
 *
 * Media disimpan sebagai string URI agar state tetap dapat diuji pada JVM
 * tanpa ketergantungan kelas Android.
 */
@Immutable
data class CreatePostUiState(
    val editingPostId: String? = null,
    val step: CreatePostStep = CreatePostStep.MediaPicker,
    val selectedContentType: CreateContentType = CreateContentType.Post,
    val mediaUriString: String? = null,
    val existingMediaUrl: String? = null,
    val mediaMimeType: String? = null,
    val mediaJenis: CreatePostJenis = CreatePostJenis.Poster,
    val title: String = "",
    val categories: List<KategoriModel> = emptyList(),
    val categoryId: Int? = null,
    val exhibitions: List<PameranItemModel> = emptyList(),
    val selectedExhibitionId: String? = null,
    val description: String = "",
    val isLoadingCategories: Boolean = false,
    val isLoadingExhibitions: Boolean = false,
    val isLoadingPost: Boolean = false,
    val isSubmitting: Boolean = false,
    @param:StringRes val errorMessageRes: Int? = null,
    val serverErrorMessage: String? = null,
) {
    val isEditMode: Boolean
        get() = editingPostId != null

    val selectedCategoryName: String
        get() = categories.firstOrNull { it.idKategori == categoryId }?.nama.orEmpty()

    val selectedExhibition: PameranItemModel?
        get() = exhibitions.firstOrNull { it.idPameran == selectedExhibitionId }

    val selectedExhibitionName: String?
        get() = selectedExhibition?.judul

    val isMediaSelected: Boolean
        get() = mediaUriString != null

    val hasMedia: Boolean
        get() = isMediaSelected || !existingMediaUrl.isNullOrBlank()

    /**
     * - Mode buat baru (Create Post): wajib media, judul, kategori, deskripsi, dan pameran.
     * - Mode edit (Edit Post): scope hanya media dan judul (posisi diatur di Editor 3D).
     */
    val isPostEnabled: Boolean
        get() = if (isEditMode) {
            hasMedia &&
                title.isNotBlank() &&
                title.length <= 100 &&
                !isSubmitting &&
                !isLoadingPost
        } else {
            hasMedia &&
                title.isNotBlank() &&
                categoryId != null &&
                description.isNotBlank() &&
                selectedExhibitionId != null &&
                !isSubmitting &&
                !isLoadingPost
        }
}
