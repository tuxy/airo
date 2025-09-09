package com.tuxy.airo.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.R
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.PreferencesInterface
import com.tuxy.airo.data.getData
import com.tuxy.airo.data.singleIntoMut
import com.tuxy.airo.screens.ApiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.disableGestures
import ovh.plrapps.mapcompose.api.disableRotation
import ovh.plrapps.mapcompose.api.disableScrolling
import ovh.plrapps.mapcompose.api.disableZooming
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Manages and provides data for the flight details screen, including map display and flight status information.
 * The `@Suppress("UNCHECKED_CAST")` annotation is present due to the type casting in the [Factory] class.
 */
@Suppress("UNCHECKED_CAST")
class DetailsViewModel(
    context: Context,
    flightDataDao: FlightDataDao,
    id: String,
    scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ViewModel() {
    // Initialise preferences interface
    val preferencesInterface = PreferencesInterface(context)
    val viewModelScope = scope

    var flightData = mutableStateOf(FlightData())
    var openDialog = mutableStateOf(false)
    var progress = mutableFloatStateOf(0.0F)
        private set

    init {
        when (id) {
            "testId" -> {
                flightData.value = FlightData()
            }

            else -> {
                singleIntoMut(
                    flightData,
                    flightDataDao,
                    id
                )
            }
        }
        // On initialisation, pass db data into flightData
        // Use empty FlightData if testId is used
    }

    fun getProgress(): Float {
        val now = LocalDateTime
            .now()
            .atZone(flightData.value.departTimeZone)
            .withZoneSameInstant(ZoneOffset.UTC)

        val departTime = flightData.value.departDate
            .atOffset(ZoneOffset.UTC)
            .withOffsetSameInstant(ZoneOffset.UTC)

        viewModelScope.launch {
            val timeFromStart = Duration.between(now, departTime).toMillis()

            if (now < departTime.toZonedDateTime()) {
                progress.floatValue = 0.0F
                return@launch
            }

            val duration = flightData.value.duration.toMillis()

            val current = timeFromStart.toFloat() / duration.toFloat()

            progress.floatValue = current.absoluteValue
            delay(10000) // Improve performance
        }
        return progress.floatValue
    }

    fun refreshData(
        flightDataDao: FlightDataDao,
        context: Context,
        settings: ApiSettings,
        isRefreshing: MutableState<Boolean>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            val collectedFlightData = getData(
                flightNumber = flightData.value.callSign.replace(
                    " ",
                    ""
                ), // Whitespace removal
                flightDataDao = flightDataDao,
                date = flightData.value.departDate.format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
                ),
                settings = settings,
                context = context,
                update = true
            )

            collectedFlightData.onSuccess { newFlight ->
                Log.d("FlightUpdate", newFlight.toString())
                val deleted = flightData.value.copy()
                flightData.value = newFlight

                flightDataDao.deleteFlight(deleted)
                flightDataDao.addFlight(
                    newFlight.copy(
                        ticketData = flightData.value.ticketData,
                    ) // Retain ticket information
                )
            }
            isRefreshing.value = false
        }
    }

    fun getDuration(context: Context): String {
        val duration = Duration.between(
            LocalDateTime.now(),
            flightData.value.departDate
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(flightData.value.departTimeZone)
        )

        val offset =
            mutableFloatStateOf(10800F) // 3 hours in seconds, potentially for check-in window start

        val days = mutableFloatStateOf(0.0F)
        val hours = mutableFloatStateOf(0.0F)
        val minutes = mutableFloatStateOf(0.0F)


        val status = getStatus(context)
        if (status == context.resources.getString(R.string.check_in)) {
            // If in check-in phase and time to departure is between 1hr and 3hrs,
            // this function returns empty, implying getEndTime might be more relevant.
            if (duration <= Duration.ofSeconds(10800) && duration >= Duration.ofSeconds(3600)) {
                return ""
            }
        } else {
            offset.floatValue = 0.0F // Duration calculated directly to departure time
            if (duration <= Duration.ofSeconds(0)) { // If already departed or at departure time
                return ""
            }
        }

        // Calculate remaining seconds after offset (if any)
        val seconds = (duration.toMillis().toFloat() / 1000F) - offset.floatValue

        days.floatValue = floor((seconds / 86400))
        hours.floatValue = floor((seconds - days.floatValue * 86400.0F) / 3600.0F)
        minutes.floatValue =
            floor((seconds - days.floatValue * 86400.0F - hours.floatValue * 3600.0F) / 60.0F)

        return if (days.floatValue >= 1.0F) {
            "${context.resources.getString(R.string.ins)} ${days.floatValue.toInt()}d ${hours.floatValue.toInt()}h"
        } else if (hours.floatValue >= 1.0F) {
            "${context.resources.getString(R.string.ins)} ${hours.floatValue.toInt()}h ${minutes.floatValue.toInt()}m"
        } else {
            "${context.resources.getString(R.string.ins)} ${minutes.floatValue.toInt()}m"
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getEndTime(context: Context): String {
        val status = getStatus(context)
        val time = flightData.value.departDate

        var timeFormatWait = ""
        GlobalScope.launch {
            val timeFormat = preferencesInterface.getValueTimeFormat("24_time")
            timeFormatWait = timeFormat
        }

        if (status == context.getString(R.string.check_in)) {
            val correctedTime = time.minusHours(1) // Check-in ends 1 hour before departure

            return "${context.getString(R.string.ends_at)} ${
                correctedTime
                    .atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(flightData.value.departTimeZone)
                    .format(DateTimeFormatter.ofPattern(timeFormatWait))
            }"
        }

        return ""
    }

    fun getStatus(context: Context): String {
        val duration = Duration.between(
            LocalDateTime.now(),
            flightData.value.departDate
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(flightData.value.departTimeZone)
        )
        val seconds = duration.seconds

        if (seconds >= 3600) { // More than or equal to 1 hour until departure
            return context.getString(R.string.check_in) // Check-in
        } else { // Less than 1 hour until departure or already departed
            if (progress.floatValue >= 0.9F && progress.floatValue < 1.0F) {
                return context.getString(R.string.landing) // About to land
            } else if (progress.floatValue >= 1.0F) {
                return context.getString(R.string.landed) // Already landed
            }
            return context.getString(R.string.flying) // Still flying
        }

    }

    /**
     * Provides map tiles from local application assets (`assets/tiles/...`) for offline map display.
     * This allows the map to function without needing network connectivity to fetch tiles.
     */

    private val tileStreamProvider =
        TileStreamProvider { row, col, zoomLvl -> // Local map tiles for full offline usage
            context.assets.open("tiles/${zoomLvl}/${col}/${row}.png")
        }
    private val mapSize = mapSizeAtLevel()

    @OptIn(DelicateCoroutinesApi::class)
    val mapState = MapState(6, mapSize, mapSize).apply { // Max zoom level 6
        disableZooming()
        disableGestures()
        disableRotation()
        disableScrolling()
        addLayer(tileStreamProvider)
        GlobalScope.launch {
            addMarker(
                "origin",
                x = flightData.value.mapOriginX,
                y = flightData.value.mapOriginY,
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    "",
                    modifier = Modifier.size(16.dp), // Fixed marker size
                    tint = Color.DarkGray            // Fixed marker color
                )
            }
            addMarker(
                "destination",
                x = flightData.value.mapDestinationX,
                y = flightData.value.mapDestinationY,
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    "",
                    modifier = Modifier.size(16.dp), // Fixed marker size
                    tint = Color.DarkGray            // Fixed marker color
                )
            }
            addPath("route", color = Color.DarkGray, width = 2.dp) {
                // The y-offset of -0.0007 is likely a small visual adjustment for the path line
                // relative to the marker's anchor point.
                addPoint(x = flightData.value.mapOriginX, y = flightData.value.mapOriginY)
                addPoint(
                    x = flightData.value.mapDestinationX,
                    y = flightData.value.mapDestinationY
                )
            }
            // Scroll map to show both origin and destination
            scrollTo(
                x = avr(flightData.value.mapOriginX, flightData.value.mapDestinationX), // Center X
                y = avr(flightData.value.mapOriginY, flightData.value.mapDestinationY), // Center Y
                destScale = calculateScale( // Calculated scale to fit points
                    flightData.value.mapOriginX,
                    flightData.value.mapOriginY,
                    flightData.value.mapDestinationX,
                    flightData.value.mapDestinationY
                )
            )
        }
    }

    class Factory(
        private val context: Context,
        private val flightDataDao: FlightDataDao,
        private val id: String
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailsViewModel(context, flightDataDao, id) as T
        }
    }

    private fun mapSizeAtLevel(): Int {
        return 256 * 2.0.pow(5).toInt() // Hardcoded zoom level 5 for map size calculation
    }

    fun avr(a: Double, b: Double): Double {
        return (a + b) / 2
    }

    fun calculateScale(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val zoomConstant = 25.0 // Empirically determined constant to adjust the overall zoom level.
        // A smaller value zooms out more, a larger value zooms in more.

        val a = (x2 - x1) * (x2 - x1) // Squared difference in x
        val b = (y2 - y1) * (y2 - y1) // Squared difference in y
        // The scale is inversely related to the distance.
        // sqrt(a+b) is the distance between the two points.
        return (1 / (sqrt(a + b) * zoomConstant))
    }
}
