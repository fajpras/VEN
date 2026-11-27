package com.ven.app.data.api

import com.ven.app.data.api.models.ApiResponse
import com.ven.app.data.api.models.KaryaItemModel
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiKaryaService {

    @Multipart
    @POST("karya")
    suspend fun store(
        @Part("id_kategori") idKategori: RequestBody,
        @Part("id_pameran") idPameran: RequestBody?,
        @Part("judul") judul: RequestBody,
        @Part("jenis") jenis: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part file: MultipartBody.Part,
    ): Response<ApiResponse<KaryaItemModel>>

    @Multipart
    @POST("karya/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Part("id_kategori") idKategori: RequestBody,
        @Part("id_pameran") idPameran: RequestBody,
        @Part("judul") judul: RequestBody,
        @Part("jenis") jenis: RequestBody?,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part file: MultipartBody.Part?,
    ): Response<ApiResponse<KaryaItemModel>>

    @DELETE("karya/{id}")
    suspend fun destroy(
        @Path("id") id: String,
    ): Response<ApiResponse<Any>>
}

/**
 * Unggah, perbarui, dan hapus karya (poster/video) melalui `/api/karya`.
 */
object ApiKarya {

    private val service: ApiKaryaService by lazy {
        ApiClient.createService(ApiKaryaService::class.java)
    }

    suspend fun unggah(
        file: File,
        mimeType: String,
        idKategori: Int,
        judul: String,
        jenis: String,
        deskripsi: String,
        idPameran: String? = null,
    ): Result<KaryaItemModel> {
        return try {
            val filePart = MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody(mimeType.toMediaTypeOrNull()),
            )

            val response = service.store(
                idKategori = idKategori.toString().toRequestBody(TEXT),
                idPameran = idPameran?.toString()?.toRequestBody(TEXT),
                judul = judul.toRequestBody(TEXT),
                jenis = jenis.toRequestBody(TEXT),
                deskripsi = deskripsi.toRequestBody(TEXT),
                file = filePart,
            )

            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (exception: Exception) {
            Result.failure(Exception(exception.localizedMessage ?: "Gagal mengunggah karya"))
        }
    }

    suspend fun perbarui(
        idKarya: String,
        idKategori: Int,
        judul: String,
        deskripsi: String,
        idPameran: String? = null,
        jenis: String? = null,
        file: File? = null,
        mimeType: String? = null,
    ): Result<KaryaItemModel> {
        return try {
            val filePart = if (file != null && mimeType != null) {
                MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    file.asRequestBody(mimeType.toMediaTypeOrNull()),
                )
            } else {
                null
            }

            val response = service.update(
                id = idKarya,
                idKategori = idKategori.toString().toRequestBody(TEXT),
                // Kirim string kosong saat null agar backend melepas id_pameran secara eksplisit
                idPameran = (idPameran?.toString() ?: "").toRequestBody(TEXT),
                judul = judul.toRequestBody(TEXT),
                jenis = jenis?.toRequestBody(TEXT),
                deskripsi = deskripsi.toRequestBody(TEXT),
                file = filePart,
            )

            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (exception: Exception) {
            Result.failure(Exception(exception.localizedMessage ?: "Gagal memperbarui karya"))
        }
    }

    suspend fun hapus(idKarya: String): Result<Unit> {
        return try {
            val response = service.destroy(idKarya)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(ApiClient.parseError(response.errorBody()?.string())))
            }
        } catch (exception: Exception) {
            Result.failure(Exception(exception.localizedMessage ?: "Gagal menghapus karya"))
        }
    }

    private val TEXT = "text/plain".toMediaTypeOrNull()
}
