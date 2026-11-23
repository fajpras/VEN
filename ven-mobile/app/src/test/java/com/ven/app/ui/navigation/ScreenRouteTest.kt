package com.ven.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenRouteTest {

    @Test
    fun screenRoutes_createPost_isCorrect() {
        assertEquals("create_post", Screen.CreatePost.route)
    }

    @Test
    fun screenRoutes_createExhibit_isCorrect() {
        assertEquals("create_exhibit", Screen.CreateExhibit.route)
    }

    @Test
    fun screenRoutes_createExhibitEditor_withTemplateId_isCorrect() {
        assertEquals(
            "create_exhibit_editor/tpl_small?title=&description=&bannerUrl=&tipe=kecil",
            Screen.CreateExhibitEditor.createRoute("tpl_small"),
        )
        assertEquals(
            "create_exhibit_editor/ex1?title=&description=&bannerUrl=&tipe=kecil",
            Screen.CreateExhibitEditor.createRoute("ex1"),
        )
    }

    @Test
    fun screenRoutes_postsFeed_withPostId_isCorrect() {
        assertEquals("posts_feed?postId=p1&userId=", Screen.PostsFeed.createRoute("p1"))
        assertEquals("posts_feed?postId=p1&userId=u123", Screen.PostsFeed.createRoute("p1", "u123"))
    }

    @Test
    fun screenRoutes_viewExhibit_withRoomId_isCorrect() {
        assertEquals("view_exhibit/ex1", Screen.ViewExhibit.createRoute("ex1"))
        assertEquals("view_exhibit/ex1?roomId=5", Screen.ViewExhibit.createRoute("ex1", 5))
    }

    @Test
    fun screenRoutes_intro_isCorrect() {
        assertEquals("intro", Screen.Intro.route)
    }
}
