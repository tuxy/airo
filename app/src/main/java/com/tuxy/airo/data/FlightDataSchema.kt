package com.tuxy.airo.data

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon

private val klaxon = Klaxon()

class Root(elements: Collection<RootElement>) : ArrayList<RootElement>(elements) {
    companion object {
        fun fromJson(json: String) = Root(
            klaxon.parseArray<RootElement>(json)!!
        )
    }
}

data class RootElement(
    val greatCircleDistance: GreatCircleDistance?,
    val departure: Flight,
    val arrival: Flight,

    @Json(name = "lastUpdatedUtc")
    val lastUpdatedUTC: String,
    val number: String,
    val status: String,
    val codeshareStatus: String,
    val isCargo: Boolean,
    val aircraft: Aircraft?,
    val airline: Airline?,
    // TODO add live flight location, and then ignore it for error handling
)

data class Aircraft(
    val model: String? = "N/A",
    val image: Image?
)

data class Image(
    val url: String = "",
    val webUrl: String = "",
    val author: String = "N/A",
    val title: String = "N/A",
    val description: String = "N/A",
    val license: String = "N/A",
    val htmlAttributions: List<String> = listOf(),
)

data class Airline(
    val name: String = "N/A",
    val iata: String? = "N/A",
    val icao: String? = "N/A"
)

data class Flight(
    val airport: Airport,
    val scheduledTime: EdTime? = EdTime("", ""),
    val predictedTime: EdTime? = EdTime("", ""),
    val revisedTime: EdTime? = EdTime("", ""),
    val terminal: String? = "N/A",
    val gate: String? = "N/A",
    val quality: List<String>
)

data class Airport(
    val icao: String?,
    val iata: String?,
    val name: String,
    val shortName: String?,
    val municipalityName: String?,
    val location: Location?,
    val countryCode: String?,
    val timeZone: String?
)

data class Location(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

data class EdTime( // Error handling?
    val utc: String = "2000-01-01 00:00",
    val local: String = "2000-01-01 00:00"
)

data class GreatCircleDistance(
    val meter: Double,
    val km: Double,
    val mile: Double,
    val nm: Double,
    val feet: Double
)
