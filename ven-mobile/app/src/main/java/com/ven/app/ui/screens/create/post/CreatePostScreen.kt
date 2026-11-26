package com.ven.app.ui.screens.create.post

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ven.app.R
import com.ven.app.data.api.models.KategoriModel
import com.ven.app.data.api.models.PameranItemModel
import com.ven.app.ui.components.layout.VexTopBar
import com.ven.app.ui.components.ui.CreateContentType
import com.ven.app.ui.components.ui.SegmentedPill
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.components.ui.VexButtonVariant
import com.ven.app.ui.components.ui.VexDropdown
import com.ven.app.ui.components.ui.VexTextArea
import com.ven.app.ui.components.ui.VexTextField
import com.ven.app.ui.components.ui.VexTextFieldVariant
import com.ven.app.ui.components.ui.rememberAsyncImageBitmap
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

/**
 * Layar Create Post & Edit Post (stateless) sesuai DESIGN.md:
 * - Langkah 1: Dropzone media modern dengan garis putus-putus, badge format, dan pratinjau penuh.
 * - Langkah 2: Formulir detail (Judul, Kategori, Deskripsi) + kartu pemilihan Pameran (wajib saat buat baru)
 *   yang dilanjutkan ke penempatan panel 3D di Unity.
 */
@Composable
fun CreatePostScreen(
    state: CreatePostUiState,
    mediaPreview: ImageBitmap?,
    onCloseClick: () -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    onPickMediaClick: () -> Unit,
    onPostClick: () -> Unit,
    onContentTypeSelected: (CreateContentType) -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Int) -> Unit,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onExhibitionChange: (Int?) -> Unit = {},
    onCreateExhibitClick: () -> Unit = {},
) {
    val colors = VexTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        if (state.isEditMode) {
            EditPostInstagramScreen(
                state = state,
                mediaPreview = mediaPreview,
                onCloseClick = onCloseClick,
                onSaveClick = onPostClick,
                onPickMediaClick = onPickMediaClick,
                onTitleChange = onTitleChange,
                onDescriptionChange = onDescriptionChange,
            )
        } else {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    if (targetState == CreatePostStep.Details) {
                        slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                    } else {
                        slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                    }
                },
                label = "CreatePostStepTransition",
            ) { step ->
                when (step) {
                    CreatePostStep.MediaPicker -> CreatePostMediaPickerStep(
                        state = state,
                        mediaPreview = mediaPreview,
                        onCloseClick = onCloseClick,
                        onNextClick = onNextClick,
                        onPickMediaClick = onPickMediaClick,
                        onContentTypeSelected = onContentTypeSelected,
                    )

                    CreatePostStep.Details -> CreatePostDetailsStep(
                        state = state,
                        mediaPreview = mediaPreview,
                        onBackClick = onBackClick,
                        onPostClick = onPostClick,
                        onPickMediaClick = onPickMediaClick,
                        onTitleChange = onTitleChange,
                        onCategoryChange = onCategoryChange,
                        onExhibitionChange = onExhibitionChange,
                        onDescriptionChange = onDescriptionChange,
                        onCreateExhibitClick = onCreateExhibitClick,
                    )
                }
            }
        }

        if (state.isSubmitting || state.isLoadingPost) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
        }
    }
}

@Composable
private fun CreatePostMediaPickerStep(
    state: CreatePostUiState,
    mediaPreview: ImageBitmap?,
    onCloseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPickMediaClick: () -> Unit,
    onContentTypeSelected: (CreateContentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VexTopBar.Create(
                title = stringResource(R.string.create_post_title),
                actionText = stringResource(R.string.create_post_next),
                onActionClick = onNextClick,
                onCloseOrBackClick = onCloseClick,
                isBack = false,
                actionEnabled = state.isMediaSelected,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        start = VexSpace.s6,
                        end = VexSpace.s6,
                        top = VexSpace.s3,
                        bottom = 96.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(VexSpace.s3),
            ) {
                MediaPreviewCard(
                    state = state,
                    mediaPreview = mediaPreview,
                    onClick = onPickMediaClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                FormErrorText(message = state.errorMessage(), visible = state.errorMessageRes != null)
            }
        }

        SegmentedPill(
            selectedType = state.selectedContentType,
            onTypeSelected = onContentTypeSelected,
            postLabel = stringResource(R.string.create_post_tab_post),
            exhibitLabel = stringResource(R.string.create_post_tab_exhibit),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = VexSpace.s4),
        )
    }
}

@Composable
private fun MediaPreviewCard(
    state: CreatePostUiState,
    mediaPreview: ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val shape = RoundedCornerShape(VexRadius.lg)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceInput)
            .border(1.dp, colors.border, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colors.text.copy(alpha = 0.10f)),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (mediaPreview != null) {
            Image(
                bitmap = mediaPreview,
                contentDescription = stringResource(R.string.create_post_selected_media_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        if (state.isMediaSelected) {
            if (mediaPreview == null) {
                VexIcon(
                    id = VexIcons.Play,
                    contentDescription = null,
                    size = 48.dp,
                    tint = colors.textMuted,
                )
            }

            // Tombol Ganti Media ringkas di kanan bawah
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(VexSpace.s4)
                    .clip(RoundedCornerShape(VexRadius.full))
                    .background(colors.surface.copy(alpha = 0.92f))
                    .border(1.dp, colors.border, RoundedCornerShape(VexRadius.full))
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = VexSpace.s4, vertical = VexSpace.s2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VexSpace.s2),
            ) {
                VexIcon(
                    id = VexIcons.Edit,
                    contentDescription = null,
                    size = 15.dp,
                    tint = colors.text,
                )
                Text(
                    text = stringResource(R.string.create_post_change_media),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.text,
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VexSpace.s2),
                modifier = Modifier.padding(VexSpace.s8),
            ) {
                VexIcon(
                    id = VexIcons.Tab.Posts.filled,
                    contentDescription = null,
                    size = 32.dp,
                    tint = colors.textMuted,
                )

                Spacer(modifier = Modifier.height(VexSpace.s1))

                Text(
                    text = stringResource(R.string.create_post_choose_media),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = stringResource(R.string.create_post_media_formats),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Tampilan Edit Karya bergaya Instagram minimalis:
 * - Top bar bersih (Tutup | Edit Karya | Simpan)
 * - Pratinjau media penuh edge-to-edge tanpa badge dekoratif
 * - Input Judul & Deskripsi bersih bergaris bawah (posisi panel diatur di Editor 3D)
 */
@Composable
private fun EditPostInstagramScreen(
    state: CreatePostUiState,
    mediaPreview: ImageBitmap?,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
    onPickMediaClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val scrollState = rememberScrollState()
    val remotePreviewBitmap = rememberAsyncImageBitmap(
        if (mediaPreview == null) state.existingMediaUrl else null,
    )
    val effectivePreview = mediaPreview ?: remotePreviewBitmap

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        VexTopBar.Create(
            title = stringResource(R.string.edit_post_title),
            actionText = stringResource(R.string.edit_post_save),
            onActionClick = onSaveClick,
            onCloseOrBackClick = onCloseClick,
            isBack = false,
            actionEnabled = state.isPostEnabled,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Pratinjau media penuh bergaya Instagram
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(colors.surfaceInput)
                    .clickable(role = Role.Button, onClick = onPickMediaClick),
                contentAlignment = Alignment.Center,
            ) {
                if (effectivePreview != null) {
                    Image(
                        bitmap = effectivePreview,
                        contentDescription = stringResource(R.string.create_post_selected_media_desc),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    VexIcon(
                        id = if (state.mediaJenis == CreatePostJenis.Video) {
                            VexIcons.Play
                        } else {
                            VexIcons.Tab.Posts.filled
                        },
                        contentDescription = null,
                        size = 44.dp,
                        tint = colors.textMuted,
                    )
                }

                // Tombol ringkas Ganti Media di kanan bawah
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(VexSpace.s4)
                        .clip(RoundedCornerShape(VexRadius.full))
                        .background(colors.surface.copy(alpha = 0.92f))
                        .border(1.dp, colors.border, RoundedCornerShape(VexRadius.full))
                        .clickable(role = Role.Button, onClick = onPickMediaClick)
                        .padding(horizontal = VexSpace.s4, vertical = VexSpace.s2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VexSpace.s2),
                ) {
                    VexIcon(
                        id = VexIcons.Edit,
                        contentDescription = null,
                        size = 14.dp,
                        tint = colors.text,
                    )
                    Text(
                        text = stringResource(R.string.edit_post_change_media),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.text,
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.6f))

            // Input Judul & Deskripsi bergaya Instagram
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VexSpace.s6, vertical = VexSpace.s5),
                verticalArrangement = Arrangement.spacedBy(VexSpace.s5),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(VexSpace.s1)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.create_post_title_label),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = colors.textMuted,
                        )
                        Text(
                            text = "${state.title.length}/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.title.length > 100) colors.danger else colors.textMuted,
                        )
                    }

                    BasicTextField(
                        value = state.title,
                        onValueChange = { onTitleChange(it.take(100)) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = colors.text,
                            fontWeight = FontWeight.Normal,
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = VexSpace.s2),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (state.title.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.create_post_title_placeholder),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.textMuted,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )

                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                }

                Column(verticalArrangement = Arrangement.spacedBy(VexSpace.s1)) {
                    Text(
                        text = stringResource(R.string.create_post_description_label),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.textMuted,
                    )

                    BasicTextField(
                        value = state.description,
                        onValueChange = onDescriptionChange,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.text,
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        singleLine = false,
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = VexSpace.s2),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (state.description.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.create_post_description_placeholder),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textMuted,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )

                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                }

                FormErrorText(
                    message = state.serverErrorMessage ?: state.errorMessage(),
                    visible = state.serverErrorMessage != null || state.errorMessageRes != null,
                )

                Spacer(modifier = Modifier.height(VexSpace.s4))
            }
        }
    }
}

@Composable
private fun CreatePostDetailsStep(
    state: CreatePostUiState,
    mediaPreview: ImageBitmap?,
    onBackClick: () -> Unit,
    onPostClick: () -> Unit,
    onPickMediaClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Int) -> Unit,
    onExhibitionChange: (Int?) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCreateExhibitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val scrollState = rememberScrollState()
    val categoryNames = state.categories.map { it.nama }
    val remotePreviewBitmap = rememberAsyncImageBitmap(
        if (mediaPreview == null) state.existingMediaUrl else null,
    )
    val effectivePreview = mediaPreview ?: remotePreviewBitmap

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        VexTopBar.Create(
            title = stringResource(R.string.create_post_title),
            actionText = stringResource(R.string.create_post_place_in_3d),
            onActionClick = onPostClick,
            onCloseOrBackClick = onBackClick,
            isBack = true,
            actionEnabled = state.isPostEnabled,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = VexSpace.s6, vertical = VexSpace.s4),
            verticalArrangement = Arrangement.spacedBy(VexSpace.s4),
        ) {
            // Kartu pratinjau media ringkas + tombol ganti
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VexRadius.md))
                    .background(colors.surfaceInput)
                    .border(1.dp, colors.border, RoundedCornerShape(VexRadius.md))
                    .clickable(role = Role.Button, onClick = onPickMediaClick)
                    .padding(VexSpace.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VexSpace.s3),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(VexRadius.md))
                        .background(colors.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (effectivePreview != null) {
                        Image(
                            bitmap = effectivePreview,
                            contentDescription = stringResource(R.string.create_post_selected_media_desc),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        VexIcon(
                            id = VexIcons.Play,
                            contentDescription = null,
                            size = 28.dp,
                            tint = colors.textMuted,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(VexSpace.s1),
                ) {
                    Text(
                        text = state.title.ifBlank { stringResource(R.string.create_post_title_placeholder) },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (state.title.isNotBlank()) colors.text else colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.edit_post_change_media),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primaryText,
                    )
                }
            }

            VexTextField(
                value = state.title,
                onValueChange = onTitleChange,
                variant = VexTextFieldVariant.Boxed,
                label = stringResource(R.string.create_post_title_label),
                placeholder = stringResource(R.string.create_post_title_placeholder),
            )

            VexDropdown(
                label = stringResource(R.string.create_post_category_label),
                selectedOption = state.selectedCategoryName,
                options = categoryNames,
                onOptionSelected = { nama ->
                    state.categories.firstOrNull { it.nama == nama }?.let { onCategoryChange(it.idKategori) }
                },
                enabled = !state.isLoadingCategories && categoryNames.isNotEmpty(),
            )

            VexTextArea(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = stringResource(R.string.create_post_description_label),
                placeholder = stringResource(R.string.create_post_description_placeholder),
                minHeight = 130.dp,
            )

            // Bagian Pemilihan Pameran (Kartu visual dengan thumbnail banner)
            Column(verticalArrangement = Arrangement.spacedBy(VexSpace.s2)) {
                Text(
                    text = stringResource(R.string.create_post_exhibition_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.text,
                )
                Text(
                    text = stringResource(R.string.create_post_exhibition_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )

                when {
                    state.isLoadingExhibitions -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = colors.primary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }

                    state.exhibitions.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(VexRadius.md))
                                .background(colors.surfaceInput)
                                .border(1.dp, colors.border, RoundedCornerShape(VexRadius.md))
                                .padding(VexSpace.s4),
                            verticalArrangement = Arrangement.spacedBy(VexSpace.s2),
                        ) {
                            Text(
                                text = stringResource(R.string.create_post_exhibition_empty_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.text,
                            )
                            Text(
                                text = stringResource(R.string.create_post_exhibition_empty_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                            Spacer(modifier = Modifier.height(VexSpace.s1))
                            VexButton(
                                text = stringResource(R.string.create_post_exhibition_create_cta),
                                onClick = onCreateExhibitClick,
                                variant = VexButtonVariant.Primary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(VexSpace.s2)) {
                            state.exhibitions.forEach { exhibit ->
                                ExhibitionSelectCard(
                                    exhibit = exhibit,
                                    isSelected = exhibit.idPameran == state.selectedExhibitionId,
                                    onClick = { onExhibitionChange(exhibit.idPameran) },
                                )
                            }
                        }
                    }
                }
            }

            FormErrorText(
                message = state.serverErrorMessage ?: state.errorMessage(),
                visible = state.serverErrorMessage != null || state.errorMessageRes != null,
            )

            Spacer(modifier = Modifier.height(VexSpace.s8))
        }
    }
}

@Composable
private fun ExhibitionSelectCard(
    exhibit: PameranItemModel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = VexTheme.colors
    val bannerBitmap = rememberAsyncImageBitmap(exhibit.banner)
    val shape = RoundedCornerShape(VexRadius.md)
    val capacityInfo = when (exhibit.tipe?.trim()?.lowercase()) {
        "sedang", "medium" -> "${stringResource(R.string.create_exhibit_template_medium)} • 14 panels"
        "besar", "big" -> "${stringResource(R.string.create_exhibit_template_big)} • 21 panels"
        "acara", "event" -> "${stringResource(R.string.create_exhibit_template_event)} • 60 panels"
        else -> "${stringResource(R.string.create_exhibit_template_small)} • 9 panels"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surfaceInput)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) colors.primary else colors.border,
                shape = shape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(VexSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VexSpace.s3),
    ) {
        Box(
            modifier = Modifier
                .width(88.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(VexRadius.xs))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (bannerBitmap != null) {
                Image(
                    bitmap = bannerBitmap,
                    contentDescription = exhibit.judul,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                VexIcon(
                    id = VexIcons.Tab.Exhibits.filled,
                    contentDescription = null,
                    size = 22.dp,
                    tint = colors.textMuted,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = exhibit.judul,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = capacityInfo,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.primary else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) colors.primary else colors.border,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                VexIcon(
                    id = VexIcons.Check,
                    contentDescription = null,
                    size = 13.dp,
                    tint = colors.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun FormErrorText(
    message: String?,
    visible: Boolean,
) {
    if (!visible || message == null) return

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = VexTheme.colors.danger,
        modifier = Modifier.padding(top = VexSpace.s1),
    )
}

@Composable
private fun CreatePostUiState.errorMessage(): String? {
    val resId = errorMessageRes ?: return null
    return stringResource(resId)
}

// -------------------------------------------------------------
// Previews
// -------------------------------------------------------------

private val previewCategories = listOf(
    KategoriModel(idKategori = 1, nama = "3D"),
    KategoriModel(idKategori = 2, nama = "UI/UX"),
    KategoriModel(idKategori = 3, nama = "Animation"),
)

private val previewExhibitions = listOf(
    PameranItemModel(idPameran = 1, judul = "Pameran Digital 2026", tipe = "kecil"),
    PameranItemModel(idPameran = 2, judul = "Expo Animasi Kampus", tipe = "acara"),
)

@Preview(name = "Step 1: Media Picker - Light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CreatePostMediaPickerLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        CreatePostScreen(
            state = CreatePostUiState(step = CreatePostStep.MediaPicker, categories = previewCategories),
            mediaPreview = null,
            onCloseClick = {},
            onBackClick = {},
            onNextClick = {},
            onPickMediaClick = {},
            onPostClick = {},
            onContentTypeSelected = {},
            onTitleChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
        )
    }
}

@Preview(name = "Step 1: Media Picker - Dark", showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun CreatePostMediaPickerDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        CreatePostScreen(
            state = CreatePostUiState(step = CreatePostStep.MediaPicker, categories = previewCategories),
            mediaPreview = null,
            onCloseClick = {},
            onBackClick = {},
            onNextClick = {},
            onPickMediaClick = {},
            onPostClick = {},
            onContentTypeSelected = {},
            onTitleChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
        )
    }
}

@Preview(name = "Step 2: Details - Light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CreatePostDetailsLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        CreatePostScreen(
            state = CreatePostUiState(
                step = CreatePostStep.Details,
                mediaUriString = "content://preview",
                categories = previewCategories,
                categoryId = 1,
                exhibitions = previewExhibitions,
                selectedExhibitionId = 1,
                title = "Poster Karya",
                description = "Deskripsi karya.",
            ),
            mediaPreview = null,
            onCloseClick = {},
            onBackClick = {},
            onNextClick = {},
            onPickMediaClick = {},
            onPostClick = {},
            onContentTypeSelected = {},
            onTitleChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
        )
    }
}

@Preview(name = "Step 2: Details - Dark", showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun CreatePostDetailsDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        CreatePostScreen(
            state = CreatePostUiState(
                step = CreatePostStep.Details,
                mediaUriString = "content://preview",
                categories = previewCategories,
                categoryId = 1,
                exhibitions = previewExhibitions,
                selectedExhibitionId = 2,
                title = "Poster Karya",
                description = "Deskripsi karya.",
            ),
            mediaPreview = null,
            onCloseClick = {},
            onBackClick = {},
            onNextClick = {},
            onPickMediaClick = {},
            onPostClick = {},
            onContentTypeSelected = {},
            onTitleChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
        )
    }
}
