package com.ven.app.ui.screens.schedule.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ven.app.R
import com.ven.app.ui.components.ui.VexBottomSheet
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.components.ui.VexButtonVariant
import com.ven.app.ui.components.ui.rememberAsyncImageBitmap
import com.ven.app.ui.screens.schedule.ScheduleEventItem
import com.ven.app.ui.screens.schedule.isJoinable
import com.ven.app.ui.screens.voice.VoiceChatPanel
import com.ven.app.ui.screens.voice.VoiceSesi
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

/**
 * Bottom Sheet modal untuk detail jadwal pertemuan pameran (Tab Approved, Waiting, & History).
 * Sesuai DESIGN.md §8.4.5 & Wireframe schedule-1 & schedule-3:
 * - Banner pameran besar rasio 16:9 di bagian atas.
 * - Teks penjelasan dengan tanggal & jam dicetak tebal via [AnnotatedString].
 * - Informasi jendela sesi (-15m s.d. +15m) & alasan penolakan/kedaluwarsa bila ada.
 * - Kode sesi (tautan) yang dapat disalin dengan satu ketukan.
 * - Tombol "Join Room":
 *   - Aktif jika [ScheduleEventItem.isJoinable] = true (dalam jendela waktu dan disetujui).
 *   - Nonaktif jika di luar jendela sesi atau belum disetujui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: ScheduleEventItem,
    onDismissRequest: () -> Unit,
    onJoinRoom: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),

    /** Status voice chat saat ini; ditampilkan hanya untuk ruangan privat. */
    voiceStatus: VoiceSesi = VoiceSesi.Jeda,
) {
    val colors = VexTheme.colors
    val context = LocalContext.current

    val isRejected = event.status.equals("ditolak", ignoreCase = true)
    val isExpired = !event.isApproved && !isRejected &&
        event.waktuMillis != null && (event.waktuMillis + 15 * 60_000L < nowMillis)

    VexBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VexSpace.s6)
                .padding(bottom = VexSpace.s6)
                .navigationBarsPadding(),
        ) {
            // Banner Pameran 16:9
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(VexRadius.md)),
            ) {
                val bitmap = rememberAsyncImageBitmap(event.bannerUrl)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = event.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Image(
                        painter = painterResource(id = event.bannerRes),
                        contentDescription = event.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Spacer(modifier = Modifier.height(VexSpace.s5))

            // Teks Pesan Status (AnnotatedString dengan tanggal/jam bold)
            val prefix = when {
                event.isApproved -> stringResource(R.string.schedule_approved_detail_prefix)
                isRejected -> stringResource(R.string.schedule_rejected_detail_prefix)
                isExpired -> stringResource(R.string.schedule_expired_detail_prefix)
                else -> stringResource(R.string.schedule_waiting_detail_prefix)
            }

            val suffix = when {
                event.isApproved -> stringResource(R.string.schedule_approved_detail_suffix)
                isRejected -> stringResource(R.string.schedule_rejected_detail_suffix)
                isExpired -> stringResource(R.string.schedule_expired_detail_suffix)
                else -> stringResource(R.string.schedule_waiting_detail_suffix)
            }

            val annotatedMessage = buildAnnotatedString {
                append(prefix)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = colors.text)) {
                    append(event.fullDateFormatted)
                }
                append(suffix)
            }

            Text(
                text = annotatedMessage,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = colors.text,
            )

            // Hint Jendela Waktu untuk Ruangan Privat yang Disetujui
            if (event.isApproved && event.roomId != null && event.waktuMillis != null) {
                Spacer(modifier = Modifier.height(VexSpace.s3))
                val hint = when {
                    nowMillis < event.waktuMillis - 15 * 60_000L ->
                        stringResource(R.string.schedule_window_hint_before)
                    nowMillis > event.waktuMillis + 15 * 60_000L ->
                        stringResource(R.string.schedule_window_hint_after)
                    else ->
                        stringResource(R.string.schedule_window_hint_active)
                }
                val hintColor = if (event.isJoinable(nowMillis)) colors.primary else colors.textMuted
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = hintColor,
                )
            }

            // Tampilkan Alasan jika ada (Ditolak / Kedaluwarsa)
            if (!event.alasan.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(VexSpace.s3))
                Text(
                    text = stringResource(R.string.schedule_reason_label) + event.alasan,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                )
            }

            // Tampilkan Kode Sesi (tautan) dengan aksi salin bila tersedia
            if (!event.tautan.isNullOrBlank() && event.isApproved) {
                Spacer(modifier = Modifier.height(VexSpace.s3))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceInput)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("session_code", event.tautan))
                            Toast.makeText(
                                context,
                                context.getString(R.string.schedule_session_code_copied),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        .padding(horizontal = VexSpace.s4, vertical = VexSpace.s3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.schedule_session_code_label) + event.tautan,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.text,
                    )
                    Text(
                        text = "Salin",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Kartu voice chat: hanya ruangan privat yang mendukung voice.
            if (event.isApproved && event.roomId != null) {
                VoiceChatPanel(status = voiceStatus)

                Spacer(modifier = Modifier.height(VexSpace.s4))
            }

            // Tombol Utama "Join Room" (hanya aktif jika dalam jendela sesi)
            val canJoin = event.isJoinable(nowMillis)
            VexButton(
                text = stringResource(R.string.schedule_join_room),
                onClick = onJoinRoom,
                enabled = canJoin,
                variant = VexButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "EventDetailSheet Approved - Dark Mode")
@Composable
private fun EventDetailSheetApprovedPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        EventDetailSheet(
            event = ScheduleEventItem(
                id = "1",
                title = "Open Source Competition",
                dateText = "24th Sep 2026",
                timeText = "02.00 PM",
                fullDateFormatted = "Thursday, 24th September 2026 at 02.00 PM",
                bannerRes = R.drawable.poster_3,
                isApproved = true,
            ),
            onDismissRequest = {},
            onJoinRoom = {},
        )
    }
}

@Preview(name = "EventDetailSheet Waiting - Dark Mode")
@Composable
private fun EventDetailSheetWaitingPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        EventDetailSheet(
            event = ScheduleEventItem(
                id = "2",
                title = "Virtual Exhibition",
                dateText = "24th Sep 2026",
                timeText = "02.00 PM",
                fullDateFormatted = "Thursday, 24th September 2026 at 02.00 PM",
                bannerRes = R.drawable.poster_3,
                isApproved = false,
            ),
            onDismissRequest = {},
            onJoinRoom = {},
        )
    }
}
