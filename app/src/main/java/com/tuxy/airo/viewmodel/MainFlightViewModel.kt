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
import java.time.ZonedDateTime

class MainFlightViewModel(context: Context) : ViewModel() {
    val preferencesInterface = PreferencesInterface(context)

    var flightData by mutableStateOf(emptyList<FlightData>())
        private set

    var flights = flightData.associateBy { flight ->
        flight.departDate.toEpochSecond()
    }.toSortedMap()

    var flightsUpcomingList = emptyList<List<FlightData>>()
    var flightsPastList = emptyList<List<FlightData>>()

    @OptIn(DelicateCoroutinesApi::class)
    fun loadData(flightDataDao: FlightDataDao) {
        GlobalScope.launch {
            flightData = flightDataDao.readAll()
            // Group by 1 day
            flights = flightData.associateBy { flight ->
                flight.arriveDate.toEpochSecond()
            }.toSortedMap()

            val nowInEpochSeconds = ZonedDateTime.now().toEpochSecond()

            val flightsUpcoming = flights
                .filterKeys {
                    it > nowInEpochSeconds
                }
                .toSortedMap(compareBy { it })
            val flightsPast = flights
                .filterKeys {
                    it <= nowInEpochSeconds
                }
                .toSortedMap(compareBy { it })

            flightsUpcomingList = groupFlightsByProximity(flightsUpcoming)
            flightsPastList = groupFlightsByProximity(flightsPast).reversed()
        }
    }

    fun groupFlightsByProximity(
        flights: Map<Long, FlightData>
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

    fun findClosestFlightId(): Int? {
        if (flights.isEmpty()) return null

        val nowInEpochSeconds = ZonedDateTime.now().toEpochSecond()

        val upcomingFlightKey = flights.keys.firstOrNull { it > nowInEpochSeconds }

        return when {
            upcomingFlightKey == null -> null
            else -> flights[upcomingFlightKey]?.id
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val context: Context,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainFlightViewModel(context) as T
        }
    }
}
