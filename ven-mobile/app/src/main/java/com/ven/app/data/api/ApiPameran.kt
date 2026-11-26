package com.ven.app.data.api

import com.google.gson.annotations.SerializedName
import com.ven.app.data.api.models.ApiResponse
import com.ven.app.data.api.models.KaryaItemModel
import com.ven.app.data.api.models.PaginatedData
import com.ven.app.data.api.models.PameranItemModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File

data class BannerUploadData(
    @SerializedName("url")
    val url: String,

    @SerializedName("path")
    val path: String? = null,
)

interface ApiPameranService {
    @Multipart
    @POST("pameran/banner")
    suspend fun uploadBanner(
        @Part banner: MultipartBody.Part,
    ): Response<ApiResponse<BannerUploadData>>

    @GET("pameran/{id}")
    suspend fun getPameran(
        @Path("id") id: Int,
    ): Response<ApiResponse<PameranItemModel>>

    @GET("pameran")
    suspend fun getPameranByUser(
        @Query("pengguna") userId: String,
    ): Response<ApiResponse<PaginatedData<PameranItemModel>>>

    @GET("pameran")
    suspend fun getMyExhibitions(
        @Query("milikku") milikku: Int = 1,
    ): Response<ApiResponse<PaginatedData<PameranItemModel>>>

    @Multipart
    @POST("pameran/{id}")
    suspend fun updatePameran(
        @Path("id") id: Int,
        @Part("judul") judul: RequestBody,
        @Part("tipe") tipe: RequestBody?,
        @Part("warna") warna: RequestBody?,
        @Part banner: MultipartBody.Part?,
    ): Response<ApiResponse<PameranItemModel>>

    @DELETE("pameran/{id}")
    suspend fun deletePameran(
        @Path("id") id: Int,
    ): Response<ApiResponse<Any>>

    @GET("karya/{id}")
    suspend fun getKarya(
        @Path("id") id: String,
    ): Response<ApiResponse<KaryaItemModel>>
}

object ApiPameran {
    private val service: ApiPameranService by lazy {
        ApiClient.createService(ApiPameranService::class.java)
    }

    private val TEXT = "text/plain".toMediaTypeOrNull()

    suspend fun uploadBanner(file: File, mimeType: String = "image/jpeg"): Result<BannerUploadData> {
        return try {
            val body = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("banner", file.name, body)
            val response = service.uploadBanner(part)
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

    suspend fun getPameran(id: Int): Result<PameranItemModel> {
        return try {
            val response = service.getPameran(id)
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

    suspend fun getPameranByUser(userId: String): Result<List<PameranItemModel>> {
        return try {
            val response = service.getPameranByUser(userId)
            val data = response.body()?.data?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyExhibitions(): Result<List<PameranItemModel>> {
        return try {
            val response = service.getMyExhibitions()
            val data = response.body()?.data?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePameran(
        id: Int,
        judul: String,
        tipe: String? = null,
        warna: String? = null,
        bannerFile: File? = null,
        bannerMimeType: String = "image/jpeg",
    ): Result<PameranItemModel> {
        return try {
            val bannerPart = bannerFile?.let { file ->
                MultipartBody.Part.createFormData(
                    "banner",
                    file.name,
                    file.asRequestBody(bannerMimeType.toMediaTypeOrNull()),
                )
            }

            val response = service.updatePameran(
                id = id,
                judul = judul.toRequestBody(TEXT),
                tipe = tipe?.toRequestBody(TEXT),
                warna = warna?.toRequestBody(TEXT),
                banner = bannerPart,
            )
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

    suspend fun deletePameran(id: Int): Result<Unit> {
        return try {
            val response = service.deletePameran(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getKarya(id: String): Result<KaryaItemModel> {
        return try {
            val response = service.getKarya(id)
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
