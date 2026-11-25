package com.example.pedalpalooza.notifications

import com.example.pedalpalooza.data.ShiftEvent
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object RideReminderTimeCalculator {

    const val PORTLAND_TIME_ZONE_ID = "America/Los_Angeles"
    const val DEFAULT_LEAD_TIME_MINUTES = 30L
    private const val ALL_DAY_DEFAULT_TIME = "09:00:00"

    fun reminderKey(event: ShiftEvent): String =
        "${event.date.orEmpty()}_${event.id}"

    fun uniqueWorkName(reminderKey: String): String =
        "ride_reminder_$reminderKey"

    /**
     * Computes the delay in milliseconds until a ride reminder should fire.
     *
     * Returns:
     * - [ReminderDelayResult.Scheduled] with delay >= 0 if the ride has not started yet.
     * - [ReminderDelayResult.PastEvent] if the ride start time is at or before [nowMillis].
     * - [ReminderDelayResult.InvalidDateTime] if [date] is missing or unparseable.
     */
    fun computeDelay(
        date: String?,
        time: String?,
        nowMillis: Long = System.currentTimeMillis(),
        leadTimeMinutes: Long = DEFAULT_LEAD_TIME_MINUTES
    ): ReminderDelayResult {
        val startMillis = parseEventStartMillis(date, time)
            ?: return ReminderDelayResult.InvalidDateTime

        if (startMillis <= nowMillis) {
            return ReminderDelayResult.PastEvent
        }

        val leadMillis = TimeUnit.MINUTES.toMillis(leadTimeMinutes)
        val triggerAtMillis = startMillis - leadMillis
        val delayMillis = (triggerAtMillis - nowMillis).coerceAtLeast(0L)

        return ReminderDelayResult.Scheduled(
            delayMillis = delayMillis,
            immediate = triggerAtMillis <= nowMillis
        )
    }

    internal fun parseEventStartMillis(date: String?, time: String?): Long? {
        val trimmedDate = date?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val trimmedTime = time?.trim()?.takeIf { it.isNotEmpty() } ?: ALL_DAY_DEFAULT_TIME

        val pattern = when (trimmedTime.count { it == ':' }) {
            1 -> "yyyy-MM-dd HH:mm"
            2 -> "yyyy-MM-dd HH:mm:ss"
            else -> return null
        }

        val parser = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone(PORTLAND_TIME_ZONE_ID)
        }

        return try {
            parser.parse("$trimmedDate $trimmedTime")?.time
        } catch (_: ParseException) {
            null
        }
    }
}

sealed interface ReminderDelayResult {
    data class Scheduled(val delayMillis: Long, val immediate: Boolean) : ReminderDelayResult
    data object PastEvent : ReminderDelayResult
    data object InvalidDateTime : ReminderDelayResult
}
