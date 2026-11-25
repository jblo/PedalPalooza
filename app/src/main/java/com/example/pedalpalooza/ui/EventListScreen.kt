package com.example.pedalpalooza.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pedalpalooza.data.ShiftEvent
import com.example.pedalpalooza.notifications.RideReminderTimeCalculator
import com.example.pedalpalooza.utils.CalendarUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    modifier: Modifier = Modifier,
    viewModel: EventListViewModel = viewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onEventClick: (ShiftEvent) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var pendingReminderEvent by remember { mutableStateOf<ShiftEvent?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            val event = pendingReminderEvent
            pendingReminderEvent = null
            if (granted && event != null) {
                viewModel.toggleReminder(event)
            } else if (!granted) {
                viewModel.onNotificationPermissionDenied()
            }
        }
    )

    val handleReminderClick: (ShiftEvent) -> Unit = { event ->
        val key = RideReminderTimeCalculator.reminderKey(event)
        val isAlreadyReminded = key in uiState.remindedEventKeys
        if (isAlreadyReminded || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            viewModel.toggleReminder(event)
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.toggleReminder(event)
            } else {
                pendingReminderEvent = event
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbarMessage()
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 5
        }
    }

    LaunchedEffect(
        shouldLoadMore,
        uiState.canLoadMore,
        uiState.isLoadingMore,
        uiState.loadMoreError,
        uiState.showRemindersOnly
    ) {
        if (!uiState.showRemindersOnly &&
            shouldLoadMore &&
            uiState.canLoadMore &&
            !uiState.isLoadingMore &&
            uiState.loadMoreError == null
        ) {
            viewModel.loadMore()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search rides, venues, organizers...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.clearSearch()
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.submitSearch()
                    focusManager.clearFocus()
                }
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !uiState.showRemindersOnly,
                onClick = { viewModel.toggleShowRemindersOnly(false) },
                label = { Text("All Rides") }
            )
            FilterChip(
                selected = uiState.showRemindersOnly,
                onClick = { viewModel.toggleShowRemindersOnly(true) },
                label = {
                    val count = uiState.remindedEventKeys.size
                    Text(if (count > 0) "Reminders ($count)" else "Reminders")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isInitialLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                uiState.events.isEmpty() && uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Failed to load events",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = viewModel::retry) {
                            Text("Retry")
                        }
                    }
                }

                uiState.groupedEvents.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                uiState.showRemindersOnly -> "No ride reminders set yet. Tap \"Remind Me\" on any ride to get notified 30 minutes before it starts."
                                uiState.searchQuery.isNotBlank() -> "No rides match \"${uiState.searchQuery.trim()}\"."
                                else -> "No upcoming events found."
                            },
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        when {
                            uiState.showRemindersOnly -> {
                                OutlinedButton(onClick = { viewModel.toggleShowRemindersOnly(false) }) {
                                    Text("Show all rides")
                                }
                            }
                            uiState.searchQuery.isNotBlank() -> {
                                OutlinedButton(onClick = viewModel::clearSearch) {
                                    Text("Clear search")
                                }
                            }
                            else -> {
                                OutlinedButton(onClick = viewModel::refresh) {
                                    Text("Refresh")
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.errorMessage != null) {
                            item(key = "refresh_error") {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = uiState.errorMessage ?: "Refresh failed",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = viewModel::refresh) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }

                        if (!uiState.showRemindersOnly && uiState.totalCount > 0) {
                            item(key = "result_count") {
                                Text(
                                    text = "Showing ${uiState.events.size} of ${uiState.totalCount} rides",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        uiState.groupedEvents.forEach { (date, dateEvents) ->
                            item(key = "header_$date") {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(dateEvents, key = { "${date}_${it.id}" }) { event ->
                                val isReminded = RideReminderTimeCalculator.reminderKey(event) in uiState.remindedEventKeys
                                EventCard(
                                    event = event,
                                    isReminded = isReminded,
                                    onReminderClick = { handleReminderClick(event) },
                                    onClick = { onEventClick(event) }
                                )
                            }
                        }

                        if (!uiState.showRemindersOnly) {
                            if (uiState.isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    }
                                }
                            } else if (uiState.loadMoreError != null) {
                                item(key = "load_more_error") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = uiState.loadMoreError ?: "Failed to load more rides",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )
                                        TextButton(onClick = viewModel::loadMore) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            } else if (uiState.canLoadMore) {
                                item(key = "load_more_button") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        OutlinedButton(onClick = viewModel::loadMore) {
                                            Text("Load more rides")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: ShiftEvent,
    isReminded: Boolean = false,
    onReminderClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = if (event.cancelled) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (event.cancelled) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "CANCELLED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textDecoration = if (event.cancelled) TextDecoration.LineThrough else TextDecoration.None
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${event.time ?: ""} @ ${event.venue ?: "TBD"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            if (!event.address.isNullOrBlank()) {
                Text(
                    text = event.address,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!event.cancelled) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isReminded) {
                        FilledTonalButton(
                            onClick = onReminderClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reminded", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onReminderClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remind Me", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { CalendarUtils.addToGoogleCalendar(context, event) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Cal", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
