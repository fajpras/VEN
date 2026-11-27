package com.ven.app.ui.screens.schedule.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import com.ven.app.ui.screens.voice.VoiceChatPanel
import com.ven.app.ui.screens.voice.VoiceSesi
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: ScheduleEventItem,
    onDismissRequest: () -> Unit,
    onJoinRoom: () -> Unit,
    onCopyLink: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    voiceStatus: VoiceSesi = VoiceSesi.Jeda,
) {
    val colors = VexTheme.colors
    val canJoin = event.isApproved && !event.isExpired

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

            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
            )

            Spacer(modifier = Modifier.height(VexSpace.s2))

            val prefix = if (event.isApproved) {
                stringResource(R.string.schedule_approved_detail_prefix)
            } else {
                stringResource(R.string.schedule_waiting_detail_prefix)
            }

            val suffix = if (event.isApproved) {
                stringResource(R.string.schedule_approved_detail_suffix)
            } else {
                stringResource(R.string.schedule_waiting_detail_suffix)
            }

            val durationLabel = stringResource(R.string.schedule_duration_minutes, event.durationMinutes)
            val annotatedMessage = buildAnnotatedString {
                append(prefix)
                withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.text)) {
                    append("${event.fullDateFormatted} ($durationLabel)")
                }
                append(suffix)
            }

            Text(
                text = annotatedMessage,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = colors.textMuted,
            )

            if (!event.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(VexSpace.s3))
                Text(
                    text = event.note,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = colors.text,
                )
            }

            Spacer(modifier = Modifier.height(VexSpace.s5))

            if (canJoin && event.roomId != null) {
                VoiceChatPanel(status = voiceStatus)
                Spacer(modifier = Modifier.height(VexSpace.s4))
            }

            val primaryButtonText = when {
                event.isExpired -> stringResource(R.string.schedule_session_expired)
                event.isApproved -> stringResource(R.string.schedule_join_room)
                else -> stringResource(R.string.schedule_waiting_approval)
            }

            VexButton(
                text = primaryButtonText,
                onClick = onJoinRoom,
                enabled = canJoin,
                variant = VexButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            )

            if (canJoin && !event.deepLink.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(VexSpace.s3))
                VexButton(
                    text = stringResource(R.string.schedule_copy_link),
                    onClick = { onCopyLink(event.deepLink) },
                    variant = VexButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
                dateText = "24 Sep 2026",
                timeText = "02:00 PM",
                fullDateFormatted = "Thursday, 24 September 2026 at 02:00 PM",
                bannerRes = R.drawable.poster_3,
                isApproved = true,
                durationMinutes = 45,
                deepLink = "ven://diskusi/sampletoken",
            ),
            onDismissRequest = {},
            onJoinRoom = {},
        )
    }
}
