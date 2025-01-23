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
    val greatCircleDistance: GreatCircleDistance,
    val departure: Flight,
    val arrival: Flight,

    @Json(name = "lastUpdatedUtc")
    val lastUpdatedUTC: String,
    val number: String,
    val status: String,
    val codeshareStatus: String,
    val isCargo: Boolean,
    val aircraft: Aircraft,
    val airline: Airline
)

data class Aircraft(
    val model: String,
    val image: Image
)

data class Image(
    val url: String,
    val webUrl: String,
    val author: String,
    val title: String,
    val description: String,
    val license: String,
    val htmlAttributions: List<String>
)

data class Airline(
    val name: String,
    val iata: String,
    val icao: String
)

data class Flight(
    val airport: Airport,
    val scheduledTime: EdTime = EdTime("", ""),
    val predictedTime: EdTime = EdTime("", ""),
    val revisedTime: EdTime = EdTime("", ""),
    val terminal: String = "N/A",
    val gate: String = "N/A",
    val quality: List<String>
)

data class Airport(
    val icao: String,
    val iata: String,
    val name: String,
    val shortName: String,
    val municipalityName: String,
    val location: Location,
    val countryCode: String,
    val timeZone: String
)

data class Location(
    val lat: Double,
    val lon: Double
)

data class EdTime(
    val utc: String,
    val local: String
)

data class GreatCircleDistance(
    val meter: Double,
    val km: Double,
    val mile: Double,
    val nm: Double,
    val feet: Double
)
