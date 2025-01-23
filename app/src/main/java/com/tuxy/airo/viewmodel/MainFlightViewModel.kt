package com.tuxy.airo.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("OPT_IN_USAGE")
class MainFlightViewModel(flightDataDao: FlightDataDao) : ViewModel() {
    var flightData = mutableStateOf(emptyList<FlightData>()) // Initialise empty viewmodel
        private set

    init {
        GlobalScope.launch {
            flightData.value = flightDataDao.readAll()
        } // On initialisation, pass db data into flightData
    }

    // Factory
    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val flightDataDao: FlightDataDao,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainFlightViewModel(flightDataDao) as T
        }
    }
}
