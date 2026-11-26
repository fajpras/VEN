package com.ven.app.ui.components.ui

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.ViewOutlineProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ven.app.R
import com.ven.app.data.cache.ImageCacheManager
import kotlinx.coroutines.delay
import java.io.File

private const val AUTOPLAY_DEBOUNCE_MS = 150L

/**
 * Pemutar video bergaya Instagram yang aman untuk feed data besar (bulk):
 * - Hanya mengalokasikan MediaPlayer (hardware codec) saat item benar-benar aktif &
 *   lolos debounce 150ms, sehingga paling banyak 1 codec aktif pada satu waktu.
 * - Langsung melepas MediaPlayer saat di-scroll keluar viewport atau app ke background
 *   untuk mencegah crash kehabisan MediaCodec / OOM.
 * - Ketuk video atau tombol sudut kanan bawah untuk toggle mute/unmute.
 */
@Composable
fun AutoPlayVideoPlayer(
    videoUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    thumbnailBitmap: ImageBitmap? = null,
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current

    remember {
        ImageCacheManager.init(context)
        true
    }

    var localFile by remember(videoUrl) { mutableStateOf<File?>(null) }
    var isMuted by remember { mutableStateOf(true) }
    var isLifecycleResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var debouncedShouldPlay by remember(videoUrl) { mutableStateOf(false) }
    var isPrepared by remember(videoUrl) { mutableStateOf(false) }
    var hasRenderedFrame by remember(videoUrl) { mutableStateOf(false) }
    var mediaPlayerRef by remember(videoUrl) { mutableStateOf<MediaPlayer?>(null) }
    var textureViewRef by remember(videoUrl) { mutableStateOf<TextureView?>(null) }
    var activeSurface by remember(videoUrl) { mutableStateOf<Surface?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isLifecycleResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isLifecycleResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldPlay = isPlaying && isLifecycleResumed

    // Debounce saat mulai putar agar fling cepat melewati banyak video tidak bikin churn codec,
    // tetapi langsung hentikan tanpa jeda saat item keluar fokus.
    LaunchedEffect(shouldPlay) {
        if (shouldPlay) {
            delay(AUTOPLAY_DEBOUNCE_MS)
            debouncedShouldPlay = true
        } else {
            debouncedShouldPlay = false
        }
    }

    // Unduh ke disk cache hanya ketika item benar-benar akan diputar (lazy download)
    LaunchedEffect(videoUrl, debouncedShouldPlay) {
        if (debouncedShouldPlay && localFile == null) {
            localFile = ImageCacheManager.loadCachedFile(videoUrl)
        }
    }

    LaunchedEffect(isMuted, isPrepared) {
        val player = mediaPlayerRef ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect
        try {
            val vol = if (isMuted) 0f else 1f
            player.setVolume(vol, vol)
        } catch (_: Throwable) {
        }
    }

    // Alokasikan MediaPlayer HANYA saat debouncedShouldPlay == true; lepas segera saat false
    DisposableEffect(localFile, debouncedShouldPlay) {
        val file = localFile
        if (!debouncedShouldPlay || file == null || !file.exists()) {
            isPrepared = false
            hasRenderedFrame = false
            onDispose { }
        } else {
            val player = MediaPlayer()
            mediaPlayerRef = player
            isPrepared = false
            hasRenderedFrame = false

            try {
                player.setDataSource(file.absolutePath)
                player.isLooping = true
                player.setOnVideoSizeChangedListener { _, videoW, videoH ->
                    textureViewRef?.let { tv ->
                        applyCenterCropTransform(tv, videoW, videoH)
                    }
                }
                player.setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        hasRenderedFrame = true
                    }
                    false
                }
                player.setOnPreparedListener { mp ->
                    if (mediaPlayerRef !== mp) return@setOnPreparedListener
                    isPrepared = true
                    textureViewRef?.let { tv ->
                        applyCenterCropTransform(tv, mp.videoWidth, mp.videoHeight)
                    }
                    try {
                        val vol = if (isMuted) 0f else 1f
                        mp.setVolume(vol, vol)
                        mp.start()
                        hasRenderedFrame = true
                    } catch (_: Throwable) {
                    }
                }
                player.setOnErrorListener { mp, _, _ ->
                    if (mediaPlayerRef === mp) {
                        isPrepared = false
                        hasRenderedFrame = false
                    }
                    true
                }
                activeSurface?.takeIf { it.isValid }?.let { player.setSurface(it) }
                player.prepareAsync()
            } catch (_: Throwable) {
                safeReleasePlayer(player)
                mediaPlayerRef = null
            }

            onDispose {
                isPrepared = false
                hasRenderedFrame = false
                mediaPlayerRef = null
                safeReleasePlayer(player)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                isMuted = !isMuted
            },
    ) {
        if (debouncedShouldPlay) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                factory = { ctx ->
                    TextureView(ctx).apply {
                        isOpaque = false
                        outlineProvider = ViewOutlineProvider.BOUNDS
                        clipToOutline = true
                        textureViewRef = this
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                val surface = Surface(surfaceTexture)
                                activeSurface?.release()
                                activeSurface = surface
                                mediaPlayerRef?.let { mp ->
                                    try {
                                        mp.setSurface(surface)
                                        if (isPrepared && mp.videoWidth > 0 && mp.videoHeight > 0) {
                                            applyCenterCropTransform(this@apply, mp.videoWidth, mp.videoHeight)
                                        }
                                    } catch (_: Throwable) {
                                    }
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                mediaPlayerRef?.let { mp ->
                                    if (isPrepared && mp.videoWidth > 0 && mp.videoHeight > 0) {
                                        applyCenterCropTransform(this@apply, mp.videoWidth, mp.videoHeight)
                                    }
                                }
                            }

                            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                                try {
                                    mediaPlayerRef?.setSurface(null)
                                } catch (_: Throwable) {
                                }
                                activeSurface?.release()
                                activeSurface = null
                                textureViewRef = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                                if (!hasRenderedFrame) {
                                    hasRenderedFrame = true
                                }
                            }
                        }
                    }
                },
                update = { tv ->
                    textureViewRef = tv
                    mediaPlayerRef?.let { mp ->
                        if (isPrepared && mp.videoWidth > 0 && mp.videoHeight > 0) {
                            applyCenterCropTransform(tv, mp.videoWidth, mp.videoHeight)
                        }
                    }
                },
            )
        }

        // Thumbnail selalu tampil saat idle atau selama frame pertama belum dirender
        AnimatedVisibility(
            visible = (!debouncedShouldPlay || !hasRenderedFrame) && thumbnailBitmap != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // Badge "VIDEO" tipis di sudut kiri atas
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = "VIDEO",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
            )
        }

        // Tombol mute/unmute bergaya Instagram di sudut kanan bawah (ikon vektor bersih)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.58f))
                .border(0.8.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                .clickable { isMuted = !isMuted },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    id = if (isMuted) R.drawable.ic_sound_off else R.drawable.ic_sound_on,
                ),
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun safeReleasePlayer(player: MediaPlayer) {
    try {
        if (player.isPlaying) {
            player.stop()
        }
    } catch (_: Throwable) {
    }
    try {
        player.setSurface(null)
    } catch (_: Throwable) {
    }
    try {
        player.reset()
    } catch (_: Throwable) {
    }
    try {
        player.release()
    } catch (_: Throwable) {
    }
}

private fun applyCenterCropTransform(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int,
) {
    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) return

    val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
    val viewRatio = viewWidth / viewHeight

    val scaleX: Float
    val scaleY: Float
    if (videoRatio > viewRatio) {
        scaleX = videoRatio / viewRatio
        scaleY = 1f
    } else {
        scaleX = 1f
        scaleY = viewRatio / videoRatio
    }

    val matrix = Matrix().apply {
        setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    }
    textureView.setTransform(matrix)
}
