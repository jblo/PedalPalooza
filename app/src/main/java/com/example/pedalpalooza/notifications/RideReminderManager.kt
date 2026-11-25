package com.example.pedalpalooza.notifications

import android.content.Context
import androidx.core.content.edit
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.pedalpalooza.data.ShiftEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit

sealed interface ReminderToggleResult {
    data class Scheduled(val leadTimeMinutes: Long, val immediate: Boolean) : ReminderToggleResult
    data object Cancelled : ReminderToggleResult
    data object PastEvent : ReminderToggleResult
    data object InvalidTime : ReminderToggleResult
}

interface RideReminderManager {
    val remindedKeys: StateFlow<Set<String>>
    fun toggleReminder(event: ShiftEvent): ReminderToggleResult
    fun cancelReminder(event: ShiftEvent)
}

class InMemoryRideReminderManager(
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val leadTimeMinutes: Long = RideReminderTimeCalculator.DEFAULT_LEAD_TIME_MINUTES
) : RideReminderManager {

    private val _remindedKeys = MutableStateFlow<Set<String>>(emptySet())
    override val remindedKeys: StateFlow<Set<String>> = _remindedKeys.asStateFlow()

    override fun toggleReminder(event: ShiftEvent): ReminderToggleResult {
        val key = RideReminderTimeCalculator.reminderKey(event)
        if (key in _remindedKeys.value) {
            _remindedKeys.update { it - key }
            return ReminderToggleResult.Cancelled
        }

        return when (
            val delayResult = RideReminderTimeCalculator.computeDelay(
                date = event.date,
                time = event.time,
                nowMillis = nowProvider(),
                leadTimeMinutes = leadTimeMinutes
            )
        ) {
            is ReminderDelayResult.Scheduled -> {
                _remindedKeys.update { it + key }
                ReminderToggleResult.Scheduled(
                    leadTimeMinutes = leadTimeMinutes,
                    immediate = delayResult.immediate
                )
            }
            ReminderDelayResult.PastEvent -> ReminderToggleResult.PastEvent
            ReminderDelayResult.InvalidDateTime -> ReminderToggleResult.InvalidTime
        }
    }

    override fun cancelReminder(event: ShiftEvent) {
        val key = RideReminderTimeCalculator.reminderKey(event)
        _remindedKeys.update { it - key }
    }
}

class AndroidRideReminderManager(
    context: Context,
    private val leadTimeMinutes: Long = RideReminderTimeCalculator.DEFAULT_LEAD_TIME_MINUTES
) : RideReminderManager {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(appContext)

    private val _remindedKeys = MutableStateFlow(loadKeys())
    override val remindedKeys: StateFlow<Set<String>> = _remindedKeys.asStateFlow()

    override fun toggleReminder(event: ShiftEvent): ReminderToggleResult {
        val key = RideReminderTimeCalculator.reminderKey(event)
        if (key in _remindedKeys.value) {
            cancelReminderByKey(key)
            return ReminderToggleResult.Cancelled
        }

        val delayResult = RideReminderTimeCalculator.computeDelay(
            date = event.date,
            time = event.time,
            leadTimeMinutes = leadTimeMinutes
        )

        return when (delayResult) {
            is ReminderDelayResult.Scheduled -> {
                val inputData = workDataOf(
                    RideReminderWorker.KEY_REMINDER_KEY to key,
                    RideReminderWorker.KEY_TITLE to event.title,
                    RideReminderWorker.KEY_TIME to event.time.orEmpty(),
                    RideReminderWorker.KEY_VENUE to event.venue.orEmpty(),
                    RideReminderWorker.KEY_URL to event.shareable
                )

                val request = OneTimeWorkRequestBuilder<RideReminderWorker>()
                    .setInitialDelay(delayResult.delayMillis, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build()

                workManager.enqueueUniqueWork(
                    RideReminderTimeCalculator.uniqueWorkName(key),
                    ExistingWorkPolicy.REPLACE,
                    request
                )

                val updated = _remindedKeys.value + key
                saveKeys(updated)
                _remindedKeys.value = updated

                ReminderToggleResult.Scheduled(
                    leadTimeMinutes = leadTimeMinutes,
                    immediate = delayResult.immediate
                )
            }
            ReminderDelayResult.PastEvent -> ReminderToggleResult.PastEvent
            ReminderDelayResult.InvalidDateTime -> ReminderToggleResult.InvalidTime
        }
    }

    override fun cancelReminder(event: ShiftEvent) {
        cancelReminderByKey(RideReminderTimeCalculator.reminderKey(event))
    }

    private fun cancelReminderByKey(key: String) {
        workManager.cancelUniqueWork(RideReminderTimeCalculator.uniqueWorkName(key))
        val updated = _remindedKeys.value - key
        saveKeys(updated)
        _remindedKeys.value = updated
    }

    private fun loadKeys(): Set<String> =
        prefs.getStringSet(KEY_REMINDED_SET, emptySet())?.toSet() ?: emptySet()

    private fun saveKeys(keys: Set<String>) {
        prefs.edit {
            putStringSet(KEY_REMINDED_SET, keys.toSet())
        }
    }

    companion object {
        private const val PREFS_NAME = "pedalpalooza_reminders"
        private const val KEY_REMINDED_SET = "reminded_keys"

        internal fun removePersistedKey(context: Context, key: String) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getStringSet(KEY_REMINDED_SET, emptySet())?.toSet() ?: emptySet()
            if (key in current) {
                prefs.edit {
                    putStringSet(KEY_REMINDED_SET, current - key)
                }
            }
        }
    }
}
