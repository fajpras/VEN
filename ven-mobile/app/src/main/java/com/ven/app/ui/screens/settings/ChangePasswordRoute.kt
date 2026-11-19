package com.ven.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Menghubungkan [ChangePasswordScreen] dengan [ChangePasswordViewModel].
 */
@Composable
fun ChangePasswordRoute(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ChangePasswordViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSaved()
        }
    }

    ChangePasswordScreen(
        state = state,
        onCurrentPasswordChange = viewModel::onKataSandiSaatIniChange,
        onNewPasswordChange = viewModel::onKataSandiChange,
        onConfirmPasswordChange = viewModel::onKonfirmasiChange,
        onBackClick = onBackClick,
        onSubmitClick = viewModel::simpan,
    )
}
