package com.ven.app.ui.screens.schedule

import com.ven.app.data.api.models.UndanganItemModel
import com.ven.app.ui.screens.requestmeeting.RequestMeetingValidationError
import com.ven.app.ui.screens.requestmeeting.RequestMeetingViewModel
import com.ven.app.util.DateFormatter
import java.time.LocalDate
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleAndInvitationTest {

    @Test
    fun `schedule tabs only contain Upcoming Incoming and MyRequests`() {
        assertEquals(
            listOf(ScheduleTab.Approved, ScheduleTab.Request, ScheduleTab.Waiting),
            ScheduleTab.entries,
        )
    }

    @Test
    fun `undangan model builds deep link from tautan token`() {
        val model = UndanganItemModel(
            idUndangan = 7,
            idPengguna = "user-1",
            idRuangan = 12,
            status = "disetujui",
            waktu = "2026-12-01 14:00:00",
            durasiMenit = 45,
            tautan = "abc123token",
        )
        assertEquals("ven://diskusi/abc123token", model.deepLink)

        val pending = model.copy(tautan = null)
        assertNull(pending.deepLink)
    }

    @Test
    fun `isExpiredAtMidnight returns false before 00_00 and true after 00_00`() {
        val sameDayEvening = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 1, 23, 55, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val nextDayJustAfterMidnight = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 2, 0, 1, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertFalse(
            DateFormatter.isExpiredAtMidnight(
                waktu = "2026-12-01 14:00:00",
                kadaluarsaPada = "2026-12-01 23:59:59",
                nowMillis = sameDayEvening,
            )
        )

        assertTrue(
            DateFormatter.isExpiredAtMidnight(
                waktu = "2026-12-01 14:00:00",
                kadaluarsaPada = "2026-12-01 23:59:59",
                nowMillis = nextDayJustAfterMidnight,
            )
        )
    }

    @Test
    fun `isReminderDue returns true for H-1 and false for 5 days ahead`() {
        val todayNoon = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertTrue(
            DateFormatter.isReminderDue(
                waktu = "2026-12-02 10:00:00",
                nowMillis = todayNoon,
            )
        )

        assertFalse(
            DateFormatter.isReminderDue(
                waktu = "2026-12-06 10:00:00",
                nowMillis = todayNoon,
            )
        )
    }

    @Test
    fun `request meeting validates required discussion topic and duration options`() {
        val tomorrow = LocalDate.now().plusDays(1)
        val vm = RequestMeetingViewModel(today = tomorrow)

        vm.onDurationSelected(45)
        assertEquals(45, vm.uiState.value.selectedDurationMinutes)

        // Invalid duration ignored
        vm.onDurationSelected(99)
        assertEquals(45, vm.uiState.value.selectedDurationMinutes)

        // Too short topic triggers MessageRequired validation error
        vm.onMessageChange("Hi")
        vm.sendRequest("10")
        assertEquals(RequestMeetingValidationError.MessageRequired, vm.uiState.value.validationError)
    }

    @Test
    fun `schedule ui state exposes upcoming reminder only when active item is due`() {
        val activeDue = ScheduleEventItem(
            id = "1",
            title = "Pameran A",
            dateText = "2 Dec 2026",
            timeText = "10:00 AM",
            fullDateFormatted = "Wednesday, 2 December 2026 at 10:00 AM",
            isApproved = true,
            isExpired = false,
            isReminderDue = true,
        )
        val expiredDue = activeDue.copy(id = "2", isExpired = true)

        assertTrue(ScheduleUiState(approvedItems = listOf(activeDue)).hasUpcomingReminder)
        assertFalse(ScheduleUiState(approvedItems = listOf(expiredDue)).hasUpcomingReminder)
    }
}
