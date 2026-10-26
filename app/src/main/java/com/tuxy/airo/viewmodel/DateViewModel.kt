package com.tuxy.airo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuxy.airo.data.repository.AddFlightResult
import com.tuxy.airo.data.repository.FlightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Result state of the "add flight" screen. */
sealed interface AddFlightState {
    data object Idle : AddFlightState
    data object Loading : AddFlightState
    data object Success : AddFlightState
    data object Duplicate : AddFlightState
    data object NoFlight : AddFlightState
    data class Error(val message: String) : AddFlightState
}

class DateViewModel(
    private val repository: FlightRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AddFlightState>(AddFlightState.Idle)
    val state: StateFlow<AddFlightState> = _state.asStateFlow()

    /**
     * Adds the flight for the picked date.
     *
     * The screen observes [state]; navigation and error messages belong to the
     * UI, not to the data layer. The old implementation popped the back stack in
     * a `finally` block, so a network failure silently threw away the date.
     */
    fun addFlight(flightNumber: String, timeMillis: Long?) {
        if (_state.value is AddFlightState.Loading) return

        if (timeMillis == null) {
            _state.value = AddFlightState.Error("Pick a date first.")
            return
        }

        val date = getDateAsString(timeMillis).toLocalDate()

        viewModelScope.launch {
            _state.value = AddFlightState.Loading
            _state.value = when (val result = repository.addFlight(flightNumber, date)) {
                is AddFlightResult.Added -> AddFlightState.Success
                AddFlightResult.Duplicate -> AddFlightState.Duplicate
                AddFlightResult.NoFlight -> AddFlightState.NoFlight
                is AddFlightResult.Failed -> AddFlightState.Error(result.message)
            }
        }
    }

    fun resetState() {
        _state.value = AddFlightState.Idle
    }

    /**
     * Converts the Material3 date picker value (UTC midnight) to a zoned date.
     * The calendar day is preserved; only the zone is attached for formatting.
     */
    fun getDateAsString(time: Long): ZonedDateTime {
        return LocalDateTime.ofEpochSecond(time / 1000L, 0, ZoneOffset.UTC)
            .atZone(ZoneOffset.systemDefault())
    }

    class Factory(
        private val repository: FlightRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DateViewModel(repository) as T
        }
    }
}
