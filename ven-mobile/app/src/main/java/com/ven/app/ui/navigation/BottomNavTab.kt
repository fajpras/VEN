package com.ven.app.ui.navigation

import androidx.annotation.StringRes
import com.ven.app.R
import com.ven.app.ui.icons.VexIconPair
import com.ven.app.ui.icons.VexIcons


enum class BottomNavTab(
    val route: String,
    @param:StringRes val titleRes: Int,
    val iconPair: VexIconPair? = null,
) {
    Home(
        route = "home",
        titleRes = R.string.bottom_nav_home,
        iconPair = VexIcons.Nav.Home,
    ),
    Schedule(
        route = "schedule",
        titleRes = R.string.bottom_nav_schedule,
        iconPair = VexIcons.Nav.Schedule,
    ),
    Ai(
        route = "ai",
        titleRes = R.string.bottom_nav_ai,
        iconPair = VexIcons.Nav.Ai,
    ),
    Explore(
        route = "explore",
        titleRes = R.string.bottom_nav_explore,
        iconPair = VexIcons.Nav.Explore,
    ),
    Profile(
        route = "profile",
        titleRes = R.string.bottom_nav_profile,
        iconPair = null,
    );

    val isProfile: Boolean
        get() = this == Profile
}
