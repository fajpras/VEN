package com.ven.app.ui.screens.home

import com.ven.app.data.api.ApiHome
import com.ven.app.data.api.models.ApiResponse
import com.ven.app.data.api.models.KaryaItemModel
import com.ven.app.data.api.models.KategoriModel
import com.ven.app.data.api.models.PaginatedData
import com.ven.app.data.api.models.PameranItemModel
import com.ven.app.data.api.models.UserModel
import com.ven.app.data.cache.ImageCacheManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class BulkFeedStabilityTest {

    private fun buildBulkKarya(count: Int): List<KaryaItemModel> {
        return (1..count).map { idx ->
            val isVideo = idx % 3 == 0
            KaryaItemModel(
                idKarya = "01a21286-0000-0000-0000-${idx.toString().padStart(12, '0')}",
                idPengguna = "user-${idx % 8}",
                idPameran = "01a21286-0000-7000-8000-${((idx % 10) + 1).toString().padStart(12, '0')}",
                idKategori = (idx % 14) + 1,
                judul = "Karya Bulk #$idx",
                deskripsi = "Sinopsis pendek karya #$idx.\n\nNarasi lengkap untuk pengujian bulk data.",
                file = if (isVideo) "karya/video-$idx.mp4" else "karya/poster-$idx.png",
                fileUrl = "https://ven.test/api/karya/01a21286-0000-0000-0000-${idx.toString().padStart(12, '0')}/berkas?expires=${1795600000 + idx}&signature=sig$idx",
                jenis = if (isVideo) "video" else "poster",
                createdAt = "2026-11-25T10:00:00Z",
                kategori = KategoriModel(idKategori = (idx % 14) + 1, nama = "Kategori ${idx % 14}"),
                pengguna = UserModel(
                    idPengguna = "user-${idx % 8}",
                    nama = "Kreator ${idx % 8}",
                    namaPanggilan = "kreator_${idx % 8}",
                    email = "kreator${idx % 8}@ven.test",
                ),
            )
        }
    }

    @Test
    fun bulkMappingAssignsCorrectVideoRatioAndPosterRatio() {
        val bulk = buildBulkKarya(120)
        val mapped = bulk.map { it.toPostItem() }

        assertEquals(120, mapped.size)
        assertEquals(120, mapped.map { it.id }.toSet().size)

        val videos = mapped.filter { it.isVideo }
        val posters = mapped.filterNot { it.isVideo }
        assertEquals(40, videos.size)
        assertEquals(80, posters.size)

        videos.forEach { v ->
            assertEquals(16f / 9f, v.aspectRatio, 0.001f)
            val feedItem = v.toFeedItem()
            assertTrue(feedItem.isVideo)
            assertEquals(16f / 9f, feedItem.aspectRatio, 0.001f)
        }
        posters.forEach { p ->
            assertEquals(4f / 5f, p.aspectRatio, 0.001f)
            val feedItem = p.toFeedItem()
            assertFalse(feedItem.isVideo)
            assertEquals(4f / 5f, feedItem.aspectRatio, 0.001f)
        }
    }

    @Test
    fun signedUrlCacheKeysRemainStableAcrossChangingSignatures() {
        val uuid = "01a21286-f4f1-7345-9ce0-6ebe206fa851"
        val url1 = "https://ven.test/api/karya/$uuid/berkas?expires=1700000001&signature=aaa"
        val url2 = "https://ven.test/api/karya/$uuid/berkas?expires=1700009999&signature=zzz"

        assertEquals(
            ImageCacheManager.normalizeCacheKey(url1),
            ImageCacheManager.normalizeCacheKey(url2),
        )
        assertEquals("karya_$uuid", ImageCacheManager.normalizeCacheKey(url1))
    }

    @Test
    fun bulkPaginationNeverProducesDuplicateLazyColumnKeysAndStopsCleanly() = runBlocking {
        val allItems = buildBulkKarya(75)
        val pageSize = 20

        val fakeApi = object : ApiHome {
            override suspend fun getRekomendasiPameran(page: Int): Response<ApiResponse<PaginatedData<PameranItemModel>>> {
                return Response.success(
                    ApiResponse(
                        data = PaginatedData(currentPage = page, data = emptyList(), lastPage = 1, total = 0),
                    ),
                )
            }

            override suspend fun getRekomendasiKarya(
                page: Int,
                exclude: String?,
                refresh: Boolean?,
            ): Response<ApiResponse<PaginatedData<KaryaItemModel>>> {
                val excluded = exclude?.split(',')?.toSet().orEmpty()
                val available = allItems.filterNot { it.idKarya in excluded }
                val slice = available.take(pageSize)
                return Response.success(
                    ApiResponse(
                        data = PaginatedData(
                            currentPage = page,
                            data = slice,
                            lastPage = 4,
                            total = allItems.size,
                        ),
                    ),
                )
            }
        }

        val vm = HomeViewModel(apiHome = fakeApi)
        // Tunggu init coroutine selesai
        var attempts = 0
        while (vm.uiState.value.isLoading && attempts < 50) {
            kotlinx.coroutines.delay(20)
            attempts++
        }

        // Load semua halaman hingga habis
        repeat(6) {
            if (vm.uiState.value.canLoadMore) {
                val prevSize = vm.uiState.value.posts.size
                vm.loadNextChunk()
                var wait = 0
                while (
                    (vm.uiState.value.isLoadingMore || (vm.uiState.value.posts.size == prevSize && vm.uiState.value.canLoadMore)) &&
                    wait < 50
                ) {
                    kotlinx.coroutines.delay(20)
                    wait++
                }
            }
        }

        val posts = vm.uiState.value.posts
        assertEquals(75, posts.size)
        assertEquals("Tidak boleh ada duplikat key LazyColumn", 75, posts.map { it.id }.toSet().size)
        assertFalse("Harus berhenti memanggil API setelah seluruh bulk data termuat", vm.uiState.value.canLoadMore)
    }
}
