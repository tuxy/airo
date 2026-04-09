package com.tuxy.airo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime

class MainFlightViewModel(context: Context) : ViewModel() {
    val preferencesInterface = PreferencesInterface(context)

    private val _flightData = MutableStateFlow<List<FlightData>>(emptyList())
    val flightData: StateFlow<List<FlightData>> = _flightData.asStateFlow()

    private val _flights = MutableStateFlow<Map<Long, FlightData>>(emptyMap())
    val flights: StateFlow<Map<Long, FlightData>> = _flights.asStateFlow()

    // Why mix states and flows? Why? Because I don't know.
    private val _flightsUpcomingList = MutableStateFlow<List<List<FlightData>>>(emptyList())
    val flightsUpcomingList: StateFlow<List<List<FlightData>>> = _flightsUpcomingList.asStateFlow()

    private val _flightsPastList = MutableStateFlow<List<List<FlightData>>>(emptyList())
    val flightsPastList: StateFlow<List<List<FlightData>>> = _flightsPastList.asStateFlow()

    private var collectingJob: Job? = null

    fun startCollecting(flightDataDao: FlightDataDao) {
        if (collectingJob?.isActive == true) return

        collectingJob = viewModelScope.launch {
            flightDataDao.readAll().collect { newFlightData ->
                _flightData.value = newFlightData
                processFlights(newFlightData)
            }
        }
    }

    private fun processFlights(newFlightData: List<FlightData>) {
        val newFlights = newFlightData.associateBy { flight ->
            flight.scheduledArriveDate.toEpochSecond()
        }.toSortedMap()
        _flights.value = newFlights

        val nowInEpochSeconds = ZonedDateTime.now().toEpochSecond()

        val flightsUpcoming = newFlights
            .filterKeys {
                it > nowInEpochSeconds
            }
            .toSortedMap(compareBy { it })
        val flightsPast = newFlights
            .filterKeys {
                it <= nowInEpochSeconds
            }
            .toSortedMap(compareBy { it })

        _flightsUpcomingList.value = groupFlightsByProximity(flightsUpcoming)
        _flightsPastList.value = groupFlightsByProximity(flightsPast).reversed()
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
                val timeDiff = Duration.between(
                    lastFlightInGroup.scheduledDepartDate,
                    flight.scheduledDepartDate
                ).abs()

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

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val context: Context,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainFlightViewModel(context) as T
        }
    }
}
