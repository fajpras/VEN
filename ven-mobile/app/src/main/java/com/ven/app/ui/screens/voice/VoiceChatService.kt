package com.ven.app.ui.screens.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ven.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service pemilik koneksi voice chat (LiveKit) di proses utama.
 *
 * Voice harus tetap hidup ketika pengguna berpindah ke activity Unity
 * (proses `:unity`), sehingga koneksi tidak boleh dimiliki ViewModel.
 * Kontrol bisu/tutup dipakai lewat notifikasi bergaya panggilan.
 */
class VoiceChatService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var klien: KlienVoice? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AKSI_GANTI_MIC -> gantiMic()
            AKSI_TUTUP -> putuskanDanBerhenti()
            else -> gabung(intent)
        }
        return START_NOT_STICKY
    }

    private fun gabung(intent: Intent?) {
        val url = intent?.getStringExtra(EXTRA_URL).orEmpty()
        val token = intent?.getStringExtra(EXTRA_TOKEN).orEmpty()

        mulaiSebagaiForeground()

        if (url.isBlank() || token.isBlank()) {
            VoiceSesiStatus.gagal(getString(R.string.voice_error_token))
            putuskanDanBerhenti()
            return
        }

        VoiceSesiStatus.menyiapkan()

        scope.launch {
            try {
                val penyambung = KlienVoiceLivekit(applicationContext)
                klien = penyambung
                penyambung.hubungkan(url, token)
                VoiceSesiStatus.tersambung()
                amatiPeristiwa(penyambung)
            } catch (e: Exception) {
                VoiceSesiStatus.gagal(e.message ?: getString(R.string.voice_error_gagal))
                putuskanDanBerhenti()
            }
        }
    }

    private suspend fun amatiPeristiwa(penyambung: KlienVoice) {
        penyambung.peristiwa.collect { peristiwa ->
            when (peristiwa) {
                PeristiwaVoice.PesertaBerubah ->
                    VoiceSesiStatus.setJumlahPeserta(penyambung.jumlahPeserta())

                PeristiwaVoice.Terputus -> putuskanDanBerhenti()
            }
        }
    }

    private fun gantiMic() {
        val aktif = !VoiceSesiStatus.state.value.micAktif
        VoiceSesiStatus.setMicAktif(aktif)
        perbaruiNotifikasi()

        scope.launch {
            runCatching { klien?.setMicAktif(aktif) }
        }
    }

    private fun putuskanDanBerhenti() {
        VoiceSesiStatus.reset()
        scope.launch {
            runCatching { klien?.putuskan() }
            klien = null
            stopSelf()
        }
    }

    private fun mulaiSebagaiForeground() {
        buatChannel()
        ServiceCompat.startForeground(
            this,
            ID_NOTIFIKASI,
            buatNotifikasi(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )
    }

    private fun perbaruiNotifikasi() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ID_NOTIFIKASI, buatNotifikasi())
    }

    private fun buatChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            ID_CHANNEL,
            getString(R.string.voice_channel_nama),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.voice_channel_deskripsi) }

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buatNotifikasi(): Notification {
        val micAktif = VoiceSesiStatus.state.value.micAktif
        val sesi = VoiceSesiStatus.state.value.sesi

        val aksiMic = PendingIntent.getService(
            this,
            PERMINTAAN_GANTI_MIC,
            Intent(this, VoiceChatService::class.java).setAction(AKSI_GANTI_MIC),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val aksiTutup = PendingIntent.getService(
            this,
            PERMINTAAN_TUTUP,
            Intent(this, VoiceChatService::class.java).setAction(AKSI_TUTUP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val teks = when (sesi) {
            VoiceSesi.Menyiapkan -> getString(R.string.voice_notif_menyiapkan)
            else -> getString(R.string.voice_notif_tersambung)
        }

        val builder = NotificationCompat.Builder(this, ID_CHANNEL)
            .setSmallIcon(R.drawable.ic_voice_mic)
            .setContentTitle(getString(R.string.voice_notif_judul))
            .setContentText(teks)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(if (micAktif) R.string.voice_notif_bisu else R.string.voice_notif_nyala), aksiMic)
            .addAction(0, getString(R.string.voice_notif_tutup), aksiTutup)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pengguna = androidx.core.app.Person.Builder()
                .setName(getString(R.string.voice_notif_judul))
                .setImportant(true)
                .build()

            builder.setStyle(NotificationCompat.CallStyle.forOngoingCall(pengguna, aksiTutup))
        }

        return builder.build()
    }

    override fun onDestroy() {
        scope.cancel()
        VoiceSesiStatus.reset()
        super.onDestroy()
    }

    companion object {

        private const val ID_CHANNEL = "voice_chat_privat"
        private const val ID_NOTIFIKASI = 41
        private const val PERMINTAAN_GANTI_MIC = 42
        private const val PERMINTAAN_TUTUP = 43

        private const val AKSI_GANTI_MIC = "com.ven.app.voice.GANTI_MIC"
        private const val AKSI_TUTUP = "com.ven.app.voice.TUTUP"

        private const val EXTRA_URL = "extra_voice_url"
        private const val EXTRA_TOKEN = "extra_voice_token"

        /** Nyalakan sesi voice: foreground service + koneksi LiveKit. */
        fun mulai(context: Context, url: String?, token: String) {
            val intent = Intent(context, VoiceChatService::class.java)
                .putExtra(EXTRA_URL, url.orEmpty())
                .putExtra(EXTRA_TOKEN, token)

            ContextCompat.startForegroundService(context, intent)
        }

        /** Akhiri sesi voice yang sedang berjalan (jika ada). */
        fun akhiri(context: Context) {
            context.startService(
                Intent(context, VoiceChatService::class.java).setAction(AKSI_TUTUP),
            )
        }
    }
}
