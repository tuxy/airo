package com.tuxy.airo.data

import android.content.Context
import android.widget.Toast
import com.beust.klaxon.KlaxonException
import com.tuxy.airo.screens.ApiSettings
import com.tuxy.airo.setAlarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sin

suspend fun getData(
    flightNumber: String,
    data: FlightDataDao,
    date: String,
    toasts: Array<Toast>,
    settings: ApiSettings,
    context: Context
) {
    withContext(Dispatchers.IO) {

        if (settings.endpoint.orEmpty() == "" && settings.server.orEmpty() == "") {
            toasts[0].show() // API_KEY toast
            return@withContext
        }

        val client = OkHttpClient()

        val urlChoice = if (settings.choice == "0") settings.server!! else settings.endpoint!!

        val url = "${urlChoice}/${flightNumber}/${date}".toHttpUrl()
            .newBuilder() // Adds parameter for aircraft image
            .addQueryParameter("withAircraftImage", "True")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("x-magicapi-key", settings.key.orEmpty())
            .build() // Either the api key exists or it doesn't

        if (settings.key.orEmpty() == "" && settings.choice == "1") {
            toasts[0].show() // API_KEY toast
            return@withContext
        } // If choice is == "0", then we ignore the api key check

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                toasts[1].show() // Network Error toast
                return@withContext // Stops from executing further
            }
            val jsonListResponse = response.body!!.string()

            try {
                val jsonRoot = Root.fromJson(jsonListResponse)
                val flightData = parseData(jsonRoot)
                // TODO Look through existing flights and compare without having to use api
                if (data.queryExisting(flightData.departDate, flightData.callSign) > 0) {
                    toasts[3].show() // flight already exists toast
                    return@withContext
                }
                data.addFlight(flightData) // If this errors, we give up
                setAlarm(context, flightData)
            } catch (e: KlaxonException) {
                e.printStackTrace()
                toasts[2].show() // flight not found
                return@withContext // Stops from executing further
            }
        }
    }
}


fun parseData(jsonRoot: Root): FlightData {
    val departureTime = parseDateTime(jsonRoot[0].departure.scheduledTime.orEmpty().local)
    val arrivalTime = parseDateTime(jsonRoot[0].arrival.scheduledTime.orEmpty().local)

    // Normalised coordinates for origin airport
    val (projectedXOrigin, projectedYOrigin) = doProjection(
        jsonRoot[0].departure.airport.location.orEmpty().lat,
        jsonRoot[0].departure.airport.location.orEmpty().lon
    )!!
    val xOrigin = normalize(
        projectedXOrigin,
        min = X0,
        max = -X0
    )
    val yOrigin = normalize(
        projectedYOrigin,
        min = -X0,
        max = X0
    )

    // Normalised coordinates for destination airport
    val (projectedXDest, projectedYDest) = doProjection(
        jsonRoot[0].arrival.airport.location.orEmpty().lat,
        jsonRoot[0].arrival.airport.location.orEmpty().lon
    )!!
    val xDest = normalize(
        projectedXDest,
        min = X0,
        max = -X0
    )
    val yDest = normalize(
        projectedYDest,
        min = -X0,
        max = X0
    )

    return FlightData(
        // TODO error handling for parsing
        id = 0, // Auto-assigned id
        callSign = jsonRoot[0].number,
        airline = jsonRoot[0].airline.orEmpty().name,
        airlineIcao = jsonRoot[0].airline.orEmpty().icao ?: "N/A",
        airlineIata = jsonRoot[0].airline.orEmpty().iata ?: "N/A",
        from = jsonRoot[0].departure.airport.iata ?: "N/A",
        to = jsonRoot[0].arrival.airport.iata ?: "N/A",
        fromName = jsonRoot[0].departure.airport.shortName ?: "N/A",
        toName = jsonRoot[0].arrival.airport.shortName ?: "N/A",
        departDate = departureTime,
        arriveDate = arrivalTime,
        duration = Duration.between(
            parseDateTime(jsonRoot[0].departure.scheduledTime.orEmpty().utc),
            parseDateTime(jsonRoot[0].arrival.scheduledTime.orEmpty().utc)
        ),
        // start of with no ticketData
        gate = jsonRoot[0].departure.gate ?: "N/A",
        terminal = jsonRoot[0].departure.terminal ?: "N/A",
        aircraftName = jsonRoot[0].aircraft.orEmpty().model ?: "N/A",
        aircraftUri = jsonRoot[0].aircraft.orEmpty().image.orEmpty().url,
        author = jsonRoot[0].aircraft.orEmpty().image.orEmpty().author,
        authorUri = jsonRoot[0].aircraft.orEmpty().image.orEmpty().webUrl,
        mapOriginX = xOrigin,
        mapOriginY = yOrigin,
        mapDestinationX = xDest,
        mapDestinationY = yDest,
        attribution = jsonRoot[0].aircraft.orEmpty().image.orEmpty().htmlAttributions[0]
    )
}

fun Airline?.orEmpty(): Airline {
    return this ?: Airline()
}

fun EdTime?.orEmpty(): EdTime {
    return this ?: EdTime()
}

fun Aircraft?.orEmpty(): Aircraft {
    return this ?: Aircraft(image = Image())
}

fun Image?.orEmpty(): Image {
    return this ?: Image()
}

fun Location?.orEmpty(): Location {
    return this ?: Location()
}

fun doProjection(latitude: Double, longitude: Double): Pair<Double, Double>? {
    if (abs(latitude) > 90 || abs(longitude) > 180) {
        return null
    }
    val num = longitude * 0.017453292519943295 // 2*pi / 360
    val x = 6378137.0 * num
    val a = latitude * 0.017453292519943295
    val y = 3189068.5 * ln((1.0 + sin(a)) / (1.0 - sin(a)))

    return Pair(x, y)
}

fun normalize(t: Double, min: Double, max: Double): Double {
    return (t - min) / (max - min)
}

private const val X0 = -2.0037508342789248E7 // Constant for map projection

fun parseDateTime(time: String): LocalDateTime {
    // TODO, utilise UTC time and convert into local time instead of relying on localtime
    val pattern =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmXXXXX")!!
    // val localDateTime = LocalDateTime.parse(time, pattern)
    val offsetDateTime = OffsetDateTime.parse(time, pattern)

    return offsetDateTime!!.toLocalDateTime()
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
