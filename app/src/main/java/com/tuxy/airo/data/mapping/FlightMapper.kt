package com.tuxy.airo.data.mapping

import com.tuxy.airo.data.flightdata.FlightData
import org.openapitools.client.models.FlightContract
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Pure mapping from the generated AeroDataBox contract to the persisted entity.
 *
 * Moving this out of the entity keeps the data layer testable and makes the
 * "the API omitted a field" cases explicit instead of hiding them in a
 * constructor. Live-status fields are mapped here rather than discarded.
 */
object FlightMapper {

    private val LEGACY_SPACE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmXXXXX")

    fun from(
        contract: FlightContract,
        clock: Clock = Clock.systemDefaultZone(),
    ): FlightData {
        val departureZone = zoneId(contract.departure.airport.timeZone)
        val arrivalZone = zoneId(contract.arrival.airport.timeZone)

        val scheduledDepart = parseDateTime(
            contract.departure.scheduledTime?.local,
            departureZone,
        ) ?: parseDateTime(contract.departure.scheduledTime?.utc, null)
            ?: ZonedDateTime.now(clock.withZone(ZoneOffset.UTC))

        val scheduledArrive = parseDateTime(
            contract.arrival.scheduledTime?.local,
            arrivalZone,
        ) ?: parseDateTime(contract.arrival.scheduledTime?.utc, null)
            ?: scheduledDepart.plusHours(1)

        val revisedDepart = parseDateTime(
            contract.departure.revisedTime?.local,
            departureZone,
        ) ?: parseDateTime(contract.departure.revisedTime?.utc, null)

        val revisedArrive = parseDateTime(
            contract.arrival.revisedTime?.local,
            arrivalZone,
        ) ?: parseDateTime(contract.arrival.revisedTime?.utc, null)

        val effectiveDepart = revisedDepart ?: scheduledDepart
        val effectiveArrive = revisedArrive ?: scheduledArrive
        val duration = Duration.between(effectiveDepart, effectiveArrive)
            .takeUnless { it.isZero || it.isNegative }
            ?: Duration.ofSeconds(1)

        val departureLocation = contract.departure.airport.location
        val arrivalLocation = contract.arrival.airport.location
        val hasLocation = departureLocation != null && arrivalLocation != null

        return FlightData(
            lastUpdate = LocalDateTime.now(clock),
            callSign = contract.number,

            airline = contract.airline?.name ?: "N/A",
            airlineIcao = contract.airline?.icao ?: "N/A",
            airlineIata = contract.airline?.iata ?: "N/A",

            from = contract.departure.airport.iata ?: "N/A",
            fromName = contract.departure.airport.shortName
                ?: contract.departure.airport.name,
            fromCountryCode = contract.departure.airport.countryCode ?: "---",
            fromTimeZoneId = contract.departure.airport.timeZone,
            scheduledDepartDate = scheduledDepart,
            revisedDepartDate = revisedDepart,

            to = contract.arrival.airport.iata ?: "N/A",
            toName = contract.arrival.airport.shortName ?: contract.arrival.airport.name,
            toCountryCode = contract.arrival.airport.countryCode ?: "---",
            toTimeZoneId = contract.arrival.airport.timeZone,
            scheduledArriveDate = scheduledArrive,
            revisedArriveDate = revisedArrive,

            gate = contract.departure.gate ?: "—",
            terminal = contract.departure.terminal ?: "—",
            checkInDesk = contract.departure.checkInDesk ?: "—",
            toGate = contract.arrival.gate ?: "—",
            toTerminal = contract.arrival.terminal ?: "—",
            toBaggageClaim = contract.arrival.baggageBelt ?: "—",

            aircraftName = contract.aircraft?.model ?: "N/A",
            aircraftUri = contract.aircraft?.image?.url ?: "",
            aircraftReg = contract.aircraft?.reg,
            author = contract.aircraft?.image?.author ?: "",
            authorUri = contract.aircraft?.image?.webUrl ?: "",
            attribution = contract.aircraft?.image?.htmlAttributions?.firstOrNull() ?: "",

            mapOriginLat = departureLocation?.lat?.toDouble()?.takeUnless { it.isNaN() } ?: 1.0,
            mapOriginLon = departureLocation?.lon?.toDouble()?.takeUnless { it.isNaN() } ?: 1.0,
            mapDestinationLat = arrivalLocation?.lat?.toDouble()?.takeUnless { it.isNaN() } ?: 1.0,
            mapDestinationLon = arrivalLocation?.lon?.toDouble()?.takeUnless { it.isNaN() } ?: 1.0,
            hasLocation = hasLocation,

            // Live / status data. Previously fetched (withLocation was already
            // true) and then thrown away.
            status = contract.status.value,
            lastUpdatedUtc = contract.lastUpdatedUtc,
            departureRunwayTime = parseMovementTime(
                contract.departure.runwayTime?.local,
                contract.departure.runwayTime?.utc,
                departureZone,
            ),
            arrivalRunwayTime = parseMovementTime(
                contract.arrival.runwayTime?.local,
                contract.arrival.runwayTime?.utc,
                arrivalZone,
            ),
            departurePredictedTime = parseMovementTime(
                contract.departure.predictedTime?.local,
                contract.departure.predictedTime?.utc,
                departureZone,
            ),
            arrivalPredictedTime = parseMovementTime(
                contract.arrival.predictedTime?.local,
                contract.arrival.predictedTime?.utc,
                arrivalZone,
            ),
            liveLat = contract.location?.lat?.toDouble()?.takeUnless { it.isNaN() },
            liveLon = contract.location?.lon?.toDouble()?.takeUnless { it.isNaN() },
            liveAltitudeM = contract.location?.altitude?.meter,
            liveGroundSpeedKmh = contract.location?.groundSpeed?.kmPerHour,
            liveTrackDeg = contract.location?.trueTrack?.deg,
            liveReportedAtUtc = contract.location?.reportedAtUtc,
            greatCircleDistanceKm = contract.greatCircleDistance?.km,

            duration = duration,
            ticketData = "",
        )
    }

    private fun parseMovementTime(
        local: String?,
        utc: String?,
        zone: ZoneId?,
    ): ZonedDateTime? =
        parseDateTime(local, zone) ?: parseDateTime(utc, null)

    /**
     * Tolerant parser for the API's slightly inconsistent date-time strings.
     * Accepts ISO-8601 with `T`, the legacy space-separated form, an optional
     * zone id, and zone-less timestamps interpreted in the airport's zone.
     */
    fun parseDateTime(value: String?, zoneHint: ZoneId? = null): ZonedDateTime? {
        if (value.isNullOrBlank()) return null
        val raw = value.trim()
        val withT = if (raw.contains(' ') && !raw.contains('T')) raw.replace(' ', 'T') else raw

        listOf(raw, withT).forEach { candidate ->
            runCatching {
                java.time.OffsetDateTime.parse(candidate, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toZonedDateTime()
            }.getOrNull()?.let { return it }

            runCatching {
                ZonedDateTime.parse(candidate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            }.getOrNull()?.let { return it }

            runCatching {
                ZonedDateTime.parse(candidate, LEGACY_SPACE_PATTERN)
            }.getOrNull()?.let { return it }

            runCatching {
                LocalDateTime.parse(candidate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(zoneHint ?: ZoneOffset.UTC)
            }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun zoneId(value: String?): ZoneId? =
        value?.takeIf { it.isNotBlank() }?.let {
            runCatching { ZoneId.of(it) }.getOrNull()
        }
}
