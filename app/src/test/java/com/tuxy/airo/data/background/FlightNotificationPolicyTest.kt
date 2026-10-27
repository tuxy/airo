package com.tuxy.airo.data.background

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightNotificationPolicyTest {

    @Test
    fun `zero updated flights never produces a refresh notification`() {
        assertFalse(FlightNotifications.shouldNotifyRefresh(0))
    }

    @Test
    fun `at least one updated flight produces a refresh notification`() {
        assertTrue(FlightNotifications.shouldNotifyRefresh(1))
        assertTrue(FlightNotifications.shouldNotifyRefresh(12))
    }
}
