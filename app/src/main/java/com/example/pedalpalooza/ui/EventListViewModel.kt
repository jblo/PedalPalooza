package com.example.pedalpalooza.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pedalpalooza.data.EventRepository
import com.example.pedalpalooza.data.ShiftEvent
import com.example.pedalpalooza.data.ShiftEventRepository
import com.example.pedalpalooza.notifications.AndroidRideReminderManager
import com.example.pedalpalooza.notifications.InMemoryRideReminderManager
import com.example.pedalpalooza.notifications.ReminderToggleResult
import com.example.pedalpalooza.notifications.RideReminderManager
import com.example.pedalpalooza.notifications.RideReminderTimeCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventListUiState(
    val events: List<ShiftEvent> = emptyList(),
    val groupedEvents: Map<String, List<ShiftEvent>> = emptyMap(),
    val searchQuery: String = "",
    val remindedEventKeys: Set<String> = emptySet(),
    val showRemindersOnly: Boolean = false,
    val snackbarMessage: String? = null,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null,
    val totalCount: Int = 0,
    val canLoadMore: Boolean = false
)

class EventListViewModel(
    private val repository: EventRepository = ShiftEventRepository(),
    private val reminderManager: RideReminderManager = InMemoryRideReminderManager(),
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val searchDebounceMs: Long = DEFAULT_SEARCH_DEBOUNCE_MS
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EventListUiState(remindedEventKeys = reminderManager.remindedKeys.value)
    )
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    private var activeLoadJob: Job? = null
    private var searchDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            reminderManager.remindedKeys.collect { keys ->
                _uiState.update { current ->
                    current.copy(
                        remindedEventKeys = keys,
                        groupedEvents = groupEvents(current.events, keys, current.showRemindersOnly)
                    )
                }
            }
        }
        loadFirstPage(isRefresh = false)
    }

    fun onSearchQueryChange(query: String) {
        if (query == _uiState.value.searchQuery) return
        _uiState.update { it.copy(searchQuery = query) }

        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(searchDebounceMs)
            loadFirstPage(isRefresh = false)
        }
    }

    fun submitSearch() {
        searchDebounceJob?.cancel()
        loadFirstPage(isRefresh = false)
    }

    fun clearSearch() {
        if (_uiState.value.searchQuery.isEmpty()) return
        searchDebounceJob?.cancel()
        _uiState.update { it.copy(searchQuery = "") }
        loadFirstPage(isRefresh = false)
    }

    fun toggleShowRemindersOnly(showOnly: Boolean) {
        if (_uiState.value.showRemindersOnly == showOnly) return
        _uiState.update { current ->
            current.copy(
                showRemindersOnly = showOnly,
                groupedEvents = groupEvents(current.events, current.remindedEventKeys, showOnly)
            )
        }
    }

    fun toggleReminder(event: ShiftEvent) {
        val message = when (val result = reminderManager.toggleReminder(event)) {
            is ReminderToggleResult.Scheduled -> {
                if (result.immediate) {
                    "Reminder sent — \"${event.title}\" starts soon!"
                } else {
                    "Reminder set for ${result.leadTimeMinutes} min before \"${event.title}\""
                }
            }
            ReminderToggleResult.Cancelled -> "Reminder removed for \"${event.title}\""
            ReminderToggleResult.PastEvent -> "Can't set a reminder for a ride that already started"
            ReminderToggleResult.InvalidTime -> "Can't schedule reminder — missing or invalid ride time"
        }
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun onNotificationPermissionDenied() {
        _uiState.update {
            it.copy(snackbarMessage = "Enable notification permission in settings to receive ride reminders")
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun refresh() {
        searchDebounceJob?.cancel()
        loadFirstPage(isRefresh = true)
    }

    fun retry() {
        if (_uiState.value.events.isEmpty()) {
            loadFirstPage(isRefresh = false)
        } else {
            loadMore()
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isInitialLoading || state.isRefreshing || state.isLoadingMore || !state.canLoadMore) {
            return
        }

        val currentQuery = state.searchQuery
        val nextOffset = state.events.size

        _uiState.update { it.copy(isLoadingMore = true, loadMoreError = null) }

        activeLoadJob?.cancel()
        activeLoadJob = viewModelScope.launch {
            try {
                val response = repository.searchEvents(
                    query = currentQuery,
                    limit = pageSize,
                    offset = nextOffset
                )
                _uiState.update { current ->
                    val existingKeys = current.events.mapTo(HashSet()) { "${it.date}_${it.id}" }
                    val newUniqueEvents = response.events.filter {
                        existingKeys.add("${it.date}_${it.id}")
                    }
                    val merged = current.events + newUniqueEvents
                    current.copy(
                        events = merged,
                        groupedEvents = groupEvents(merged, current.remindedEventKeys, current.showRemindersOnly),
                        isLoadingMore = false,
                        loadMoreError = null,
                        totalCount = response.pagination.fullcount,
                        canLoadMore = response.events.isNotEmpty() && merged.size < response.pagination.fullcount
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLoadingMore = false,
                        loadMoreError = e.message ?: "Failed to load more events"
                    )
                }
            }
        }
    }

    private fun loadFirstPage(isRefresh: Boolean) {
        val query = _uiState.value.searchQuery
        activeLoadJob?.cancel()

        _uiState.update { current ->
            current.copy(
                isInitialLoading = !isRefresh && current.events.isEmpty(),
                isRefreshing = isRefresh || (!isRefresh && current.events.isNotEmpty()),
                isLoadingMore = false,
                errorMessage = if (current.events.isEmpty()) null else current.errorMessage,
                loadMoreError = null
            )
        }

        activeLoadJob = viewModelScope.launch {
            try {
                val response = repository.searchEvents(
                    query = query,
                    limit = pageSize,
                    offset = 0
                )
                val loaded = response.events
                _uiState.update { current ->
                    current.copy(
                        events = loaded,
                        groupedEvents = groupEvents(loaded, current.remindedEventKeys, current.showRemindersOnly),
                        isInitialLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        loadMoreError = null,
                        totalCount = response.pagination.fullcount,
                        canLoadMore = loaded.isNotEmpty() && loaded.size < response.pagination.fullcount
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = e.message ?: "Failed to load events"
                    )
                }
            }
        }
    }

    private fun groupEvents(
        events: List<ShiftEvent>,
        remindedKeys: Set<String>,
        showRemindersOnly: Boolean
    ): Map<String, List<ShiftEvent>> {
        val filtered = if (showRemindersOnly) {
            events.filter { RideReminderTimeCalculator.reminderKey(it) in remindedKeys }
        } else {
            events
        }
        return filtered.groupBy { it.date?.takeIf { d -> d.isNotBlank() } ?: "Upcoming" }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
        const val DEFAULT_SEARCH_DEBOUNCE_MS = 350L

        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EventListViewModel(
                        repository = ShiftEventRepository(),
                        reminderManager = AndroidRideReminderManager(appContext)
                    ) as T
                }
            }
        }
    }
}
