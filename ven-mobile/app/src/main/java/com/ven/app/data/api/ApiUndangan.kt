package com.ven.app.data.api

import com.ven.app.data.api.models.AksesTautanModel
import com.ven.app.data.api.models.AntreanRuanganModel
import com.ven.app.data.api.models.ApiResponse
import com.ven.app.data.api.models.CreateUndanganRequest
import com.ven.app.data.api.models.PaginatedData
import com.ven.app.data.api.models.PermintaanGabungModel
import com.ven.app.data.api.models.TanggapiAntreanRequest
import com.ven.app.data.api.models.TanggapiUndanganRequest
import com.ven.app.data.api.models.UndanganItemModel
import com.ven.app.data.api.models.VoiceTokenModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

class ApiHttpException(
    val statusCode: Int,
    override val message: String,
) : Exception(message)

interface ApiUndanganService {

    @GET("undangan")
    suspend fun getUndangan(
        @Query("sebagai") sebagai: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int? = 1,
    ): Response<ApiResponse<PaginatedData<UndanganItemModel>>>

    @POST("undangan")
    suspend fun createUndangan(
        @Body request: CreateUndanganRequest,
    ): Response<ApiResponse<UndanganItemModel>>

    @GET("undangan/{id}")
    suspend fun getUndanganDetail(
        @Path("id") id: String,
    ): Response<ApiResponse<UndanganItemModel>>

    @PATCH("undangan/{id}")
    suspend fun tanggapiUndangan(
        @Path("id") id: String,
        @Body request: TanggapiUndanganRequest,
    ): Response<ApiResponse<UndanganItemModel>>

    @GET("undangan/tautan/{token}")
    suspend fun aksesTautan(
        @Path("token") token: String,
    ): Response<ApiResponse<AksesTautanModel>>

    @GET("ruangan/{id}/antrean")
    suspend fun getAntrean(
        @Path("id") id: Int,
    ): Response<ApiResponse<AntreanRuanganModel>>

    @PATCH("ruangan/{id}/antrean/{permintaan}")
    suspend fun tanggapiAntrean(
        @Path("id") id: Int,
        @Path("permintaan") permintaanId: Int,
        @Body request: TanggapiAntreanRequest,
    ): Response<ApiResponse<PermintaanGabungModel>>

    @GET("ruangan/{id}/voice")
    suspend fun getVoiceToken(
        @Path("id") id: Int,
        @Query("undangan") undangan: Int? = null,
    ): Response<ApiResponse<VoiceTokenModel>>
}

object ApiUndangan {

    private val service: ApiUndanganService by lazy {
        ApiClient.createService(ApiUndanganService::class.java)
    }

    suspend fun getUndangan(
        sebagai: String? = null,
        status: String? = null,
        page: Int? = 1,
    ): Result<PaginatedData<UndanganItemModel>> {
        return try {
            val response = service.getUndangan(sebagai, status, page)
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(ApiHttpException(response.code(), ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUndangan(request: CreateUndanganRequest): Result<UndanganItemModel> {
        return try {
            val response = service.createUndangan(request)
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(ApiHttpException(response.code(), ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUndanganDetail(id: String): Result<UndanganItemModel> {
        return try {
            val response = service.getUndanganDetail(id)
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(ApiHttpException(response.code(), ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun tanggapiUndangan(id: String, request: TanggapiUndanganRequest): Result<UndanganItemModel> {
        return try {
            val response = service.tanggapiUndangan(id, request)
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(ApiHttpException(response.code(), ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun aksesTautan(token: String): Result<AksesTautanModel> {
        return try {
            val response = service.aksesTautan(token)
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(ApiHttpException(response.code(), ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAntrean(idRuangan: Int): Result<AntreanRuanganModel> {
        return try {
            val response = service.getAntrean(idRuangan)
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(ApiHttpException(response.code(), ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun tanggapiAntrean(
        idRuangan: Int,
        permintaanId: Int,
        status: String,
    ): Result<PermintaanGabungModel> {
        return try {
            val response = service.tanggapiAntrean(idRuangan, permintaanId, TanggapiAntreanRequest(status))
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(ApiHttpException(response.code(), ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun voiceToken(idRuangan: Int, idUndangan: Int? = null): Result<VoiceTokenModel> {
        return try {
            val response = service.getVoiceToken(idRuangan, idUndangan)
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(ApiHttpException(response.code(), ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
