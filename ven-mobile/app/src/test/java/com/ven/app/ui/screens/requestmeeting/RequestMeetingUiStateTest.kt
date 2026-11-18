package com.ven.app.ui.screens.requestmeeting

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestMeetingUiStateTest {

    @Test
    fun defaultState_hasNeutralValuesConsistentWithToday() {
        val state = RequestMeetingUiState()

        assertEquals("", state.pameranId)
        assertEquals("", state.pameranTitle)
        assertNull(state.bannerUrl)
        assertNull(state.bannerColor)

        // Nilai kalender default mengikuti hari ini dan selalu konsisten.
        assertEquals(java.time.YearMonth.from(LocalDate.now()), state.visibleMonth)
        assertEquals(LocalDate.now(), state.selectedDate)
        assertEquals(
            com.ven.app.util.DateFormatter.formatRequestMeetingDate(state.selectedDate),
            state.selectedDateText,
        )
        assertEquals(
            com.ven.app.util.DateFormatter.formatMonthYear(state.visibleMonth),
            state.monthTitleText,
        )
        assertTrue(state.calendarDays.isEmpty())

        assertEquals("02", state.hourText)
        assertEquals("30", state.minuteText)
        assertFalse("Default should be PM (isAm = false)", state.isAm)
        assertEquals("", state.message)

        assertFalse(state.isLoading)
        assertFalse(state.isSending)
        assertNull(state.validationError)
        assertNull(state.submitErrorText)
    }

    @Test
    fun calendarDay_dayNumberComputedFromDate() {
        val day = CalendarDay(
            date = LocalDate.of(2026, 9, 15),
            isCurrentMonth = true,
        )

        assertEquals(15, day.dayNumber)
        assertFalse(day.isSelected)
        assertTrue(day.isCurrentMonth)
    }
}
