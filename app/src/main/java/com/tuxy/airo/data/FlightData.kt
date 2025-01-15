package com.tuxy.airo.data

data class FlightData(
    val from: String,
    val to: String,
    val ticketSeat: String,
    val ticketData: String,
    val ticketQr: String,
    val aircraftIcao: String,
    val aircraftName: String,
    val aircraftUri: String,
    val mapOrigin: String,
    val mapDestination: String,
    val progress: Int,
)
