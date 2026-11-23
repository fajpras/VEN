package com.ven.app.ui.screens.onboarding

/**
 * State layar intro pra-login.
 */
data class IntroUiState(
    val pageIndex: Int = 0,
    val totalPages: Int = IntroSlide.entries.size,
) {
    val isLastPage: Boolean get() = pageIndex >= totalPages - 1
}
