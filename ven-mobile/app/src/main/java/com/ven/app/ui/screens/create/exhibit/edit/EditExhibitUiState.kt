package com.ven.app.ui.screens.create.exhibit.edit

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * State UI untuk layar Edit Pameran (Info, Banner, Warna Dinding, Ukuran Ruang).
 */
@Immutable
data class EditExhibitUiState(
    val exhibitId: String = "",
    val title: String = "",
    val tipe: String = "kecil",
    val wallColorHex: String = "#FFFFFF",
    val existingBannerUrl: String? = null,
    val newBannerUriString: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    @param:StringRes val errorMessageRes: Int? = null,
    val serverErrorMessage: String? = null,
) {
    val canSave: Boolean
        get() = title.isNotBlank() && title.length <= 100 && !isLoading && !isSaving

    val unityTemplateId: String
        get() = when (tipe.trim().lowercase()) {
            "sedang", "medium" -> "template02"
            "besar", "big" -> "template03"
            "acara", "event" -> "template04"
            else -> "template01"
        }
}
