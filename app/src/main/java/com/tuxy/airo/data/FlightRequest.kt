package com.tuxy.airo.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Optional

suspend fun getData(
    flightNumber: String,
    data: FlightDataDao
) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()


        val request = Request.Builder()
            .url("https://api.magicapi.dev/api/v1/aedbx/aerodatabox/flights/number/${flightNumber}")
            .header("Accept", "application/json")
            .header("x-magicapi-key", "") // API Key here
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val jsonListResponse = response.body!!.string()

            val jsonRoot = Root.fromJson(jsonListResponse)
            data.addFlight(parseData(jsonRoot))
            Log.d("APIACCESS", jsonRoot.toString())
        }
    }
}

fun parseData(jsonRoot: Root): FlightData {
    return FlightData(
        id = 0, // Auto-assigned id
        from = jsonRoot[0].departure.airport.icao,
        to = jsonRoot[0].arrival.airport.icao,
        fromName = jsonRoot[0].departure.airport.shortName,
        toName = jsonRoot[0].arrival.airport.shortName,
        ticketSeat = "", // TODO
        ticketData = "", // TODO
        ticketQr = "", // TODO
        ticketGate = jsonRoot[0].departure.gate,
        ticketTerminal = jsonRoot[0].departure.terminal,
        aircraftIcao = "", // TODO
        aircraftName = jsonRoot[0].aircraft.model,
        aircraftUri = "", // TODO
        mapOriginLat = jsonRoot[0].departure.airport.location.lat,
        mapOriginLong = jsonRoot[0].departure.airport.location.lon,
        mapDestinationLat = jsonRoot[0].arrival.airport.location.lat,
        mapDestinationLong = jsonRoot[0].arrival.airport.location.lon,
        progress = 50, // TODO
    )
}

/* This is some sample json data for quick reference

[
  {
    "greatCircleDistance": {
      "meter": 6556365.47,
      "km": 6556.37,
      "mile": 4073.94,
      "nm": 3540.15,
      "feet": 21510385.4
    },
    "departure": {
      "airport": {
        "icao": "YBBN",
        "iata": "BNE",
        "name": "Brisbane City Brisbane",
        "shortName": "Brisbane",
        "municipalityName": "Brisbane City",
        "location": {
          "lat": -27.3842,
          "lon": 153.117
        },
        "countryCode": "AU",
        "timeZone": "Australia/Brisbane"
      },
      "scheduledTime": {
        "utc": "2025-01-16 13:40Z",
        "local": "2025-01-16 23:40+10:00"
      },
      "revisedTime": {
        "utc": "2025-01-16 13:40Z",
        "local": "2025-01-16 23:40+10:00"
      },
      "terminal": "I",
      "gate": "79",
      "quality": [
        "Basic",
        "Live"
      ]
    },
    "arrival": {
      "airport": {
        "icao": "VVTS",
        "iata": "SGN",
        "name": "Ho Chi Minh City Tan Son Nhat",
        "shortName": "Tan Son Nhat",
        "municipalityName": "Ho Chi Minh City",
        "location": {
          "lat": 10.818799,
          "lon": 106.652
        },
        "countryCode": "VN",
        "timeZone": "Asia/Ho_Chi_Minh"
      },
      "scheduledTime": {
        "utc": "2025-01-16 22:05Z",
        "local": "2025-01-17 05:05+07:00"
      },
      "predictedTime": {
        "utc": "2025-01-16 21:35Z",
        "local": "2025-01-17 04:35+07:00"
      },
      "terminal": "2",
      "quality": [
        "Basic"
      ]
    },
    "lastUpdatedUtc": "2025-01-14 14:45Z",
    "number": "VJ 84",
    "status": "Expected",
    "codeshareStatus": "IsOperator",
    "isCargo": false,
    "aircraft": {
      "model": "Airbus A330"
    },
    "airline": {
      "name": "VietJetAir",
      "iata": "VJ",
      "icao": "VJC"
    }
  }
]

 */
