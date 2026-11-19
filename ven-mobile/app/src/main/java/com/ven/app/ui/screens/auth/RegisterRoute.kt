package com.ven.app.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Penghubung antara RegisterViewModel dan RegisterScreen (stateless). */
@Composable
fun RegisterRoute(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RegisterEvent.NavigateToOnboarding -> onNavigateToOnboarding()
                RegisterEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    if (state.isOtpDialogOpen) {
        VerifyOtpScreen(
            state = ForgotPasswordUiState(
                email = state.email,
                otp = state.otp,
                isLoading = state.isLoading,
                resendCooldown = state.resendCooldown,
                errorMessage = state.errorMessage,
            ),
            onOtpChange = viewModel::onOtpChange,
            onVerifyClick = { viewModel.submitRegisterWithOtp(context) },
            onResendClick = viewModel::resendOtp,
            onBackClick = viewModel::dismissOtpDialog,
        )
        return
    }

    RegisterScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onRegisterClick = viewModel::requestOtpAndOpenDialog,
        onGoogleClick = { viewModel.daftarDenganGoogle(context) },
        onLoginClick = onNavigateToLogin,
    )
}
