package com.example.pedalpalooza.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.example.pedalpalooza.data.ShiftEvent
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object CalendarUtils {

    private const val PORTLAND_TIME_ZONE_ID = "America/Los_Angeles"
    private const val DEFAULT_DURATION_HOURS = 2

    fun addToGoogleCalendar(context: Context, event: ShiftEvent): Boolean {
        val url = buildGoogleCalendarUrl(event)
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No browser or calendar app available to open this event.",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    internal fun buildGoogleCalendarUrl(
        event: ShiftEvent,
        durationHours: Int = DEFAULT_DURATION_HOURS
    ): String {
        val params = mutableListOf(
            "action=TEMPLATE",
            "text=${encode(event.title)}",
            "details=${encode(event.details.orEmpty())}"
        )

        val location = listOfNotNull(
            event.venue?.trim()?.takeIf { it.isNotEmpty() },
            event.address?.trim()?.takeIf { it.isNotEmpty() }
        ).joinToString(", ")
        if (location.isNotEmpty()) {
            params += "location=${encode(location)}"
        }

        val dateRange = formatGoogleCalendarDates(
            date = event.date,
            time = event.time,
            durationHours = durationHours
        )
        if (dateRange != null) {
            params += "dates=${dateRange.datesParam}"
            if (dateRange.includeTimeZone) {
                params += "ctz=${encode(PORTLAND_TIME_ZONE_ID)}"
            }
        }

        return "https://www.google.com/calendar/render?${params.joinToString("&")}"
    }

    private data class FormattedDateRange(
        val datesParam: String,
        val includeTimeZone: Boolean
    )

    private fun formatGoogleCalendarDates(
        date: String?,
        time: String?,
        durationHours: Int
    ): FormattedDateRange? {
        val trimmedDate = date?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val timeZone = TimeZone.getTimeZone(PORTLAND_TIME_ZONE_ID)
        val trimmedTime = time?.trim()?.takeIf { it.isNotEmpty() }

        if (trimmedTime == null) {
            val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
                this.timeZone = timeZone
            }
            val parsedDate = try {
                dateParser.parse(trimmedDate)
            } catch (_: ParseException) {
                return null
            } ?: return null

            val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                this.timeZone = timeZone
            }
            val calendar = Calendar.getInstance(timeZone, Locale.US).apply {
                this.time = parsedDate
            }
            val start = dateFormatter.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val end = dateFormatter.format(calendar.time)
            return FormattedDateRange(datesParam = "$start/$end", includeTimeZone = false)
        }

        val pattern = when (trimmedTime.count { it == ':' }) {
            1 -> "yyyy-MM-dd HH:mm"
            2 -> "yyyy-MM-dd HH:mm:ss"
            else -> return null
        }
        val dateTimeParser = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            this.timeZone = timeZone
        }
        val parsedDateTime = try {
            dateTimeParser.parse("$trimmedDate $trimmedTime")
        } catch (_: ParseException) {
            return null
        } ?: return null

        val dateTimeFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply {
            this.timeZone = timeZone
        }
        val calendar = Calendar.getInstance(timeZone, Locale.US).apply {
            this.time = parsedDateTime
        }
        val start = dateTimeFormatter.format(calendar.time)
        calendar.add(Calendar.HOUR_OF_DAY, durationHours)
        val end = dateTimeFormatter.format(calendar.time)
        return FormattedDateRange(datesParam = "$start/$end", includeTimeZone = true)
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
