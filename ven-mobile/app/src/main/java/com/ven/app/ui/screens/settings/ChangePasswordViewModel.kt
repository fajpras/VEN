package com.ven.app.ui.screens.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.R
import com.ven.app.core.localization.AppStrings
import com.ven.app.data.api.ApiProfil
import com.ven.app.ui.screens.auth.PasswordValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ChangePasswordUiState(
    val kataSandiSaatIni: String = "",
    val kataSandi: String = "",
    val konfirmasiKataSandi: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
) {
    val isPasswordStrong: Boolean get() = PasswordValidator.isStrong(kataSandi)

    val isSameAsCurrent: Boolean
        get() = kataSandiSaatIni.isNotEmpty() && kataSandi.isNotEmpty() && kataSandi == kataSandiSaatIni

    val isPasswordMatch: Boolean get() = kataSandi == konfirmasiKataSandi

    val isFormValid: Boolean
        get() = kataSandiSaatIni.isNotBlank() &&
                isPasswordStrong &&
                !isSameAsCurrent &&
                konfirmasiKataSandi.isNotEmpty() &&
                isPasswordMatch &&
                !isLoading
}

/**
 * Mengganti kata sandi pengguna melalui endpoint PUT /api/profil/kata-sandi.
 */
class ChangePasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onKataSandiSaatIniChange(value: String) =
        _uiState.update { it.copy(kataSandiSaatIni = value, errorMessage = null) }

    fun onKataSandiChange(value: String) =
        _uiState.update { it.copy(kataSandi = value, errorMessage = null) }

    fun onKonfirmasiChange(value: String) =
        _uiState.update { it.copy(konfirmasiKataSandi = value, errorMessage = null) }

    fun simpan() {
        val state = _uiState.value
        if (!state.isPasswordStrong) {
            _uiState.update {
                it.copy(errorMessage = AppStrings.get(R.string.settings_change_password_helper))
            }
            return
        }
        if (state.isSameAsCurrent) {
            _uiState.update {
                it.copy(errorMessage = AppStrings.get(R.string.settings_change_password_same_as_current))
            }
            return
        }
        if (!state.isPasswordMatch) {
            _uiState.update {
                it.copy(errorMessage = AppStrings.get(R.string.register_error_confirm))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = ApiProfil.updateKataSandi(
                kataSandiSaatIni = state.kataSandiSaatIni,
                kataSandi = state.kataSandi,
            )

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Gagal mengganti kata sandi",
                    )
                }
            }
        }
    }
}
