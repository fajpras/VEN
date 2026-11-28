package com.ven.app.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.R
import com.ven.app.VenApplication
import com.ven.app.core.localization.AppStrings
import com.ven.app.data.api.ApiClient
import com.ven.app.data.api.ApiPencarian
import com.ven.app.data.api.ApiProfil
import com.ven.app.data.api.models.KaryaItemModel
import com.ven.app.data.api.models.PameranItemModel
import com.ven.app.data.api.models.UserModel
import com.ven.app.data.preference.ExploreHistoryPreferences
import com.ven.app.data.preference.ExploreHistoryStore
import com.ven.app.ui.components.feature.MediaGridItem
import com.ven.app.util.DateFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val apiPencarian: ApiPencarian = ApiClient.createService(ApiPencarian::class.java),
    private val historyStore: ExploreHistoryStore = ExploreHistoryPreferences.getInstance(VenApplication.instance),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val loadCategories: suspend () -> List<String> = {
        ApiProfil.kategori().getOrNull().orEmpty().map { it.nama }.filter { it.isNotBlank() }
    },
    autoLoad: Boolean = true,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var discoveryJob: Job? = null

    init {
        val riwayat = historyStore.load()
            .take(10)
            .map { RecentSearchItem.Query(id = "query:$it", query = it) }

        _uiState.update {
            it.copy(
                recentSearches = riwayat,
                availableCategories = exploreCategories,
            )
        }

        if (autoLoad) {
            muatKategori()
            muatDiscoveryMedia()
        }
    }

    fun muatUlang(filterSeen: Boolean = true) {
        _uiState.update { it.copy(isRefreshing = true) }

        if (_uiState.value.isShowingResults) {
            val query = _uiState.value.searchQuery
            val tab = _uiState.value.selectedTab
            searchJob?.cancel()
            searchJob = viewModelScope.launch(dispatcher) {
                lakukanPencarian(query, tab)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        } else {
            muatDiscoveryMedia(isRefresh = true, filterSeen = filterSeen)
        }
    }

    fun muatDiscoveryMedia(isRefresh: Boolean = false, filterSeen: Boolean = false) {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch(dispatcher) {
            _uiState.update { it.copy(isLoadingDiscovery = true, hasDiscoveryError = false) }

            val state = _uiState.value
            val kategori = state.selectedCategories.toList().ifEmpty { null }
            val currentSeen = if (filterSeen) {
                state.seenMediaIds + state.discoveryMedia.map { it.id }.toSet()
            } else {
                state.seenMediaIds
            }
            val excludeParam = if (filterSeen && currentSeen.isNotEmpty()) {
                currentSeen.take(30).joinToString(",")
            } else {
                null
            }

            try {
                val karyaRes = apiPencarian.searchKarya(
                    query = null,
                    tipe = "karya",
                    page = 1,
                    exclude = excludeParam,
                    refresh = if (isRefresh) true else null,
                    kategori = kategori,
                )

                var items: List<MediaGridItem>? = null
                var feedList: List<PostsFeedItem>? = null
                var hasMore: Boolean? = null
                var currentPage: Int? = null
                var updatedSeen: Set<String>? = null

                if (karyaRes.isSuccessful) {
                    val paginated = karyaRes.body()?.data
                    val rawKarya = paginated?.data.orEmpty()
                    var page = 1
                    var lastPage = paginated?.lastPage ?: 1

                    var filteredKarya = if (filterSeen && currentSeen.isNotEmpty()) {
                        rawKarya.filterNot { it.idKarya in currentSeen }
                    } else {
                        rawKarya
                    }

                    while (filteredKarya.isEmpty() && page < lastPage) {
                        page++
                        val nextRes = apiPencarian.searchKarya(
                            query = null,
                            tipe = "karya",
                            page = page,
                            exclude = excludeParam,
                            refresh = if (isRefresh) true else null,
                            kategori = kategori,
                        )
                        if (nextRes.isSuccessful) {
                            val nextPaginated = nextRes.body()?.data
                            val nextData = nextPaginated?.data.orEmpty()
                            lastPage = nextPaginated?.lastPage ?: lastPage
                            filteredKarya = if (filterSeen && currentSeen.isNotEmpty()) {
                                nextData.filterNot { it.idKarya in currentSeen }
                            } else {
                                nextData
                            }
                        } else {
                            break
                        }
                    }

                    val finalKarya = when {
                        filteredKarya.isNotEmpty() -> filteredKarya
                        rawKarya.isNotEmpty() -> {
                            val currentFirstId = state.discoveryMedia.firstOrNull()?.id
                            val shuffled = rawKarya.shuffled()
                            if (shuffled.firstOrNull()?.idKarya == currentFirstId && shuffled.size > 1) {
                                shuffled.drop(1) + shuffled.take(1)
                            } else {
                                shuffled
                            }
                        }
                        else -> emptyList()
                    }

                    items = finalKarya.map { it.toMediaGridItem() }
                    feedList = finalKarya.map { it.toFeedItem() }
                    hasMore = page < lastPage && items.isNotEmpty()
                    currentPage = page
                    updatedSeen = if (filteredKarya.isNotEmpty()) {
                        currentSeen + items.map { it.id }.toSet()
                    } else {
                        items.map { it.id }.toSet()
                    }
                }

                _uiState.update {
                    it.copy(
                        discoveryMedia = items ?: it.discoveryMedia,
                        resultPosts = items ?: it.resultPosts,
                        feedPosts = feedList ?: it.feedPosts,
                        currentPostsPage = currentPage ?: it.currentPostsPage,
                        canLoadMorePosts = hasMore ?: it.canLoadMorePosts,
                        seenMediaIds = updatedSeen ?: it.seenMediaIds,
                        isLoadingDiscovery = false,
                        isRefreshing = false,
                        hasDiscoveryError = !karyaRes.isSuccessful && items.isNullOrEmpty(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDiscovery = false,
                        isRefreshing = false,
                        hasDiscoveryError = true,
                    )
                }
            }
        }
    }

    fun setSearchActive(active: Boolean) {
        if (!active) {
            searchJob?.cancel()
            _uiState.update {
                it.copy(
                    isSearchActive = false,
                    searchQuery = "",
                    isLoading = false,
                    hasSearchError = false,
                    resultAccounts = emptyList(),
                    resultPosts = emptyList(),
                    resultExhibits = emptyList(),
                )
            }
            return
        }

        _uiState.update { it.copy(isSearchActive = true) }
        if (_uiState.value.searchQuery.isNotBlank()) {
            val query = _uiState.value.searchQuery
            searchJob?.cancel()
            searchJob = viewModelScope.launch(dispatcher) {
                lakukanPencarian(query, _uiState.value.selectedTab)
            }
        }
    }

    fun setFilterSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isFilterSheetOpen = isOpen) }
        if (!isOpen) {
            val state = _uiState.value
            if (state.isShowingResults) {
                searchJob?.cancel()
                searchJob = viewModelScope.launch(dispatcher) {
                    lakukanPencarian(state.searchQuery, state.selectedTab)
                }
            } else {
                muatDiscoveryMedia()
            }
        }
    }

    fun toggleCategory(category: String) {
        _uiState.update { state ->
            val updated = state.selectedCategories.toMutableSet().apply {
                if (!add(category)) remove(category)
            }
            state.copy(selectedCategories = updated)
        }
    }

    fun resetFilter() {
        _uiState.update { it.copy(selectedCategories = emptySet()) }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchActive = true, hasSearchError = false) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    resultAccounts = emptyList(),
                    resultPosts = emptyList(),
                    resultExhibits = emptyList(),
                )
            }
            return
        }

        searchJob = viewModelScope.launch(dispatcher) {
            delay(350)
            lakukanPencarian(query, _uiState.value.selectedTab)
        }
    }

    fun onSearchSubmit() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) return

        simpanRiwayat(query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatcher) {
            lakukanPencarian(query, _uiState.value.selectedTab)
        }
    }

    fun onTabChange(tab: ExploreSearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatcher) {
            lakukanPencarian(query, tab)
        }
    }

    private suspend fun lakukanPencarian(query: String, tab: ExploreSearchTab) {
        val qParam = query.trim().ifBlank { null }
        val kategori = _uiState.value.selectedCategories.toList().ifEmpty { null }
        _uiState.update { it.copy(isLoading = true, isSearchActive = true, hasSearchError = false) }

        try {
            when (tab) {
                ExploreSearchTab.ForYou -> {
                    val response = apiPencarian.searchSemua(qParam, "semua", kategori)
                    if (!response.isSuccessful) {
                        _uiState.update { it.copy(hasSearchError = true) }
                        return
                    }

                    val body = response.body()?.data
                    val accounts = body?.pengguna.orEmpty().map { it.toAccountItem() }
                    val rawKarya = body?.karya.orEmpty()
                    val posts = rawKarya.map { it.toMediaGridItem() }
                    val feedList = rawKarya.map { it.toFeedItem() }
                    val exhibits = body?.pameran.orEmpty().map { it.toExhibitItem() }

                    _uiState.update {
                        it.copy(
                            resultAccounts = accounts,
                            resultPosts = posts,
                            resultExhibits = exhibits,
                            feedPosts = if (feedList.isNotEmpty()) feedList else it.feedPosts,
                            currentPostsPage = 1,
                            canLoadMorePosts = false,
                        )
                    }
                }

                ExploreSearchTab.Accounts -> {
                    val response = apiPencarian.searchPengguna(qParam)
                    if (!response.isSuccessful) {
                        _uiState.update { it.copy(hasSearchError = true) }
                        return
                    }

                    val accounts = response.body()?.data?.data.orEmpty().map { it.toAccountItem() }
                    _uiState.update {
                        it.copy(
                            resultAccounts = accounts,
                            canLoadMorePosts = false,
                        )
                    }
                }

                ExploreSearchTab.Posts -> {
                    val response = apiPencarian.searchKarya(qParam, page = 1, kategori = kategori)
                    if (!response.isSuccessful) {
                        _uiState.update { it.copy(hasSearchError = true) }
                        return
                    }

                    val paginated = response.body()?.data
                    val rawKarya = paginated?.data.orEmpty()
                    val posts = rawKarya.map { it.toMediaGridItem() }
                    val feedList = rawKarya.map { it.toFeedItem() }
                    val hasMore = paginated != null && paginated.currentPage < paginated.lastPage

                    _uiState.update {
                        it.copy(
                            resultPosts = posts,
                            feedPosts = if (feedList.isNotEmpty()) feedList else it.feedPosts,
                            currentPostsPage = 1,
                            canLoadMorePosts = hasMore,
                        )
                    }
                }

                ExploreSearchTab.Exhibits -> {
                    val response = apiPencarian.searchPameran(qParam, kategori = kategori)
                    if (!response.isSuccessful) {
                        _uiState.update { it.copy(hasSearchError = true) }
                        return
                    }

                    val exhibits = response.body()?.data?.data.orEmpty().map { it.toExhibitItem() }
                    _uiState.update {
                        it.copy(
                            resultExhibits = exhibits,
                            canLoadMorePosts = false,
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _uiState.update { it.copy(hasSearchError = true) }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadMorePostsChunk() {
        val currentState = _uiState.value
        if (currentState.isShowingResults ||
            currentState.isLoading ||
            currentState.isLoadingMorePosts ||
            !currentState.canLoadMorePosts
        ) {
            return
        }

        viewModelScope.launch(dispatcher) {
            _uiState.update { it.copy(isLoadingMorePosts = true) }
            val nextPage = currentState.currentPostsPage + 1
            val currentSeen = currentState.seenMediaIds + currentState.discoveryMedia.map { it.id }.toSet()
            val excludeParam = if (currentSeen.isNotEmpty()) currentSeen.take(50).joinToString(",") else null
            val kategori = currentState.selectedCategories.toList().ifEmpty { null }

            try {
                var response = apiPencarian.searchKarya(
                    query = null,
                    tipe = "karya",
                    page = nextPage,
                    exclude = excludeParam,
                    kategori = kategori,
                )
                var rawKarya = if (response.isSuccessful) {
                    response.body()?.data?.data.orEmpty()
                } else {
                    emptyList()
                }

                if (rawKarya.isEmpty()) {
                    val fallbackRes = apiPencarian.searchKarya(
                        query = null,
                        tipe = "karya",
                        page = 1,
                        exclude = excludeParam,
                        kategori = kategori,
                    )
                    if (fallbackRes.isSuccessful) {
                        rawKarya = fallbackRes.body()?.data?.data.orEmpty()
                    }
                }

                if (rawKarya.isEmpty()) {
                    val freshRes = apiPencarian.searchKarya(
                        query = null,
                        tipe = "karya",
                        page = 1,
                        refresh = true,
                        kategori = kategori,
                    )
                    if (freshRes.isSuccessful) {
                        rawKarya = freshRes.body()?.data?.data.orEmpty().shuffled()
                    }
                }

                val existingIds = currentState.discoveryMedia.map { it.id }.toSet()
                val newMedia = rawKarya
                    .map { it.toMediaGridItem() }
                    .filterNot { it.id in existingIds }
                val newFeed = rawKarya
                    .filterNot { it.idKarya in existingIds }
                    .map { it.toFeedItem() }

                if (newMedia.isNotEmpty()) {
                    val updatedSeen = currentState.seenMediaIds + newMedia.map { it.id }.toSet()

                    _uiState.update {
                        it.copy(
                            isLoadingMorePosts = false,
                            canLoadMorePosts = true,
                            currentPostsPage = nextPage,
                            seenMediaIds = updatedSeen,
                            resultPosts = (it.resultPosts + newMedia).distinctBy { item -> item.id },
                            discoveryMedia = (it.discoveryMedia + newMedia).distinctBy { item -> item.id },
                            feedPosts = (it.feedPosts + newFeed).distinctBy { item -> item.id },
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingMorePosts = false, canLoadMorePosts = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMorePosts = false) }
            }
        }
    }

    fun onAccountClicked(account: ExploreAccountItem) {
        _uiState.update { state ->
            val newEntry = RecentSearchItem.Account(
                id = account.id,
                username = if (account.username.startsWith("@")) account.username else "@${account.username}",
                fullName = account.fullName,
                avatarUrl = account.avatarUrl,
            )
            val updated = (listOf(newEntry) + state.recentSearches.filterNot { it.id == account.id }).take(10)
            state.copy(recentSearches = updated)
        }
    }

    fun removeRecentSearch(item: RecentSearchItem) {
        val remaining = _uiState.value.recentSearches.filterNot { it.id == item.id }
        _uiState.update { it.copy(recentSearches = remaining) }

        if (item is RecentSearchItem.Query) {
            historyStore.save(remaining.filterIsInstance<RecentSearchItem.Query>().map { it.query })
        }
    }

    private fun simpanRiwayat(query: String) {
        val entry = RecentSearchItem.Query(id = "query:$query", query = query)
        val updated = (
            listOf(entry) + _uiState.value.recentSearches.filterNot {
                it is RecentSearchItem.Query && it.query.equals(query, ignoreCase = true)
            }
        ).take(10)

        _uiState.update { it.copy(recentSearches = updated) }
        historyStore.save(updated.filterIsInstance<RecentSearchItem.Query>().map { it.query })
    }

    private fun muatKategori() {
        viewModelScope.launch(dispatcher) {
            val kategori = try {
                loadCategories()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }

            if (kategori.isNotEmpty()) {
                _uiState.update { it.copy(availableCategories = kategori) }
            }
        }
    }

    private fun KaryaItemModel.toMediaGridItem(): MediaGridItem {
        return MediaGridItem(
            id = idKarya,
            title = judul,
            imageUrl = fileUrl ?: file,
            category = kategori?.nama,
        )
    }

    private fun KaryaItemModel.toFeedItem(): PostsFeedItem {
        val author = pengguna?.namaPanggilan ?: pengguna?.nama ?: AppStrings.get(R.string.common_creator)
        val relativeTime = DateFormatter.formatRelativeTime(createdAt)
        val videoPost = jenis.equals("video", ignoreCase = true)
        return PostsFeedItem(
            id = idKarya,
            username = if (author.startsWith("@")) author else "@$author",
            userId = pengguna?.idPengguna,
            userAvatarUrl = pengguna?.fotoProfil,
            title = judul,
            category = kategori?.nama,
            timestampText = relativeTime,
            description = deskripsi ?: "",
            imageUrl = fileUrl ?: file,
            isVideo = videoPost,
            pameranId = idPameran?.toString(),
            aspectRatio = if (videoPost) 16f / 9f else 4f / 5f,
        )
    }

    private fun PameranItemModel.toExhibitItem(): ExhibitItem {
        return ExhibitItem(
            id = idPameran.toString(),
            title = judul,
            bannerUrl = banner,
            category = kategori,
        )
    }

    private fun UserModel.toAccountItem(): ExploreAccountItem {
        return ExploreAccountItem(
            id = idPengguna ?: "",
            username = namaPanggilan ?: "",
            fullName = nama ?: "",
            avatarUrl = fotoProfil,
        )
    }
}
