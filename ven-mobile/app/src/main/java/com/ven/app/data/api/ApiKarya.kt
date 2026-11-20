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
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiKaryaService {

    @Multipart
    @POST("karya")
    suspend fun store(
        @Part("id_kategori") idKategori: RequestBody,
        @Part("judul") judul: RequestBody,
        @Part("jenis") jenis: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part file: MultipartBody.Part,
    ): Response<ApiResponse<KaryaItemModel>>
}

/**
 * Unggah karya (poster/video) ke `POST /api/karya`.
 *
 * Endpoint backend mewajibkan payload multipart dengan field
 * `id_kategori`, `judul`, `jenis`, `deskripsi`, dan berkas `file`.
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
    ): Result<KaryaItemModel> {
        return try {
            val filePart = MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody(mimeType.toMediaTypeOrNull()),
            )

            val response = service.store(
                idKategori = idKategori.toString().toRequestBody(TEXT),
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

    private val TEXT = "text/plain".toMediaTypeOrNull()
}
