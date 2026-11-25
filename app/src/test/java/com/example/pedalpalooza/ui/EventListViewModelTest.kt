package com.example.pedalpalooza.ui

import com.example.pedalpalooza.data.EventRepository
import com.example.pedalpalooza.data.Pagination
import com.example.pedalpalooza.data.ShiftEvent
import com.example.pedalpalooza.data.ShiftResponse
import com.example.pedalpalooza.notifications.InMemoryRideReminderManager
import com.example.pedalpalooza.notifications.RideReminderTimeCalculator
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load populates grouped events and pagination state`() = runTest {
        val repo = FakeEventRepository { _, limit, offset ->
            ShiftResponse(
                events = listOf(
                    sampleEvent("1", "Coffee Ride", "2026-07-01"),
                    sampleEvent("2", "Sunset Cruise", "2026-07-01"),
                    sampleEvent("3", "Bridge Tour", "2026-07-02")
                ),
                pagination = Pagination(offset = offset, limit = limit, fullcount = 5)
            )
        }

        val viewModel = EventListViewModel(repository = repo, pageSize = 3)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInitialLoading)
        assertNull(state.errorMessage)
        assertEquals(3, state.events.size)
        assertEquals(2, state.groupedEvents.size)
        assertEquals(2, state.groupedEvents["2026-07-01"]?.size)
        assertEquals(5, state.totalCount)
        assertTrue(state.canLoadMore)
    }

    @Test
    fun `initial error recovers cleanly on retry`() = runTest {
        var shouldFail = true
        val repo = FakeEventRepository { _, limit, offset ->
            if (shouldFail) throw RuntimeException("Network unavailable")
            ShiftResponse(
                events = listOf(sampleEvent("1", "Recovered Ride", "2026-07-01")),
                pagination = Pagination(offset = offset, limit = limit, fullcount = 1)
            )
        }

        val viewModel = EventListViewModel(repository = repo, pageSize = 10)
        advanceUntilIdle()

        assertEquals("Network unavailable", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.events.isEmpty())

        shouldFail = false
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, viewModel.uiState.value.events.size)
        assertFalse(viewModel.uiState.value.canLoadMore)
    }

    @Test
    fun `loadMore appends second page and updates canLoadMore`() = runTest {
        val repo = FakeEventRepository { _, limit, offset ->
            when (offset) {
                0 -> ShiftResponse(
                    events = listOf(
                        sampleEvent("1", "Ride 1", "2026-07-01"),
                        sampleEvent("2", "Ride 2", "2026-07-01")
                    ),
                    pagination = Pagination(offset = 0, limit = limit, fullcount = 3)
                )
                2 -> ShiftResponse(
                    events = listOf(sampleEvent("3", "Ride 3", "2026-07-02")),
                    pagination = Pagination(offset = 2, limit = limit, fullcount = 3)
                )
                else -> ShiftResponse(emptyList(), Pagination(offset, limit, 3))
            }
        }

        val viewModel = EventListViewModel(repository = repo, pageSize = 2)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canLoadMore)

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.events.size)
        assertFalse(state.canLoadMore)
        assertFalse(state.isLoadingMore)
        assertNull(state.loadMoreError)
    }

    @Test
    fun `loadMore failure preserves existing events and exposes loadMoreError`() = runTest {
        val repo = FakeEventRepository { _, limit, offset ->
            if (offset > 0) throw RuntimeException("Timeout loading page 2")
            ShiftResponse(
                events = listOf(sampleEvent("1", "Ride 1", "2026-07-01")),
                pagination = Pagination(offset = 0, limit = limit, fullcount = 4)
            )
        }

        val viewModel = EventListViewModel(repository = repo, pageSize = 1)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.events.size)
        assertEquals("Timeout loading page 2", state.loadMoreError)
        assertTrue(state.canLoadMore)
    }

    @Test
    fun `search debounces rapid input and clearSearch reloads immediately`() = runTest {
        val calls = mutableListOf<String>()
        val repo = FakeEventRepository { query, limit, offset ->
            calls += query
            ShiftResponse(
                events = listOf(sampleEvent("1", "Match for $query", "2026-07-01")),
                pagination = Pagination(offset = offset, limit = limit, fullcount = 1)
            )
        }

        val viewModel = EventListViewModel(
            repository = repo,
            pageSize = 20,
            searchDebounceMs = 300L
        )
        advanceUntilIdle()
        assertEquals(listOf(""), calls)

        viewModel.onSearchQueryChange("c")
        advanceTimeBy(100L)
        viewModel.onSearchQueryChange("co")
        advanceTimeBy(100L)
        viewModel.onSearchQueryChange("coffee")
        advanceTimeBy(299L)
        assertEquals(listOf(""), calls)

        advanceUntilIdle()
        assertEquals(listOf("", "coffee"), calls)

        viewModel.clearSearch()
        advanceUntilIdle()
        assertEquals(listOf("", "coffee", ""), calls)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `refresh reloads page 0 and clears refreshing flag`() = runTest {
        var callCount = 0
        val repo = FakeEventRepository { _, limit, offset ->
            callCount++
            ShiftResponse(
                events = listOf(sampleEvent("$callCount", "Ride #$callCount", "2026-07-01")),
                pagination = Pagination(offset = offset, limit = limit, fullcount = 1)
            )
        }

        val viewModel = EventListViewModel(repository = repo, pageSize = 10)
        advanceUntilIdle()
        assertEquals("Ride #1", viewModel.uiState.value.events.single().title)

        viewModel.refresh()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals("Ride #2", viewModel.uiState.value.events.single().title)
    }

    @Test
    fun `toggleReminder schedules, filters reminders-only view, and cancels`() = runTest {
        val futureEvent = sampleEvent("1", "Future Ride", "2026-07-22")
        val otherEvent = sampleEvent("2", "Other Ride", "2026-07-22")
        val startMillis = RideReminderTimeCalculator.parseEventStartMillis("2026-07-22", "18:00:00")!!
        val reminderManager = InMemoryRideReminderManager(
            nowProvider = { startMillis - TimeUnit.HOURS.toMillis(3) }
        )
        val repo = FakeEventRepository { _, limit, offset ->
            ShiftResponse(
                events = listOf(futureEvent, otherEvent),
                pagination = Pagination(offset = offset, limit = limit, fullcount = 2)
            )
        }

        val viewModel = EventListViewModel(repository = repo, reminderManager = reminderManager)
        advanceUntilIdle()

        viewModel.toggleReminder(futureEvent)
        advanceUntilIdle()

        val key = RideReminderTimeCalculator.reminderKey(futureEvent)
        assertTrue(key in viewModel.uiState.value.remindedEventKeys)
        assertTrue(viewModel.uiState.value.snackbarMessage?.contains("30 min") == true)

        viewModel.toggleShowRemindersOnly(true)
        assertEquals(1, viewModel.uiState.value.groupedEvents.values.flatten().size)
        assertEquals("1", viewModel.uiState.value.groupedEvents.values.flatten().single().id)

        viewModel.toggleReminder(futureEvent)
        advanceUntilIdle()
        assertFalse(key in viewModel.uiState.value.remindedEventKeys)
        assertTrue(viewModel.uiState.value.groupedEvents.isEmpty())
        assertTrue(viewModel.uiState.value.snackbarMessage?.contains("removed") == true)
    }

    @Test
    fun `toggleReminder on past event shows message without adding key`() = runTest {
        val pastEvent = sampleEvent("1", "Past Ride", "2026-07-22")
        val startMillis = RideReminderTimeCalculator.parseEventStartMillis("2026-07-22", "18:00:00")!!
        val reminderManager = InMemoryRideReminderManager(
            nowProvider = { startMillis + TimeUnit.HOURS.toMillis(1) }
        )
        val repo = FakeEventRepository { _, limit, offset ->
            ShiftResponse(
                events = listOf(pastEvent),
                pagination = Pagination(offset = offset, limit = limit, fullcount = 1)
            )
        }

        val viewModel = EventListViewModel(repository = repo, reminderManager = reminderManager)
        advanceUntilIdle()

        viewModel.toggleReminder(pastEvent)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.remindedEventKeys.isEmpty())
        assertTrue(viewModel.uiState.value.snackbarMessage?.contains("already started") == true)
    }

    private fun sampleEvent(id: String, title: String, date: String) = ShiftEvent(
        id = id,
        title = title,
        date = date,
        time = "18:00:00",
        venue = "Test Park"
    )

    private class FakeEventRepository(
        private val handler: suspend (query: String, limit: Int, offset: Int) -> ShiftResponse
    ) : EventRepository {
        override suspend fun searchEvents(
            query: String,
            limit: Int,
            offset: Int
        ): ShiftResponse = handler(query, limit, offset)
    }
}
