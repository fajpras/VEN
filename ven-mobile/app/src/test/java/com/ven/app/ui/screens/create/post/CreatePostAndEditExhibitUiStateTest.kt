package com.ven.app.ui.screens.create.post

import com.ven.app.data.api.models.KategoriModel
import com.ven.app.data.api.models.PameranItemModel
import com.ven.app.ui.screens.create.exhibit.edit.EditExhibitUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatePostAndEditExhibitUiStateTest {

    private val sampleCategories = listOf(
        KategoriModel(idKategori = 1, nama = "3D"),
        KategoriModel(idKategori = 2, nama = "UI/UX"),
    )

    private val sampleExhibitions = listOf(
        PameranItemModel(idPameran = "01a21286-0010-7000-8000-000000000010", judul = "Pameran Digital 2026", tipe = "kecil"),
        PameranItemModel(idPameran = "01a21286-0020-7000-8000-000000000020", judul = "Galeri Animasi", tipe = "sedang"),
    )

    @Test
    fun `create post wajib memilih pameran sebelum lanjut ke penempatan 3D`() {
        val withoutExhibit = CreatePostUiState(
            step = CreatePostStep.Details,
            mediaUriString = "content://poster.jpg",
            categories = sampleCategories,
            categoryId = 1,
            exhibitions = sampleExhibitions,
            selectedExhibitionId = null,
            title = "Poster Baru",
            description = "Karya siap dipajang",
        )
        assertNull(withoutExhibit.selectedExhibitionName)
        assertFalse(withoutExhibit.isPostEnabled)

        val linked = withoutExhibit.copy(selectedExhibitionId = "01a21286-0020-7000-8000-000000000020")
        assertEquals("Galeri Animasi", linked.selectedExhibitionName)
        assertTrue(linked.isPostEnabled)
    }

    @Test
    fun `edit post hanya memerlukan media dan judul tanpa wajib deskripsi atau pameran`() {
        val state = CreatePostUiState(
            editingPostId = "uuid-karya-1",
            step = CreatePostStep.Details,
            mediaUriString = null,
            existingMediaUrl = "https://ven.test/storage/karya/poster.jpg",
            categories = sampleCategories,
            categoryId = null,
            exhibitions = emptyList(),
            selectedExhibitionId = null,
            title = "Poster Diperbarui",
            description = "",
        )
        assertTrue(state.isEditMode)
        assertTrue(state.hasMedia)
        assertTrue(state.isPostEnabled)
        assertFalse(state.copy(title = "   ").isPostEnabled)
    }

    @Test
    fun `edit exhibit validasi judul dan pemetaan template Unity`() {
        val emptyTitle = EditExhibitUiState(exhibitId = "01a21286-0005-7000-8000-000000000005", title = "   ", tipe = "sedang", isLoading = false)
        assertFalse(emptyTitle.canSave)

        val valid = EditExhibitUiState(exhibitId = "01a21286-0005-7000-8000-000000000005", title = "Pameran Utama", tipe = "besar", isLoading = false)
        assertTrue(valid.canSave)
        assertEquals("template03", valid.unityTemplateId)
    }
}
