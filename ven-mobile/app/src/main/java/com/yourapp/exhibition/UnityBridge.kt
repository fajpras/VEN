package com.yourapp.exhibition

import android.util.Log
import com.unity3d.player.UnityPlayer
import com.ven.app.BuildConfig

/**
 * Bridge class matching Unity's PlayBridge default package and method signatures:
 * Unity calls:
 * - PlayBridge_RequestRoom -> com.yourapp.exhibition.UnityBridge.requestRoom()
 * - PlayBridge_OpenSchedule -> com.yourapp.exhibition.UnityBridge.openSchedule(roomId)
 */
object UnityBridge {

    private const val TAG = "UnityBridge"

    var currentRoomJson: String? = null
    var onScheduleRequested: ((roomId: String) -> Unit)? = null

    @JvmStatic
    fun requestRoom() {
        if (BuildConfig.DEBUG) Log.d(TAG, "Unity requested room data")
        Thread {
            var json: String? = null
            var lastErrorCode = 0
            var lastErrorMessage: String? = null
            var roomType = "pameran"
            try {
                val activity = UnityPlayer.currentActivity
                val intent = activity?.intent
                val roomId = intent?.getStringExtra("roomId")?.takeIf { it.isNotBlank() }
                    ?: intent?.getStringExtra("room_id")?.takeIf { it.isNotBlank() }
                val apiBaseUrl = intent?.getStringExtra("apiBaseUrl")?.takeIf { it.isNotBlank() }
                    ?: intent?.getStringExtra("api_base_url")?.takeIf { it.isNotBlank() }
                    ?: BuildConfig.BASE_URL
                val authToken = intent?.getStringExtra("authToken").orEmpty()
                roomType = intent?.getStringExtra("roomType")?.takeIf { it.isNotBlank() } ?: "pameran"

                val isValidBackendId = !roomId.isNullOrBlank() &&
                    (roomId.all { it.isDigit() } || roomId.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))

                if (isValidBackendId) {
                    val endpoint = if (roomType == "ruangan") {
                        apiBaseUrl.trimEnd('/') + "/ruangan/$roomId/room-dto"
                    } else {
                        apiBaseUrl.trimEnd('/') + "/pameran/$roomId/room-dto"
                    }
                    if (BuildConfig.DEBUG) Log.d(TAG, "Fetching room DTO from: $endpoint")
                    val conn = (java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                        setRequestProperty("Accept", "application/json")
                        if (authToken.isNotBlank()) {
                            setRequestProperty("Authorization", "Bearer $authToken")
                        }
                    }
                    lastErrorCode = conn.responseCode
                    if (conn.responseCode in 200..299) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        if (body.contains("\"roomId\"") && body.contains("\"templateId\"")) {
                            json = body
                            currentRoomJson = body
                            if (BuildConfig.DEBUG) Log.i(TAG, "Fetched room DTO from backend for room $roomId")
                        }
                    } else {
                        val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() }
                        lastErrorMessage = errorStream
                        if (BuildConfig.DEBUG) Log.w(TAG, "Backend room-dto returned HTTP ${conn.responseCode}: $errorStream")
                    }
                }
            } catch (e: Exception) {
                lastErrorMessage = e.message
                if (BuildConfig.DEBUG) Log.w(TAG, "Failed to fetch room-dto from backend: ${e.message}")
            }

            if (json.isNullOrBlank()) {
                if (roomType == "ruangan") {
                    val message = when (lastErrorCode) {
                        403 -> "Akses ruangan privat ditolak. Jadwal belum tiba atau sudah berakhir."
                        404 -> "Ruangan privat tidak ditemukan."
                        else -> "Gagal memuat ruangan privat: ${lastErrorMessage ?: "Koneksi terputus"}"
                    }
                    if (BuildConfig.DEBUG) Log.w(TAG, "Sending OnRoomError to PlayBridge: $message")
                    UnityPlayer.UnitySendMessage("PlayBridge", "OnRoomError", message)
                    return@Thread
                }
                json = """{"roomId":"viewer_room","templateId":"template01","title":"Pameran 3D","description":"","wallColor":"#FFFFFF","isPublic":true,"ownerId":"owner","viewerId":"visitor","items":[
                    {"prefabId":"hpanel_01","position":{"x":0,"y":1.8,"z":5.2},"rotation":{"x":0,"y":180,"z":0}},
                    {"prefabId":"vpanel_01","position":{"x":-4.8,"y":1.8,"z":1.5},"rotation":{"x":0,"y":90,"z":0}},
                    {"prefabId":"vpanel_02","position":{"x":4.8,"y":1.8,"z":1.5},"rotation":{"x":0,"y":-90,"z":0}},
                    {"prefabId":"hpanel_02","position":{"x":-2.5,"y":1.8,"z":-4.8},"rotation":{"x":0,"y":0,"z":0}},
                    {"prefabId":"sculpture_01","position":{"x":2,"y":0,"z":0},"rotation":{"x":0,"y":-45,"z":0}},
                    {"prefabId":"bench_01","position":{"x":-2,"y":0,"z":0},"rotation":{"x":0,"y":90,"z":0}}
                ]}"""
                if (BuildConfig.DEBUG) Log.i(TAG, "Delivering fallback RoomDto to Unity PlayBridge")
            }
            UnityPlayer.UnitySendMessage("PlayBridge", "OnRoomData", json)
        }.start()
    }

    @JvmStatic
    fun openSchedule(roomId: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Unity requested open schedule for room: $roomId")
        com.ven.app.ui.screens.unity.VenUnityActivity.openScheduleSheet(roomId)
        onScheduleRequested?.invoke(roomId)
    }

    @JvmStatic
    fun openArtworkDetail(
        title: String,
        description: String,
        contentType: String,
        contentUrl: String,
    ) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Unity requested artwork detail: title='$title', type='$contentType'")
        com.ven.app.ui.screens.unity.VenUnityActivity.showArtworkDetail(
            title = title,
            description = description,
            contentType = contentType,
            contentUrl = contentUrl,
        )
    }

    @JvmStatic
    fun sendRoomData(json: String) {
        currentRoomJson = json
        UnityPlayer.UnitySendMessage("PlayBridge", "OnRoomData", json)
    }

    @JvmStatic
    fun sendError(message: String) {
        UnityPlayer.UnitySendMessage("PlayBridge", "OnRoomError", message)
    }
}
