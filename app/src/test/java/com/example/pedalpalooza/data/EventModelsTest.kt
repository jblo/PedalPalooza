package com.example.pedalpalooza.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventModelsTest {

    @Test
    fun `deserializes ShiftResponse ignoring unknown keys and handling optional fields`() {
        val rawJson = """
            {
              "events": [
                {
                  "id": "101",
                  "title": "World Naked Bike Ride",
                  "venue": "Laurelhurst Park",
                  "address": "SE 39th & Stark",
                  "organizer": "Shift",
                  "details": "Ride safely!",
                  "time": "19:00:00",
                  "date": "2026-07-25",
                  "shareable": "https://www.shift2bikes.org/calendar/event-101",
                  "cancelled": false,
                  "extra_unknown_field": "ignored"
                },
                {
                  "id": "102",
                  "title": "Cancelled Rain Ride",
                  "cancelled": true
                }
              ],
              "pagination": {
                "offset": 0,
                "limit": 50,
                "fullcount": 2
              }
            }
        """.trimIndent()

        val response = ShiftApiService.json.decodeFromString<ShiftResponse>(rawJson)

        assertEquals(2, response.events.size)
        assertEquals(2, response.pagination.fullcount)

        val first = response.events[0]
        assertEquals("101", first.id)
        assertEquals("Laurelhurst Park", first.venue)
        assertFalse(first.cancelled)

        val second = response.events[1]
        assertEquals("102", second.id)
        assertTrue(second.cancelled)
        assertNull(second.venue)
        assertNull(second.time)
    }
}
