package com.tuxy.airo.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataDao
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.SortedMap

/**
 * ViewModel for the main flight screen.
 *
 * This ViewModel is responsible for loading all flight data, sorting them into upcoming and past flights,
 * and grouping them by proximity for display.
 *
 * @property preferencesInterface For accessing user preferences.
 * @property flightData The raw list of all flight data.
 * @property flights A sorted map of flights, with the departure time in epoch seconds as the key.
 * @property flightsUpcomingList A list of upcoming flights, grouped by proximity.
 * @property flightsPastList A list of past flights, grouped by proximity.
 * @param context The application context.
 */
class MainFlightViewModel(context: Context) : ViewModel() {
    /**
     * Interface for accessing user preferences.
     */
    val preferencesInterface = PreferencesInterface(context)

    /**
     * Holds the raw, unsorted list of all [FlightData] objects.
     * This list is populated asynchronously via the [loadData] function.
     * It is privately set to ensure data modification occurs only through controlled mechanisms.
     */
    var flightData by mutableStateOf(emptyList<FlightData>()) // Initialise empty viewmodel
        private set

    /**
     * A [SortedMap] that groups flights for display, derived from [flightData].
     *
     * The grouping logic is based on the flight's departure date and time ([FlightData.departDate]).
     * The keys of the map are epoch seconds (UTC), representing the flight's departure time.
     * The map is sorted by its keys (departure times).
     */
    var flights = flightData.associateBy { flight ->
        flight.departDate.toEpochSecond(ZoneOffset.UTC)
    }.toSortedMap()

    var flightsUpcomingList = emptyList<List<FlightData>>()
    var flightsPastList = emptyList<List<FlightData>>()

    /**
     * Asynchronously loads flight data from the persistent storage using the provided DAO.
     *
     * Upon successful loading, it updates the [flightData] property, sorts the flights into
     * upcoming and past lists, and groups them by proximity.
     *
     * @param flightDataDao The Data Access Object ([FlightDataDao]) used for retrieving flight data.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun loadData(flightDataDao: FlightDataDao) {
        GlobalScope.launch {
            flightData = flightDataDao.readAll()
            // Group by 1 day
            flights = flightData.associateBy { flight ->
                flight.arriveDate.toEpochSecond(ZoneOffset.UTC)
            }.toSortedMap()

            val nowInEpochSeconds = LocalDateTime.now().atZone(ZoneOffset.systemDefault()).toEpochSecond()


            val flightsUpcoming = flights
                .filterKeys { it > nowInEpochSeconds }
                .toSortedMap(compareBy { it })
            val flightsPast = flights
                .filterKeys {
                    println(nowInEpochSeconds)
                    println(it)
                    it <= nowInEpochSeconds
                }
                .toSortedMap(compareBy { it })

            flightsUpcomingList = groupFlightsByProximity(flightsUpcoming)
            flightsPastList = groupFlightsByProximity(flightsPast).reversed()
        }
    }

    /**
     * Groups flights based on their departure time proximity.
     *
     * The grouping process is as follows:
     * 1. Initialize with an empty list of groups.
     * 2. For each flight, check if it fits into the last group.
     * 3. If it fits (its departure time is within 24 hours of the last flight in the last group), add it to that group.
     * 4. If it does not fit, start a new group containing just this flight.
     * @param flights A map of flights to be grouped.
     * @return A list of lists, where each inner list represents a group of flights.
     */
    fun groupFlightsByProximity(
        flights: Map<Long, FlightData> // Implement threshold with preferencesInterface
    ): List<List<FlightData>> {
        var list = emptyList<FlightData>()

        for ((_, value) in flights) {
            list = list + value
        }

        return list.fold(initial = emptyList()) { current: List<List<FlightData>>, flight: FlightData ->
            val lastGroup = current.lastOrNull()
            val lastFlightInGroup = lastGroup?.lastOrNull()

            if (lastFlightInGroup != null) {
                val timeDiff = Duration.between(lastFlightInGroup.departDate, flight.departDate).abs()

                if (timeDiff <= Duration.ofHours(24)) {
                    val updatedLastGroup = lastGroup + flight

                    current.dropLast(1) + listOf(updatedLastGroup)
                } else {
                    current + listOf(listOf(flight))
                }
            } else {
                current + listOf(listOf(flight))
            }
        }
    }

    /**
     * Finds the ID of the closest upcoming flight.
     * @return The ID of the closest upcoming flight, or null if there are no upcoming flights.
     */
    fun findClosestFlightId(): Int? {
        if (flights.isEmpty()) return null

        val nowInEpochSeconds = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC)

        val upcomingFlightKey = flights.keys.firstOrNull { it > nowInEpochSeconds }

        return when {
            upcomingFlightKey == null -> null
            else -> flights[upcomingFlightKey]?.id
        }
    }

    /**
     * Factory for creating [MainFlightViewModel] instances.
     */
    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val context: Context,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainFlightViewModel(context) as T
        }
    }

}
