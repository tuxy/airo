package com.tuxy.airo.data.repository

import androidx.room.withTransaction
import com.tuxy.airo.data.background.ReminderScheduler
import com.tuxy.airo.data.flightdata.FlightChange
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataBase
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.data.mapping.FlightMapper
import com.tuxy.airo.data.network.ApiResult
import com.tuxy.airo.data.network.FlightApi
import com.tuxy.airo.data.network.bestFlight
import com.tuxy.airo.data.network.formatFlightNumber
import com.tuxy.airo.data.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

sealed interface AddFlightResult {
    data class Added(val id: Int) : AddFlightResult
    data object Duplicate : AddFlightResult
    data object NoFlight : AddFlightResult
    data class Failed(val message: String) : AddFlightResult
}

sealed interface RefreshFlightResult {
    data class Updated(val flight: FlightData, val change: FlightChange?) : RefreshFlightResult
    data object NoData : RefreshFlightResult
    data class Failed(val message: String) : RefreshFlightResult
}

data class RefreshSummary(
    val total: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val changes: List<FlightChange> = emptyList(),
)

/**
 * Single entry point for flight data.
 *
 * Owns the DAO + API + settings wiring. The background worker, the details
 * screen and the add-flight flow all go through here, so the configured API
 * server/key and the duplicate/constraint handling cannot diverge again.
 */
class FlightRepository(
    private val database: FlightDataBase,
    private val dao: FlightDataDao,
    private val api: FlightApi,
    private val preferences: AppPreferences,
    private val reminders: ReminderScheduler,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun observeFlights(): Flow<List<FlightData>> = dao.readAll()

    fun observeFlight(id: Int): Flow<FlightData?> = dao.observeSingle(id)

    suspend fun getFlight(id: Int): FlightData? = dao.readSingle(id)

    suspend fun addFlight(flightNumber: String, departureDate: LocalDate): AddFlightResult {
        if (flightNumber.isBlank()) {
            return AddFlightResult.Failed("Enter a flight number.")
        }

        return when (val result = api.getFlight(flightNumber, departureDate)) {
            is ApiResult.Success -> {
                val contract = result.data.bestFlight()
                    ?: return AddFlightResult.NoFlight
                val mapped = FlightMapper.from(contract, clock)
                val rowId = dao.addFlight(mapped)
                if (rowId == -1L) {
                    AddFlightResult.Duplicate
                } else {
                    preferences.queueRecentFlight(formatFlightNumber(flightNumber))
                    reminders.schedule(mapped.copy(id = rowId.toInt()))
                    AddFlightResult.Added(rowId.toInt())
                }
            }

            is ApiResult.HttpError -> AddFlightResult.Failed(result.message)

            is ApiResult.NetworkError -> AddFlightResult.Failed(result.message)
            is ApiResult.ParseError -> AddFlightResult.Failed(result.message)
            is ApiResult.ConfigError -> AddFlightResult.Failed(result.message)
        }
    }

    /**
     * Refreshes every flight that has not arrived and departs within a week.
     * Requests run with bounded parallelism; database writes happen in one
     * transaction so Room emits once instead of once per flight.
     */
    suspend fun refreshAll(): RefreshSummary = withContext(Dispatchers.IO) {
        val allFlights = dao.readAll().first()
        val now = ZonedDateTime.now(clock)
        val horizon = now.plusDays(REFRESH_HORIZON_DAYS)

        val candidates = allFlights.filter { flight ->
            flight.effectiveArriveDate.isAfter(now) &&
                flight.effectiveDepartDate.isBefore(horizon)
        }
        val skipped = allFlights.size - candidates.size
        if (candidates.isEmpty()) {
            preferences.markRefreshed(clock.instant())
            return@withContext RefreshSummary(total = allFlights.size, skipped = skipped)
        }

        val semaphore = Semaphore(MAX_PARALLEL_REQUESTS)
        val fetched = coroutineScope {
            candidates.map { flight ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        flight to api.getFlight(
                            flightNumber = flight.callSign,
                            date = flight.effectiveDepartDate.toLocalDate(),
                            withAircraftImage = true,
                            withLocation = true,
                        )
                    }
                }
            }.awaitAll()
        }

        var updated = 0
        var failed = 0
        val changes = mutableListOf<FlightChange>()
        val toReschedule = mutableListOf<FlightData>()

        database.withTransaction {
            for ((old, result) in fetched) {
                when (result) {
                    is ApiResult.Success -> {
                        val contract = result.data.bestFlight()
                        if (contract == null) {
                            failed++
                            continue
                        }
                        val mapped = FlightMapper.from(contract, clock)
                        val merged = applyRefresh(old, mapped)
                        toReschedule += merged
                        updated++
                        detectChange(old, merged)?.let(changes::add)
                    }

                    is ApiResult.HttpError,
                    is ApiResult.NetworkError,
                    is ApiResult.ParseError,
                    is ApiResult.ConfigError -> failed++
                }
            }
        }

        toReschedule.forEach { reminders.schedule(it) }
        if (updated > 0 && failed == 0) {
            preferences.markRefreshed(clock.instant())
        } else if (updated > 0) {
            preferences.markRefreshed(clock.instant())
        }

        RefreshSummary(
            total = allFlights.size,
            updated = updated,
            failed = failed,
            skipped = skipped + candidates.size - fetched.size,
            changes = changes,
        )
    }

    /** Refresh a single flight (pull-to-refresh on the details screen). */
    suspend fun refreshFlight(id: Int): RefreshFlightResult = withContext(Dispatchers.IO) {
        val old = dao.readSingle(id) ?: return@withContext RefreshFlightResult.NoData
        val result = api.getFlight(
            flightNumber = old.callSign,
            date = old.effectiveDepartDate.toLocalDate(),
            withAircraftImage = true,
            withLocation = true,
        )
        when (result) {
            is ApiResult.Success -> {
                val contract = result.data.bestFlight()
                    ?: return@withContext RefreshFlightResult.NoData
                val mapped = FlightMapper.from(contract, clock)
                val merged = database.withTransaction { applyRefresh(old, mapped) }
                reminders.schedule(merged)
                RefreshFlightResult.Updated(merged, detectChange(old, merged))
            }

            is ApiResult.HttpError -> RefreshFlightResult.Failed(result.message)
            is ApiResult.NetworkError -> RefreshFlightResult.Failed(result.message)
            is ApiResult.ParseError -> RefreshFlightResult.Failed(result.message)
            is ApiResult.ConfigError -> RefreshFlightResult.Failed(result.message)
        }
    }

    suspend fun updateTicket(flight: FlightData) {
        dao.updateFlight(flight)
    }

    suspend fun deleteFlight(flight: FlightData) {
        reminders.cancel(flight)
        dao.deleteFlight(flight)
    }

    suspend fun lastRefreshInstant(): Instant? = preferences.lastRefreshInstant.first()

    /**
     * Applies an update without ever violating the `(callSign,
     * scheduledDepartDate)` unique index. If the API moved the flight onto a
     * date that is already tracked, the existing row wins and absorbs the new
     * data; the stale row is removed. Ticket data is always preserved.
     */
    private suspend fun applyRefresh(old: FlightData, mapped: FlightData): FlightData {
        val clash = dao.findByKey(mapped.callSign, mapped.scheduledDepartDate)
        return if (clash != null && clash.id != old.id) {
            val merged = mapped.copy(
                id = clash.id,
                ticketData = clash.ticketData.ifBlank { old.ticketData },
            )
            dao.updateFlight(merged)
            dao.deleteFlight(old)
            merged
        } else {
            val merged = mapped.copy(
                id = old.id,
                ticketData = old.ticketData,
            )
            dao.updateFlight(merged)
            merged
        }
    }

    internal fun detectChange(old: FlightData, new: FlightData): FlightChange? {
        // Do not notify on the first successful fetch after adding a flight.
        if (old.lastUpdatedUtc == null && !old.hasKnownStatus) return null
        if (old.lastUpdatedUtc != null && old.lastUpdatedUtc == new.lastUpdatedUtc) return null

        val changes = mutableListOf<String>()

        if (new.hasKnownStatus && old.status != new.status) {
            changes += if (old.hasKnownStatus) {
                "status from ${old.status.humanize()} to ${new.status.humanize()}"
            } else {
                "status to ${new.status.humanize()}"
            }
        }

        describeValueChange(
            label = "gate",
            oldValue = old.gate,
            newValue = new.gate,
        )?.let(changes::add)
        describeValueChange(
            label = "terminal",
            oldValue = old.terminal,
            newValue = new.terminal,
        )?.let(changes::add)
        describeValueChange(
            label = "baggage claim",
            oldValue = old.toBaggageClaim,
            newValue = new.toBaggageClaim,
        )?.let(changes::add)

        describeTimeChange(
            label = "departure",
            oldTime = old.effectiveDepartDate,
            newTime = new.effectiveDepartDate,
            threshold = Duration.ofMinutes(5),
        )?.let(changes::add)

        describeTimeChange(
            label = "arrival",
            oldTime = old.effectiveArriveDate,
            newTime = new.effectiveArriveDate,
            threshold = Duration.ofMinutes(10),
        )?.let(changes::add)

        if (changes.isEmpty()) return null
        return FlightChange(
            flightId = new.id,
            callSign = new.callSign,
            changes = changes,
        )
    }

    private fun describeValueChange(
        label: String,
        oldValue: String,
        newValue: String,
    ): String? {
        if (!newValue.isUseful() || oldValue == newValue) return null
        return if (oldValue.isUseful()) {
            "$label from $oldValue to $newValue"
        } else {
            "$label to $newValue"
        }
    }

    private fun describeTimeChange(
        label: String,
        oldTime: ZonedDateTime,
        newTime: ZonedDateTime,
        threshold: Duration,
    ): String? {
        if (Duration.between(oldTime, newTime).abs() < threshold) return null
        return if (oldTime.toLocalDate() != newTime.toLocalDate()) {
            "$label date from ${DATE_FORMAT.format(oldTime)} to ${DATE_FORMAT.format(newTime)}"
        } else {
            "$label time from ${TIME_FORMAT.format(oldTime)} to ${TIME_FORMAT.format(newTime)}"
        }
    }

    private fun String.isUseful(): Boolean =
        isNotBlank() && this != "---" && this != "—" && this != "N/A"

    private fun String.humanize(): String = replace(Regex("([a-z])([A-Z])"), "$1 $2")

    companion object {
        // AeroDataBox's BASIC plan is limited to roughly one request per
        // second. Keep refreshes sequential so a handful of tracked flights
        // does not trip the limiter; WorkManager absorbs the extra duration.
        private const val MAX_PARALLEL_REQUESTS = 1
        private const val REFRESH_HORIZON_DAYS = 7L
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    }
}
