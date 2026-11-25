package com.ven.app.data.api

import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.ven.app.data.api.models.ApiResponse
import com.ven.app.data.api.models.AuthData
import com.ven.app.data.api.models.GoogleAuthRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class AccountSuspensionInfo(
    val reason: String,
    val untilText: String? = null,
    val isPermanent: Boolean = false,
)

class AccountSuspendedException(
    val info: AccountSuspensionInfo,
    override val message: String,
) : Exception(message)

// KirimOtpRequest dipakai bersama dengan ApiRegister

data class LoginRequest(
    @SerializedName("identitas")
    val identitas: String,

    @SerializedName("kata_sandi")
    val kataSandi: String
)

data class ResetKataSandiRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("otp")
    val otp: String,

    @SerializedName("kata_sandi")
    val kataSandi: String
)

interface ApiLoginService {

    @POST("auth/masuk")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthData>>

    @POST("auth/google/masuk")
    suspend fun loginGoogle(
        @Body request: GoogleAuthRequest
    ): Response<ApiResponse<AuthData>>

    @POST("auth/keluar")
    suspend fun logout(): Response<ApiResponse<Any>>

    @POST("auth/lupa-kata-sandi")
    suspend fun lupaKataSandi(
        @Body request: KirimOtpRequest
    ): Response<ApiResponse<Any>>

    @POST("auth/reset-kata-sandi")
    suspend fun resetKataSandi(
        @Body request: ResetKataSandiRequest
    ): Response<ApiResponse<Any>>
}

object ApiLogin {

    private val service: ApiLoginService by lazy {
        ApiClient.createService(ApiLoginService::class.java)
    }

    suspend fun login(identitas: String, kataSandi: String): Result<AuthData> {
        return try {
            val request = LoginRequest(
                identitas = identitas.trim(),
                kataSandi = kataSandi
            )
            val response = service.login(request)
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                val rawBody = response.errorBody()?.string()
                val errorMsg = ApiClient.parseError(rawBody)
                if (response.code() == 403) {
                    parseSuspension(rawBody, errorMsg)?.let { info ->
                        return Result.failure(AccountSuspendedException(info, errorMsg))
                    }
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal terhubung ke server"))
        }
    }

    suspend fun loginGoogle(idToken: String?, accessToken: String? = null): Result<AuthData> {
        return try {
            val request = GoogleAuthRequest(
                idToken = idToken,
                accessToken = accessToken
            )
            val response = service.loginGoogle(request)
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                val rawBody = response.errorBody()?.string()
                val errorMsg = ApiClient.parseError(rawBody)
                if (response.code() == 403) {
                    parseSuspension(rawBody, errorMsg)?.let { info ->
                        return Result.failure(AccountSuspendedException(info, errorMsg))
                    }
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal terhubung ke server"))
        }
    }

    private fun parseSuspension(rawBody: String?, fallbackMessage: String): AccountSuspensionInfo? {
        if (!rawBody.isNullOrBlank()) {
            try {
                val root = JsonParser.parseString(rawBody).asJsonObject
                val sanksi = root.getAsJsonObject("sanksi")
                if (sanksi != null) {
                    val reason = sanksi.get("alasan")?.takeIf { !it.isJsonNull }?.asString?.trim().orEmpty()
                    val until = sanksi.get("berakhir_teks")?.takeIf { !it.isJsonNull }?.asString?.trim()
                    val permanent = sanksi.get("permanen")?.takeIf { !it.isJsonNull }?.asBoolean ?: (until == null)
                    if (reason.isNotEmpty() || until != null || permanent) {
                        return AccountSuspensionInfo(
                            reason = reason.ifBlank { fallbackMessage },
                            untilText = if (permanent) null else until,
                            isPermanent = permanent,
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }

        // Fallback parsing dari teks pesan bila backend lama tidak mengirim objek sanksi
        val reasonMatch = Regex("""(?i)(?:Alasan|Reason)\s*:\s*(.+)$""").find(fallbackMessage)
        val untilMatch = Regex("""(?i)(?:sampai|until)\s+([^.]+)\.""").find(fallbackMessage)
        val isPerm = fallbackMessage.contains("permanen", ignoreCase = true) ||
            fallbackMessage.contains("permanent", ignoreCase = true)
        if (reasonMatch != null || untilMatch != null || isPerm) {
            return AccountSuspensionInfo(
                reason = reasonMatch?.groupValues?.getOrNull(1)?.trim()?.ifBlank { fallbackMessage } ?: fallbackMessage,
                untilText = if (isPerm) null else untilMatch?.groupValues?.getOrNull(1)?.trim(),
                isPermanent = isPerm,
            )
        }
        return null
    }

    suspend fun logout(): Result<String> {
        return try {
            val response = service.logout()
            if (response.isSuccessful) {
                val message = response.body()?.message ?: "Logout berhasil."
                Result.success(message)
            } else {
                val errorMsg = ApiClient.parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal terhubung ke server"))
        }
    }

    suspend fun lupaKataSandi(email: String): Result<String> {
        return try {
            val response = service.lupaKataSandi(KirimOtpRequest(email = email.trim()))
            if (response.isSuccessful) {
                val message = response.body()?.message ?: "Jika email terdaftar, kode verifikasi telah dikirim."
                Result.success(message)
            } else {
                val errorMsg = ApiClient.parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal terhubung ke server"))
        }
    }

    suspend fun resetKataSandi(email: String, otp: String, kataSandiBaru: String): Result<String> {
        return try {
            val request = ResetKataSandiRequest(
                email = email.trim(),
                otp = otp.trim(),
                kataSandi = kataSandiBaru
            )
            val response = service.resetKataSandi(request)
            if (response.isSuccessful) {
                val message = response.body()?.message ?: "Kata sandi berhasil diubah."
                Result.success(message)
            } else {
                val errorMsg = ApiClient.parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal terhubung ke server"))
        }
    }
}

