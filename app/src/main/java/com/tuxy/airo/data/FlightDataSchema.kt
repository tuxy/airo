package com.tuxy.airo.data

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon

private val klaxon = Klaxon()

// Root type alias for the JSON array - using Klaxon's direct parsing to List<RootElement>
// typealias Root = ArrayList<RootElement> // Keep this if it's used elsewhere, or parse directly to List
// For consistency with `Root.fromJson` in `FlightRequest.kt`, we'll keep the Root class for now.
class Root(elements: Collection<RootElement>) : ArrayList<RootElement>(elements) {
    companion object {
        fun fromJson(json: String): Root? = klaxon.parseArray<RootElement>(json)?.let { Root(it) }
    }
}

data class RootElement(
    val greatCircleDistance: GreatCircleDistance?, // Optional section
    val departure: Flight?, // API seems to always provide departure/arrival, but safer as nullable
    val arrival: Flight?,   // API seems to always provide departure/arrival, but safer as nullable

    @Json(name = "lastUpdatedUtc")
    val lastUpdatedUTC: String?, // Optional
    val number: String?, // Critical, checked in parseData. Made nullable to allow parseData to catch.
    val status: String?, // Optional
    val codeshareStatus: String?, // Optional
    val isCargo: Boolean?, // Optional
    val aircraft: Aircraft?, // Optional section
    val airline: Airline?    // Optional section
    // TODO add live flight location, and then ignore it for error handling (already present)
)

data class Aircraft(
    val model: String? = "N/A", // Optional, handled by ifNullOrEmptyLog in parseData
    val image: Image? = Image() // Optional sub-section
)

data class Image(
    val url: String = "",
    val webUrl: String = "",
    val author: String = "N/A",
    val title: String = "N/A",
    val description: String = "N/A",
    val license: String = "N/A",
    val htmlAttributions: List<String> = emptyList<String>(),
)

data class Airline(
    val name: String = "N/A",
    val iata: String? = "N/A",
    val icao: String? = "N/A"
)

// Represents Departure or Arrival structures
data class Flight(
    val airport: Airport?, // Seems always present if Flight object is, but safer as nullable
    val scheduledTime: EdTime?, // Optional sub-section
    val predictedTime: EdTime?, // Optional sub-section (typically for arrival)
    val revisedTime: EdTime?,   // Optional sub-section
    val terminal: String?,      // Optional
    val gate: String?,          // Optional
    val quality: List<String>?  // Optional
)

data class Airport(
    val icao: String?, // Optional
    val iata: String?, // Critical, checked in parseData. Made nullable.
    val name: String?, // Optional
    val shortName: String?, // Critical, checked in parseData. Made nullable.
    val municipalityName: String?, // Optional
    val location: Location?, // Sub-fields lat/lon are critical. Made nullable itself.
    val countryCode: String?, // Optional
    @Json(name = "timeZone") // Matches sample JSON field name
    val timeZone: String?    // Optional
)

data class Location(
    val lat: Double? = 0.0, // Critical, checked in parseData. Made nullable.
    val lon: Double? = 0.0  // Critical, checked in parseData. Made nullable.
)

// Represents ScheduledTime, RevisedTime, PredictedTime structures
data class EdTime( // Error handling?
    val utc: String = "2000-01-01 00:00",
    val local: String = "2000-01-01 00:00"
)

data class GreatCircleDistance(
    val meter: Double?, // Optional
    val km: Double?,    // Optional
    val mile: Double?,   // Optional
    val nm: Double?,     // Optional
    val feet: Double?    // Optional
)
