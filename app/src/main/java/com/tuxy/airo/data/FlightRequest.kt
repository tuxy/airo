package com.tuxy.airo.data

import android.util.Log
import android.widget.Toast
import com.beust.klaxon.KlaxonException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

suspend fun getData(
    flightNumber: String,
    data: FlightDataDao,
    date: String,
    toasts: Array<Toast>
) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val apiKey = "" // API Key here
        val webUrl = "https://api.magicapi.dev/api/v1/aedbx/aerodatabox/flights/number/${flightNumber}/${date}"
        // TODO Implement DataStore for apikey + other settings

        val url = webUrl.toHttpUrl().newBuilder() // Adds parameter for aircraft image
            .addQueryParameter("withAircraftImage", "True")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("x-magicapi-key", apiKey)
            .build()

        if(apiKey == "") {
            toasts[0].show() // API_KEY toast
            return@withContext
        }

        client.newCall(request).execute().use { response ->
            if(!response.isSuccessful) {
                toasts[1].show() // Network Error toast
                return@use // Stops from executing further
            }
            val jsonListResponse = response.body!!.string()

            try {
                val jsonRoot = Root.fromJson(jsonListResponse)
                data.addFlight(parseData(jsonRoot)) // If this errors, we give up
            } catch (e: KlaxonException) {
                Log.e("APIACCESS", e.toString())
                toasts[2].show() // flight not found
                return@use // Stops from executing further
            }
        }
    }
}

fun parseData(jsonRoot: Root): FlightData {
    val departureTime = parseDateTime(jsonRoot[0].departure.scheduledTime.local)
    val arrivalTime = parseDateTime(jsonRoot[0].arrival.scheduledTime.local)
    return FlightData(
        id = 0, // Auto-assigned id
        callSign = jsonRoot[0].airline.iata,
        airline = jsonRoot[0].airline.name,
        airlineIata = jsonRoot[0].airline.iata,
        from = jsonRoot[0].departure.airport.iata,
        to = jsonRoot[0].arrival.airport.iata,
        fromName = jsonRoot[0].departure.airport.shortName,
        toName = jsonRoot[0].arrival.airport.shortName,
        localDepartDate = departureTime[0],
        localDepartTime = departureTime[1],
        localArriveDate = arrivalTime[0],
        localArriveTime = arrivalTime[1],
        ticketSeat = "N/A", // TODO
        ticketData = "N/A", // TODO
        ticketQr = "N/A", // TODO
        ticketGate = jsonRoot[0].departure.gate,
        ticketTerminal = jsonRoot[0].departure.terminal,
        aircraftIcao = "N/A", // TODO
        aircraftName = jsonRoot[0].aircraft.model,
        aircraftUri = jsonRoot[0].aircraft.image.url, // TODO
        author = jsonRoot[0].aircraft.image.author,
        authorUri = jsonRoot[0].aircraft.image.webUrl,
        mapOriginLat = jsonRoot[0].departure.airport.location.lat,
        mapOriginLong = jsonRoot[0].departure.airport.location.lon,
        mapDestinationLat = jsonRoot[0].arrival.airport.location.lat,
        mapDestinationLong = jsonRoot[0].arrival.airport.location.lon,
        progress = 50, // TODO
    )
}

fun parseDateTime(time: String): Array<String> { // TODO Date is the first element shown in format DD-MM-YYYY and time is shown in 24-hours (Locale default?)
    val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmXXXXX") // Ignore time-zone, as time is set to local by default
    val localDateTime = LocalDateTime.parse(time, pattern)

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return arrayOf(
        localDateTime.format(timeFormatter),
        localDateTime.format(dateFormatter)
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
