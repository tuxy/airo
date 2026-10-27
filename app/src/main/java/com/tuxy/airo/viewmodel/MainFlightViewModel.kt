package com.tuxy.airo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.repository.FlightRepository
import com.tuxy.airo.data.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime

class MainFlightViewModel(
    private val repository: FlightRepository,
    preferences: AppPreferences,
) : ViewModel() {

    private val _flightData = MutableStateFlow<List<FlightData>>(emptyList())
    val flightData: StateFlow<List<FlightData>> = _flightData.asStateFlow()

    private val _flightsUpcomingList = MutableStateFlow<List<List<FlightData>>>(emptyList())
    val flightsUpcomingList: StateFlow<List<List<FlightData>>> = _flightsUpcomingList.asStateFlow()

    private val _flightsPastList = MutableStateFlow<List<List<FlightData>>>(emptyList())
    val flightsPastList: StateFlow<List<List<FlightData>>> = _flightsPastList.asStateFlow()

    val timeFormat: StateFlow<String> = preferences.timeFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "hh:mm a")

    val recentFlights: StateFlow<List<String>> = preferences.recentFlights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var collectingJob: Job? = null

    fun startCollecting() {
        if (collectingJob?.isActive == true) return

        collectingJob = viewModelScope.launch {
            repository.observeFlights().collect { newFlightData ->
                _flightData.value = newFlightData
                processFlights(newFlightData)
            }
        }
    }

    private suspend fun processFlights(newFlightData: List<FlightData>) {
        withContext(Dispatchers.Default) {
            val now = ZonedDateTime.now()
            val sorted = newFlightData.sortedBy { it.effectiveArriveDate.toInstant() }

            val upcoming = sorted.filter { it.effectiveArriveDate.isAfter(now) }
            val past = sorted.filter { !it.effectiveArriveDate.isAfter(now) }.reversed()

            _flightsUpcomingList.value = groupFlightsByProximity(upcoming)
            _flightsPastList.value = groupFlightsByProximity(past)
        }
    }

    /**
     * Groups consecutive flights whose departures are no more than 24 h apart.
     * Uses a single pass and the effective (revised when available) times.
     */
    fun groupFlightsByProximity(flights: List<FlightData>): List<List<FlightData>> {
        if (flights.isEmpty()) return emptyList()

        val groups = mutableListOf<MutableList<FlightData>>()
        for (flight in flights) {
            val currentGroup = groups.lastOrNull()
            val previous = currentGroup?.lastOrNull()
            val closeEnough = previous != null &&
                Duration.between(
                    previous.effectiveDepartDate,
                    flight.effectiveDepartDate,
                ).abs() <= GROUPING_WINDOW
            if (closeEnough) {
                currentGroup.add(flight)
            } else {
                groups.add(mutableListOf(flight))
            }
        }
        return groups.map { it.toList() }
    }

    override fun onCleared() {
        super.onCleared()
        collectingJob?.cancel()
    }

    class Factory(
        private val repository: FlightRepository,
        private val preferences: AppPreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainFlightViewModel(repository, preferences) as T
        }
    }

    private companion object {
        val GROUPING_WINDOW: Duration = Duration.ofHours(24)
    }
}
