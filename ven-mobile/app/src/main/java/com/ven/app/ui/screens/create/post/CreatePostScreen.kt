package com.ven.app.ui.screens.create.post

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ven.app.R
import com.ven.app.data.api.models.KategoriModel
import com.ven.app.ui.components.layout.VexTopBar
import com.ven.app.ui.components.ui.CreateContentType
import com.ven.app.ui.components.ui.SegmentedPill
import com.ven.app.ui.components.ui.VexDropdown
import com.ven.app.ui.components.ui.VexTextArea
import com.ven.app.ui.components.ui.VexTextField
import com.ven.app.ui.components.ui.VexTextFieldVariant
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

/**
 * Layar Create Post (stateless) � Langkah 1 pemilihan media dan Langkah 2 formulir detail.
 *
 * Seluruh efek Android (photo picker, pratinjau bitmap, unggah berkas) ditangani oleh
 * [CreatePostRoute] agar layar ini tetap murni dan dapat dipratinjau/diuji tanpa konteks.
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
) {
    val colors = VexTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
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
                    onTitleChange = onTitleChange,
                    onCategoryChange = onCategoryChange,
                    onDescriptionChange = onDescriptionChange,
                )
            }
        }

        if (state.isSubmitting) {
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
                        top = VexSpace.s4,
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

                Text(
                    text = if (state.isMediaSelected) {
                        stringResource(R.string.create_post_change_media)
                    } else {
                        stringResource(R.string.create_post_media_formats)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
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
    val shape = RoundedCornerShape(VexRadius.md)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceInput)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colors.text.copy(alpha = 0.12f)),
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
                    size = 56.dp,
                    tint = colors.textMuted,
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VexSpace.s3),
                modifier = Modifier.padding(VexSpace.s8),
            ) {
                VexIcon(
                    id = VexIcons.Add,
                    contentDescription = null,
                    size = 40.dp,
                    tint = colors.textMuted,
                )

                Text(
                    text = stringResource(R.string.create_post_choose_media),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.text,
                )

                Text(
                    text = stringResource(R.string.create_post_media_formats),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
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
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Int) -> Unit,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val scrollState = rememberScrollState()
    val categoryNames = state.categories.map { it.nama }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        VexTopBar.Create(
            title = stringResource(R.string.create_post_title),
            actionText = stringResource(R.string.create_post_action),
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
                .imePadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(colors.surfaceInput),
                contentAlignment = Alignment.Center,
            ) {
                if (mediaPreview != null) {
                    Image(
                        bitmap = mediaPreview,
                        contentDescription = stringResource(R.string.create_post_selected_media_desc),
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = VexSpace.s3),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    VexIcon(
                        id = VexIcons.Play,
                        contentDescription = null,
                        size = 56.dp,
                        tint = colors.textMuted,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VexSpace.s6, vertical = VexSpace.s5),
                verticalArrangement = Arrangement.spacedBy(VexSpace.s4),
            ) {
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
                    minHeight = 175.dp,
                )

                FormErrorText(
                    message = state.serverErrorMessage ?: state.errorMessage(),
                    visible = state.serverErrorMessage != null || state.errorMessageRes != null,
                )

                Spacer(modifier = Modifier.height(VexSpace.s8))
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
