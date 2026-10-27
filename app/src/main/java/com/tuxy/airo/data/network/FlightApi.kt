package com.tuxy.airo.data.network

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tuxy.airo.data.settings.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.openapitools.client.models.ErrorContract
import org.openapitools.client.models.FlightContract
import org.openapitools.client.models.FlightDirection
import org.openapitools.client.models.FlightSearchByEnum
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.min

/** Result of a network call, with enough detail for the UI to say what happened. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    /** Server answered with a non-2xx status. [message] is the server's message when available. */
    data class HttpError(val code: Int, val message: String) : ApiResult<Nothing>

    /** Transport failure (offline, timeout, DNS, TLS). */
    data class NetworkError(val message: String, val cause: Throwable? = null) : ApiResult<Nothing>

    /** The response arrived but could not be understood. */
    data class ParseError(val message: String, val cause: Throwable? = null) : ApiResult<Nothing>

    /** The user's API configuration is missing or invalid. */
    data class ConfigError(val message: String) : ApiResult<Nothing>
}

/**
 * Thin AeroDataBox client.
 *
 * The old code built the URL with string concatenation, created a reflective
 * Moshi per request, threw away every non-2xx response body (so callers could
 * only show "network error") and never closed the response. This class fixes
 * those and adds a small retry for transient failures. It stays dependency-free
 * (OkHttp + Moshi, both already used by the app).
 */
class FlightApi(
    private val configProvider: suspend () -> ApiConfig,
    private val client: OkHttpClient = defaultClient(),
    private val moshi: Moshi = defaultMoshi,
) {
    private val flightListAdapter: JsonAdapter<List<FlightContract?>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, FlightContract::class.java))
    private val errorAdapter: JsonAdapter<ErrorContract> = moshi.adapter(ErrorContract::class.java)

    suspend fun getFlight(
        flightNumber: String,
        date: LocalDate,
        withAircraftImage: Boolean = true,
        withLocation: Boolean = true,
    ): ApiResult<List<FlightContract>> = withContext(Dispatchers.IO) {
        val config = configProvider()
        if (!config.isConfigured) {
            return@withContext ApiResult.ConfigError(
                "No API server is configured. Add an endpoint in Settings → API."
            )
        }
        val url = config.baseUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addPathSegment("flights")
            // These path segments are case-sensitive on the server: "Number"
            // (not "number") is what AeroDataBox/AiroAPI routes on.
            ?.addPathSegment(FlightSearchByEnum.Number.value)
            ?.addPathSegment(formatFlightNumber(flightNumber))
            ?.addPathSegment(date.toString())
            ?.addQueryParameter("dateLocalRole", FlightDirection.Departure.value)
            ?.addQueryParameter("withAircraftImage", withAircraftImage.toString())
            ?.addQueryParameter("withLocation", withLocation.toString())
            // withFlightPlan can be billed as two requests and the app does not
            // render it, so it is deliberately not requested.
            ?.build()
            ?: return@withContext ApiResult.ConfigError(
                "The configured API endpoint is not a valid URL."
            )

        Log.d(TAG, "GET $url")

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .apply {
                config.apiKey?.let { header("x-magicapi-key", it) }
            }
            .build()

        var lastError: ApiResult<Nothing>? = null
        for (attempt in 0 until MAX_ATTEMPTS) {
            when (val outcome = executeOnce(request)) {
                is ExecOutcome.Ok -> {
                    if (outcome.code == 204 || outcome.body.isBlank()) {
                        return@withContext ApiResult.Success(emptyList())
                    }
                    return@withContext parseFlights(outcome.body, url.toString())
                }

                is ExecOutcome.Http -> {
                    Log.w(
                        TAG,
                        "HTTP ${outcome.code} from $url: ${outcome.body.take(300)}",
                    )
                    val apiError = ApiResult.HttpError(
                        code = outcome.code,
                        message = parseError(outcome.body) ?: defaultMessage(outcome.code),
                    )
                    lastError = apiError
                    if (!outcome.code.isRetryable() || attempt == MAX_ATTEMPTS - 1) {
                        return@withContext apiError
                    }
                }

                is ExecOutcome.Network -> {
                    Log.w(TAG, "Network error from $url", outcome.exception)
                    lastError = ApiResult.NetworkError(
                        message = outcome.exception.message ?: "Network request failed",
                        cause = outcome.exception,
                    )
                    if (attempt == MAX_ATTEMPTS - 1) {
                        return@withContext lastError
                    }
                }
            }
            delay(backoffMillis(attempt))
        }
        return@withContext lastError ?: ApiResult.NetworkError("Unknown network error")
    }

    private fun executeOnce(request: Request): ExecOutcome = try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful || response.code == 204) {
                ExecOutcome.Ok(response.code, body)
            } else {
                ExecOutcome.Http(response.code, body)
            }
        }
    } catch (e: IOException) {
        ExecOutcome.Network(e)
    }

    private fun parseFlights(
        body: String,
        url: String,
    ): ApiResult<List<FlightContract>> = runCatching {
        flightListAdapter.fromJson(body) ?: emptyList()
    }.fold(
        onSuccess = { ApiResult.Success(it.filterNotNull()) },
        onFailure = {
            Log.w(TAG, "Could not parse response from $url: ${body.take(300)}", it)
            ApiResult.ParseError(
                message = "The server returned an unexpected flight response.",
                cause = it,
            )
        },
    )

    private fun parseError(body: String): String? = runCatching {
        errorAdapter.fromJson(body)?.message?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun defaultMessage(code: Int): String = when (code) {
        401, 403 -> "The API key was rejected. Check Settings → API."
        404 -> "The flight service endpoint was not found (HTTP 404). Check Settings → API."
        429 -> "The API rate limit was reached. Try again later."
        in 500..599 -> "The flight service is temporarily unavailable."
        else -> "The flight service returned an error ($code)."
    }

    private fun Int.isRetryable(): Boolean = this == 429 || this in 500..599

    private fun backoffMillis(attempt: Int): Long =
        min(MAX_BACKOFF_MILLIS, BASE_BACKOFF_MILLIS * (1L shl attempt))

    private sealed interface ExecOutcome {
        data class Ok(val code: Int, val body: String) : ExecOutcome
        data class Http(val code: Int, val body: String) : ExecOutcome
        data class Network(val exception: IOException) : ExecOutcome
    }

    companion object {
        private const val TAG = "FlightApi"
        private const val MAX_ATTEMPTS = 3
        private const val BASE_BACKOFF_MILLIS = 1_000L
        private const val MAX_BACKOFF_MILLIS = 8_000L
        private const val USER_AGENT = "Airo/1.0 (+https://github.com/tuxy/airo)"

        val defaultMoshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

/**
 * Selects the most complete [FlightContract] from an API response.
 *
 * The API can return multiple entries for one flight number, including
 * incomplete "stub" entries. Ranking avoids taking index 0 blindly.
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

/**
 * Normalises a user-entered flight number to the form the API expects.
 * Keeps the existing behaviour (split on a single space or dash).
 */
fun formatFlightNumber(string: String): String {
    val splitString = string.trim().split("[- ]".toRegex())
    return if (splitString.size == 2 && splitString[0].isNotEmpty() && splitString[1].isNotEmpty()) {
        "${splitString[0]}${splitString[1]}"
    } else {
        string.trim()
    }
}
