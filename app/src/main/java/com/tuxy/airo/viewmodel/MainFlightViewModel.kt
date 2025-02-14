package com.tuxy.airo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.ZoneOffset

class MainFlightViewModel() : ViewModel() {
    var flightData by mutableStateOf(emptyList<FlightData>()) // Initialise empty viewmodel
        private set
    var progress = mutableFloatStateOf(0.0F)
        private set
    var selectedTabIndex = mutableIntStateOf(0)
        private set

    // Group by 3 days
    var flights = flightData.groupBy { flight ->
        Math.round(
            flight.departDate.toEpochSecond(ZoneOffset.UTC).toDouble() / 172800
        ) * 172800
    }.toSortedMap()

    @OptIn(DelicateCoroutinesApi::class)
    fun loadData(flightDataDao: FlightDataDao) {
        GlobalScope.launch {
            flightData = flightDataDao.readAll()
            // Group by 3 days
            flights = flightData.groupBy { flight ->
                Math.round(
                    flight.departDate.toEpochSecond(ZoneOffset.UTC).toDouble() / 172800
                ) * 172800
            }.toSortedMap()
        }
    }
}
