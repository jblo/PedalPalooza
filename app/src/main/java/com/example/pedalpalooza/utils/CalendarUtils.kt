package com.example.pedalpalooza.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.pedalpalooza.data.ShiftEvent
import java.net.URLEncoder

object CalendarUtils {

    fun addToGoogleCalendar(context: Context, event: ShiftEvent) {
        val baseUrl = "https://www.google.com/calendar/render?action=TEMPLATE"
        
        val title = URLEncoder.encode(event.title, "UTF-8")
        val details = URLEncoder.encode(event.details ?: "", "UTF-8")
        val location = URLEncoder.encode("${event.venue ?: ""}, ${event.address ?: ""}", "UTF-8")
        
        // Format date/time: YYYYMMDDTHHMMSSZ/YYYYMMDDTHHMMSSZ
        // Shift API provides "date": "2026-07-22" and "time": "06:00:00"
        val datePart = event.date?.replace("-", "") ?: ""
        val timePart = event.time?.replace(":", "") ?: "000000"
        
        // For simplicity, we'll assume a 2-hour duration if not specified
        val startDateTime = "${datePart}T${timePart}"
        val endDateTime = "${datePart}T${(timePart.toIntOrNull() ?: 0) + 20000}" // Rough estimate
        
        val url = "$baseUrl&text=$title&details=$details&location=$location&dates=$startDateTime/$endDateTime"
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
