package com.ven.app.data.api

import com.ven.app.data.api.models.ApiResponse
import com.ven.app.data.api.models.CreateUndanganRequest
import com.ven.app.data.api.models.PaginatedData
import com.ven.app.data.api.models.TanggapiUndanganRequest
import com.ven.app.data.api.models.UndanganItemModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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
                Result.failure(Exception(ApiClient.parseError(response.errorBody()?.string())))
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
                Result.failure(Exception(ApiClient.parseError(response.errorBody()?.string())))
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
                Result.failure(Exception(ApiClient.parseError(response.errorBody()?.string())))
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
                Result.failure(Exception(ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
