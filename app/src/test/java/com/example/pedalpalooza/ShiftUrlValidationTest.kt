package com.example.pedalpalooza

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftUrlValidationTest {

    @Test
    fun `allows shift2bikes apex and subdomains over http or https`() {
        assertTrue(isInternalShiftUrl("https://www.shift2bikes.org/calendar/"))
        assertTrue(isInternalShiftUrl("https://shift2bikes.org/calendar/event-123"))
        assertTrue(isInternalShiftUrl("http://cal.shift2bikes.org/rides"))
    }

    @Test
    fun `rejects spoofed hosts and unsafe schemes`() {
        assertFalse(isInternalShiftUrl("https://evil.com/?q=shift2bikes.org"))
        assertFalse(isInternalShiftUrl("https://shift2bikes.org.evil.com/calendar/"))
        assertFalse(isInternalShiftUrl("https://notshift2bikes.org/calendar/"))
        assertFalse(isInternalShiftUrl("file:///etc/passwd?shift2bikes.org"))
        assertFalse(isInternalShiftUrl("javascript:alert('shift2bikes.org')"))
        assertFalse(isInternalShiftUrl(null))
        assertFalse(isInternalShiftUrl(""))
    }
}
