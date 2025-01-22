package com.tuxy.airo.viewmodel

import android.content.Context
import androidx.compose.material3.Badge
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.singleIntoMut
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("UNCHECKED_CAST")
class DetailsViewModel(context: Context, flightDataDao: FlightDataDao, id: String): ViewModel() {
    var flightData = mutableStateOf(FlightData())
    var openDialog = mutableStateOf(false)

    var progress = mutableFloatStateOf(0.0F)
        private set

    init {
        singleIntoMut(flightData, flightDataDao, id) // On initialisation, pass db data into flightData
    }

    private val tileStreamProvider = TileStreamProvider { row, col, zoomLvl -> // Local map tiles for full offline usage
        context.assets.open("tiles/${zoomLvl}/${col}/${row}.png")
    }
    private val mapSize = mapSizeAtLevel()
    @OptIn(DelicateCoroutinesApi::class)
    val mapState = MapState(6, mapSize, mapSize).apply {
        addLayer(tileStreamProvider)
        addMarker("origin", x = flightData.value.mapOriginX, y = flightData.value.mapOriginY) {
            Badge(contentColor = Color.Black, containerColor = Color.Black)
        }
        addMarker(
            "destination",
            x = flightData.value.mapDestinationX,
            y = flightData.value.mapDestinationY
        ) {
            Badge(contentColor = Color.Black, containerColor = Color.Black)
        }
        GlobalScope.launch {
            scrollTo(
                avr(flightData.value.mapOriginX, flightData.value.mapDestinationX),
                avr(flightData.value.mapOriginY, flightData.value.mapDestinationY),
                calculateScale(
                    flightData.value.mapOriginX,
                    flightData.value.mapOriginY,
                    flightData.value.mapDestinationX,
                    flightData.value.mapDestinationY
                )
            )
            addPath("route", color = Color.Black, width = 2.dp) {
                addPoint(x = flightData.value.mapOriginX, y = flightData.value.mapOriginY - 0.0007)
                addPoint(
                    x = flightData.value.mapDestinationX,
                    y = flightData.value.mapDestinationY - 0.0007
                )
            }
        }
    }

    // Factory
    class Factory(
        private val context: Context,
        private val flightDataDao: FlightDataDao,
        private val id: String,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailsViewModel(context, flightDataDao, id) as T
        }
    }

    private fun mapSizeAtLevel(): Int {
        return 256 * 2.0.pow(5).toInt()
    }

    private fun avr(a: Double, b: Double): Double {
        return (a + b) / 2
    }

    private fun calculateScale(x1: Double, y1: Double, x2: Double, y2: Double): Float {
        val zoomConstant = 12.0 // Trial and error

        val a = (x2 - x1) * (x2 - x1)
        val b = (y2 - y1) * (y2 - y1)
        return (1/(sqrt(a + b) * zoomConstant)).toFloat()
    }
}
