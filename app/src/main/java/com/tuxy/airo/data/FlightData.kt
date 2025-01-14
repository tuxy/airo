package com.tuxy.airo.data

data class FlightData(
    val from: String,
    val to: String,
    val ticket: Ticket,
    val aircraft: Aircraft,
    val map: MapData,
    val progress: Int,
)

data class MapData(
    val origin: String,
    val destination: String,
)

data class Aircraft(
    val icao: String,
    val full_name: String,
    val image_uri: String,
)

data class Ticket( // TODO scan an actual ticket and see what data lies in there...
    val seat: String,
    val data: String,
    val qr: String,
)
