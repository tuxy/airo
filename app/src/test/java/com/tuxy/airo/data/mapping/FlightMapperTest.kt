package com.tuxy.airo.data.mapping

import com.tuxy.airo.data.FULL_FLIGHT_JSON
import com.tuxy.airo.data.network.FlightApi
import com.tuxy.airo.data.network.bestFlight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openapitools.client.models.FlightContract
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class FlightMapperTest {

    private val contract: FlightContract = requireNotNull(
        FlightApi.defaultMoshi.adapter(FlightContract::class.java).fromJson(FULL_FLIGHT_JSON)
    )

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `parses local time with offset`() {
        val parsed = FlightMapper.parseDateTime("2026-01-01 07:30-05:00")
        assertNotNull(parsed)
        assertEquals(7, parsed!!.hour)
        assertEquals(30, parsed.minute)
        assertEquals(-5 * 60, parsed.offset.totalSeconds / 60)
    }

    @Test
    fun `parses ISO offset and zone-less values`() {
        val iso = FlightMapper.parseDateTime("2026-01-01T12:30:00Z")
        assertEquals(Instant.parse("2026-01-01T12:30:00Z"), iso?.toInstant())

        val zoneLess = FlightMapper.parseDateTime(
            "2026-01-01 07:30",
            java.time.ZoneId.of("America/New_York"),
        )
        assertEquals(Instant.parse("2026-01-01T12:30:00Z"), zoneLess?.toInstant())
    }

    @Test
    fun `returns null for blank and unparseable values`() {
        assertNull(FlightMapper.parseDateTime(null))
        assertNull(FlightMapper.parseDateTime(""))
        assertNull(FlightMapper.parseDateTime("not a date"))
    }

    @Test
    fun `maps status, live position and effective duration`() {
        val flight = FlightMapper.from(contract, clock)

        assertEquals("EnRoute", flight.status)
        assertTrue(flight.hasKnownStatus)
        assertEquals("EWR", flight.from)
        assertEquals("LAX", flight.to)
        assertEquals("N12345", flight.aircraftReg)
        assertTrue(flight.hasLocation)
        assertTrue(flight.hasLivePosition)
        assertEquals(40.0, flight.liveLat!!, 0.001)
        assertEquals(-100.0, flight.liveLon!!, 0.001)
        assertEquals(10000.0, flight.liveAltitudeM!!, 0.001)
        assertEquals(900.0, flight.liveGroundSpeedKmh!!, 0.001)
        assertEquals(90.0, flight.liveTrackDeg!!, 0.001)
        assertEquals(1000.0, flight.greatCircleDistanceKm!!, 0.001)

        // Departure 07:30-05:00, arrival 09:30-08:00 => 5 hours.
        assertEquals(Duration.ofHours(5), flight.effectiveDuration)
        assertEquals(Instant.parse("2026-01-01T12:30:00Z"), flight.effectiveDepartDate.toInstant())
        assertEquals(Instant.parse("2026-01-01T17:30:00Z"), flight.effectiveArriveDate.toInstant())
    }

    @Test
    fun `bestFlight prefers the complete entry`() {
        val adapter = FlightApi.defaultMoshi.adapter<List<FlightContract?>>(
            com.squareup.moshi.Types.newParameterizedType(
                List::class.java,
                FlightContract::class.java,
            )
        )
        val list = adapter.fromJson(com.tuxy.airo.data.FLIGHT_LIST_JSON).orEmpty()
        assertEquals(2, list.size)
        assertEquals("EWR", list.bestFlight()?.departure?.airport?.iata)
    }

    @Test
    fun `mapper accepts an explicit date for the request`() {
        assertEquals(LocalDate.of(2026, 1, 1), contract.departure.scheduledTime?.let {
            FlightMapper.parseDateTime(it.local)?.toLocalDate()
        })
    }
}
