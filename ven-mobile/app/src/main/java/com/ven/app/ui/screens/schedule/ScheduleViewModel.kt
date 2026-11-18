package com.ven.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.R
import com.ven.app.data.api.ApiUndangan
import com.ven.app.data.api.models.TanggapiUndanganRequest
import com.ven.app.data.api.models.UndanganItemModel
import com.ven.app.util.DateFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val apiUndangan: ApiUndangan = ApiUndangan,
    autoLoad: Boolean = true,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScheduleUiState(isLoading = autoLoad)
    )
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

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

    fun loadSchedules(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh) {
                _uiState.update { it.copy(isLoading = true) }
            }

            val pengundangDeferred = async { apiUndangan.getUndangan(sebagai = "pengundang") }
            val penyelenggaraDeferred = async { apiUndangan.getUndangan(sebagai = "penyelenggara") }

            val pengundangResult = pengundangDeferred.await()
            val penyelenggaraResult = penyelenggaraDeferred.await()

            val pengundangList = pengundangResult.getOrNull()?.data ?: emptyList()
            val penyelenggaraList = penyelenggaraResult.getOrNull()?.data ?: emptyList()

            val approved = mutableListOf<ScheduleEventItem>()
            val waiting = mutableListOf<ScheduleEventItem>()
            val requests = mutableListOf<ScheduleRequestItem>()
            val history = mutableListOf<ScheduleEventItem>()

            // Undangan yang diajukan user (sebagai pengundang)
            for (u in pengundangList) {
                val isPast = DateFormatter.isPastDateTime(u.waktu)
                val status = u.status.lowercase()

                if (isPast || status == "ditolak") {
                    history.add(mapToEventItem(u))
                } else if (status == "disetujui") {
                    approved.add(mapToEventItem(u))
                } else if (status == "diproses") {
                    waiting.add(mapToEventItem(u))
                }
            }

            // Undangan masuk untuk pameran user (sebagai penyelenggara)
            for (u in penyelenggaraList) {
                val isPast = DateFormatter.isPastDateTime(u.waktu)
                val status = u.status.lowercase()

                if (isPast || status == "ditolak") {
                    history.add(mapToEventItem(u))
                } else if (status == "disetujui") {
                    // Hindari duplikasi jika user adalah pengundang sekaligus penyelenggara
                    if (approved.none { it.id == u.idUndangan.toString() }) {
                        approved.add(mapToEventItem(u))
                    }
                } else if (status == "diproses") {
                    requests.add(mapToRequestItem(u))
                }
            }

            // Data dummy tidak lagi digunakan: API gagal/kosong tetap
            // menampilkan empty state asli agar tidak menipu pengguna.
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    approvedItems = approved,
                    waitingItems = waiting,
                    requestItems = requests,
                    historyItems = history,
                )
            }
        }
    }

    fun approveRequest(request: ScheduleRequestItem) {
        viewModelScope.launch {
            val result = apiUndangan.tanggapiUndangan(
                id = request.id,
                request = TanggapiUndanganRequest(status = "disetujui")
            )

            if (result.isSuccess) {
                val updatedRequests = _uiState.value.requestItems.filterNot { it.id == request.id }
                val approvedEvent = ScheduleEventItem(
                    id = request.id,
                    title = request.exhibitTitle ?: request.requesterName,
                    dateText = request.dateText,
                    timeText = request.timeText,
                    fullDateFormatted = request.fullDateFormatted,
                    bannerRes = request.bannerRes,
                    bannerUrl = request.bannerUrl,
                    exhibitId = request.exhibitId,
                    isApproved = true,
                    status = "disetujui",
                    note = request.note,
                )
                val updatedApproved = listOf(approvedEvent) + _uiState.value.approvedItems

                _uiState.update {
                    it.copy(
                        requestItems = updatedRequests,
                        approvedItems = updatedApproved,
                        activeRequestDetail = null,
                        userFeedbackMessage = "Permintaan berhasil disetujui",
                    )
                }
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
                request = TanggapiUndanganRequest(status = "ditolak", catatan = note)
            )

            if (result.isSuccess) {
                val updatedRequests = _uiState.value.requestItems.filterNot { it.id == request.id }
                val rejectedEvent = ScheduleEventItem(
                    id = request.id,
                    title = request.exhibitTitle ?: request.requesterName,
                    dateText = request.dateText,
                    timeText = request.timeText,
                    fullDateFormatted = request.fullDateFormatted,
                    bannerRes = request.bannerRes,
                    bannerUrl = request.bannerUrl,
                    exhibitId = request.exhibitId,
                    isApproved = false,
                    status = "ditolak",
                    note = note ?: request.note,
                )
                val updatedHistory = listOf(rejectedEvent) + _uiState.value.historyItems

                _uiState.update {
                    it.copy(
                        requestItems = updatedRequests,
                        historyItems = updatedHistory,
                        activeRequestDetail = null,
                        userFeedbackMessage = "Permintaan ditolak",
                    )
                }
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

        return ScheduleEventItem(
            id = u.idUndangan.toString(),
            title = exhibitTitle,
            dateText = DateFormatter.formatScheduleDate(u.waktu),
            timeText = DateFormatter.formatScheduleTime(u.waktu),
            fullDateFormatted = DateFormatter.formatScheduleFull(u.waktu),
            bannerRes = R.drawable.poster_1,
            bannerUrl = bannerUrl,
            exhibitId = exhibitId,
            isApproved = isApproved,
            status = u.status,
            note = u.catatan,
        )
    }

    private fun mapToRequestItem(u: UndanganItemModel): ScheduleRequestItem {
        val requesterName = u.pengguna?.namaPanggilan?.ifBlank { null }
            ?: u.pengguna?.nama?.ifBlank { null }
            ?: "Kreator"
        val bannerUrl = u.ruangan?.pameran?.banner
        val exhibitTitle = u.ruangan?.pameran?.judul ?: "Pameran"
        val exhibitId = u.ruangan?.pameran?.idPameran?.toString()

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
        )
    }
}
