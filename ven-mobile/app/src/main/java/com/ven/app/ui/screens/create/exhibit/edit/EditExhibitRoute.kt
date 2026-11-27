package com.ven.app.ui.screens.create.exhibit.edit

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditExhibitRoute(
    exhibitId: String,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    onOpen3dEditor: (templateId: String, title: String, tipe: String, bannerUrl: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditExhibitViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var bannerFile by remember { mutableStateOf<File?>(null) }
    var bannerMimeType by remember { mutableStateOf("image/jpeg") }
    var bannerPreview by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(exhibitId) {
        viewModel.muat(exhibitId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditExhibitEvent.SaveSuccess -> onSaved()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { bannerFile?.delete() }
    }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val mime = (context.contentResolver.getType(uri) ?: "image/jpeg").lowercase()
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    val ext = when (mime) {
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        else -> "jpg"
                    }
                    val temp = File(context.cacheDir, "edit_banner_${System.currentTimeMillis()}.$ext")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    temp
                }.getOrNull()
            }
            if (file != null && file.exists() && file.length() > 0L) {
                bannerFile?.delete()
                bannerFile = file
                bannerMimeType = mime
                bannerPreview = withContext(Dispatchers.IO) {
                    runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
                }
                viewModel.onBannerSelected(uri.toString())
            }
        }
    }

    EditExhibitScreen(
        state = state,
        bannerPreview = bannerPreview,
        onBackClick = onBackClick,
        onPickBannerClick = {
            bannerPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onTitleChange = viewModel::onTitleChange,
        onOpen3dEditorClick = {
            onOpen3dEditor(
                state.unityTemplateId,
                state.title,
                state.tipe,
                state.existingBannerUrl.orEmpty(),
            )
        },
        onSaveClick = {
            viewModel.simpan(bannerFile = bannerFile, bannerMimeType = bannerMimeType)
        },
        modifier = modifier,
    )
}
