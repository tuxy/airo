package com.tuxy.airo.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZonedDateTime

/** Which backend the user configured in Settings → API. */
enum class ApiProvider {
    /** The public AiroAPI instance. */
    PUBLIC,

    /** A self-hosted AiroAPI-compatible server. */
    AIRO_API,

    /** AeroDataBox (or compatible) endpoint, authenticated with an API key. */
    DIRECT;

    companion object {
        fun fromRaw(raw: String?): ApiProvider = when (raw) {
            "1" -> AIRO_API
            "2" -> DIRECT
            else -> PUBLIC
        }

        fun toRaw(provider: ApiProvider): String = when (provider) {
            PUBLIC -> "0"
            AIRO_API -> "1"
            DIRECT -> "2"
        }
    }
}

/**
 * Fully resolved network configuration.
 *
 * [baseUrl] always ends with `/`. It is empty when the selected provider needs a
 * URL that the user has not entered yet; callers must treat that as a
 * configuration error instead of silently falling back to the public server.
 */
data class ApiConfig(
    val provider: ApiProvider,
    val baseUrl: String,
    val apiKey: String?,
) {
    val isConfigured: Boolean
        get() = baseUrl.startsWith("https://") || baseUrl.startsWith("http://")

    companion object {
        const val DEFAULT_BASE_URL = "https://airoapi.tuxy.stream/"
    }
}

/**
 * Typed, process-wide access to user settings.
 *
 * DataStore `Preferences` keys are typed. Before this class existed the
 * update-interval slider wrote a `floatPreferencesKey` while every reader used
 * `stringPreferencesKey`, so the user's interval was silently ignored. Keep
 * every key definition in one place and expose typed flows.
 */
class AppPreferences(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        // The Compose slider writes a Float; kept for backwards compatibility.
        val updateIntervalFloat = floatPreferencesKey("update_interval")
        val updateIntervalHours = longPreferencesKey("update_interval_hours")
        val selectedApi = stringPreferencesKey("selected_api")
        val airoApiUrl = stringPreferencesKey("endpoint_airoapi")
        val directUrl = stringPreferencesKey("endpoint_adb")
        val directKey = stringPreferencesKey("endpoint_adb_key")
        val lastRefresh = stringPreferencesKey("last_refresh_time")
        val use24Hour = booleanPreferencesKey("24_time")
        val recentFlights = stringPreferencesKey("recent_flights")
        val alertsEnabled = booleanPreferencesKey("enable_alerts")
        val batteryPromptDismissed = booleanPreferencesKey("is_ignoring_optimisation")
    }

    val selectedApi: Flow<String> =
        dataStore.data.map { it[Keys.selectedApi] ?: ApiProvider.toRaw(ApiProvider.PUBLIC) }

    val apiConfig: Flow<ApiConfig> = dataStore.data.map { prefs ->
        val provider = ApiProvider.fromRaw(prefs[Keys.selectedApi])
        val rawBase = when (provider) {
            ApiProvider.PUBLIC -> ApiConfig.DEFAULT_BASE_URL
            ApiProvider.AIRO_API ->
                prefs[Keys.airoApiUrl]?.trim().orEmpty().ifBlank { ApiConfig.DEFAULT_BASE_URL }
            ApiProvider.DIRECT -> prefs[Keys.directUrl]?.trim().orEmpty()
        }
        val key = if (provider == ApiProvider.DIRECT) {
            prefs[Keys.directKey]?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
        ApiConfig(provider, rawBase.withTrailingSlash(), key)
    }

    /**
     * Reads both the legacy Float key and the new Long key. Defaults to 48 h,
     * matching the default shown by the settings slider.
     */
    val updateIntervalHours: Flow<Long> = dataStore.data.map { prefs ->
        val value = prefs[Keys.updateIntervalFloat]?.toLong()
            ?: prefs[Keys.updateIntervalHours]
            ?: DEFAULT_UPDATE_INTERVAL_HOURS
        value.coerceIn(MIN_UPDATE_INTERVAL_HOURS, MAX_UPDATE_INTERVAL_HOURS)
    }

    val use24Hour: Flow<Boolean> = dataStore.data.map { it[Keys.use24Hour] ?: false }

    val timeFormat: Flow<String> = use24Hour.map { if (it) "HH:mm" else "hh:mm a" }

    val dateFormat: Flow<String> = use24Hour.map { if (it) "yyyy-MM-dd" else "dd MMM yyyy" }

    val alertsEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.alertsEnabled] ?: true }

    val batteryPromptDismissed: Flow<Boolean> =
        dataStore.data.map { it[Keys.batteryPromptDismissed] ?: false }

    val recentFlights: Flow<List<String>> = dataStore.data.map { prefs ->
        stringToList(prefs[Keys.recentFlights].orEmpty())
    }

    val lastRefreshTime: Flow<String> = dataStore.data.map { it[Keys.lastRefresh] ?: "" }

    val lastRefreshInstant: Flow<Instant?> = lastRefreshTime.map(::parseInstant)

    suspend fun updateIntervalHours(): Long = updateIntervalHours.first()

    suspend fun setUpdateIntervalHours(hours: Long) {
        val clamped = hours.coerceIn(MIN_UPDATE_INTERVAL_HOURS, MAX_UPDATE_INTERVAL_HOURS)
        dataStore.edit { prefs ->
            prefs[Keys.updateIntervalHours] = clamped
            // Keep the legacy key in sync for the Compose slider UI.
            prefs[Keys.updateIntervalFloat] = clamped.toFloat()
        }
    }

    suspend fun markRefreshed(instant: Instant = Instant.now()) {
        dataStore.edit { it[Keys.lastRefresh] = instant.toString() }
    }

    suspend fun queueRecentFlight(flightNumber: String) {
        val normalized = flightNumber.uppercase()
        if (normalized.isBlank()) return
        dataStore.edit { prefs ->
            val current = stringToList(prefs[Keys.recentFlights].orEmpty())
            val updated = (current + normalized).distinct().takeLast(MAX_RECENT_FLIGHTS)
            prefs[Keys.recentFlights] = updated.joinToString(SEPARATOR)
        }
    }

    suspend fun setBatteryPromptDismissed(dismissed: Boolean = true) {
        dataStore.edit { it[Keys.batteryPromptDismissed] = dismissed }
    }

    suspend fun isAlertsEnabled(): Boolean = alertsEnabled.first()

    suspend fun isBatteryPromptDismissed(): Boolean = batteryPromptDismissed.first()

    private fun stringToList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    companion object {
        const val DEFAULT_UPDATE_INTERVAL_HOURS = 48L
        const val MIN_UPDATE_INTERVAL_HOURS = 1L
        const val MAX_UPDATE_INTERVAL_HOURS = 168L
        private const val MAX_RECENT_FLIGHTS = 5
        private const val SEPARATOR = "^"

        /** Accepts both the current `Instant` strings and legacy `ZonedDateTime` strings. */
        fun parseInstant(value: String?): Instant? {
            if (value.isNullOrBlank()) return null
            return runCatching { Instant.parse(value) }.getOrElse {
                runCatching { ZonedDateTime.parse(value).toInstant() }.getOrNull()
            }
        }

        private fun String.withTrailingSlash(): String = when {
            isBlank() -> ""
            endsWith("/") -> this
            else -> "$this/"
        }
    }
}
