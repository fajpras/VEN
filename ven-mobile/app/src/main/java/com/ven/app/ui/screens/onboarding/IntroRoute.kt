package com.ven.app.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ven.app.data.preference.OnboardingPreferences

/**
 * Penghubung antara [IntroViewModel] dan [IntroScreen] (stateless).
 */
@Composable
fun IntroRoute(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) { OnboardingPreferences.getInstance(context) }
    val viewModel: IntroViewModel = viewModel { IntroViewModel(preferences) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                IntroEvent.Finished -> onFinished()
            }
        }
    }

    IntroScreen(
        state = state,
        onPageChange = viewModel::onPageChange,
        onNextClick = viewModel::next,
        onSkipClick = viewModel::skip,
        onStartClick = viewModel::start,
    )
}
