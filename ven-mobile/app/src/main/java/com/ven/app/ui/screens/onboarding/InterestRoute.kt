package com.ven.app.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ven.app.data.api.SessionManager

/**
 * Penghubung antara [InterestViewModel] dan [InterestScreen] (stateless).
 */
@Composable
fun InterestRoute(
    onFinished: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: InterestViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                InterestEvent.Saved -> onFinished()
            }
        }
    }

    LaunchedEffect(state.isSessionExpired) {
        if (state.isSessionExpired) {
            SessionManager.getInstance(context).clearSession()
            onSessionExpired()
        }
    }

    InterestScreen(
        state = state,
        onCategoryToggle = viewModel::toggleKategori,
        onSaveClick = viewModel::simpan,
        onMaybeLaterClick = viewModel::lewati,
        onRetryClick = viewModel::muatKategori,
    )
}
