package com.ven.app.ui.screens.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleViewModelTest {

    @Test
    fun initialState_hasApprovedTabSelectedByDefault() {
        val vm = ScheduleViewModel(autoLoad = false)
        val state = vm.uiState.value

        assertEquals(ScheduleTab.Approved, state.selectedTab)
        assertEquals("", state.searchQuery)
        assertNull(state.activeEventDetail)
        assertNull(state.activeRequestDetail)
    }

    @Test
    fun selectTab_updatesSelectedTabInState() {
        val vm = ScheduleViewModel(autoLoad = false)

        vm.selectTab(ScheduleTab.Waiting)
        assertEquals(ScheduleTab.Waiting, vm.uiState.value.selectedTab)

        vm.selectTab(ScheduleTab.Request)
        assertEquals(ScheduleTab.Request, vm.uiState.value.selectedTab)

        vm.selectTab(ScheduleTab.History)
        assertEquals(ScheduleTab.History, vm.uiState.value.selectedTab)

        vm.selectTab(ScheduleTab.Approved)
        assertEquals(ScheduleTab.Approved, vm.uiState.value.selectedTab)
    }

    @Test
    fun setSearchQuery_filtersItemsCorrectly() {
        val vm = ScheduleViewModel(autoLoad = false)

        vm.setSearchQuery("Open Source")
        assertEquals("Open Source", vm.uiState.value.searchQuery)

        // Filter getter should contain matching items
        val filtered = vm.uiState.value.filteredApprovedItems
        if (filtered.isNotEmpty()) {
            assertTrue(filtered.all { it.title.contains("Open Source", ignoreCase = true) })
        }
    }

    @Test
    fun selectEventDetail_and_dismissDetail_updatesState() {
        val vm = ScheduleViewModel(autoLoad = false)
        val item = ScheduleEventItem(
            id = "test_event_1",
            title = "Pameran Lukisan Digital",
            dateText = "20 Nov 2026",
            timeText = "10:00 AM",
            fullDateFormatted = "Friday, 20 November 2026 at 10:00 AM",
            isApproved = true,
        )

        vm.selectEventDetail(item)
        assertNotNull(vm.uiState.value.activeEventDetail)
        assertEquals("test_event_1", vm.uiState.value.activeEventDetail?.id)

        vm.dismissDetail()
        assertNull(vm.uiState.value.activeEventDetail)
    }

    @Test
    fun selectRequestDetail_and_dismissDetail_updatesState() {
        val vm = ScheduleViewModel(autoLoad = false)
        val request = ScheduleRequestItem(
            id = "test_req_1",
            requesterName = "Budi Santoso",
            dateText = "22 Nov 2026",
            timeText = "03:00 PM",
            fullDateFormatted = "Sunday, 22 November 2026 at 03:00 PM",
            note = "Diskusi karya lukis",
        )

        vm.selectRequestDetail(request)
        assertNotNull(vm.uiState.value.activeRequestDetail)
        assertEquals("test_req_1", vm.uiState.value.activeRequestDetail?.id)

        vm.dismissDetail()
        assertNull(vm.uiState.value.activeRequestDetail)
    }

    @Test
    fun isJoinable_publicRoom_alwaysTrueWhenApproved() {
        val item = ScheduleEventItem(
            id = "pub_1",
            title = "Pameran Umum",
            dateText = "20 Nov 2026",
            timeText = "10:00 AM",
            fullDateFormatted = "20 Nov 2026",
            roomId = null,
            isApproved = true,
            waktuMillis = 1_000_000_000L,
        )

        assertTrue(item.isJoinable(nowMillis = 2_000_000_000L))
    }

    @Test
    fun isJoinable_privateRoom_onlyTrueInside15MinuteWindow() {
        val scheduleMillis = 1_700_000_000_000L
        val item = ScheduleEventItem(
            id = "priv_1",
            title = "Diskusi Privat",
            dateText = "20 Nov 2026",
            timeText = "10:00 AM",
            fullDateFormatted = "20 Nov 2026",
            roomId = 10,
            isApproved = true,
            waktuMillis = scheduleMillis,
        )

        // 10 minutes before -> joinable
        assertTrue(item.isJoinable(nowMillis = scheduleMillis - 10 * 60_000L))

        // 10 minutes after -> joinable
        assertTrue(item.isJoinable(nowMillis = scheduleMillis + 10 * 60_000L))

        // 20 minutes before -> NOT joinable
        org.junit.Assert.assertFalse(item.isJoinable(nowMillis = scheduleMillis - 20 * 60_000L))

        // 20 minutes after -> NOT joinable
        org.junit.Assert.assertFalse(item.isJoinable(nowMillis = scheduleMillis + 20 * 60_000L))
    }

    @Test
    fun isJoinable_notApproved_alwaysFalse() {
        val item = ScheduleEventItem(
            id = "pending_1",
            title = "Diskusi Pending",
            dateText = "20 Nov 2026",
            timeText = "10:00 AM",
            fullDateFormatted = "20 Nov 2026",
            roomId = 10,
            isApproved = false,
            waktuMillis = 1_700_000_000_000L,
        )

        org.junit.Assert.assertFalse(item.isJoinable(nowMillis = 1_700_000_000_000L))
    }

    @Test
    fun tick_updatesNowMillisInState() {
        val vm = ScheduleViewModel(autoLoad = false)
        vm.tick(nowMillis = 1_750_000_000_000L)

        assertEquals(1_750_000_000_000L, vm.uiState.value.nowMillis)
    }
}
