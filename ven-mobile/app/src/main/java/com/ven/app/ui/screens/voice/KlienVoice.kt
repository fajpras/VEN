package com.ven.app.ui.screens.voice

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/** Peristiwa koneksi voice dalam istilah domain VEN (bebas tipe SDK). */
sealed interface PeristiwaVoice {
    /** Peserta masuk atau keluar — hitungan peserta perlu disegarkan. */
    data object PesertaBerubah : PeristiwaVoice

    /** Koneksi terputus dari sisi server. */
    data object Terputus : PeristiwaVoice
}

/**
 * Koneksi voice chat yang digunakan VoiceChatService. Diabstraksi agar
 * logika pemilik koneksi dapat diuji tanpa SDK WebRTC.
 */
interface KlienVoice {
    val peristiwa: Flow<PeristiwaVoice>

    suspend fun hubungkan(url: String, token: String)

    suspend fun setMicAktif(aktif: Boolean)

    suspend fun putuskan()

    fun jumlahPeserta(): Int
}

/**
 * Implementasi LiveKit: audio-only, tanpa video. Satu koneksi mewakili
 * satu sesi diskusi ruangan privat.
 */
class KlienVoiceLivekit(context: Context) : KlienVoice {

    private val appContext = context.applicationContext
    private var room: Room? = null

    override val peristiwa: Flow<PeristiwaVoice>
        get() {
            val ruang = room ?: return kotlinx.coroutines.flow.emptyFlow()
            // Room.events adalah EventListenable; SharedFlow aslinya ada di .events
            return ruang.events.events.mapNotNull { event ->
                when (event) {
                    is RoomEvent.ParticipantConnected, is RoomEvent.ParticipantDisconnected ->
                        PeristiwaVoice.PesertaBerubah

                    is RoomEvent.Disconnected -> PeristiwaVoice.Terputus

                    else -> null
                }
            }
        }

    override suspend fun hubungkan(url: String, token: String) {
        val ruang = LiveKit.create(appContext)
        room = ruang
        ruang.connect(url, token)
        ruang.localParticipant.setMicrophoneEnabled(true)
    }

    override suspend fun setMicAktif(aktif: Boolean) {
        room?.localParticipant?.setMicrophoneEnabled(aktif)
    }

    override suspend fun putuskan() {
        room?.disconnect()
        room?.release()
        room = null
    }

    override fun jumlahPeserta(): Int = 1 + (room?.remoteParticipants?.size ?: 0)
}
