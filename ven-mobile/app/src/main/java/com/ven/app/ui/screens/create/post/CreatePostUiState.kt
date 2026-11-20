package com.ven.app.ui.screens.create.post

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ven.app.data.api.models.KategoriModel
import com.ven.app.ui.components.ui.CreateContentType

enum class CreatePostStep {
    MediaPicker, // Langkah 1: pilih media dari galeri & pratinjau besar
    Details,     // Langkah 2: formulir metadata (Judul, Kategori, Deskripsi)
}

enum class CreatePostJenis(val nilaiApi: String) {
    Poster("poster"),
    Video("video"),
}

/**
 * State UI untuk alur Create Post (Langkah 1 & Langkah 2).
 *
 * Media disimpan sebagai string URI agar state tetap dapat diuji pada JVM
 * tanpa ketergantungan kelas Android.
 */
@Immutable
data class CreatePostUiState(
    val step: CreatePostStep = CreatePostStep.MediaPicker,
    val selectedContentType: CreateContentType = CreateContentType.Post,
    val mediaUriString: String? = null,
    val mediaMimeType: String? = null,
    val mediaJenis: CreatePostJenis = CreatePostJenis.Poster,
    val title: String = "",
    val categories: List<KategoriModel> = emptyList(),
    val categoryId: Int? = null,
    val description: String = "",
    val isLoadingCategories: Boolean = false,
    val isSubmitting: Boolean = false,
    @param:StringRes val errorMessageRes: Int? = null,
    val serverErrorMessage: String? = null,
) {
    val selectedCategoryName: String
        get() = categories.firstOrNull { it.idKategori == categoryId }?.nama.orEmpty()

    val isMediaSelected: Boolean
        get() = mediaUriString != null

    val isPostEnabled: Boolean
        get() = isMediaSelected &&
            title.isNotBlank() &&
            categoryId != null &&
            description.isNotBlank() &&
            !isSubmitting
}
