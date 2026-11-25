package com.ven.app.ui.screens.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangePasswordViewModelTest {

    @Test
    fun `form tidak valid jika kata sandi baru lemah`() {
        val state = ChangePasswordUiState(
            kataSandiSaatIni = "Password.123",
            kataSandi = "lemah123",
            konfirmasiKataSandi = "lemah123",
        )
        assertFalse(state.isPasswordStrong)
        assertFalse(state.isFormValid)
    }

    @Test
    fun `form tidak valid jika kata sandi baru sama dengan kata sandi saat ini`() {
        val state = ChangePasswordUiState(
            kataSandiSaatIni = "Password.123",
            kataSandi = "Password.123",
            konfirmasiKataSandi = "Password.123",
        )
        assertTrue(state.isSameAsCurrent)
        assertFalse(state.isFormValid)
    }

    @Test
    fun `form tidak valid jika konfirmasi kata sandi tidak cocok`() {
        val state = ChangePasswordUiState(
            kataSandiSaatIni = "Password.123",
            kataSandi = "Bangpras.07",
            konfirmasiKataSandi = "Bangpras.08",
        )
        assertFalse(state.isPasswordMatch)
        assertFalse(state.isFormValid)
    }

    @Test
    fun `form valid jika semua syarat kata sandi terpenuhi`() {
        val state = ChangePasswordUiState(
            kataSandiSaatIni = "Password.123",
            kataSandi = "Bangpras.07",
            konfirmasiKataSandi = "Bangpras.07",
        )
        assertTrue(state.isPasswordStrong)
        assertFalse(state.isSameAsCurrent)
        assertTrue(state.isPasswordMatch)
        assertTrue(state.isFormValid)
    }
}
