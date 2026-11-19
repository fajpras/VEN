package com.ven.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Menghubungkan [ChangeEmailScreen] dengan [ChangeEmailViewModel].
 * Setelah OTP terbit, memicu [onOtpSent] untuk berpindah ke layar verifikasi.
 */
@Composable
fun ChangeEmailRoute(
    viewModel: ChangeEmailViewModel,
    onBackClick: () -> Unit,
    onOtpSent: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isOtpSent) {
        if (state.isOtpSent) {
            viewModel.onOtpConsumed()
            onOtpSent()
        }
    }

    ChangeEmailScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onKataSandiChange,
        onConfirmPasswordChange = viewModel::onKonfirmasiChange,
        onBackClick = onBackClick,
        onSendOtpClick = viewModel::kirimOtp,
    )
}
