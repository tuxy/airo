package com.tuxy.airo.data.flightdata

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.util.concurrent.TimeUnit
import org.openapitools.client.models.ErrorContract
import org.openapitools.client.models.FlightContract
import org.openapitools.client.models.FlightDirection
import org.openapitools.client.models.FlightSearchByEnum

// A few helper enums & classes
enum class RequestMethod { // Simple enum for now
    GET,
    POST
}

sealed interface Outcome
data class Success(val result: List<FlightContract?>) : Outcome
data class Error(val result: ErrorContract) : Outcome
data class CaughtException(val exception: Exception) : Outcome

/**
 * Selects the most complete [FlightContract] from an API response.
 *
 * The API can return multiple entries for a single flight number, and incomplete
 * "stub" entries (e.g. only a city name with no IATA/ICAO
 * code, no location and no scheduled time) may appear before the fully populated
 * entry. Blindly taking index 0 then yields a flight with no airport code, no
 * country flag and default map coordinates.
 *
 * Contracts are ranked by how much of the data the app relies on is present, and
 * the highest-scoring non-null contract is returned (ties fall back to the first).
 */
fun List<FlightContract?>.bestFlight(): FlightContract? =
    mapNotNull { it }.maxByOrNull { it.completenessScore() }

private fun FlightContract.completenessScore(): Int {
    var score = 0
    if (departure.airport.iata != null) score += 4
    if (arrival.airport.iata != null) score += 4
    if (departure.airport.countryCode != null) score += 2
    if (arrival.airport.countryCode != null) score += 2
    if (departure.airport.location != null) score += 2
    if (arrival.airport.location != null) score += 2
    if (departure.scheduledTime != null) score += 2
    if (arrival.scheduledTime != null) score += 2
    return score
}


class FlightDataRequest(
    val baseUrl: String = "https://airoapi.tuxy.stream/",
    val key: String? = null,
) {
    fun getFlightOnSpecificDate(
        searchParam: String,
        dateLocal: String,
        searchBy: FlightSearchByEnum,
        dateLocalRole: FlightDirection,
        withAircraftImage: Boolean = true,
        withLocation: Boolean = false,
        withFlightPlan: Boolean = false
    ): Outcome { // GetFlight_FlightOnSpecificDate

        val request = ApiClient(
            baseUrl = this.baseUrl,
            path = "flights/$searchBy/$searchParam/$dateLocal",
            method = RequestMethod.GET,
            headers = if (this.key != null) mapOf("x-magicapi-key" to this.key) else emptyMap(),
            params = mapOf(
                "dateLocalRole" to dateLocalRole.toString().lowercase(),
                "withAircraftImage" to withAircraftImage.toString(),
                "withLocation" to withLocation.toString(),
                "withFlightPlan" to withFlightPlan.toString()
            )
        )

        try {
            val result = request.execute()

            if (!result.isNullOrEmpty()) {
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

                /*
                2 different attempts to fit, due to 2 possible responses:
                    - 1. Could return a flight contract, in which case it's perfect
                    - 2. Could return an error message from 400 status codes (Returns just a message)
                    - 3. TODO Maybe they all fail, in which case the context provided will make a toast message
                 */
                runCatching {
                    val listType =
                        Types.newParameterizedType(List::class.java, FlightContract::class.java)
                    val jsonAdapter: JsonAdapter<List<FlightContract?>> = moshi.adapter(listType)
                    return Success(jsonAdapter.fromJson(result)!!)
                }.getOrNull() ?: runCatching {
                    val jsonAdapter = moshi.adapter(ErrorContract::class.java)
                    return Error(jsonAdapter.fromJson(result)!!)
                }.getOrNull()
            }


        } catch (e: Exception) {
            return CaughtException(e)
        }
        return CaughtException(IOException("Unknown Error"))
    }
}


class ApiClient(
    val baseUrl: String,
    val path: String,
    val method: RequestMethod,
    val headers: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap(),
) {
    /** Shared OkHttp client with sensible timeouts. */
    companion object {
        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun execute(): String? { // synchronously fetches the data
        when (method) {
            RequestMethod.GET -> {
                val concatUrl = baseUrl + path
                val url = concatUrl.toHttpUrlOrNull() // Checks for valid base url
                    ?: throw InvalidApiUrlException("Invalid base URL")

                val urlBuilder = url.newBuilder()

                params.forEach { (k, v) -> // Add url params
                    urlBuilder.addQueryParameter(k, v)
                }

                val request = Request.Builder()
                    .url(urlBuilder.build())
                    .headers(headers.toHeaders()) // Adds headers
                    .build()

                val res =
                    try {
                        httpClient.newCall(request).execute()
                    } catch (e: IOException) {
                        throw e
                    }
                if (!res.isSuccessful) throw UnexpectedResponseException("Unexpected response $res")
                return res.body?.string() // Completes the request and returns the raw string data
            }

            RequestMethod.POST -> {
                throw NotImplementedError("POST Method Not yet implemented")
            } // Not yet implemented
        }
    }
}