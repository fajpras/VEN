package com.ven.app.ui.screens.unity

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.unity3d.player.UnityPlayer
import com.unity3d.player.UnityPlayerGameActivity
import com.ven.app.core.config.AppConfig
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexTheme
import java.io.DataOutputStream
import java.io.File
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.json.JSONObject

/**
 * Activity Unity khusus VEN yang membungkus [UnityPlayerGameActivity] dengan:
 * 1. SATU loading screen minimalis sejak frame 0 (tanpa blank screen & tanpa loading ganda).
 * 2. Bottom Sheet Compose (slide-up dari bawah) untuk form karya di EditorScene.
 * 3. Pop-up Detail Karya Compose di PlayScene (sinkron tema & bahasa aplikasi, isi karya asli).
 */
class VenUnityActivity : UnityPlayerGameActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var loadingOverlay: FrameLayout? = null
    private var composeOverlayView: ComposeView? = null
    private var progressFillView: View? = null
    private var progressAnimator: ValueAnimator? = null
    private var isDismissed = false

    private var isDarkMode by mutableStateOf(false)
    private var isIndonesian by mutableStateOf(true)

    // State Compose untuk Form EditorScene (slide-up) & Pop-up Detail Karya PlayScene
    private var isArtworkFormVisible by mutableStateOf(false)
    private var artworkFormState by mutableStateOf(UnityArtworkFormState())

    private var isArtworkDetailVisible by mutableStateOf(false)
    private var artworkDetailState by mutableStateOf(UnityArtworkDetailState())

    private lateinit var mediaPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    private val fallbackDismissRunnable = Runnable {
        dismissOverlayInternal()
    }

    private val hideComposeOverlayRunnable = Runnable {
        if (!isArtworkFormVisible && !isArtworkDetailVisible) {
            composeOverlayView?.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mediaPickerLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            if (uri != null) {
                handlePickedArtworkMedia(uri)
            }
        }
        syncThemeAndLanguageFromIntent(intent)
        super.onCreate(savedInstanceState)
        applySystemBarTheme()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        syncThemeAndLanguageFromIntent(intent)
        applySystemBarTheme()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            if (isArtworkFormVisible) {
                if (!artworkFormState.isUploading) cancelArtworkForm()
                return true
            }
            if (isArtworkDetailVisible) {
                dismissArtworkDetail()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun syncThemeAndLanguageFromIntent(targetIntent: Intent?) {
        isDarkMode = targetIntent?.getStringExtra("themeMode")?.equals("dark", ignoreCase = true) == true
        val lang = targetIntent?.getStringExtra("language") ?: "id"
        isIndonesian = lang.equals("id", ignoreCase = true) || lang.equals("in", ignoreCase = true)
    }

    override fun onCreateSurfaceView() {
        super.onCreateSurfaceView()
        activeInstance = WeakReference(this)
        syncThemeAndLanguageFromIntent(intent)

        val rootLayout = findViewById<FrameLayout>(contentViewId)
        if (rootLayout != null) {
            attachComposeOverlay(rootLayout)
            attachMinimalLoadingOverlay(rootLayout)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(fallbackDismissRunnable)
        mainHandler.removeCallbacks(hideComposeOverlayRunnable)
        progressAnimator?.cancel()
        if (activeInstance?.get() === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    private fun showComposeOverlay() {
        mainHandler.removeCallbacks(hideComposeOverlayRunnable)
        composeOverlayView?.visibility = View.VISIBLE
    }

    private fun scheduleHideComposeOverlayIfIdle() {
        mainHandler.removeCallbacks(hideComposeOverlayRunnable)
        if (!isArtworkFormVisible && !isArtworkDetailVisible) {
            mainHandler.postDelayed(hideComposeOverlayRunnable, 280L)
        }
    }

    private fun applySystemBarTheme() {
        isDarkMode = intent?.getStringExtra("themeMode")?.equals("dark", ignoreCase = true) == true
        val bgColor = if (isDarkMode) Color.parseColor("#1F1F1F") else Color.parseColor("#FFFFFF")
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
    }

    private fun attachComposeOverlay(rootLayout: FrameLayout) {
        val composeView = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            elevation = dp(16).toFloat()
            visibility = View.GONE
            setViewTreeLifecycleOwner(this@VenUnityActivity)
            setViewTreeViewModelStoreOwner(this@VenUnityActivity)
            setViewTreeSavedStateRegistryOwner(this@VenUnityActivity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val mode = if (isDarkMode) ThemeMode.Dark else ThemeMode.Light
                VexTheme(mode = mode) {
                    UnityArtworkFormSheet(
                        visible = isArtworkFormVisible,
                        state = artworkFormState,
                        isIndonesian = isIndonesian,
                        onTitleChange = {
                            artworkFormState = artworkFormState.copy(title = it, errorMessage = null)
                        },
                        onCategoryChange = {
                            artworkFormState = artworkFormState.copy(categoryId = it, errorMessage = null)
                        },
                        onDescriptionChange = {
                            artworkFormState = artworkFormState.copy(description = it, errorMessage = null)
                        },
                        onPickMediaClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                            )
                        },
                        onSaveClick = { submitArtworkFormToUnity() },
                        onDismiss = { cancelArtworkForm() },
                    )

                    UnityArtworkDetailDialog(
                        visible = isArtworkDetailVisible,
                        detail = artworkDetailState,
                        isIndonesian = isIndonesian,
                        onDismiss = { dismissArtworkDetail() },
                    )
                }
            }
        }
        composeOverlayView = composeView
        rootLayout.addView(composeView)
    }

    private fun attachMinimalLoadingOverlay(rootLayout: FrameLayout) {
        isDismissed = false
        val isEditor = !intent?.getStringExtra("mode").equals("viewer", ignoreCase = true)

        val bgColor = if (isDarkMode) Color.parseColor("#1F1F1F") else Color.parseColor("#FFFFFF")
        val primaryColor = Color.parseColor("#BC18F6")
        val trackColor = if (isDarkMode) Color.parseColor("#35353D") else Color.parseColor("#E6E6EC")
        val mutedColor = if (isDarkMode) Color.parseColor("#9E9EA8") else Color.parseColor("#6E6E78")

        val overlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(bgColor)
            elevation = dp(24).toFloat()
            isClickable = true
            isFocusable = true
        }

        val centerCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ).apply {
                marginStart = dp(32)
                marginEnd = dp(32)
            }
        }

        // Brand VEN minimalis
        val brandText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(12)
            }
            text = "VEN"
            setTextColor(primaryColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
        }
        centerCol.addView(brandText)

        // 1 baris status singkat sesuai bahasa aplikasi
        val statusText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(20)
            }
            text = when {
                isEditor && isIndonesian -> "Membuka Ruang 3D…"
                isEditor -> "Opening 3D Editor…"
                isIndonesian -> "Memuat Pameran 3D…"
                else -> "Opening 3D Exhibition…"
            }
            setTextColor(mutedColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
        }
        centerCol.addView(statusText)

        // Progress bar tipis (3dp)
        val trackWidthPx = (resources.displayMetrics.widthPixels * 0.55f).toInt().coerceAtLeast(dp(160))
        val trackHeightPx = dp(3)
        val progressTrack = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(trackWidthPx, trackHeightPx)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = trackHeightPx / 2f
                setColor(trackColor)
            }
        }

        val fill = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(12), ViewGroup.LayoutParams.MATCH_PARENT)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = trackHeightPx / 2f
                setColor(primaryColor)
            }
        }
        progressFillView = fill
        progressTrack.addView(fill)
        centerCol.addView(progressTrack)

        overlay.addView(centerCol)
        rootLayout.addView(overlay)
        loadingOverlay = overlay

        progressAnimator = ValueAnimator.ofFloat(0.08f, 0.92f).apply {
            duration = if (isEditor) 2200L else 2800L
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener { anim ->
                updateProgressBar(anim.animatedValue as Float, trackWidthPx)
            }
            start()
        }

        val fallbackDelayMs = if (isEditor) 2700L else 3400L
        mainHandler.postDelayed(fallbackDismissRunnable, fallbackDelayMs)
    }

    private fun updateProgressBar(fraction: Float, trackWidthPx: Int) {
        val fill = progressFillView ?: return
        val clamped = fraction.coerceIn(0.05f, 1f)
        val lp = fill.layoutParams
        lp.width = (trackWidthPx * clamped).toInt().coerceAtLeast(dp(10))
        fill.layoutParams = lp
    }

    private fun dismissOverlayInternal() {
        if (isDismissed) return
        isDismissed = true
        mainHandler.removeCallbacks(fallbackDismissRunnable)
        progressAnimator?.cancel()

        val overlay = loadingOverlay ?: return
        val trackWidthPx = (resources.displayMetrics.widthPixels * 0.55f).toInt().coerceAtLeast(dp(160))
        updateProgressBar(1f, trackWidthPx)

        overlay.animate()
            .alpha(0f)
            .setDuration(260L)
            .withEndAction {
                (overlay.parent as? ViewGroup)?.removeView(overlay)
                loadingOverlay = null
                progressFillView = null
            }
            .start()
    }

    // ---------- EditorScene Artwork Form (Kotlin Compose Bottom Sheet) ----------

    private fun openArtworkFormInternal(jsonPayload: String) {
        try {
            val obj = if (jsonPayload.isNotBlank()) JSONObject(jsonPayload) else JSONObject()
            artworkFormState = UnityArtworkFormState(
                title = obj.optString("title", ""),
                description = obj.optString("description", ""),
                categoryId = obj.optInt("categoryId", 1).coerceIn(1, UNITY_ARTWORK_CATEGORIES.size),
                contentUrl = obj.optString("contentUrl", ""),
                isVideo = obj.optBoolean("isVideo", false),
                isUploading = false,
                localPreview = null,
                errorMessage = null,
            )
            showComposeOverlay()
            isArtworkFormVisible = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse openArtworkForm payload: ${e.message}")
        }
    }

    private fun handlePickedArtworkMedia(uri: Uri) {
        val mime = (contentResolver.getType(uri) ?: "image/jpeg").lowercase()
        val isVideo = mime.startsWith("video/")
        artworkFormState = artworkFormState.copy(
            isUploading = true,
            isVideo = isVideo,
            errorMessage = null,
        )

        Thread {
            try {
                val ext = when (mime) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    "video/mp4" -> "mp4"
                    "video/webm" -> "webm"
                    "video/quicktime" -> "mov"
                    else -> "jpg"
                }
                val tempFile = File(cacheDir, "unity_panel_${System.currentTimeMillis()}.$ext")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                val localThumb = if (!isVideo) {
                    runCatching { BitmapFactory.decodeFile(tempFile.absolutePath)?.asImageBitmap() }.getOrNull()
                } else {
                    null
                }

                val uploadedUrl = uploadMediaToServer(tempFile, mime) ?: tempFile.absolutePath

                runOnUiThread {
                    artworkFormState = artworkFormState.copy(
                        contentUrl = uploadedUrl,
                        isVideo = isVideo,
                        localPreview = localThumb,
                        isUploading = false,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle picked media: ${e.message}")
                runOnUiThread {
                    artworkFormState = artworkFormState.copy(
                        isUploading = false,
                        errorMessage = if (isIndonesian) "Gagal memproses media." else "Failed to process media.",
                    )
                }
            }
        }.start()
    }

    private fun uploadMediaToServer(file: File, mimeType: String): String? {
        return try {
            val baseUrl = intent?.getStringExtra("apiBaseUrl")?.takeIf { it.isNotBlank() }
                ?: AppConfig.baseUrl
            val token = intent?.getStringExtra("authToken").orEmpty()
            val endpoint = baseUrl.trimEnd('/') + "/karya/upload-media"
            val boundary = "----VenBoundary${UUID.randomUUID()}"

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            DataOutputStream(conn.outputStream).use { out ->
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n")
                out.writeBytes("Content-Type: $mimeType\r\n\r\n")
                file.inputStream().use { it.copyTo(out) }
                out.writeBytes("\r\n--$boundary--\r\n")
                out.flush()
            }

            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val data = json.optJSONObject("data")
                val url = data?.optString("url")?.takeIf { it.isNotBlank() }
                    ?: data?.optString("file_url")?.takeIf { it.isNotBlank() }
                    ?: json.optString("url").takeIf { it.isNotBlank() }
                url
            } else {
                Log.w(TAG, "upload-media returned HTTP ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "upload-media failed: ${e.message}")
            null
        }
    }

    private fun submitArtworkFormToUnity() {
        val state = artworkFormState
        if (state.isUploading) return
        if (state.title.isBlank()) {
            artworkFormState = state.copy(
                errorMessage = if (isIndonesian) "Judul karya wajib diisi." else "Title is required.",
            )
            return
        }
        if (state.contentUrl.isBlank()) {
            artworkFormState = state.copy(
                errorMessage = if (isIndonesian) "Pilih foto atau video terlebih dahulu." else "Please choose a photo or video first.",
            )
            return
        }

        val resultJson = JSONObject().apply {
            put("title", state.title.trim())
            put("description", state.description.trim())
            put("categoryId", state.categoryId)
            put("contentUrl", state.contentUrl.trim())
            put("isVideo", state.isVideo)
        }.toString()

        isArtworkFormVisible = false
        scheduleHideComposeOverlayIfIdle()
        UnityPlayer.UnitySendMessage("ArtworkDialog", "OnNativeArtworkFormSaved", resultJson)
    }

    private fun cancelArtworkForm() {
        isArtworkFormVisible = false
        scheduleHideComposeOverlayIfIdle()
        UnityPlayer.UnitySendMessage("ArtworkDialog", "OnNativeArtworkFormCancelled", "")
    }

    // ---------- PlayScene Artwork Detail Pop-up ----------

    private fun showArtworkDetailInternal(
        title: String,
        description: String,
        contentType: String,
        contentUrl: String,
    ) {
        artworkDetailState = UnityArtworkDetailState(
            title = title,
            description = description,
            contentType = contentType,
            contentUrl = contentUrl,
        )
        showComposeOverlay()
        isArtworkDetailVisible = true
    }

    private fun dismissArtworkDetail() {
        isArtworkDetailVisible = false
        scheduleHideComposeOverlayIfIdle()
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics,
        ).toInt()

    companion object {
        private const val TAG = "VenUnityActivity"

        @Volatile
        private var activeInstance: WeakReference<VenUnityActivity>? = null

        @JvmStatic
        fun dismissLoadingOverlay() {
            val activity = activeInstance?.get() ?: return
            activity.runOnUiThread {
                activity.dismissOverlayInternal()
            }
        }

        @JvmStatic
        fun openArtworkForm(jsonPayload: String) {
            val activity = activeInstance?.get() ?: return
            activity.runOnUiThread {
                activity.openArtworkFormInternal(jsonPayload)
            }
        }

        @JvmStatic
        fun showArtworkDetail(
            title: String,
            description: String,
            contentType: String,
            contentUrl: String,
        ) {
            val activity = activeInstance?.get() ?: return
            activity.runOnUiThread {
                activity.showArtworkDetailInternal(title, description, contentType, contentUrl)
            }
        }
    }
}
