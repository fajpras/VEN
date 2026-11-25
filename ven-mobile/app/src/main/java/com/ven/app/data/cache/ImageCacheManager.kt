package com.ven.app.data.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import com.ven.app.core.config.AppConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object ImageCacheManager {

    private const val BUFFER_SIZE = 8192
    private const val DISK_CACHE_SUBDIR = "ven_image_cache"
    private const val MAX_DISK_CACHE_BYTES = 80L * 1024L * 1024L

    private val mutex = Mutex()
    private val fileMutex = Mutex()
    private val downloadSemaphore = Semaphore(permits = 3)
    private val videoDecodeSemaphore = Semaphore(permits = 2)
    private var isInitialized = false
    private lateinit var diskCacheDir: File

    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSizeKb = (maxMemoryKb / 8).coerceAtLeast(16 * 1024)

        object : LruCache<String, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    private val inFlightRequests = ConcurrentHashMap<String, CompletableDeferred<Bitmap?>>()
    private val inFlightFileRequests = ConcurrentHashMap<String, CompletableDeferred<File?>>()

    fun init(context: Context) {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    diskCacheDir = File(context.cacheDir, DISK_CACHE_SUBDIR).apply {
                        if (!exists()) mkdirs()
                    }
                    isInitialized = true
                }
            }
        }
    }

    fun getMemory(url: String?, maxWidth: Int = 1080, maxHeight: Int = 1350): Bitmap? {
        if (url.isNullOrBlank()) return null
        val memKey = buildMemoryKey(url, maxWidth, maxHeight)
        return memoryCache.get(memKey)
    }

    /**
     * Mengunduh (jika belum ada di disk cache) dan mengembalikan file lokal untuk
     * pemutaran media (mis. video MP4) secara aman & terdeduplikasi.
     */
    suspend fun loadCachedFile(url: String?): File? {
        if (url.isNullOrBlank() || !isInitialized) return null
        val diskKey = normalizeCacheKey(url)
        val cached = File(diskCacheDir, diskKey)
        if (cached.exists() && cached.length() > 0L) {
            cached.setLastModified(System.currentTimeMillis())
            return cached
        }

        var isInitiator = false
        val deferred = fileMutex.withLock {
            val existing = inFlightFileRequests[diskKey]
            if (existing != null) {
                existing
            } else {
                val created = CompletableDeferred<File?>()
                inFlightFileRequests[diskKey] = created
                isInitiator = true
                created
            }
        }

        if (!isInitiator) {
            return deferred.await()
        }

        return try {
            val result = withContext(Dispatchers.IO) {
                val resolvedUrl = resolveNetworkUrl(url)
                val downloaded = downloadSemaphore.withPermit {
                    if (hasValidDiskCache(diskKey)) {
                        true
                    } else {
                        downloadToDiskChunked(resolvedUrl, diskKey)
                    }
                }
                if (downloaded && cached.exists() && cached.length() > 0L) {
                    cached.setLastModified(System.currentTimeMillis())
                    trimDiskCacheIfNeeded()
                    cached
                } else {
                    null
                }
            }
            deferred.complete(result)
            result
        } catch (_: Throwable) {
            deferred.complete(null)
            null
        } finally {
            inFlightFileRequests.remove(diskKey)
        }
    }

    suspend fun loadBitmap(
        url: String?,
        maxWidth: Int = 1080,
        maxHeight: Int = 1350,
    ): Bitmap? {
        if (url.isNullOrBlank()) return null
        val memKey = buildMemoryKey(url, maxWidth, maxHeight)
        val diskKey = normalizeCacheKey(url)

        memoryCache.get(memKey)?.let { return it }

        var isInitiator = false
        val deferred = mutex.withLock {
            val existing = inFlightRequests[memKey]
            if (existing != null) {
                existing
            } else {
                val created = CompletableDeferred<Bitmap?>()
                inFlightRequests[memKey] = created
                isInitiator = true
                created
            }
        }

        if (!isInitiator) {
            return deferred.await()
        }

        return try {
            val result = withContext(Dispatchers.IO) {
                var bmp = loadFromDisk(diskKey, maxWidth, maxHeight)
                if (bmp != null) {
                    memoryCache.put(memKey, bmp)
                    return@withContext bmp
                }

                val resolvedUrl = resolveNetworkUrl(url)
                val downloaded = downloadSemaphore.withPermit {
                    if (hasValidDiskCache(diskKey)) {
                        true
                    } else {
                        downloadToDiskChunked(resolvedUrl, diskKey)
                    }
                }

                if (downloaded) {
                    trimDiskCacheIfNeeded()
                    bmp = loadFromDisk(diskKey, maxWidth, maxHeight)
                    if (bmp != null) {
                        memoryCache.put(memKey, bmp)
                    }
                }
                bmp
            }
            deferred.complete(result)
            result
        } catch (_: OutOfMemoryError) {
            memoryCache.evictAll()
            deferred.complete(null)
            null
        } catch (_: Throwable) {
            deferred.complete(null)
            null
        } finally {
            inFlightRequests.remove(memKey)
        }
    }

    private fun hasValidDiskCache(key: String): Boolean {
        if (!isInitialized) return false
        val file = File(diskCacheDir, key)
        return file.exists() && file.length() > 0L
    }

    private suspend fun loadFromDisk(key: String, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (!isInitialized) return null
        val file = File(diskCacheDir, key)
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            if (options.outWidth > 0 && options.outHeight > 0) {
                options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.RGB_565

                return BitmapFactory.decodeFile(file.absolutePath, options)
            }

            if (isMp4File(file)) {
                return videoDecodeSemaphore.withPermit {
                    extractVideoFrame(file, maxWidth, maxHeight)
                }
            }

            null
        } catch (_: OutOfMemoryError) {
            memoryCache.evictAll()
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun trimDiskCacheIfNeeded() {
        if (!isInitialized) return
        try {
            val files = diskCacheDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") } ?: return
            var totalBytes = files.sumOf { it.length() }
            if (totalBytes <= MAX_DISK_CACHE_BYTES) return

            val targetBytes = (MAX_DISK_CACHE_BYTES * 0.75).toLong()
            for (file in files.sortedBy { it.lastModified() }) {
                if (totalBytes <= targetBytes) break
                val size = file.length()
                if (file.delete()) {
                    totalBytes -= size
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun isMp4File(file: File): Boolean {
        if (file.length() < 12L) return false
        return try {
            val header = ByteArray(12)
            file.inputStream().use { input ->
                if (input.read(header) < 12) return false
            }
            header[4] == 'f'.code.toByte() &&
                header[5] == 't'.code.toByte() &&
                header[6] == 'y'.code.toByte() &&
                header[7] == 'p'.code.toByte()
        } catch (_: Exception) {
            false
        }
    }

    private fun extractVideoFrame(file: File, maxWidth: Int, maxHeight: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    0L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    maxWidth,
                    maxHeight,
                )
            } else {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun downloadToDiskChunked(urlStr: String, key: String): Boolean {
        if (!isInitialized) return false
        val targetFile = File(diskCacheDir, key)
        val tempFile = File(diskCacheDir, "$key.tmp")

        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 8000
                doInput = true
                instanceFollowRedirects = true
            }
            connection.connect()

            if (connection.responseCode !in 200..299) {
                return false
            }

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            // Atomic rename dari .tmp ke file final
            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            } else {
                tempFile.delete()
                false
            }
        } catch (_: Exception) {
            if (tempFile.exists()) tempFile.delete()
            false
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Hitung faktor pembagian sampel (inSampleSize) untuk mencegah pemborosan RAM.
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun buildMemoryKey(url: String, maxWidth: Int, maxHeight: Int): String {
        val baseKey = normalizeCacheKey(url)
        return "${baseKey}_${maxWidth}x${maxHeight}"
    }

    /**
     * Menormalkan cache key disk agar tetap konsisten walaupun temporary signature URL berubah.
     */
    fun normalizeCacheKey(url: String): String {
        val berkasRegex = Regex("""/karya/([a-zA-Z0-9-]+)/berkas""")
        val match = berkasRegex.find(url)
        if (match != null) {
            return "karya_${match.groupValues[1]}"
        }

        if (url.contains("/storage/")) {
            val clean = url.substringAfter("/storage/").replace('/', '_')
            return "storage_$clean"
        }

        val base = url.substringBefore('?')
        return sha256(base)
    }

    /**
     * Origin API (skema + host + port) yang diambil dari konfigurasi dasar
     * aplikasi, dipakai untuk mengabsolutkan URL relatif dari backend.
     */
    private val apiOrigin: String by lazy {
        val base = AppConfig.baseUrl.trim()
        val schemeEnd = base.indexOf("://")
        if (schemeEnd < 0) return@lazy ""
        val pathStart = base.indexOf('/', schemeEnd + 3)
        if (pathStart < 0) base else base.substring(0, pathStart)
    }

    /**
     * Memetakan localhost/127.0.0.1 ke host loopback emulator 10.0.2.2.
     */
    fun resolveNetworkUrl(url: String): String {
        return when {
            url.contains("localhost") -> url.replace("localhost", "10.0.2.2")
            url.contains("127.0.0.1") -> url.replace("127.0.0.1", "10.0.2.2")
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("/") -> apiOrigin + url
            else -> "$apiOrigin/storage/" + url.removePrefix("storage/")
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
