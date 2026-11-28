package com.ven.app.ui.screens.explore

import com.ven.app.data.api.ApiPencarian
import com.ven.app.data.api.models.ApiResponse
import com.ven.app.data.api.models.KaryaItemModel
import com.ven.app.data.api.models.KategoriModel
import com.ven.app.data.api.models.PaginatedData
import com.ven.app.data.api.models.PameranItemModel
import com.ven.app.data.api.models.SearchSummaryModel
import com.ven.app.data.api.models.UserModel
import com.ven.app.data.preference.ExploreHistoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun muatDiscoveryMedia_memuat_karya() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        api.karyaResponder = { successKaryaPage(listOf(karya("k1"))) }
        val viewModel = viewModel(api)

        viewModel.muatDiscoveryMedia()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("k1"), state.discoveryMedia.map { it.id })
        assertFalse(state.isLoadingDiscovery)
        assertFalse(state.hasDiscoveryError)
        assertTrue(api.pameranCalls.isEmpty())
        assertTrue(api.penggunaCalls.isEmpty())
    }

    @Test
    fun pencarian_for_you_mengirim_filter_kategori() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        val viewModel = viewModel(api)

        viewModel.toggleCategory("3D")
        viewModel.toggleCategory("UI/UX")
        viewModel.onQueryChange("robot")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        assertEquals(setOf("3D", "UI/UX"), api.semuaCalls.last().kategori?.toSet())
    }

    @Test
    fun pencarian_posts_mengirim_filter_kategori() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        val viewModel = viewModel(api)

        viewModel.toggleCategory("3D")
        viewModel.onTabChange(ExploreSearchTab.Posts)
        viewModel.onQueryChange("robot")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        assertEquals(listOf("3D"), api.karyaCalls.last().kategori)
    }

    @Test
    fun pencarian_tanpa_filter_tidak_mengirim_kategori() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        val viewModel = viewModel(api)

        viewModel.onQueryChange("robot")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        assertNull(api.semuaCalls.last().kategori)
    }

    @Test
    fun menutup_filter_saat_hasil_aktif_mengulang_pencarian_dengan_filter() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        val viewModel = viewModel(api)

        viewModel.onQueryChange("robot")
        viewModel.onSearchSubmit()
        advanceUntilIdle()
        assertNull(api.semuaCalls.last().kategori)

        viewModel.toggleCategory("3D")
        viewModel.setFilterSheetOpen(true)
        viewModel.setFilterSheetOpen(false)
        advanceUntilIdle()

        assertEquals(listOf("3D"), api.semuaCalls.last().kategori)
    }

    @Test
    fun filter_kategori_dikirim_ke_api_saat_sheet_ditutup() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        val viewModel = viewModel(api)

        viewModel.toggleCategory("3D")
        viewModel.setFilterSheetOpen(true)
        advanceUntilIdle()
        assertTrue(api.karyaCalls.isEmpty())
        assertTrue(api.pameranCalls.isEmpty())

        viewModel.setFilterSheetOpen(false)
        advanceUntilIdle()

        assertEquals(listOf("3D"), api.karyaCalls.single().kategori)
        assertTrue(api.pameranCalls.isEmpty())
    }

    @Test
    fun discovery_gagal_menandai_error_dan_mematikan_loading() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        api.karyaResponder = { errorResponse() }
        val viewModel = viewModel(api)

        viewModel.muatDiscoveryMedia()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasDiscoveryError)
        assertFalse(state.isLoadingDiscovery)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun onSearchSubmit_menyimpan_riwayat_dan_menampilkan_hasil() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        api.semuaResponder = {
            Response.success(
                ApiResponse(
                    SearchSummaryModel(karya = listOf(karya("k1", "Robot"))),
                ),
            )
        }
        val store = FakeHistoryStore()
        val viewModel = viewModel(api, store)

        viewModel.onQueryChange("robot")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("robot"), store.saved)
        assertTrue(state.recentSearches.any { it is RecentSearchItem.Query && it.query == "robot" })
        assertEquals(listOf("k1"), state.resultPosts.map { it.id })
        assertFalse(state.isLoading)
        assertFalse(state.hasSearchError)
    }

    @Test
    fun pencarian_kosong_tidak_fallback_ke_discovery() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        api.karyaResponder = { successKaryaPage(listOf(karya("discovery"))) }
        api.semuaResponder = { Response.success(ApiResponse(SearchSummaryModel())) }
        val viewModel = viewModel(api)

        viewModel.muatDiscoveryMedia()
        advanceUntilIdle()
        viewModel.onQueryChange("tidak ada hasil")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.resultPosts.isEmpty())
        assertTrue(state.resultExhibits.isEmpty())
        assertEquals(listOf("discovery"), state.discoveryMedia.map { it.id })
    }

    @Test
    fun pencarian_gagal_mereset_loading_dan_menandai_error() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        api.semuaResponder = { errorResponse() }
        val viewModel = viewModel(api)

        viewModel.onQueryChange("gagal")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasSearchError)
    }

    @Test
    fun onTabChange_tanpa_query_tidak_memanggil_api() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        val viewModel = viewModel(api)

        viewModel.onTabChange(ExploreSearchTab.Accounts)
        advanceUntilIdle()

        assertEquals(ExploreSearchTab.Accounts, viewModel.uiState.value.selectedTab)
        assertTrue(api.penggunaCalls.isEmpty())
    }

    @Test
    fun loadMore_tidak_mengubah_discovery_saat_hasil_pencarian_aktif() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        api.semuaResponder = {
            Response.success(
                ApiResponse(
                    SearchSummaryModel(karya = listOf(karya("k1"))),
                ),
            )
        }
        val viewModel = viewModel(api)

        viewModel.onQueryChange("robot")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        val discoveryBefore = viewModel.uiState.value.discoveryMedia
        viewModel.loadMorePostsChunk()
        advanceUntilIdle()

        assertEquals(discoveryBefore, viewModel.uiState.value.discoveryMedia)
        assertTrue(api.karyaCalls.none { it.page > 1 })
    }

    @Test
    fun onQueryChange_kosong_mengosongkan_hasil_pencarian() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        api.semuaResponder = {
            Response.success(
                ApiResponse(
                    SearchSummaryModel(karya = listOf(karya("k1"))),
                ),
            )
        }
        val viewModel = viewModel(api)

        viewModel.onQueryChange("robot")
        viewModel.onSearchSubmit()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.resultPosts.isNotEmpty())

        viewModel.onQueryChange("")

        val state = viewModel.uiState.value
        assertTrue(state.resultPosts.isEmpty())
        assertTrue(state.isShowingHistory)
    }

    @Test
    fun init_memuat_riwayat_dari_store() = runTest(dispatcher) {
        val store = FakeHistoryStore(listOf("robot", "3d"))

        val viewModel = viewModel(FakeApiPencarian(), store)

        val queries = viewModel.uiState.value.recentSearches
            .filterIsInstance<RecentSearchItem.Query>()
            .map { it.query }
        assertEquals(listOf("robot", "3d"), queries)
    }

    @Test
    fun removeRecentSearch_query_menyimpan_sisa_riwayat() = runTest(dispatcher) {
        val store = FakeHistoryStore(listOf("robot", "3d"))
        val viewModel = viewModel(FakeApiPencarian(), store)
        val first = viewModel.uiState.value.recentSearches.first()

        viewModel.removeRecentSearch(first)
        advanceUntilIdle()

        assertEquals(listOf("3d"), store.saved)
        assertEquals(listOf("3d"), viewModel.uiState.value.recentSearches.map { (it as RecentSearchItem.Query).query })
    }

    @Test
    fun loadMore_discovery_menambahkan_halaman_berikutnya() = runTest(dispatcher) {
        val api = FakeApiPencarian()
        api.karyaResponder = { call ->
            if (call.page == 1) {
                successKaryaPage(listOf(karya("k1")), currentPage = 1, lastPage = 2)
            } else {
                successKaryaPage(listOf(karya("k2")), currentPage = 2, lastPage = 2)
            }
        }
        val viewModel = viewModel(api)

        viewModel.muatDiscoveryMedia()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canLoadMorePosts)

        viewModel.loadMorePostsChunk()
        advanceUntilIdle()

        assertEquals(listOf("k1", "k2"), viewModel.uiState.value.discoveryMedia.map { it.id })
    }

    private fun viewModel(
        api: ApiPencarian,
        store: ExploreHistoryStore = FakeHistoryStore(),
    ): ExploreViewModel {
        return ExploreViewModel(
            apiPencarian = api,
            historyStore = store,
            dispatcher = dispatcher,
            loadCategories = { emptyList() },
            autoLoad = false,
        )
    }

    private fun karya(
        id: String,
        title: String = "Karya $id",
        kategori: String? = null,
    ): KaryaItemModel {
        return KaryaItemModel(
            idKarya = id,
            judul = title,
            kategori = kategori?.let { KategoriModel(idKategori = 1, nama = it) },
        )
    }

    private fun pameran(
        id: String,
        title: String = "Pameran $id",
        kategori: String? = null,
    ): PameranItemModel {
        return PameranItemModel(
            idPameran = id,
            judul = title,
            tipe = "kecil",
            kategori = kategori,
        )
    }

    private fun successKaryaPage(
        items: List<KaryaItemModel>,
        currentPage: Int = 1,
        lastPage: Int = 1,
    ): Response<ApiResponse<PaginatedData<KaryaItemModel>>> {
        return Response.success(
            ApiResponse(
                PaginatedData(
                    currentPage = currentPage,
                    data = items,
                    lastPage = lastPage,
                ),
            ),
        )
    }

    private fun successPameranPage(
        items: List<PameranItemModel>,
        currentPage: Int = 1,
        lastPage: Int = 1,
    ): Response<ApiResponse<PaginatedData<PameranItemModel>>> {
        return Response.success(
            ApiResponse(
                PaginatedData(
                    currentPage = currentPage,
                    data = items,
                    lastPage = lastPage,
                ),
            ),
        )
    }

    private fun <T> errorResponse(): Response<T> {
        return Response.error(500, "{}".toResponseBody("application/json".toMediaType()))
    }

    private class FakeHistoryStore(
        private var queries: List<String> = emptyList(),
    ) : ExploreHistoryStore {
        var saved: List<String> = queries

        override fun load(): List<String> = queries

        override fun save(queries: List<String>) {
            this.queries = queries
            saved = queries
        }
    }

    private data class KaryaCall(
        val query: String?,
        val tipe: String,
        val page: Int,
        val exclude: String?,
        val refresh: Boolean?,
        val kategori: List<String>?,
    )

    private data class PameranCall(
        val query: String?,
        val tipe: String,
        val page: Int,
        val refresh: Boolean?,
        val kategori: List<String>?,
    )

    private data class PenggunaCall(
        val query: String?,
        val tipe: String,
        val page: Int,
    )

    private data class SemuaCall(
        val query: String?,
        val tipe: String,
        val kategori: List<String>?,
    )

    private class FakeApiPencarian : ApiPencarian {
        var karyaResponder: (KaryaCall) -> Response<ApiResponse<PaginatedData<KaryaItemModel>>> = {
            Response.success(ApiResponse(PaginatedData()))
        }
        var pameranResponder: (PameranCall) -> Response<ApiResponse<PaginatedData<PameranItemModel>>> = {
            Response.success(ApiResponse(PaginatedData()))
        }
        var semuaResponder: () -> Response<ApiResponse<SearchSummaryModel>> = {
            Response.success(ApiResponse(SearchSummaryModel()))
        }
        var penggunaResponder: (PenggunaCall) -> Response<ApiResponse<PaginatedData<UserModel>>> = {
            Response.success(ApiResponse(PaginatedData()))
        }

        val karyaCalls = mutableListOf<KaryaCall>()
        val pameranCalls = mutableListOf<PameranCall>()
        val penggunaCalls = mutableListOf<PenggunaCall>()
        val semuaCalls = mutableListOf<SemuaCall>()

        override suspend fun searchSemua(
            query: String?,
            tipe: String,
            kategori: List<String>?,
        ): Response<ApiResponse<SearchSummaryModel>> {
            semuaCalls += SemuaCall(query, tipe, kategori)
            return semuaResponder()
        }

        override suspend fun searchKarya(
            query: String?,
            tipe: String,
            page: Int,
            exclude: String?,
            refresh: Boolean?,
            kategori: List<String>?,
        ): Response<ApiResponse<PaginatedData<KaryaItemModel>>> {
            val call = KaryaCall(query, tipe, page, exclude, refresh, kategori)
            karyaCalls += call
            return karyaResponder(call)
        }

        override suspend fun searchPameran(
            query: String?,
            tipe: String,
            page: Int,
            refresh: Boolean?,
            kategori: List<String>?,
        ): Response<ApiResponse<PaginatedData<PameranItemModel>>> {
            val call = PameranCall(query, tipe, page, refresh, kategori)
            pameranCalls += call
            return pameranResponder(call)
        }

        override suspend fun searchPengguna(
            query: String?,
            tipe: String,
            page: Int,
        ): Response<ApiResponse<PaginatedData<UserModel>>> {
            val call = PenggunaCall(query, tipe, page)
            penggunaCalls += call
            return penggunaResponder(call)
        }

        override suspend fun getDetailPengguna(
            idPengguna: String,
        ): Response<ApiResponse<com.ven.app.data.api.models.UserProfileDetailModel>> {
            return Response.success(ApiResponse(com.ven.app.data.api.models.UserProfileDetailModel()))
        }
    }
}
