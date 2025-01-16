package com.tuxy.airo.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class Response( // The full response, which is a JSON array
    val departure: Flight,
    val arrival: Flight,
    val aircraft: Aircraft,
)

data class Flight( // The actual flight object, either as "departure" or "arrival" in JSON response
    val airport: Airport,
    val terminal: String,
    val scheduledTime: Time,
)

data class Airport( // ICAO code of the airport as well as it's location
    val icao: String,
    val location: Location,
)

data class Aircraft( // TODO The header may also show an image, which should be handled and cached
    val model: String,
)

data class Location( // Represents the latitude and longitude of airport as a float
    val lat: Float,
    val long: Float
)

data class Time( // Represents utc and local time for flight
    val utc: String,
    val local: String,
)

suspend fun getData(flightNumber: String) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.magicapi.dev/api/v1/aedbx/aerodatabox/flights/number/${flightNumber}")
            .header("Accept", "application/json")
            .header("x-magicapi-key", "") // API Key here
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            Log.d("APIACCESS", response.body!!.string())
        }
    }
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