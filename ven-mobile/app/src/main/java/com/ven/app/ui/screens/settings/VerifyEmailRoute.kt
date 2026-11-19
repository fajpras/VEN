package com.ven.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Menghubungkan [VerifyEmailScreen] dengan [ChangeEmailViewModel] yang dibagikan
 * dengan [ChangeEmailRoute]. Setelah verifikasi sukses, memicu [onVerified].
 */
@Composable
fun VerifyEmailRoute(
    viewModel: ChangeEmailViewModel,
    onBackClick: () -> Unit,
    onVerified: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onVerified()
        }
    }

    VerifyEmailScreen(
        email = state.emailBaru,
        state = state,
        onOtpChange = viewModel::onOtpChange,
        onVerifyClick = viewModel::gantiEmail,
        onResendClick = viewModel::kirimUlangOtp,
        onBackClick = onBackClick,
    )
}
