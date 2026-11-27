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

    @SerializedName("durasi_menit")
    val durasiMenit: Int = 30,

    @SerializedName("kadaluarsa_pada")
    val kadaluarsaPada: String? = null,

    @SerializedName("catatan")
    val catatan: String? = null,

    @SerializedName("alasan")
    val alasan: String? = null,

    @SerializedName("tautan")
    val tautan: String? = null,

    @SerializedName("ruangan")
    val ruangan: RuanganModel? = null,

    @SerializedName("pengguna")
    val pengguna: PenggunaRingkasModel? = null,
) {
    val deepLink: String?
        get() = tautan?.takeIf { it.isNotBlank() }?.let { "ven://diskusi/$it" }
}

data class RuanganModel(
    @SerializedName("id_ruangan")
    val idRuangan: Int,

    @SerializedName("id_pameran")
    val idPameran: String? = null,

    @SerializedName("jenis")
    val jenis: String? = null,

    @SerializedName("pameran")
    val pameran: PameranRingkasModel? = null,
)

data class PameranRingkasModel(
    @SerializedName("id_pameran")
    val idPameran: String,

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
    val idPameran: String? = null,

    @SerializedName("id_karya")
    val idKarya: String? = null,

    @SerializedName("waktu")
    val waktu: String,

    @SerializedName("durasi_menit")
    val durasiMenit: Int = 30,

    @SerializedName("catatan")
    val catatan: String? = null,
)

data class TanggapiUndanganRequest(
    @SerializedName("status")
    val status: String,

    @SerializedName("catatan")
    val catatan: String? = null,

    @SerializedName("alasan")
    val alasan: String? = null,
)

data class VoiceTokenModel(
    @SerializedName("url")
    val url: String? = null,

    @SerializedName("room")
    val room: String,

    @SerializedName("token")
    val token: String,
)

data class AksesTautanModel(
    @SerializedName("status_akses")
    val statusAkses: String,

    @SerializedName("id_undangan")
    val idUndangan: Int,

    @SerializedName("id_ruangan")
    val idRuangan: Int,

    @SerializedName("id_pameran")
    val idPameran: String? = null,

    @SerializedName("judul_pameran")
    val judulPameran: String? = null,

    @SerializedName("waktu")
    val waktu: String? = null,

    @SerializedName("durasi_menit")
    val durasiMenit: Int? = 30,

    @SerializedName("kadaluarsa_pada")
    val kadaluarsaPada: String? = null,

    @SerializedName("id_permintaan")
    val idPermintaan: Int? = null,
)

data class PermintaanGabungModel(
    @SerializedName("id_permintaan")
    val idPermintaan: Int,

    @SerializedName("id_ruangan")
    val idRuangan: Int,

    @SerializedName("id_undangan")
    val idUndangan: Int,

    @SerializedName("id_pengguna")
    val idPengguna: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("pengguna")
    val pengguna: PenggunaRingkasModel? = null,
)

data class AntreanRuanganModel(
    @SerializedName("id_ruangan")
    val idRuangan: Int,

    @SerializedName("id_pameran")
    val idPameran: String? = null,

    @SerializedName("judul_pameran")
    val judulPameran: String? = null,

    @SerializedName("tautan")
    val tautan: String? = null,

    @SerializedName("deep_link")
    val deepLink: String? = null,

    @SerializedName("kadaluarsa_pada")
    val kadaluarsaPada: String? = null,

    @SerializedName("pengaju_utama")
    val pengajuUtama: PenggunaRingkasModel? = null,

    @SerializedName("menunggu")
    val menunggu: List<PermintaanGabungModel> = emptyList(),

    @SerializedName("disetujui")
    val disetujui: List<PermintaanGabungModel> = emptyList(),
)

data class TanggapiAntreanRequest(
    @SerializedName("status")
    val status: String,
)
