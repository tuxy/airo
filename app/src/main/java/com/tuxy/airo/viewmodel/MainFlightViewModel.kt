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

class MainFlightViewModel: ViewModel() {
    var flightData by mutableStateOf(emptyList<FlightData>()) // Initialise empty viewmodel
        private set

    @OptIn(DelicateCoroutinesApi::class)
    fun loadData(flightDataDao: FlightDataDao) {
        GlobalScope.launch {
            flightData = flightDataDao.readAll()
        }
    }
}
