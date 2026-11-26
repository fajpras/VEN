package com.ven.app.ui.screens.unity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.components.ui.VexButtonVariant
import com.ven.app.ui.components.ui.VexDropdown
import com.ven.app.ui.components.ui.VexTextArea
import com.ven.app.ui.components.ui.VexTextField
import com.ven.app.ui.components.ui.VexTextFieldVariant
import com.ven.app.ui.components.ui.rememberAsyncImageBitmap
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

val UNITY_ARTWORK_CATEGORIES: List<String> = listOf(
    "Cybersecurity",
    "Software",
    "UI/UX",
    "Videography",
    "Photography",
    "3D",
    "Animation",
    "Internet Of Things",
    "Automation System",
    "Game Dev",
    "Fabrication",
    "Manufacturing",
    "2D",
    "Others",
)

data class UnityArtworkFormState(
    val title: String = "",
    val description: String = "",
    val categoryId: Int = 1,
    val contentUrl: String = "",
    val isVideo: Boolean = false,
    val isUploading: Boolean = false,
    val localPreview: ImageBitmap? = null,
    val errorMessage: String? = null,
)

/**
 * Form detail karya untuk Unity EditorScene yang muncul slide-up dari bawah layar.
 * Menggunakan desain minimalis sesuai DESIGN.md tanpa pill atau badge berlebihan.
 */
@Composable
fun UnityArtworkFormSheet(
    visible: Boolean,
    state: UnityArtworkFormState,
    isIndonesian: Boolean,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Int) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPickMediaClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = VexTheme.colors
    val remotePreview = rememberAsyncImageBitmap(
        if (state.localPreview == null && !state.isVideo && state.contentUrl.startsWith("http")) {
            state.contentUrl
        } else {
            null
        },
    )
    val effectivePreview = state.localPreview ?: remotePreview
    val selectedCategoryIndex = (state.categoryId - 1).coerceIn(0, UNITY_ARTWORK_CATEGORIES.lastIndex)
    val selectedCategoryName = UNITY_ARTWORK_CATEGORIES[selectedCategoryIndex]

    val titleHeader = if (isIndonesian) "Detail Karya" else "Artwork Details"
    val labelTitle = if (isIndonesian) "Judul" else "Title"
    val placeholderTitle = if (isIndonesian) "Masukkan judul karya..." else "Enter artwork title..."
    val labelCategory = if (isIndonesian) "Kategori" else "Category"
    val labelDesc = if (isIndonesian) "Deskripsi" else "Description"
    val placeholderDesc = if (isIndonesian) "Tulis deskripsi singkat karya..." else "Write a short description..."
    val labelPickMedia = if (isIndonesian) "Pilih Foto / Video" else "Choose Photo or Video"
    val labelChangeMedia = if (isIndonesian) "Ganti Media" else "Change Media"
    val labelUploading = if (isIndonesian) "Mengunggah media..." else "Uploading media..."
    val labelMediaAttached = if (isIndonesian) "Media terpasang" else "Media attached"
    val labelSave = if (isIndonesian) "Simpan" else "Save"

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (!state.isUploading) onDismiss() },
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(sheetShape)
                    .background(colors.bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                // Drag handle minimalis
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(VexRadius.full))
                            .background(colors.border),
                    )
                }

                // Header minimalis
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = VexSpace.s6, end = VexSpace.s3, top = VexSpace.s1, bottom = VexSpace.s2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = titleHeader,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.text,
                    )
                    IconButton(
                        onClick = { if (!state.isUploading) onDismiss() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        VexIcon(
                            id = VexIcons.Close,
                            contentDescription = null,
                            tint = colors.textMuted,
                            size = 20.dp,
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.5f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = VexSpace.s6, vertical = VexSpace.s4),
                    verticalArrangement = Arrangement.spacedBy(VexSpace.s4),
                ) {
                    // Baris pilih media ringkas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VexRadius.md))
                            .background(colors.surfaceInput)
                            .border(1.dp, colors.border, RoundedCornerShape(VexRadius.md))
                            .clickable(enabled = !state.isUploading, role = Role.Button, onClick = onPickMediaClick)
                            .padding(VexSpace.s3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VexSpace.s3),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(VexRadius.xs))
                                .background(colors.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                state.isUploading -> {
                                    CircularProgressIndicator(
                                        color = colors.primary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                effectivePreview != null -> {
                                    Image(
                                        bitmap = effectivePreview,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                else -> {
                                    VexIcon(
                                        id = if (state.isVideo) VexIcons.Play else VexIcons.Tab.Posts.filled,
                                        contentDescription = null,
                                        size = 24.dp,
                                        tint = colors.textMuted,
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = when {
                                    state.isUploading -> labelUploading
                                    state.contentUrl.isNotBlank() -> labelChangeMedia
                                    else -> labelPickMedia
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (state.contentUrl.isNotBlank() && !state.isUploading) {
                                Text(
                                    text = labelMediaAttached,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // Input Judul
                    Column(verticalArrangement = Arrangement.spacedBy(VexSpace.s1)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = labelTitle,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = colors.text,
                            )
                            Text(
                                text = "${state.title.length}/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.title.length > 100) colors.danger else colors.textMuted,
                            )
                        }
                        VexTextField(
                            value = state.title,
                            onValueChange = { onTitleChange(it.take(100)) },
                            variant = VexTextFieldVariant.Boxed,
                            placeholder = placeholderTitle,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Dropdown Kategori
                    VexDropdown(
                        label = labelCategory,
                        selectedOption = selectedCategoryName,
                        options = UNITY_ARTWORK_CATEGORIES,
                        onOptionSelected = { selected ->
                            val idx = UNITY_ARTWORK_CATEGORIES.indexOf(selected)
                            if (idx >= 0) onCategoryChange(idx + 1)
                        },
                    )

                    // Input Deskripsi
                    VexTextArea(
                        value = state.description,
                        onValueChange = onDescriptionChange,
                        label = labelDesc,
                        placeholder = placeholderDesc,
                        minHeight = 96.dp,
                    )

                    if (!state.errorMessage.isNullOrBlank()) {
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.danger,
                        )
                    }

                    VexButton(
                        text = labelSave,
                        onClick = onSaveClick,
                        variant = VexButtonVariant.Primary,
                        loading = state.isUploading,
                        enabled = !state.isUploading,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(VexSpace.s2))
                }
            }
        }
    }
}
