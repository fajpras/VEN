package com.ven.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.R
import com.ven.app.data.api.ApiUndangan
import com.ven.app.data.api.models.TanggapiUndanganRequest
import com.ven.app.data.api.models.UndanganItemModel
import com.ven.app.util.DateFormatter
import java.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val apiUndangan: ApiUndangan = ApiUndangan,
    autoLoad: Boolean = true,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScheduleUiState(
            isLoading = autoLoad,
            nowMillis = clock.millis(),
        )
    )
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val pengundangItems = mutableListOf<UndanganItemModel>()
    private val penyelenggaraItems = mutableListOf<UndanganItemModel>()
    private var pengundangPage = 0
    private var penyelenggaraPage = 0
    private var pengundangLastPage = 1
    private var penyelenggaraLastPage = 1
    private var loadJob: Job? = null

    init {
        if (autoLoad) {
            loadSchedules()
        }
    }

    fun selectTab(tab: ScheduleTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectEventDetail(item: ScheduleEventItem?) {
        _uiState.update { it.copy(activeEventDetail = item) }
    }

    fun selectRequestDetail(item: ScheduleRequestItem?) {
        _uiState.update { it.copy(activeRequestDetail = item) }
    }

    fun dismissDetail() {
        _uiState.update {
            it.copy(
                activeEventDetail = null,
                activeRequestDetail = null,
            )
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadSchedules(isRefresh = true)
    }

    fun refreshSilently() {
        loadSchedules(isRefresh = true)
    }

    fun tick(nowMillis: Long = clock.millis()) {
        _uiState.update { it.copy(nowMillis = nowMillis) }
    }

    fun loadSchedules(isRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!isRefresh) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }

            val pengundangDeferred = async { apiUndangan.getUndangan(sebagai = "pengundang", page = 1) }
            val penyelenggaraDeferred = async { apiUndangan.getUndangan(sebagai = "penyelenggara", page = 1) }

            val pengundangResult = pengundangDeferred.await()
            val penyelenggaraResult = penyelenggaraDeferred.await()

            var errorMessage: String? = null
            if (pengundangResult.isFailure && penyelenggaraResult.isFailure) {
                errorMessage = pengundangResult.exceptionOrNull()?.message
                    ?: penyelenggaraResult.exceptionOrNull()?.message
                    ?: "Gagal memuat jadwal"
            }

            val pengundangData = pengundangResult.getOrNull()
            val penyelenggaraData = penyelenggaraResult.getOrNull()

            pengundangItems.clear()
            pengundangData?.data?.let { pengundangItems.addAll(it) }
            pengundangPage = pengundangData?.currentPage ?: 1
            pengundangLastPage = pengundangData?.lastPage ?: 1

            penyelenggaraItems.clear()
            penyelenggaraData?.data?.let { penyelenggaraItems.addAll(it) }
            penyelenggaraPage = penyelenggaraData?.currentPage ?: 1
            penyelenggaraLastPage = penyelenggaraData?.lastPage ?: 1

            rebuildState(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                errorMessage = errorMessage,
            )
        }
    }

    fun loadMore() {
        val hasMorePengundang = pengundangPage < pengundangLastPage
        val hasMorePenyelenggara = penyelenggaraPage < penyelenggaraLastPage
        if (!hasMorePengundang && !hasMorePenyelenggara) return
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val nextPagePengundang = if (hasMorePengundang) pengundangPage + 1 else null
            val nextPagePenyelenggara = if (hasMorePenyelenggara) penyelenggaraPage + 1 else null

            val pDeferred = nextPagePengundang?.let { p ->
                async { apiUndangan.getUndangan(sebagai = "pengundang", page = p) }
            }
            val sDeferred = nextPagePenyelenggara?.let { p ->
                async { apiUndangan.getUndangan(sebagai = "penyelenggara", page = p) }
            }

            pDeferred?.await()?.getOrNull()?.let { data ->
                val newItems = data.data.filter { item -> pengundangItems.none { it.idUndangan == item.idUndangan } }
                pengundangItems.addAll(newItems)
                pengundangPage = data.currentPage
                pengundangLastPage = data.lastPage
            }

            sDeferred?.await()?.getOrNull()?.let { data ->
                val newItems = data.data.filter { item -> penyelenggaraItems.none { it.idUndangan == item.idUndangan } }
                penyelenggaraItems.addAll(newItems)
                penyelenggaraPage = data.currentPage
                penyelenggaraLastPage = data.lastPage
            }

            rebuildState(isLoadingMore = false)
        }
    }

    private fun rebuildState(
        isLoading: Boolean = _uiState.value.isLoading,
        isRefreshing: Boolean = _uiState.value.isRefreshing,
        isLoadingMore: Boolean = _uiState.value.isLoadingMore,
        errorMessage: String? = _uiState.value.errorMessage,
    ) {
        val now = clock.millis()
        val approved = mutableListOf<ScheduleEventItem>()
        val waiting = mutableListOf<ScheduleEventItem>()
        val requests = mutableListOf<ScheduleRequestItem>()
        val history = mutableListOf<ScheduleEventItem>()

        // Undangan yang diajukan user (sebagai pengundang)
        for (u in pengundangItems) {
            val isPast = DateFormatter.isPastWithGrace(u.waktu, graceMinutes = 15, nowMillis = now)
            val status = u.status.lowercase()

            if (isPast || status == "ditolak") {
                if (history.none { it.id == u.idUndangan.toString() }) {
                    history.add(mapToEventItem(u))
                }
            } else if (status == "disetujui") {
                if (approved.none { it.id == u.idUndangan.toString() }) {
                    approved.add(mapToEventItem(u))
                }
            } else if (status == "diproses") {
                if (waiting.none { it.id == u.idUndangan.toString() }) {
                    waiting.add(mapToEventItem(u))
                }
            }
        }

        // Undangan masuk untuk pameran user (sebagai penyelenggara)
        for (u in penyelenggaraItems) {
            val isPast = DateFormatter.isPastWithGrace(u.waktu, graceMinutes = 15, nowMillis = now)
            val status = u.status.lowercase()

            if (isPast || status == "ditolak") {
                if (history.none { it.id == u.idUndangan.toString() }) {
                    history.add(mapToEventItem(u))
                }
            } else if (status == "disetujui") {
                if (approved.none { it.id == u.idUndangan.toString() }) {
                    approved.add(mapToEventItem(u))
                }
            } else if (status == "diproses") {
                if (requests.none { it.id == u.idUndangan.toString() }) {
                    requests.add(mapToRequestItem(u))
                }
            }
        }

        // Sorting: Approved, Waiting, Request urut naik (terdekat duluan); History urut turun (terbaru duluan)
        approved.sortBy { it.waktuMillis ?: Long.MAX_VALUE }
        waiting.sortBy { it.waktuMillis ?: Long.MAX_VALUE }
        requests.sortBy { it.waktuMillis ?: Long.MAX_VALUE }
        history.sortByDescending { it.waktuMillis ?: Long.MIN_VALUE }

        val hasMore = pengundangPage < pengundangLastPage || penyelenggaraPage < penyelenggaraLastPage

        _uiState.update {
            it.copy(
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                isLoadingMore = isLoadingMore,
                hasMore = hasMore,
                errorMessage = errorMessage,
                nowMillis = now,
                approvedItems = approved,
                waitingItems = waiting,
                requestItems = requests,
                historyItems = history,
            )
        }
    }

    fun approveRequest(request: ScheduleRequestItem) {
        viewModelScope.launch {
            val result = apiUndangan.tanggapiUndangan(
                id = request.id,
                request = TanggapiUndanganRequest(status = "disetujui")
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        activeRequestDetail = null,
                        userFeedbackMessage = "Permintaan berhasil disetujui",
                    )
                }
                refreshSilently()
            } else {
                _uiState.update {
                    it.copy(
                        userFeedbackMessage = result.exceptionOrNull()?.message ?: "Gagal menyetujui permintaan",
                    )
                }
            }
        }
    }

    fun rejectRequest(request: ScheduleRequestItem, note: String? = null) {
        viewModelScope.launch {
            val result = apiUndangan.tanggapiUndangan(
                id = request.id,
                request = TanggapiUndanganRequest(status = "ditolak", alasan = note)
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        activeRequestDetail = null,
                        userFeedbackMessage = "Permintaan ditolak",
                    )
                }
                refreshSilently()
            } else {
                _uiState.update {
                    it.copy(
                        userFeedbackMessage = result.exceptionOrNull()?.message ?: "Gagal menolak permintaan",
                    )
                }
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(userFeedbackMessage = null) }
    }

    private fun mapToEventItem(u: UndanganItemModel): ScheduleEventItem {
        val bannerUrl = u.ruangan?.pameran?.banner
        val exhibitTitle = u.ruangan?.pameran?.judul ?: "Diskusi Pameran"
        val exhibitId = u.ruangan?.pameran?.idPameran?.toString()
        val isApproved = u.status.equals("disetujui", ignoreCase = true)
        val millis = DateFormatter.parseIsoToMillis(u.waktu)

        return ScheduleEventItem(
            id = u.idUndangan.toString(),
            title = exhibitTitle,
            dateText = DateFormatter.formatScheduleDate(u.waktu),
            timeText = DateFormatter.formatScheduleTime(u.waktu),
            fullDateFormatted = DateFormatter.formatScheduleFull(u.waktu),
            bannerRes = R.drawable.poster_1,
            bannerUrl = bannerUrl,
            exhibitId = exhibitId,
            roomId = u.ruangan?.idRuangan,
            isApproved = isApproved,
            status = u.status,
            note = u.catatan,
            waktuMillis = millis,
            alasan = u.alasan,
            tautan = u.tautan,
        )
    }

    private fun mapToRequestItem(u: UndanganItemModel): ScheduleRequestItem {
        val requesterName = u.pengguna?.namaPanggilan?.ifBlank { null }
            ?: u.pengguna?.nama?.ifBlank { null }
            ?: "Kreator"
        val bannerUrl = u.ruangan?.pameran?.banner
        val exhibitTitle = u.ruangan?.pameran?.judul ?: "Pameran"
        val exhibitId = u.ruangan?.pameran?.idPameran?.toString()
        val millis = DateFormatter.parseIsoToMillis(u.waktu)

        return ScheduleRequestItem(
            id = u.idUndangan.toString(),
            requesterName = requesterName,
            dateText = DateFormatter.formatScheduleDate(u.waktu),
            timeText = DateFormatter.formatScheduleTime(u.waktu),
            fullDateFormatted = DateFormatter.formatScheduleFull(u.waktu),
            note = u.catatan ?: "",
            bannerRes = R.drawable.poster_1,
            bannerUrl = bannerUrl,
            exhibitId = exhibitId,
            exhibitTitle = exhibitTitle,
            requesterAvatarUrl = u.pengguna?.fotoProfil,
            roomId = u.ruangan?.idRuangan,
            waktuMillis = millis,
        )
    }
}
