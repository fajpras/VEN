package com.ven.app.ui.screens.requestmeeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ven.app.R
import com.ven.app.core.localization.AppStrings
import com.ven.app.data.api.ApiPameran
import com.ven.app.data.api.ApiUndangan
import com.ven.app.data.api.models.CreateUndanganRequest
import com.ven.app.util.DateFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RequestMeetingEvent {
    data object RequestSuccess : RequestMeetingEvent
}

class RequestMeetingViewModel(
    private val apiUndangan: ApiUndangan = ApiUndangan,
    private val apiPameran: ApiPameran = ApiPameran,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val initialMonth = YearMonth.from(today)
    private val initialDate = today
    private var currentTargetId: String? = null

    private val _uiState = MutableStateFlow(
        RequestMeetingUiState(
            visibleMonth = initialMonth,
            selectedDate = initialDate,
            selectedDateText = DateFormatter.formatRequestMeetingDate(initialDate),
            monthTitleText = DateFormatter.formatMonthYear(initialMonth),
            calendarDays = buildMonthGrid(initialMonth, initialDate),
        )
    )
    val uiState: StateFlow<RequestMeetingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RequestMeetingEvent>()
    val events: SharedFlow<RequestMeetingEvent> = _events.asSharedFlow()

    fun setTargetId(targetId: String?) {
        if (targetId == currentTargetId && !targetId.isNullOrBlank()) return
        currentTargetId = targetId
        if (targetId.isNullOrBlank()) return

        loadTargetDetails(targetId)
    }

    private fun loadTargetDetails(targetId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val pameranIdInt = targetId.toIntOrNull()
            if (pameranIdInt != null) {
                val res = apiPameran.getPameran(pameranIdInt)
                if (res.isSuccess) {
                    val pameran = res.getOrNull()
                    if (pameran != null) {
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                pameranId = pameran.idPameran.toString(),
                                pameranTitle = pameran.judul,
                                bannerUrl = pameran.banner,
                                bannerColor = pameran.warna,
                            )
                        }
                        return@launch
                    }
                }
            } else {
                // targetId adalah UUID karya
                val karyaRes = apiPameran.getKarya(targetId)
                if (karyaRes.isSuccess) {
                    val karya = karyaRes.getOrNull()
                    if (karya != null) {
                        val pameran = karya.pameran
                        if (pameran != null) {
                            _uiState.update { current ->
                                current.copy(
                                    isLoading = false,
                                    pameranId = pameran.idPameran.toString(),
                                    pameranTitle = pameran.judul,
                                    bannerUrl = pameran.banner,
                                    bannerColor = pameran.warna,
                                )
                            }
                            return@launch
                        } else if (karya.idPameran != null) {
                            val pamRes = apiPameran.getPameran(karya.idPameran)
                            val p = pamRes.getOrNull()
                            if (p != null) {
                                _uiState.update { current ->
                                    current.copy(
                                        isLoading = false,
                                        pameranId = p.idPameran.toString(),
                                        pameranTitle = p.judul,
                                        bannerUrl = p.banner,
                                        bannerColor = p.warna,
                                    )
                                }
                                return@launch
                            }
                        } else if (!karya.idPengguna.isNullOrBlank()) {
                            // Fallback jika karya belum terhubung pameran: ambil pameran terbaru milik pembuat
                            val userPamRes = apiPameran.getPameranByUser(karya.idPengguna)
                            val latestPameran = userPamRes.getOrNull()?.firstOrNull()
                            if (latestPameran != null) {
                                _uiState.update { current ->
                                    current.copy(
                                        isLoading = false,
                                        pameranId = latestPameran.idPameran.toString(),
                                        pameranTitle = latestPameran.judul,
                                        bannerUrl = latestPameran.banner,
                                        bannerColor = latestPameran.warna,
                                    )
                                }
                                return@launch
                            }
                        }

                        // Jika tidak ada pameran sama sekali, gunakan judul dan file karya
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                pameranTitle = karya.judul,
                                bannerUrl = karya.fileUrl ?: karya.file,
                            )
                        }
                        return@launch
                    }
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onDaySelected(day: CalendarDay) {
        if (!day.isCurrentMonth) return
        _uiState.update { current ->
            val newSelectedDate = day.date
            current.copy(
                selectedDate = newSelectedDate,
                selectedDateText = DateFormatter.formatRequestMeetingDate(newSelectedDate),
                calendarDays = buildMonthGrid(current.visibleMonth, newSelectedDate),
            )
        }
    }

    fun onPrevMonth() {
        _uiState.update { current ->
            val prevMonth = current.visibleMonth.minusMonths(1)
            current.copy(
                visibleMonth = prevMonth,
                monthTitleText = DateFormatter.formatMonthYear(prevMonth),
                calendarDays = buildMonthGrid(prevMonth, current.selectedDate),
            )
        }
    }

    fun onNextMonth() {
        _uiState.update { current ->
            val nextMonth = current.visibleMonth.plusMonths(1)
            current.copy(
                visibleMonth = nextMonth,
                monthTitleText = DateFormatter.formatMonthYear(nextMonth),
                calendarDays = buildMonthGrid(nextMonth, current.selectedDate),
            )
        }
    }

    fun onHourChange(hour: String) {
        val sanitized = hour.take(2).filter { it.isDigit() }
        _uiState.update { it.copy(hourText = sanitized) }
    }

    fun onMinuteChange(minute: String) {
        val sanitized = minute.take(2).filter { it.isDigit() }
        _uiState.update { it.copy(minuteText = sanitized) }
    }

    fun onPeriodChange(isAm: Boolean) {
        _uiState.update { it.copy(isAm = isAm) }
    }

    fun onMessageChange(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun sendRequest(overrideTargetId: String? = null) {
        val current = _uiState.value
        if (current.hourText.isBlank() || current.minuteText.isBlank()) {
            _uiState.update { it.copy(validationError = RequestMeetingValidationError.TimeNotSet) }
            return
        }

        val hourVal = current.hourText.toIntOrNull() ?: 0
        val minuteVal = current.minuteText.toIntOrNull() ?: 0
        if (hourVal !in 1..12 || minuteVal !in 0..59) {
            _uiState.update { it.copy(validationError = RequestMeetingValidationError.InvalidSchedule) }
            return
        }

        var hour24 = hourVal
        if (!current.isAm && hour24 < 12) hour24 += 12
        if (current.isAm && hour24 == 12) hour24 = 0

        val scheduledDateTime = current.selectedDate.atTime(hour24, minuteVal)

        if (scheduledDateTime.isBefore(LocalDateTime.now())) {
            _uiState.update { it.copy(validationError = RequestMeetingValidationError.InvalidSchedule) }
            return
        }

        val formattedWaktu = scheduledDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val target = overrideTargetId ?: currentTargetId

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, validationError = null, submitErrorText = null) }

            val request = if (target?.toIntOrNull() != null) {
                CreateUndanganRequest(
                    idPameran = target.toInt(),
                    waktu = formattedWaktu,
                    catatan = current.message.ifBlank { null },
                )
            } else {
                CreateUndanganRequest(
                    idKarya = target?.ifBlank { null },
                    waktu = formattedWaktu,
                    catatan = current.message.ifBlank { null },
                )
            }

            val result = apiUndangan.createUndangan(request)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSending = false) }
                _events.emit(RequestMeetingEvent.RequestSuccess)
            } else {
                val errorMsg = result.exceptionOrNull()?.message
                    ?: AppStrings.get(R.string.request_meeting_error_send_failed)
                _uiState.update { it.copy(isSending = false, submitErrorText = errorMsg) }
            }
        }
    }
}
