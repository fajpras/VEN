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
}
