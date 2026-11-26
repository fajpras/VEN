package com.ven.app.util

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import com.ven.app.core.config.AppConfig
import com.ven.app.data.api.SessionManager
import com.ven.app.data.preference.AppLanguage
import com.ven.app.data.preference.ThemePreferences
import com.ven.app.ui.screens.unity.VenUnityActivity
import com.ven.app.ui.theme.ThemeMode

data class PendingArtworkPlacement(
    val karyaId: String,
    val title: String,
    val description: String,
    val contentUrl: String,
    val categoryId: Int,
    val isVideo: Boolean,
)

object UnityLauncher {

    @Volatile
    var pendingArtworkPlacement: PendingArtworkPlacement? = null

    /**
     * Memetakan ID template dari format VEN mobile ke format template Unity yang ada di TemplateLoader:
     * - tpl_small  -> template01 (9 panels)
     * - tpl_medium -> template02 (14 panels)
     * - tpl_big    -> template03 (21 panels)
     * - tpl_event  -> template04 (60 panels)
     */
    fun mapTemplateId(templateId: String): String {
        return when (templateId.trim().lowercase()) {
            "tpl_small", "small", "kecil", "template01" -> "template01"
            "tpl_medium", "medium", "sedang", "template02" -> "template02"
            "tpl_big", "big", "besar", "template03" -> "template03"
            "tpl_event", "event", "acara", "template04" -> "template04"
            else -> "template01"
        }
    }

    private fun resolveThemeMode(context: Context): String {
        val mode = ThemePreferences.getInstance(context).themeMode.value
        return when (mode) {
            ThemeMode.Dark -> "dark"
            ThemeMode.Light -> "light"
            ThemeMode.System -> {
                val nightFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (nightFlags == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
            }
        }
    }

    private fun resolveLanguageCode(): String {
        return when (AppLanguage.current()) {
            AppLanguage.English -> "en"
            AppLanguage.Indonesian -> "id"
        }
    }

    fun createEditorIntent(
        context: Context,
        templateId: String = "template01",
        title: String = "",
        description: String = "",
        bannerUrl: String = "",
        tipe: String = "kecil",
        editRoomId: String = "",
    ): Intent {
        val sessionManager = SessionManager.getInstance(context)
        val token = sessionManager.getToken().orEmpty()
        val baseUrl = AppConfig.baseUrl
        val finalTemplateId = mapTemplateId(templateId)

        return Intent(context, VenUnityActivity::class.java).apply {
            putExtra("mode", "editor")
            putExtra("templateId", finalTemplateId)
            putExtra("template_id", finalTemplateId)
            putExtra("roomId", editRoomId)
            putExtra("room_id", editRoomId)
            putExtra("tipe", tipe)
            putExtra("tipe_pameran", tipe)
            putExtra("title", title)
            putExtra("description", description)
            putExtra("bannerUrl", bannerUrl)
            putExtra("banner", bannerUrl)
            putExtra("banner_url", bannerUrl)
            putExtra("bannerUri", bannerUrl)
            putExtra("banner_uri", bannerUrl)
            putExtra("authToken", token)
            putExtra("token", token)
            putExtra("apiBaseUrl", baseUrl)
            putExtra("api_base_url", baseUrl)
            putExtra("themeMode", resolveThemeMode(context))
            putExtra("language", resolveLanguageCode())
            pendingArtworkPlacement?.let { pending ->
                putExtra("pendingKaryaId", pending.karyaId)
                putExtra("pendingKaryaTitle", pending.title)
                putExtra("pendingKaryaDesc", pending.description)
                putExtra("pendingKaryaUrl", pending.contentUrl)
                putExtra("pendingKaryaCategoryId", pending.categoryId.toString())
                putExtra("pendingKaryaIsVideo", if (pending.isVideo) "1" else "0")
                pendingArtworkPlacement = null
            }
        }
    }

    fun createViewerIntent(context: Context, roomIdOrExhibitId: String, roomId: Int? = null): Intent {
        val sessionManager = SessionManager.getInstance(context)
        val token = sessionManager.getToken().orEmpty()
        val baseUrl = AppConfig.baseUrl
        val finalTemplateId = mapTemplateId(roomIdOrExhibitId)
        // Ruangan privat memakai endpoint room-dto tersendiri (menurut id ruangan).
        val roomType = if (roomId != null) "ruangan" else "pameran"
        val targetId = roomId?.toString() ?: roomIdOrExhibitId

        // Pre-populate UnityBridge with valid room DTO matching template & room id
        if (com.yourapp.exhibition.UnityBridge.currentRoomJson.isNullOrBlank()) {
            com.yourapp.exhibition.UnityBridge.currentRoomJson =
                """{"roomId":"$roomIdOrExhibitId","templateId":"$finalTemplateId","title":"Pameran 3D","description":"","wallColor":"#FFFFFF","isPublic":true,"ownerId":"owner","viewerId":"visitor","items":[
                    {"prefabId":"hpanel_01","position":{"x":0,"y":1.8,"z":5.2},"rotation":{"x":0,"y":180,"z":0}},
                    {"prefabId":"vpanel_01","position":{"x":-4.8,"y":1.8,"z":1.5},"rotation":{"x":0,"y":90,"z":0}},
                    {"prefabId":"vpanel_02","position":{"x":4.8,"y":1.8,"z":1.5},"rotation":{"x":0,"y":-90,"z":0}},
                    {"prefabId":"hpanel_02","position":{"x":-2.5,"y":1.8,"z":-4.8},"rotation":{"x":0,"y":0,"z":0}},
                    {"prefabId":"sculpture_01","position":{"x":2,"y":0,"z":0},"rotation":{"x":0,"y":-45,"z":0}},
                    {"prefabId":"bench_01","position":{"x":-2,"y":0,"z":0},"rotation":{"x":0,"y":90,"z":0}}
                ]}"""
        }

        return Intent(context, VenUnityActivity::class.java).apply {
            putExtra("mode", "viewer")
            putExtra("roomType", roomType)
            putExtra("templateId", finalTemplateId)
            putExtra("template_id", finalTemplateId)
            putExtra("roomId", targetId)
            putExtra("room_id", targetId)
            putExtra("authToken", token)
            putExtra("token", token)
            putExtra("apiBaseUrl", baseUrl)
            putExtra("api_base_url", baseUrl)
            putExtra("themeMode", resolveThemeMode(context))
            putExtra("language", resolveLanguageCode())
        }
    }

    fun launchEditor(
        context: Context,
        templateId: String = "template01",
        title: String = "",
        description: String = "",
        bannerUrl: String = "",
    ) {
        val intent = createEditorIntent(context, templateId, title, description, bannerUrl).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun launchViewer(context: Context, roomIdOrExhibitId: String, roomId: Int? = null) {
        val intent = createViewerIntent(context, roomIdOrExhibitId, roomId).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
