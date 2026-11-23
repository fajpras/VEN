package com.ven.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.data.preference.OnboardingPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface IntroEvent {
    data object Finished : IntroEvent
}

/**
 * Mengatur perpindahan slide serta menandai intro selesai (tombol terakhir
 * maupun "Skip"). Penanda disimpan agar intro hanya tampil sekali per perangkat.
 */
class IntroViewModel(
    private val onboardingPreferences: OnboardingPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntroUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<IntroEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var isFinished = false

    fun onPageChange(page: Int) {
        _uiState.update { state ->
            state.copy(pageIndex = page.coerceIn(0, state.totalPages - 1))
        }
    }

    fun next() {
        val state = _uiState.value
        if (state.isLastPage) {
            start()
        } else {
            onPageChange(state.pageIndex + 1)
        }
    }

    fun skip() {
        start()
    }

    fun start() {
        if (isFinished) return
        isFinished = true
        onboardingPreferences.markIntroDone()
        viewModelScope.launch { _events.send(IntroEvent.Finished) }
    }
}
