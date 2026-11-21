package com.ven.app.data.api.models

import com.google.gson.annotations.SerializedName

data class UndanganItemModel(
    @SerializedName("id_undangan")
    val idUndangan: Int,

    @SerializedName("id_pengguna")
    val idPengguna: String,

    @SerializedName("id_ruangan")
    val idRuangan: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("waktu")
    val waktu: String,

    @SerializedName("catatan")
    val catatan: String? = null,

    @SerializedName("tautan")
    val tautan: String? = null,

    @SerializedName("ruangan")
    val ruangan: RuanganModel? = null,

    @SerializedName("pengguna")
    val pengguna: PenggunaRingkasModel? = null,
)

data class RuanganModel(
    @SerializedName("id_ruangan")
    val idRuangan: Int,

    @SerializedName("id_pameran")
    val idPameran: Int? = null,

    @SerializedName("jenis")
    val jenis: String? = null,

    @SerializedName("pameran")
    val pameran: PameranRingkasModel? = null,
)

data class PameranRingkasModel(
    @SerializedName("id_pameran")
    val idPameran: Int,

    @SerializedName("judul")
    val judul: String,

    @SerializedName("id_pengguna")
    val idPengguna: String? = null,

    @SerializedName("banner")
    val banner: String? = null,
)

data class PenggunaRingkasModel(
    @SerializedName("id_pengguna")
    val idPengguna: String,

    @SerializedName("nama")
    val nama: String,

    @SerializedName("nama_panggilan")
    val namaPanggilan: String? = null,

    @SerializedName("foto_profil")
    val fotoProfil: String? = null,
)

data class CreateUndanganRequest(
    @SerializedName("id_ruangan")
    val idRuangan: Int? = null,

    @SerializedName("id_pameran")
    val idPameran: Int? = null,

    @SerializedName("id_karya")
    val idKarya: String? = null,

    @SerializedName("waktu")
    val waktu: String,

    @SerializedName("catatan")
    val catatan: String? = null,
)

data class TanggapiUndanganRequest(
    @SerializedName("status")
    val status: String,

    @SerializedName("catatan")
    val catatan: String? = null,
)

data class VoiceTokenModel(
    @SerializedName("url")
    val url: String? = null,

    @SerializedName("room")
    val room: String,

    @SerializedName("token")
    val token: String,
)
