package com.ven.app.ui.screens.requestmeeting

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ven.app.R
import com.ven.app.util.DateFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Jenis kesalahan validasi pada form pengajuan pertemuan. */
enum class RequestMeetingValidationError(@param:StringRes val messageRes: Int) {
    TimeNotSet(R.string.request_meeting_error_time_required),
    InvalidSchedule(R.string.request_meeting_error_invalid_schedule),
}

@Immutable
data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean = false,
    val isEnabled: Boolean = true,
) {
    val dayNumber: Int get() = date.dayOfMonth
}

@Immutable
data class RequestMeetingUiState(
    val pameranId: String = "",
    val pameranTitle: String = "",
    val bannerUrl: String? = null,
    val bannerColor: String? = null,
    val visibleMonth: YearMonth = YearMonth.from(LocalDate.now()),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDateText: String = DateFormatter.formatRequestMeetingDate(selectedDate),
    val monthTitleText: String = DateFormatter.formatMonthYear(visibleMonth),
    val calendarDays: List<CalendarDay> = emptyList(),
    val hourText: String = "02",
    val minuteText: String = "30",
    val isAm: Boolean = false,
    val message: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val validationError: RequestMeetingValidationError? = null,
    val submitErrorText: String? = null,
)

fun buildMonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate?,
    today: LocalDate = LocalDate.now(),
): List<CalendarDay> {
    val firstDayOfMonth = visibleMonth.atDay(1)
    // DayOfWeek: MONDAY = 1 ... SUNDAY = 7
    val daysBefore = firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value
    val startDate = firstDayOfMonth.minusDays(daysBefore.toLong())

    val totalDays = 42 // 6 baris x 7 kolom agar tinggi grid selalu konsisten
    val days = ArrayList<CalendarDay>(totalDays)

    for (i in 0 until totalDays) {
        val currentDate = startDate.plusDays(i.toLong())
        val isCurrentMonth = (currentDate.month == visibleMonth.month && currentDate.year == visibleMonth.year)
        val isSelected = (selectedDate != null && currentDate == selectedDate)
        val isEnabled = isCurrentMonth && !currentDate.isBefore(today)

        days.add(
            CalendarDay(
                date = currentDate,
                isCurrentMonth = isCurrentMonth,
                isSelected = isSelected,
                isEnabled = isEnabled,
            )
        )
    }

    return days
}
