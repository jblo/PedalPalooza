package com.example.pedalpalooza.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class RideReminderTimeCalculatorTest {

    @Test
    fun `future ride schedules 30 minutes before start`() {
        val startMillis = RideReminderTimeCalculator.parseEventStartMillis("2026-07-22", "18:00:00")!!
        val nowMillis = startMillis - TimeUnit.HOURS.toMillis(2)

        val result = RideReminderTimeCalculator.computeDelay(
            date = "2026-07-22",
            time = "18:00:00",
            nowMillis = nowMillis,
            leadTimeMinutes = 30L
        )

        assertTrue(result is ReminderDelayResult.Scheduled)
        val scheduled = result as ReminderDelayResult.Scheduled
        assertEquals(TimeUnit.MINUTES.toMillis(90), scheduled.delayMillis)
        assertFalse(scheduled.immediate)
    }

    @Test
    fun `ride starting within lead window schedules immediately`() {
        val startMillis = RideReminderTimeCalculator.parseEventStartMillis("2026-07-22", "18:00:00")!!
        val nowMillis = startMillis - TimeUnit.MINUTES.toMillis(15)

        val result = RideReminderTimeCalculator.computeDelay(
            date = "2026-07-22",
            time = "18:00:00",
            nowMillis = nowMillis,
            leadTimeMinutes = 30L
        )

        assertTrue(result is ReminderDelayResult.Scheduled)
        val scheduled = result as ReminderDelayResult.Scheduled
        assertEquals(0L, scheduled.delayMillis)
        assertTrue(scheduled.immediate)
    }

    @Test
    fun `ride that already started returns PastEvent`() {
        val startMillis = RideReminderTimeCalculator.parseEventStartMillis("2026-07-22", "18:00:00")!!
        val nowMillis = startMillis + TimeUnit.MINUTES.toMillis(5)

        val result = RideReminderTimeCalculator.computeDelay(
            date = "2026-07-22",
            time = "18:00:00",
            nowMillis = nowMillis
        )

        assertEquals(ReminderDelayResult.PastEvent, result)
    }

    @Test
    fun `all day ride without time defaults to 9am Portland time`() {
        val expectedStart = RideReminderTimeCalculator.parseEventStartMillis("2026-07-22", "09:00:00")!!
        val nowMillis = expectedStart - TimeUnit.HOURS.toMillis(1)

        val result = RideReminderTimeCalculator.computeDelay(
            date = "2026-07-22",
            time = null,
            nowMillis = nowMillis,
            leadTimeMinutes = 30L
        )

        assertTrue(result is ReminderDelayResult.Scheduled)
        val scheduled = result as ReminderDelayResult.Scheduled
        assertEquals(TimeUnit.MINUTES.toMillis(30), scheduled.delayMillis)
    }

    @Test
    fun `missing or malformed date returns InvalidDateTime`() {
        assertEquals(
            ReminderDelayResult.InvalidDateTime,
            RideReminderTimeCalculator.computeDelay(date = null, time = "18:00:00")
        )
        assertEquals(
            ReminderDelayResult.InvalidDateTime,
            RideReminderTimeCalculator.computeDelay(date = "not-a-date", time = "18:00:00")
        )
    }
}
