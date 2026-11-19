package com.ven.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Penghubung antara [AccountStatusViewModel] dan [AccountStatusScreen] (stateless).
 */
@Composable
fun AccountStatusRoute(
    onBackClick: () -> Unit,
    onDeactivated: () -> Unit,
    viewModel: AccountStatusViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onDeactivated()
        }
    }

    AccountStatusScreen(
        state = state,
        isConfirmDialogOpen = isDialogOpen,
        onKataSandiChange = viewModel::onKataSandiChange,
        onSetujuChange = viewModel::onSetujuChange,
        onBackClick = onBackClick,
        onDeactivateClick = { isDialogOpen = true },
        onConfirmDeactivate = {
            isDialogOpen = false
            viewModel.deaktivasi()
        },
        onDismissDialog = {
            isDialogOpen = false
            viewModel.reset()
        },
    )
}
