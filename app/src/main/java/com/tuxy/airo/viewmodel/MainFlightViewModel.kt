package com.tuxy.airo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.util.SortedMap
import kotlin.math.roundToLong

/**
 * ViewModel responsible for managing and providing flight data for the main flight display screen.
 * It handles loading flight data from a persistent source and organizing it for presentation.
 */
class MainFlightViewModel() : ViewModel() {
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
     * Flights are bucketed into 2-day intervals. The constant `172800` represents the number of seconds
     * in two days (i.e., `2 * 24 * 60 * 60`).
     *
     * **Note:** The existing inline code comment "Group by 3 days" is inconsistent with the
     * actual divisor `172800` (2 days). This KDoc describes the current 2-day grouping behavior.
     * For 3-day grouping, the divisor should be `259200`. This discrepancy should be addressed
     * in the code or the comment for clarity.
     *
     * The keys of the map are epoch seconds (UTC), representing the calculated start of each 2-day interval.
     * This is achieved by dividing the flight's UTC departure epoch second by `172800`, rounding the result
     * using `Math.round()`, and then multiplying by `172800` again.
     * The values are lists of [FlightData] objects that fall within that specific 2-day interval.
     * The map is sorted by its keys (interval start times).
     */
    // Group by 1 day
    var flights = flightData.groupBy { flight ->
        // 86400 seconds = 1 day
                (flight.departDate.toEpochSecond(ZoneOffset.UTC)
                    .toDouble() / 86400).roundToLong() * 86400 // Maybe for the future, make something to smart-combine flights close together.
    }.toSortedMap()

    /**
     * Asynchronously loads flight data from the persistent storage using the provided DAO.
     *
     * This function uses [GlobalScope.launch] to initiate a coroutine for data loading.
     * `@OptIn(DelicateCoroutinesApi::class)` is used to acknowledge the use of `GlobalScope`.
     * For improved lifecycle management, consider using `viewModelScope` if this ViewModel
     * is tied to a specific Android lifecycle.
     *
     * Upon successful loading, it updates the [flightData] property with the retrieved list
     * and subsequently re-computes the [flights] grouped map.
     *
     * @param flightDataDao The Data Access Object ([FlightDataDao]) used for retrieving flight data.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun loadData(flightDataDao: FlightDataDao) {
        GlobalScope.launch {
            flightData = flightDataDao.readAll()
            // Group by 1 day
            flights = flightData.groupBy { flight ->
                (
                        (flight.departDate.toEpochSecond(ZoneOffset.UTC)
                            .toDouble() / 86400).roundToLong() * 86400
                        )
            }.toSortedMap()
        }
    }
}
