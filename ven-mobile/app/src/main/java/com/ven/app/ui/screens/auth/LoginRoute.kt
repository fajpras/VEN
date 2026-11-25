package com.ven.app.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Penghubung antara [LoginViewModel] dan [LoginScreen] (stateless).
 */
@Composable
fun LoginRoute(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val context = LocalContext.current
    val onAuthenticated: (Boolean) -> Unit = { needsOnboarding ->
        if (needsOnboarding) {
            onNavigateToOnboarding()
        } else {
            onNavigateToHome()
        }
    }

    LoginScreen(
        state = viewModel.state,
        onIdentifierChange = viewModel::onIdentifierChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = { viewModel.login(context, onAuthenticated) },
        onForgotPasswordClick = onForgotPassword,
        onGoogleClick = { viewModel.loginWithGoogle(context, onAuthenticated) },
        onRegisterClick = onNavigateToRegister,
        onDismissSuspension = viewModel::dismissSuspension,
    )
}
