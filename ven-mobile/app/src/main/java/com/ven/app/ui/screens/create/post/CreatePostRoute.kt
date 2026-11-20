package com.ven.app.ui.screens.create.post

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.activity.compose.BackHandler
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
import com.ven.app.R
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_MEDIA_BYTES = 50L * 1024 * 1024

private val MIME_IMAGE = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
private val MIME_VIDEO = setOf("video/mp4", "video/webm", "video/quicktime")

/**
 * Menghubungkan [CreatePostScreen] dengan pemilih media Android, pratinjau lokal,
 * serta pengunggahan nyata ke `POST /api/karya`.
 */
@Composable
fun CreatePostRoute(
    onCloseClick: () -> Unit,
    onPostSuccess: () -> Unit,
    onNavigateToExhibit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreatePostViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var mediaFile by remember { mutableStateOf<File?>(null) }
    var mediaPreview by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CreatePostEvent.PostSuccess -> onPostSuccess()
                CreatePostEvent.SwitchToExhibit -> onNavigateToExhibit()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { mediaFile?.delete() }
    }

    BackHandler {
        if (!viewModel.onBackClick()) {
            onCloseClick()
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            val mimeType = (context.contentResolver.getType(uri) ?: "application/octet-stream").lowercase()
            if (mimeType !in MIME_IMAGE && mimeType !in MIME_VIDEO) {
                viewModel.onMediaDitolak(R.string.create_post_media_invalid)
                return@launch
            }

            val berkas = withContext(Dispatchers.IO) {
                runCatching {
                    val ekstensi = ekstensiUntuk(mimeType)
                    val file = File(context.cacheDir, "karya_${System.currentTimeMillis()}.$ekstensi")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    file
                }.getOrNull()
            }

            if (berkas == null || !berkas.exists() || berkas.length() == 0L) {
                berkas?.delete()
                viewModel.onMediaDitolak(R.string.create_post_media_invalid)
                return@launch
            }

            if (berkas.length() > MAX_MEDIA_BYTES) {
                berkas.delete()
                viewModel.onMediaDitolak(R.string.create_post_media_too_large)
                return@launch
            }

            mediaFile?.delete()
            mediaFile = berkas
            mediaPreview = withContext(Dispatchers.IO) { buatPratinjau(berkas, mimeType) }
            viewModel.onMediaSelected(uri.toString(), mimeType)
        }
    }

    CreatePostScreen(
        state = state,
        mediaPreview = mediaPreview,
        onCloseClick = onCloseClick,
        onBackClick = { viewModel.onBackClick() },
        onNextClick = viewModel::onNextClick,
        onPickMediaClick = {
            mediaPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
            )
        },
        onPostClick = {
            val berkas = mediaFile
            val mimeType = state.mediaMimeType
            if (berkas != null && mimeType != null) {
                viewModel.submitPost(berkas, mimeType)
            } else {
                viewModel.onMediaDitolak(R.string.create_post_media_required)
            }
        },
        onContentTypeSelected = viewModel::onContentTypeSelected,
        onTitleChange = viewModel::onTitleChange,
        onCategoryChange = viewModel::onCategoryChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        modifier = modifier,
    )
}

private fun ekstensiUntuk(mimeType: String): String = when (mimeType) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    "video/webm" -> "webm"
    "video/quicktime" -> "mov"
    "video/mp4" -> "mp4"
    else -> "jpg"
}

private fun buatPratinjau(file: File, mimeType: String): ImageBitmap? {
    return if (mimeType.startsWith("video/")) {
        buatThumbnailVideo(file)
    } else {
        dekodeGambar(file)
    }
}

private fun dekodeGambar(file: File, dimensiMaks: Int = 1080): ImageBitmap? {
    val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, batas)

    if (batas.outWidth <= 0 || batas.outHeight <= 0) return null

    var sampel = 1
    while (max(batas.outWidth, batas.outHeight) / sampel > dimensiMaks * 2) {
        sampel *= 2
    }

    val opsi = BitmapFactory.Options().apply { inSampleSize = sampel }
    return BitmapFactory.decodeFile(file.absolutePath, opsi)?.asImageBitmap()
}

private fun buatThumbnailVideo(file: File): ImageBitmap? {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retriever.release()
        frame?.asImageBitmap()
    }.getOrNull()
}
