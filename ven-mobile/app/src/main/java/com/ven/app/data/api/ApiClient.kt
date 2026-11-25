package com.ven.app.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.ven.app.BuildConfig
import com.ven.app.R
import com.ven.app.core.config.AppConfig
import com.ven.app.core.localization.AppStrings
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var currentBaseUrl: String = AppConfig.baseUrl
    private var tokenProvider: (() -> String?)? = null

    private val gson: Gson by lazy { Gson() }

    /**
     * Logging level BODY hanya pada build debug. Interceptor ini mencetak
     * header (termasuk Authorization) dan seluruh body request/respons yang
     * memuat kata sandi, OTP, dan token — tidak boleh menyala di release.
     */
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val lang = AppStrings.locale().language.lowercase()
        val acceptLanguage = if (lang == "in" || lang == "id") "id" else "en"

        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")
            .header("Accept-Language", acceptLanguage)

        // Content-Type sengaja tidak di-set di sini: OkHttp sudah menurunkannya
        // dari body (application/json untuk @Body, multipart/form-data + boundary
        // untuk unggahan). Memaksanya menjadi application/json membuat endpoint
        // unggah banner/foto/karya gagal mem-parsing multipart.

        val token = tokenProvider?.invoke()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(loggingInterceptor)
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private var retrofit: Retrofit = buildRetrofit(currentBaseUrl)

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun init(tokenCallback: (() -> String?)? = null) {
        tokenProvider = tokenCallback
    }

    fun setBaseUrl(newBaseUrl: String) {
        val formattedUrl = if (newBaseUrl.endsWith("/")) newBaseUrl else "$newBaseUrl/"
        if (!BuildConfig.DEBUG && !formattedUrl.startsWith("https://", ignoreCase = true)) {
            throw IllegalArgumentException("Base URL wajib HTTPS pada build release: $formattedUrl")
        }
        currentBaseUrl = formattedUrl
        retrofit = buildRetrofit(formattedUrl)
    }

    fun getBaseUrl(): String = currentBaseUrl

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    fun parseError(errorBody: String?): String {
        val serverFallback = AppStrings.get(R.string.api_error_server).ifBlank { "Server error" }
        val requestFallback = AppStrings.get(R.string.api_error_request).ifBlank { "Request error" }
        val responseFallback = AppStrings.get(R.string.api_error_response).ifBlank { "Invalid server response" }

        if (errorBody.isNullOrBlank()) return serverFallback
        return try {
            val json = gson.fromJson(errorBody, JsonObject::class.java)
            when {
                json.has("message") -> json.get("message").asString
                json.has("error") -> json.get("error").asString
                json.has("errors") -> json.getAsJsonObject("errors")
                    ?.entrySet()
                    ?.firstOrNull()
                    ?.value
                    ?.takeIf { it.isJsonArray }
                    ?.asJsonArray
                    ?.firstOrNull()
                    ?.asString
                    ?: requestFallback
                else -> requestFallback
            }
        } catch (e: Exception) {
            responseFallback
        }
    }
}
