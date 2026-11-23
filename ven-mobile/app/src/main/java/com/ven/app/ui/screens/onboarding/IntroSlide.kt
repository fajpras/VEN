package com.ven.app.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ven.app.R

/**
 * Konten tiap slide intro pra-login. [imageRes] bernilai null pada slide
 * penutup "Let's Start" yang hanya menampilkan logo dan teks.
 */
enum class IntroSlide(
    @get:DrawableRes val imageRes: Int?,
    @get:StringRes val titleRes: Int,
    @get:StringRes val descriptionRes: Int,
    @get:StringRes val imageContentDescriptionRes: Int,
) {
    Exhibit3D(
        imageRes = R.drawable.intro_3d_exhibit,
        titleRes = R.string.intro_3d_title,
        descriptionRes = R.string.intro_3d_description,
        imageContentDescriptionRes = R.string.intro_3d_content_desc,
    ),
    MeetConnect(
        imageRes = R.drawable.intro_meet_connect,
        titleRes = R.string.intro_meet_title,
        descriptionRes = R.string.intro_meet_description,
        imageContentDescriptionRes = R.string.intro_meet_content_desc,
    ),
    Start(
        imageRes = null,
        titleRes = R.string.intro_start_title,
        descriptionRes = R.string.intro_start_description,
        imageContentDescriptionRes = R.string.splash_logo_content_desc,
    ),
}
