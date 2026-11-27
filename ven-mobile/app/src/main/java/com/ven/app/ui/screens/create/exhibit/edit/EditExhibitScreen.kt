package com.ven.app.ui.screens.create.exhibit.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ven.app.R
import com.ven.app.ui.components.layout.VexBottomActionBar
import com.ven.app.ui.components.layout.VexTopBar
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.components.ui.VexButtonVariant
import com.ven.app.ui.components.ui.VexTextField
import com.ven.app.ui.components.ui.VexTextFieldVariant
import com.ven.app.ui.components.ui.rememberAsyncImageBitmap
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

@Composable
fun EditExhibitScreen(
    state: EditExhibitUiState,
    bannerPreview: ImageBitmap?,
    onBackClick: () -> Unit,
    onPickBannerClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onOpen3dEditorClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val remoteBannerBitmap = rememberAsyncImageBitmap(
        if (bannerPreview == null) state.existingBannerUrl else null,
    )
    val effectiveBanner = bannerPreview ?: remoteBannerBitmap

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            VexTopBar.Back(
                title = stringResource(R.string.edit_exhibit_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            VexBottomActionBar {
                VexButton(
                    text = stringResource(R.string.edit_exhibit_save),
                    onClick = onSaveClick,
                    variant = VexButtonVariant.Primary,
                    loading = state.isSaving,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = VexSpace.s6, vertical = VexSpace.s4),
                verticalArrangement = Arrangement.spacedBy(VexSpace.s5),
            ) {
                // Cover Banner (16:9)
                Column(verticalArrangement = Arrangement.spacedBy(VexSpace.s2)) {
                    Text(
                        text = stringResource(R.string.create_exhibit_cover_banner),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.text,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(VexRadius.md))
                            .background(colors.surfaceInput)
                            .border(1.dp, colors.border, RoundedCornerShape(VexRadius.md))
                            .clickable(role = Role.Button, onClick = onPickBannerClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (effectiveBanner != null) {
                            Image(
                                bitmap = effectiveBanner,
                                contentDescription = stringResource(R.string.create_exhibit_cover_banner),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.create_exhibit_cover_placeholder),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(VexSpace.s4),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(VexSpace.s3)
                                .clip(RoundedCornerShape(VexRadius.full))
                                .background(colors.surface.copy(alpha = 0.88f))
                                .padding(horizontal = VexSpace.s3, vertical = VexSpace.s1),
                        ) {
                            Text(
                                text = stringResource(R.string.edit_post_change_media),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.text,
                            )
                        }
                    }
                }

                // Exhibition Title
                Column(verticalArrangement = Arrangement.spacedBy(VexSpace.s1)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.create_exhibit_name_label),
                            style = MaterialTheme.typography.titleSmall,
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
                        onValueChange = onTitleChange,
                        variant = VexTextFieldVariant.Boxed,
                        placeholder = stringResource(R.string.create_exhibit_name_placeholder),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Open 3D Room Editor Button
                VexButton(
                    text = stringResource(R.string.edit_exhibit_open_3d),
                    onClick = onOpen3dEditorClick,
                    variant = VexButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth(),
                )

                val errorText = state.serverErrorMessage ?: state.errorMessageRes?.let { stringResource(it) }
                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.danger,
                    )
                }

                Spacer(modifier = Modifier.height(VexSpace.s4))
            }

            if (state.isLoading) {
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
}

@Preview(name = "EditExhibitScreen - Light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun EditExhibitScreenLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        EditExhibitScreen(
            state = EditExhibitUiState(
                exhibitId = "01a21286-0001-7000-8000-000000000001",
                title = "Galeri Digital 2026",
                tipe = "sedang",
                wallColorHex = "#E8DFCA",
                isLoading = false,
            ),
            bannerPreview = null,
            onBackClick = {},
            onPickBannerClick = {},
            onTitleChange = {},
            onOpen3dEditorClick = {},
            onSaveClick = {},
        )
    }
}

@Preview(name = "EditExhibitScreen - Dark", showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun EditExhibitScreenDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        EditExhibitScreen(
            state = EditExhibitUiState(
                exhibitId = "01a21286-0001-7000-8000-000000000001",
                title = "Galeri Digital 2026",
                tipe = "sedang",
                wallColorHex = "#2D3142",
                isLoading = false,
            ),
            bannerPreview = null,
            onBackClick = {},
            onPickBannerClick = {},
            onTitleChange = {},
            onOpen3dEditorClick = {},
            onSaveClick = {},
        )
    }
}
