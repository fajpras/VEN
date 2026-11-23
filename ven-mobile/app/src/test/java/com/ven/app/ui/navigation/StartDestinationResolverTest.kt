package com.ven.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class StartDestinationResolverTest {

    @Test
    fun debugExplore_alwaysOpensHome() {
        assertEquals(
            Screen.Home,
            StartDestinationResolver.resolve(
                showExplore = true,
                isLoggedIn = false,
                introDone = false,
            ),
        )
    }

    @Test
    fun loggedInUser_skipsIntroAndOpensHome() {
        assertEquals(
            Screen.Home,
            StartDestinationResolver.resolve(
                showExplore = false,
                isLoggedIn = true,
                introDone = false,
            ),
        )
    }

    @Test
    fun firstLaunch_opensIntro() {
        assertEquals(
            Screen.Intro,
            StartDestinationResolver.resolve(
                showExplore = false,
                isLoggedIn = false,
                introDone = false,
            ),
        )
    }

    @Test
    fun returningGuest_opensLogin() {
        assertEquals(
            Screen.Login,
            StartDestinationResolver.resolve(
                showExplore = false,
                isLoggedIn = false,
                introDone = true,
            ),
        )
    }
}
