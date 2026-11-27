package com.ven.app.ui.screens.unity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.components.ui.VexButtonVariant
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

@Immutable
data class UnityWaitingGuestItem(
    val idPermintaan: Int,
    val name: String,
    val status: String,
)

@Immutable
data class UnityScheduleSheetState(
    val roomId: Int = 0,
    val exhibitionTitle: String = "",
    val deepLink: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val waitingGuests: List<UnityWaitingGuestItem> = emptyList(),
    val approvedGuests: List<UnityWaitingGuestItem> = emptyList(),
)

/**
 * Minimalist slide-up Bottom Sheet di dalam Unity PlayScene (khusus ruang privat)
 * untuk menyalin link ruang privat & menyetujui/menolak tamu Waiting Room tanpa keluar 3D.
 */
@Composable
fun UnityScheduleSheet(
    visible: Boolean,
    state: UnityScheduleSheetState,
    isIndonesian: Boolean,
    onCopyLink: (String) -> Unit,
    onApproveGuest: (Int) -> Unit,
    onRejectGuest: (Int) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = VexTheme.colors

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = colors.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = VexSpace.s5, vertical = VexSpace.s4)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.border),
                        )

                        Spacer(modifier = Modifier.height(VexSpace.s4))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (isIndonesian) "Akses Ruang Privat" else "Private Room Access",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.text,
                            )
                            Text(
                                text = if (isIndonesian) "Muat ulang" else "Refresh",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary,
                                modifier = Modifier.clickable(onClick = onRefresh),
                            )
                        }

                        if (state.exhibitionTitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.exhibitionTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(modifier = Modifier.height(VexSpace.s4))

                        if (state.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = VexSpace.s6),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = colors.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        } else if (!state.errorMessage.isNullOrBlank()) {
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                                modifier = Modifier.padding(vertical = VexSpace.s4),
                            )
                        } else {
                            if (!state.deepLink.isNullOrBlank()) {
                                VexButton(
                                    text = if (isIndonesian) "Salin Link Ruang Privat" else "Copy Private Room Link",
                                    onClick = { onCopyLink(state.deepLink) },
                                    variant = VexButtonVariant.Outline,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(VexSpace.s4))
                            }

                            HorizontalDivider(thickness = 1.dp, color = colors.border)
                            Spacer(modifier = Modifier.height(VexSpace.s3))

                            Text(
                                text = if (isIndonesian) "Menunggu Izin Masuk" else "Waiting to Join",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.text,
                            )

                            Spacer(modifier = Modifier.height(VexSpace.s2))

                            if (state.waitingGuests.isEmpty()) {
                                Text(
                                    text = if (isIndonesian) "Belum ada tamu yang menunggu" else "No one is waiting to join",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(vertical = VexSpace.s2),
                                )
                            } else {
                                state.waitingGuests.forEach { guest ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = VexSpace.s2),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = guest.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = colors.text,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(modifier = Modifier.width(VexSpace.s2))
                                        Row(horizontalArrangement = Arrangement.spacedBy(VexSpace.s2)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.primary)
                                                    .clickable { onApproveGuest(guest.idPermintaan) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                            ) {
                                                Text(
                                                    text = if (isIndonesian) "ACC" else "Allow",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = colors.onPrimary,
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.surfaceInput)
                                                    .clickable { onRejectGuest(guest.idPermintaan) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                            ) {
                                                Text(
                                                    text = if (isIndonesian) "Tolak" else "Deny",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.danger,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (state.approvedGuests.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(VexSpace.s3))
                                HorizontalDivider(thickness = 1.dp, color = colors.border)
                                Spacer(modifier = Modifier.height(VexSpace.s3))
                                Text(
                                    text = if (isIndonesian) "Peserta Diizinkan" else "Allowed Participants",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.text,
                                )
                                Spacer(modifier = Modifier.height(VexSpace.s2))
                                state.approvedGuests.forEach { guest ->
                                    Text(
                                        text = guest.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                        modifier = Modifier.padding(vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(VexSpace.s4))
                        VexButton(
                            text = if (isIndonesian) "Tutup" else "Close",
                            onClick = onDismiss,
                            variant = VexButtonVariant.Outline,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
