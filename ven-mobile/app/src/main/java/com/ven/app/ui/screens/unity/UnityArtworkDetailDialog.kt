package com.ven.app.ui.screens.unity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.components.ui.VexButtonVariant
import com.ven.app.ui.components.ui.rememberAsyncImageBitmap
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

data class UnityArtworkDetailState(
    val title: String = "",
    val description: String = "",
    val contentType: String = "image",
    val contentUrl: String = "",
)

/**
 * Pop-up Detail Karya minimalis di dalam PlayScene:
 * - Tema (Light/Dark) dan label UI mengikuti pengaturan aplikasi.
 * - Isi data karya (judul & deskripsi) ditampilkan asli apa adanya tanpa diterjemahkan.
 */
@Composable
fun UnityArtworkDetailDialog(
    visible: Boolean,
    detail: UnityArtworkDetailState,
    isIndonesian: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = VexTheme.colors
    val isVideo = detail.contentType.equals("video", ignoreCase = true)
    val previewBitmap = rememberAsyncImageBitmap(
        if (!isVideo && detail.contentUrl.isNotBlank()) detail.contentUrl else null,
    )

    val labelHeader = if (isIndonesian) "Detail Karya" else "Artwork Details"
    val labelDesc = if (isIndonesian) "Deskripsi" else "Description"
    val labelEmptyDesc = if (isIndonesian) "Tidak ada deskripsi." else "No description."
    val labelClose = if (isIndonesian) "Tutup" else "Close"

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f),
        ) {
            val cardShape = RoundedCornerShape(VexRadius.lg)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VexSpace.s6, vertical = VexSpace.s8)
                    .heightIn(max = 560.dp)
                    .clip(cardShape)
                    .background(colors.bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = VexSpace.s5, end = VexSpace.s2, top = VexSpace.s3, bottom = VexSpace.s2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = labelHeader,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.text,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp),
                    ) {
                        VexIcon(
                            id = VexIcons.Close,
                            contentDescription = labelClose,
                            tint = colors.textMuted,
                            size = 20.dp,
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.5f))

                // Konten (scrollable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = VexSpace.s5, vertical = VexSpace.s4),
                    verticalArrangement = Arrangement.spacedBy(VexSpace.s4),
                ) {
                    // Media preview
                    if (detail.contentUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .clip(RoundedCornerShape(VexRadius.md))
                                .background(colors.surfaceInput),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap,
                                    contentDescription = detail.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                VexIcon(
                                    id = if (isVideo) VexIcons.Play else VexIcons.Tab.Posts.filled,
                                    contentDescription = null,
                                    size = 36.dp,
                                    tint = colors.textMuted,
                                )
                            }
                        }
                    }

                    // Judul asli karya (tidak diterjemahkan)
                    if (detail.title.isNotBlank()) {
                        Text(
                            text = detail.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.text,
                        )
                    }

                    // Deskripsi asli karya (label mengikuti bahasa aplikasi, isi tetap asli)
                    Column(verticalArrangement = Arrangement.spacedBy(VexSpace.s1)) {
                        Text(
                            text = labelDesc,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = colors.textMuted,
                        )
                        Text(
                            text = detail.description.ifBlank { labelEmptyDesc },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (detail.description.isNotBlank()) colors.text else colors.textMuted,
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.5f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VexSpace.s5, vertical = VexSpace.s3),
                ) {
                    VexButton(
                        text = labelClose,
                        onClick = onDismiss,
                        variant = VexButtonVariant.Outline,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
