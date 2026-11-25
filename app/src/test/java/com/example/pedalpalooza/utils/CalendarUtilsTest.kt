package com.example.pedalpalooza.utils

import com.example.pedalpalooza.data.ShiftEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarUtilsTest {

    @Test
    fun `morning ride preserves leading zeros and includes Portland timezone`() {
        val event = ShiftEvent(
            id = "1",
            title = "Sunrise Coffee Ride",
            venue = "Laurelhurst Park",
            address = "SE Cesar E Chavez Blvd",
            details = "Bring a mug!",
            date = "2026-07-22",
            time = "06:00:00"
        )

        val url = CalendarUtils.buildGoogleCalendarUrl(event)

        assertTrue(url.contains("dates=20260722T060000/20260722T080000"))
        assertTrue(url.contains("ctz=America%2FLos_Angeles"))
        assertTrue(url.contains("location=Laurelhurst+Park%2C+SE+Cesar+E+Chavez+Blvd"))
    }

    @Test
    fun `late night ride rolls over midnight and month boundary cleanly`() {
        val event = ShiftEvent(
            id = "2",
            title = "Midnight Mystery Ride",
            date = "2026-07-31",
            time = "23:00:00"
        )

        val url = CalendarUtils.buildGoogleCalendarUrl(event)

        assertTrue(url.contains("dates=20260731T230000/20260801T010000"))
        assertTrue(url.contains("ctz=America%2FLos_Angeles"))
        assertFalse(url.contains("location="))
    }

    @Test
    fun `all day ride without time formats date range without timezone`() {
        val event = ShiftEvent(
            id = "3",
            title = "All Day Bike Fest",
            venue = "Waterfront Park",
            address = null,
            date = "2026-06-15",
            time = null
        )

        val url = CalendarUtils.buildGoogleCalendarUrl(event)

        assertTrue(url.contains("dates=20260615/20260616"))
        assertFalse(url.contains("ctz="))
        assertTrue(url.contains("location=Waterfront+Park"))
        assertFalse(url.contains("%2C"))
    }

    @Test
    fun `missing or invalid date omits dates parameter`() {
        val event = ShiftEvent(
            id = "4",
            title = "TBD Ride",
            date = null,
            time = "18:00:00"
        )

        val url = CalendarUtils.buildGoogleCalendarUrl(event)

        assertFalse(url.contains("dates="))
        assertFalse(url.contains("ctz="))
    }
}
