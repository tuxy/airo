package com.tuxy.airo.data.repository

import com.tuxy.airo.data.flightdata.FlightChange
import com.tuxy.airo.data.flightdata.FlightData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import java.time.ZonedDateTime

class FlightChangeTest {

    private val repository = FlightRepository(
        database = mock(),
        dao = mock(),
        api = mock(),
        preferences = mock(),
        reminders = mock(),
    )

    private fun flight(
        id: Int = 1,
        status: String = "Expected",
        gate: String = "A1",
        updated: String = "old",
        depart: String = "2026-11-20T10:00:00Z",
        arrive: String = "2026-11-20T12:00:00Z",
    ) = FlightData(
        id = id,
        callSign = "TR39",
        status = status,
        lastUpdatedUtc = updated,
        gate = gate,
        scheduledDepartDate = ZonedDateTime.parse(depart),
        scheduledArriveDate = ZonedDateTime.parse(arrive),
    )

    @Test
    fun `combines all changes for one flight into one natural sentence`() {
        val old = flight()
        val new = flight(
            status = "EnRoute",
            gate = "B2",
            updated = "new",
            depart = "2026-11-20T11:00:00Z",
        )

        val change = requireNotNull(repository.detectChange(old, new))

        assertEquals("TR39 has changed", change.title)
        assertEquals(
            listOf(
                "status from Expected to En Route",
                "gate from A1 to B2",
                "departure time from 10:00 to 11:00",
            ),
            change.changes,
        )
        assertEquals(
            "status from Expected to En Route, gate from A1 to B2, " +
                "and departure time from 10:00 to 11:00",
            change.message,
        )
    }

    @Test
    fun `describes a date move as a date change`() {
        val old = flight()
        val new = flight(updated = "new", depart = "2026-11-21T10:00:00Z")

        val change = requireNotNull(repository.detectChange(old, new))

        assertEquals(listOf("departure date from 20 Nov to 21 Nov"), change.changes)
    }

    @Test
    fun `returns null when nothing user-visible changed`() {
        val old = flight()
        val new = flight(updated = "new")

        assertNull(repository.detectChange(old, new))
    }

    @Test
    fun `joins two and three fragments with a natural conjunction`() {
        assertEquals(
            "gate to B2, and departure date to 21 Nov",
            FlightChange(1, "TR39", listOf("gate to B2", "departure date to 21 Nov")).message,
        )
        assertEquals(
            "gate to B2, departure date to 21 Nov, and terminal to 2",
            FlightChange(
                1,
                "TR39",
                listOf("gate to B2", "departure date to 21 Nov", "terminal to 2"),
            ).message,
        )
    }
}
