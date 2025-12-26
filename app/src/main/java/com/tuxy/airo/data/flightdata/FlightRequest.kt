package com.tuxy.airo.data.flightdata

import android.content.Context
import android.util.Log
import com.beust.klaxon.KlaxonException
import com.tuxy.airo.screens.ApiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sin

sealed class FlightDataError {
    object ApiKeyMissing : FlightDataError()
    object NetworkError : FlightDataError()
    object ParsingError : FlightDataError()
    object IncompleteDataError : FlightDataError() // Added for more specific error
    object FlightAlreadyExists : FlightDataError()
    object UnknownError : FlightDataError()
    object UpdateError : FlightDataError()
    object FlightNotFoundError : FlightDataError()
}

// Custom exception for critical data missing during parsing
class MissingCriticalDataException(message: String) : Exception(message)

private val sharedOkHttpClient = OkHttpClient()

/**
 * Builds an OkHttp Request object for fetching flight data from the API.
 *
 * This function constructs the API request URL based on the provided flight number, date, and API settings.
 * It supports different API endpoint configurations:
 * - "0": Uses the default Airo API endpoint.
 * - "1": Uses a custom server URL provided in `settings.server` and requires an API key in `settings.key`.
 * - "2": Uses a custom endpoint URL provided in `settings.endpoint`.
 *
 * It also adds a query parameter `withAircraftImage=True` to request aircraft images and sets the
 * "Accept" header to "application/json". If a custom server is used (choice "1"), it includes
 * the API key in the "x-magicapi-key" header.
 *
 * @param flightNumber The flight number (e.g., "VJ 84").
 * @param date The date of the flight in "YYYY-MM-DD" format.
 * @param settings An [ApiSettings] object containing the API endpoint configuration and key.
 * @return An OkHttp [Request] object if the configuration is valid and all required information is present,
 *         otherwise returns `null`. Returns `null` if:
 *         - The `settings.choice` is invalid or results in an empty URL.
 *         - `settings.choice` is "1" (custom server) but `settings.key` is null or empty.
 */
private fun buildFlightApiRequest(
    flightNumber: String,
    date: String,
    settings: ApiSettings
): Request? {
    val urlChoice = when (settings.choice) {
        "" -> "https://airoapi.tuxy.stream/flights" // When datastore hasn't initialised (user hasn't picked)
        "0" -> "https://airoapi.tuxy.stream/flights"
        "1" -> settings.server
        "2" -> settings.adbEndpoint
        else -> null
    }

    if (urlChoice.isNullOrEmpty()) {
        return null // Invalid choice or empty server/endpoint
    }

    if (settings.choice == "1" && settings.adbKey.isNullOrEmpty()) {
        return null // API key required for custom endpoint but missing
    }

    val url = "${urlChoice}/${flightNumber}/${date}".toHttpUrl()
        .newBuilder()
        .addQueryParameter("withAircraftImage", "True")
        .build()

    return Request.Builder()
        .url(url)
        .header("Accept", "application/json")
        .also {
            if (settings.choice == "1") { // Only add API key if custom endpoint is chosen
                it.header("x-magicapi-key", settings.adbKey.orEmpty())
            }
        }
        .build()
}

/**
 * Fetches flight data from an API, parses it, and optionally saves it to a local database.
 *
 * This function handles the entire process of retrieving flight information:
 * 1. Constructs an API request using [buildFlightApiRequest].
 * 2. Executes the network call using a shared OkHttpClient.
 * 3. Parses the JSON response.
 * 4. Transforms the parsed JSON into a [FlightData] object using [parseData].
 * 5. If `update` is false:
 *    - Checks if the flight already exists in the local database using [FlightDataDao.queryExisting].
 *    - If it doesn't exist, adds the new [FlightData] to the database using [FlightDataDao.addFlight].
 *    - Sets an alarm for the flight using [setAlarm].
 * 6. Returns a [Result] object containing either the successfully fetched/processed [FlightData]
 *    or a [FlightDataFetchException] wrapping a [FlightDataError] if any step fails.
 *
 * The function operates on the [Dispatchers.IO] coroutine dispatcher for network and database operations.
 *
 * @param flightNumber The flight number (e.g., "VJ 84") to fetch data for.
 * @param flightDataDao The Data Access Object for interacting with the flight data database.
 * @param date The date of the flight in "YYYY-MM-DD" format.
 * @param settings An [ApiSettings] object containing API configuration (endpoint, key).
 * @param context The Android [Context] used for operations like setting alarms.
 * @param update A boolean flag indicating whether this is an update to existing flight data.
 *               If `true`, the flight existence check and database insertion are skipped.
 * @return A [Result<FlightData>] which is:
 *         - [Result.Success] containing the [FlightData] on successful fetch and parse.
 *         - [Result.Failure] containing a [FlightDataFetchException] with a specific [FlightDataError]
 *           in case of any failure (e.g., API key missing, network error, parsing error,
 *           incomplete data, flight already exists, or unknown error).
 * @throws FlightDataFetchException for various errors encountered during the process.
 * @see buildFlightApiRequest
 */
suspend fun getData(
    flightNumber: String,
    flightDataDao: FlightDataDao,
    date: String,
    settings: ApiSettings,
    context: Context,
    update: Boolean
): Result<FlightData> { // Changed return type
    return withContext(Dispatchers.IO) {

        val request = buildFlightApiRequest(flightNumber, date, settings)
            ?: return@withContext Result.failure(FlightDataFetchException(FlightDataError.ApiKeyMissing))

        try {
            sharedOkHttpClient.newCall(request).execute().use { response ->

                val body = response.body!!.string()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(FlightDataFetchException(FlightDataError.NetworkError))
                }

                if (body == "{\"message\":\"Could not parse server response\"}") {
                    return@withContext Result.failure(FlightDataFetchException(FlightDataError.FlightNotFoundError))
                }

                try {
                    // Root.fromJson returns Root?, handle null if jsonListResponse is empty or invalid
                    val jsonRoot =
                        Root.fromJson(body) ?: return@withContext Result.failure(
                            FlightDataFetchException(FlightDataError.ParsingError)
                        )

                    val flightData =
                        parseData(jsonRoot) // parseData can throw MissingCriticalDataException


                    if (!update) {
                        if (flightDataDao.queryExisting(
                                flightData.departDate,
                                flightData.callSign
                            ) > 0
                        ) {
                            return@withContext Result.failure(
                                FlightDataFetchException(
                                    FlightDataError.FlightAlreadyExists
                                )
                            )
                        }
                        flightDataDao.addFlight(flightData) // Not sure why this entire thing would both add a flight and then return it, but sure...
                    }

                    return@withContext Result.success(flightData)
                } catch (e: KlaxonException) {
                    e.printStackTrace()
                    return@withContext Result.failure(FlightDataFetchException(FlightDataError.ParsingError))
                } catch (e: MissingCriticalDataException) {
                    e.printStackTrace()
                    Log.e("FlightRequest", "Missing critical data: ${e.message}")
                    return@withContext Result.failure(FlightDataFetchException(FlightDataError.IncompleteDataError))
                }
            }
        } catch (e: Exception) {
            // Catch any other exceptions during the network call or processing
            e.printStackTrace()
            // Consider specific exception types if needed, e.g. IOException for network issues
            // For now, all other exceptions map to UnknownError
            return@withContext Result.failure(FlightDataFetchException(FlightDataError.UnknownError))
        }
    }
}


fun parseData(jsonRoot: Root): FlightData {
    // Root.fromJson can return null if parsing fails (e.g. empty or malformed JSON)
    // However, getData already calls Root.fromJson and would likely fail earlier if jsonRoot is null.
    // For safety, if jsonRoot can be empty (though API implies it's an array with one element):
    val flightInfo =
        jsonRoot.firstOrNull() ?: throw MissingCriticalDataException("JSON root is null or empty")

    // Critical data checks using safe navigation due to schema changes
    val callSign =
        flightInfo.number ?: throw MissingCriticalDataException("Call sign (number) is missing")

    val departureFlight = flightInfo.departure
        ?: throw MissingCriticalDataException("Departure information is missing")
    val arrivalFlight =
        flightInfo.arrival ?: throw MissingCriticalDataException("Arrival information is missing")

    val departureAirport = departureFlight.airport
        ?: throw MissingCriticalDataException("Departure airport data is missing")
    val arrivalAirport = arrivalFlight.airport
        ?: throw MissingCriticalDataException("Arrival airport data is missing")

    val departureAirportIata = departureAirport.iata
        ?: throw MissingCriticalDataException("Departure airport IATA code is missing")
    val arrivalAirportIata = arrivalAirport.iata
        ?: throw MissingCriticalDataException("Arrival airport IATA code is missing")
    val fromCountryCode = departureAirport.countryCode
        ?: throw MissingCriticalDataException("From country code missing")
    val toCountryCode =
        arrivalAirport.countryCode ?: throw MissingCriticalDataException("To country code missing")
    val departureAirportShortName = departureAirport.shortName
        ?: throw MissingCriticalDataException("Departure airport short name is missing")
    val arrivalAirportShortName = arrivalAirport.shortName
        ?: throw MissingCriticalDataException("Arrival airport short name is missing")

    val departureScheduledTime = departureFlight.scheduledTime
        ?: throw MissingCriticalDataException("Departure scheduled time data is missing")
    val arrivalScheduledTime = arrivalFlight.scheduledTime
        ?: throw MissingCriticalDataException("Arrival scheduled time data is missing")

    val departureTimeZone = departureAirport.timeZone
        ?: throw MissingCriticalDataException("Departure timezone is missing")
    val arrivalTimeZone = arrivalAirport.timeZone
        ?: throw MissingCriticalDataException("Arrival time zone is missing")

    val departureLocation = departureAirport.location
        ?: throw MissingCriticalDataException("Departure airport location data is missing")
    val arrivalLocation = arrivalAirport.location
        ?: throw MissingCriticalDataException("Arrival airport location data is missing")

    val departureLat = departureLocation.lat
        ?: throw MissingCriticalDataException("Departure airport latitude is missing")
    val departureLon = departureLocation.lon
        ?: throw MissingCriticalDataException("Departure airport longitude is missing")
    val arrivalLat = arrivalLocation.lat
        ?: throw MissingCriticalDataException("Arrival airport latitude is missing")
    val arrivalLon = arrivalLocation.lon
        ?: throw MissingCriticalDataException("Arrival airport longitude is missing")

    val departureTime = parseDateTime(departureScheduledTime.utc)
    val arrivalTime = parseDateTime(arrivalScheduledTime.utc)

    // Normalised coordinates for origin airport
    val (projectedXOrigin, projectedYOrigin) = MapProjectionUtils.doProjection(
        departureLat,
        departureLon
    )
    // doProjection now throws, so null check removed as per previous step, but the prompt implies it might return null.
    // Re-checking doProjection: it returns Pair<Double, Double> and throws on error.
    // So, no need for `?: throw MissingCriticalDataException("Failed to project origin coordinates")` here.
    val xOrigin = MapProjectionUtils.normalize(
        projectedXOrigin,
        min = MapProjectionUtils.X0,
        max = -MapProjectionUtils.X0
    )
    val yOrigin = MapProjectionUtils.normalize(
        projectedYOrigin,
        min = -MapProjectionUtils.X0,
        max = MapProjectionUtils.X0
    )

    // Normalised coordinates for destination airport
    val (projectedXDest, projectedYDest) = MapProjectionUtils.doProjection(arrivalLat, arrivalLon)
    val xDest = MapProjectionUtils.normalize(
        projectedXDest,
        min = MapProjectionUtils.X0,
        max = -MapProjectionUtils.X0
    )
    val yDest = MapProjectionUtils.normalize(
        projectedYDest,
        min = -MapProjectionUtils.X0,
        max = MapProjectionUtils.X0
    )

    // Non-critical fields with defaults
    val airlineName = flightInfo.airline.orEmpty().name.ifNullOrEmptyLog("airlineName", "N/A")
    val airlineIcao = flightInfo.airline.orEmpty().icao.ifNullOrEmptyLog("airlineIcao", "N/A")
    val airlineIata = flightInfo.airline.orEmpty().iata.ifNullOrEmptyLog("airlineIata", "N/A")
    val gate = flightInfo.departure.gate.ifNullOrEmptyLog("gate", "—")
    val terminal = flightInfo.departure.terminal.ifNullOrEmptyLog("terminal", "—")
    val aircraftModel = flightInfo.aircraft.orEmpty().model.ifNullOrEmptyLog("aircraftModel", "N/A")
    val aircraftImageUrl = flightInfo.aircraft.orEmpty().image.orEmpty().url
    val imageAuthor = flightInfo.aircraft.orEmpty().image.orEmpty().author
    val imageAuthorUrl = flightInfo.aircraft.orEmpty().image.orEmpty().webUrl
    val attribution =
        flightInfo.aircraft.orEmpty().image.orEmpty().htmlAttributions.firstOrNull() ?: ""

    val toGate = flightInfo.arrival.gate.orEmpty().ifNullOrEmptyLog("toGate", "—")
    val toTerminal = flightInfo.arrival.terminal.orEmpty().ifNullOrEmptyLog("toTerminal", "—")
    val toBaggageClaim =
        flightInfo.arrival.baggageBelt.orEmpty().ifNullOrEmptyLog("toBaggageClaim", "—")
    val checkInDesk =
        flightInfo.departure.checkInDesk.orEmpty().ifNullOrEmptyLog("checkInDesk", "—")

    return FlightData(
        id = 0, // Auto-assigned id
        lastUpdate = LocalDateTime.now(),
        callSign = callSign,
        airline = airlineName,
        airlineIcao = airlineIcao,
        airlineIata = airlineIata,
        from = departureAirportIata,
        to = arrivalAirportIata,
        fromName = departureAirportShortName,
        toName = arrivalAirportShortName,
        fromCountryCode = fromCountryCode,
        toCountryCode = toCountryCode,
        departDate = departureTime,
        arriveDate = arrivalTime,
        departTimeZone = ZoneId.of(departureTimeZone),
        arriveTimeZone = ZoneId.of(arrivalTimeZone),
        duration = Duration.between(
            departureTime,
            arrivalTime
        ),
        gate = gate,
        terminal = terminal,
        toGate = toGate,
        toTerminal = toTerminal,
        toBaggageClaim = toBaggageClaim,
        checkInDesk = checkInDesk,
        aircraftName = aircraftModel,
        aircraftUri = aircraftImageUrl,
        author = imageAuthor,
        authorUri = imageAuthorUrl,
        mapOriginX = xOrigin,
        mapOriginY = yOrigin,
        mapDestinationX = xDest,
        mapDestinationY = yDest,
        attribution = attribution
    )
}

// Helper function for logging and providing default for non-critical nullable strings
private fun String?.ifNullOrEmptyLog(fieldName: String, default: String): String {
    if (this.isNullOrEmpty()) {
        Log.w("FlightRequest", "Missing non-critical field: $fieldName, using default '$default'.")
        return default
    }
    return this
}


fun Airline?.orEmpty(): Airline {
    return this ?: Airline()
}

fun Aircraft?.orEmpty(): Aircraft {
    return this ?: Aircraft()
}

fun Image?.orEmpty(): Image {
    return this ?: Image()
}

// Moved X0, doProjection, and normalize into this object
internal object MapProjectionUtils {
    internal const val X0 = -2.0037508342789248E7 // Constant for map projection

    internal fun doProjection(
        latitude: Double,
        longitude: Double
    ): Pair<Double, Double> { // Return non-nullable, throw if invalid
        if (abs(latitude) > 90 || abs(longitude) > 180) {
            throw MissingCriticalDataException("Invalid latitude or longitude for projection: lat=$latitude, lon=$longitude")
        }
        val num = longitude * 0.017453292519943295 // 2*pi / 360
        val x = 6378137.0 * num
        val a = latitude * 0.017453292519943295
        val y = 3189068.5 * ln((1.0 + sin(a)) / (1.0 - sin(a)))

        return Pair(x, y)
    }

    internal fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }
}

fun parseDateTime(time: String): LocalDateTime {
    val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmXXXXX")
    return LocalDateTime.parse(time, pattern)
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
