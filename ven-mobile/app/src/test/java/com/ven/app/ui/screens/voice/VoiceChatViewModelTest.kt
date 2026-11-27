package com.ven.app.ui.screens.voice

import com.ven.app.data.api.models.VoiceTokenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        VoiceSesiStatus.reset()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        VoiceSesiStatus.reset()
    }

    private fun viewModelDengan(hasil: Result<VoiceTokenModel>) = VoiceChatViewModel(
        penyediaToken = { _, _ -> hasil },
    )

    @Test
    fun mulaiSesi_sukses_menerbitkanAcaraSiapGabungDanStatusMenyiapkan() = runTest(dispatcher) {
        val token = VoiceTokenModel(url = "wss://livekit-uji.test", room = "diskusi-2-9", token = "jwt-uji")
        val viewModel = viewModelDengan(Result.success(token))

        val acaraDiterima = mutableListOf<VoiceAcara>()
        val pengumpul = launch { viewModel.acara.collect { acaraDiterima += it } }

        viewModel.mulaiSesi(idRuangan = 2, exhibitId = "7")
        advanceUntilIdle()

        assertEquals(VoiceSesi.Menyiapkan, VoiceSesiStatus.state.value.sesi)

        val acara = acaraDiterima.single() as VoiceAcara.SiapGabung
        assertEquals(token, acara.token)
        assertEquals(2, acara.idRuangan)
        assertEquals("7", acara.exhibitId)

        pengumpul.cancel()
    }

    @Test
    fun mulaiSesi_gagal_mengubahStatusMenjadiGagalDenganPesanApi() = runTest(dispatcher) {
        val viewModel = viewModelDengan(Result.failure(IllegalStateException("Voice chat hanya tersedia di ruangan privat.")))

        val acaraDiterima = mutableListOf<VoiceAcara>()
        val pengumpul = launch { viewModel.acara.collect { acaraDiterima += it } }

        viewModel.mulaiSesi(idRuangan = 3)
        advanceUntilIdle()

        val acara = acaraDiterima.single() as VoiceAcara.Gagal
        assertEquals("Voice chat hanya tersedia di ruangan privat.", acara.pesan)
        assertEquals(VoiceSesi.Gagal, VoiceSesiStatus.state.value.sesi)
        assertEquals("Voice chat hanya tersedia di ruangan privat.", VoiceSesiStatus.state.value.pesanGalat)

        pengumpul.cancel()
    }

    @Test
    fun mulaiSesi_gagalTanpaPesan_memakaiPesanGalatBawaan() = runTest(dispatcher) {
        val viewModel = viewModelDengan(Result.failure(IllegalStateException()))

        val acaraDiterima = mutableListOf<VoiceAcara>()
        val pengumpul = launch { viewModel.acara.collect { acaraDiterima += it } }

        viewModel.mulaiSesi(idRuangan = 4)
        advanceUntilIdle()

        val acara = acaraDiterima.single() as VoiceAcara.Gagal
        assertTrue(acara.pesan.isNotBlank())
        assertEquals(VoiceSesi.Gagal, VoiceSesiStatus.state.value.sesi)

        pengumpul.cancel()
    }
}
