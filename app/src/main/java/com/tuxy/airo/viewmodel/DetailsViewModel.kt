package com.tuxy.airo.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.R
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.data.flightdata.getData
import com.tuxy.airo.screens.ApiSettings
import com.tuxy.airo.screens.CustomMapMarker
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
import ovh.plrapps.mapcompose.api.snapScrollTo
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.toKotlinDuration

/**
 * ViewModel for the flight details screen.
 *
 * This ViewModel is responsible for fetching and holding the details of a specific flight,
 * calculating flight progress, handling flight deletion, and managing the map display.
 *
 * @property preferencesInterface For accessing user preferences.
 * @property viewModelScope The coroutine scope for this ViewModel.
 * @property flightData The flight data for the current flight.
 * @property openDialog A mutable state to control the visibility of the delete confirmation dialog.
 * @property progress The current progress of the flight.
 * @param context The application context.
 * @param scheme The color scheme for the map.
 * @param scope The coroutine scope for this ViewModel.
 */
@Suppress("UNCHECKED_CAST")
class DetailsViewModel(
    context: Context,
    val preferencesInterface: PreferencesInterface,
    scheme: ColorScheme,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ViewModel() {
    private val contextForAssets = context
    val viewModelScope = scope

    var flightData = mutableStateOf(FlightData())
    var openDialog = mutableStateOf(false)
    var progress = mutableFloatStateOf(0.0F)
        private set

    fun loadFlightById(id: String, dao: FlightDataDao) {
        viewModelScope.launch(Dispatchers.IO) {
            val flight = dao.readSingle(id)
            flightData.value = flight
        }
    }
    /**
     * Calculates the time difference between the departure and arrival timezones.
     * @return A string representing the time difference, e.g., "+02:00".
     */
    fun getZoneDifference(): String {
        val departZoneSeconds = flightData.value.departDate.offset
        val arriveZoneSeconds = flightData.value.arriveDate.offset

        val differenceInSeconds = arriveZoneSeconds.totalSeconds - departZoneSeconds.totalSeconds

        val ahead = if (differenceInSeconds > 0) "+" else "-"

        val difference = Duration.ofSeconds(differenceInSeconds.toLong()).toKotlinDuration()
        difference.toComponents { hours, minutes, _, _ ->
            if (hours == 0L && minutes == 0) {
                return ""
            }
            return "$ahead${hours.time()}:${minutes.toLong().time()}"
        }
    }

    /**
     * Formats a Long to a string with a leading zero if needed.
     */
    fun Long.time(): String { // Add trailing zeroes and convert to string
        return this.absoluteValue.toString().padStart(2, '0')
    }

    /**
     * Calculates the flight's progress as a float between 0.0 and 1.0.
     * @return The flight's progress.
     */
    fun getProgress(): Float {
        val now = ZonedDateTime.now()
        val departTime = flightData.value.departDate

        viewModelScope.launch {
            val timeFromStart = Duration.between(now, departTime).toMillis()

            if (now < departTime) {
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

    /**
     * Deletes the current flight from the database.
     * @param flightDataDao The DAO for accessing flight data.
     */
    fun deleteFlight(
        flightDataDao: FlightDataDao,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            flightDataDao.deleteFlight(flightData.value)
        }
        openDialog.value = false
    }

    /**
     * Refreshes the flight data from the API.
     * @param flightDataDao The DAO for accessing flight data.
     * @param context The application context.
     * @param settings The API settings.
     * @param isRefreshing A mutable state to indicate if the data is being refreshed.
     */
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

    /**
     * Returns a string representing the duration until departure.
     * @param context The application context.
     * @return A string like "in 2d 3h" or "in 4h 5m".
     */
    fun getDuration(context: Context): String {
        val duration = Duration.between(
            ZonedDateTime.now(),
            flightData.value.departDate
        )

        val offset =
            mutableFloatStateOf(3600F) // 3 hours in seconds, potentially for check-in window start

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

    /**
     * Returns the end time for check-in.
     * @param context The application context.
     * @return "Ends at" string if check-in is active, otherwise empty.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun getEndTime(context: Context): String {
        val status = getStatus(context)

        if (status == context.getString(R.string.check_in)) {
            return context.getString(R.string.ends_at)
        }

        return ""
    }

    /**
     * Returns the current status of the flight.
     * @param context The application context.
     * @return "Check-in", "Flying", "Landed", or "Landing".
     */
    fun getStatus(context: Context): String {
        val duration = Duration.between(
            ZonedDateTime.now(),
            flightData.value.departDate
        )
        val seconds = duration.seconds

        if (seconds >= 3600) { // More than or equal to 1 hour until departure
            return context.getString(R.string.check_in) // Check-in
        } else { // Less than 1 hour until departure or already departed
            if (progress.floatValue in 0.9F..<1.0F) {
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
            contextForAssets.assets.open("tiles/${zoomLvl}/${col}/${row}.png")
        }
    private val mapSize = mapSizeAtLevel()

    /**
     * The state of the map, including layers, markers, and paths.
     */
    @OptIn(DelicateCoroutinesApi::class)
    val mapState = MapState(6, mapSize, mapSize).apply { // Max zoom level 6
        disableZooming()
        disableGestures()
        disableRotation()
        disableScrolling()
        addLayer(tileStreamProvider)
        GlobalScope.launch {
            // Scroll map to show both origin and destination
            snapScrollTo(
                x = avr(flightData.value.mapOriginX, flightData.value.mapDestinationX), // Center X
                y = avr(flightData.value.mapOriginY, flightData.value.mapDestinationY), // Center Y
            )
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
            addPath("route", color = scheme.primary, width = 2.dp) {
                // The y-offset of -0.0007 is likely a small visual adjustment for the path line
                // relative to the marker's anchor point.
                addPoint(x = flightData.value.mapOriginX, y = flightData.value.mapOriginY)
                addPoint(
                    x = flightData.value.mapDestinationX,
                    y = flightData.value.mapDestinationY
                )
            }
            addMarker(
                "origin",
                x = flightData.value.mapOriginX,
                y = flightData.value.mapOriginY,
            ) {
                CustomMapMarker()
            }
            addMarker(
                "destination",
                x = flightData.value.mapDestinationX,
                y = flightData.value.mapDestinationY,
            ) {
                CustomMapMarker()
            }
        }
    }

    private fun mapSizeAtLevel(): Int {
        return 256 * 2.0.pow(5).toInt() // Hardcoded zoom level 5 for map size calculation
    }

    /**
     * Calculates the average of two Double values.
     */
    fun avr(a: Double, b: Double): Double {
        return (a + b) / 2
    }

    /**
     * Calculates the appropriate map scale to fit two points on the screen.
     */
    fun calculateScale(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val zoomConstant = 12.0 // Empirically determined constant to adjust the overall zoom level.

        val a = (x2 - x1) * (x2 - x1) // Squared difference in x
        val b = (y2 - y1) * (y2 - y1) // Squared difference in y
        // The scale is inversely related to the distance.
        // sqrt(a+b) is the distance between the two points.
        return (1 / (sqrt(a + b) * zoomConstant))
    }

    /**
     * Factory for creating [DetailsViewModel] instances.
     */
    class Factory(
        private val context: Context,
        private val scheme: ColorScheme,
        private val scope: CoroutineScope
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailsViewModel(context, PreferencesInterface(context), scheme, scope) as T
        }
    }
}
