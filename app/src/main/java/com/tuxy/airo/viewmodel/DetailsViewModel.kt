package com.tuxy.airo.viewmodel

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
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
import com.tuxy.airo.data.singleIntoMut
import com.tuxy.airo.viewmodel.DetailsViewModel.Factory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
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
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Manages and provides data for the flight details screen, including map display and flight status information.
 * The `@Suppress("UNCHECKED_CAST")` annotation is present due to the type casting in the [Factory] class.
 *
 * @property context The Android application context.
 * @property flightDataDao The Data Access Object for flight data.
 * @property id The unique identifier of the flight to be detailed.
 */
@Suppress("UNCHECKED_CAST")
class DetailsViewModel(context: Context, flightDataDao: FlightDataDao, id: String) : ViewModel() {
    /**
     * Holds the state for the current flight's details. It is initialized as an empty [FlightData]
     * object and populated via the `init` block.
     */
    var flightData = mutableStateOf(FlightData())

    /**
     * State for controlling a dialog. Its specific purpose is not detailed here but is likely
     * used for user interactions within the details screen.
     */
    var openDialog = mutableStateOf(false)

    /**
     * Represents the flight's progress, potentially as a fraction (e.g., 0.0 to 1.0),
     * used to determine flight status like "Landing".
     * It is privately set to control updates from within the ViewModel.
     */
    var progress = mutableFloatStateOf(0.0F)
        private set

    /**
     * Initializes the ViewModel by loading the specific flight's data using the provided [id]
     * from the [flightDataDao] and updating the [flightData] state.
     */
    init {
        singleIntoMut(
            flightData,
            flightDataDao,
            id
        ) // On initialisation, pass db data into flightData
    }

    /**
     * Calculates and formats a human-readable duration string indicating the time remaining
     * until check-in closes or until departure, depending on the flight's current status.
     *
     * The calculation logic varies based on the flight status:
     * - If the status is "Check-in" ([R.string.check_in]):
     *   - An `offset` of 10800 seconds (3 hours) is initially considered.
     *   - If the duration until departure is between 1 and 3 hours (inclusive of 1 hour, exclusive of 3 hours as per current logic due to "<=" & ">="),
     *     it returns an empty string, implying check-in is actively open and ending soon (handled by [getEndTime]).
     * - Otherwise (e.g., flight is not yet in check-in phase or is already flying):
     *   - The `offset` is set to 0, meaning the duration is calculated directly to the departure time.
     *   - If the flight has already departed (duration is zero or negative), it returns an empty string.
     *
     * The remaining `seconds` (after applying the offset) are then broken down into `days`, `hours`, and `minutes`.
     * The output string is formatted based on these components:
     * - If `days` >= 1: "in Xd Yh"
     * - Else if `hours` >= 1: "in Xh Ym"
     * - Else: "in Ym"
     *
     * @param context The Android application context, used for accessing string resources.
     * @return A formatted string representing the duration, or an empty string under specific conditions.
     */
    fun getDuration(context: Context): String {
        val duration = Duration.between(LocalDateTime.now(), flightData.value.departDate)

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

    /**
     * Calculates and formats the time when check-in ends, which is assumed to be 1 hour before departure.
     *
     * This function only returns a formatted time string if the current flight status is "Check-in"
     * ([R.string.check_in]). In all other cases (e.g., flight is already flying, landed, or not yet
     * in check-in period), it returns an empty string.
     *
     * The formatted time is "ends at HH:mm".
     *
     * @param context The Android application context, used for accessing string resources.
     * @return A formatted string indicating when check-in ends, or an empty string if not applicable.
     */
    fun getEndTime(context: Context): String {
        val status = getStatus(context)
        val time = flightData.value.departDate

        if (status == context.getString(R.string.check_in)) {
            val correctedTime = time.minusHours(1) // Check-in ends 1 hour before departure

            return "${context.resources.getString(R.string.ends_at)} ${
                correctedTime
                    .atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(flightData.value.departTimeZone)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            }"
        }

        return ""
    }

    /**
     * Determines and returns the current status of the flight (e.g., Check-in, Flying, Landing, Landed).
     *
     * The status is derived based on the duration until departure and the flight's [progress]:
     * - If the time until departure (`duration.seconds`) is 3600 seconds (1 hour) or more,
     *   the status is "Check-in" ([R.string.check_in]).
     * - Otherwise (less than 1 hour until departure or already departed):
     *   - If [progress] is between 0.9 and 1.0 (exclusive of 1.0), status is "Landing" ([R.string.landing]).
     *   - If [progress] is 1.0 or greater, status is "Landed" ([R.string.landed]).
     *   - Otherwise (e.g., progress < 0.9), status is "Flying" ([R.string.flying]).
     *
     * @param context The Android application context, used for accessing string resources.
     * @return A string representing the current flight status.
     */
    fun getStatus(context: Context): String {
        val duration = Duration.between(LocalDateTime.now(), flightData.value.departDate)
        val seconds = duration.seconds

        if (seconds >= 3600) { // More than or equal to 1 hour until departure
            return context.resources.getString(R.string.check_in) // Check-in
        } else { // Less than 1 hour until departure or already departed
            if (progress.floatValue >= 0.9F && progress.floatValue < 1.0F) {
                return context.resources.getString(R.string.landing) // About to land
            } else if (progress.floatValue >= 1.0F) {
                return context.resources.getString(R.string.landed) // Already landed
            }
            return context.resources.getString(R.string.flying) // Still flying
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

    /**
     * Initializes and configures the [MapState] for the `ovh.plrapps.mapcompose` library,
     * setting up the map's appearance and initial view.
     *
     * It uses `@OptIn(DelicateCoroutinesApi::class)` because it launches a coroutine on [GlobalScope]
     * to perform asynchronous setup of map elements like markers and paths. For production apps,
     * using a ViewModel-scoped coroutine (`viewModelScope`) is generally preferred for better
     * lifecycle management.
     *
     * The setup includes:
     * - Adding a tile layer using the local [tileStreamProvider].
     * - Adding markers for the flight's origin and destination:
     *   - Markers are represented by a `LocationOn` icon.
     *   - Icon size is fixed at `16.dp`.
     *   - Icon color is `Color.DarkGray`.
     * - Adding a path ("route") between origin and destination:
     *   - Path color is `Color.DarkGray` and width is `2.dp`.
     *   - Path points have a slight `y` offset of `-0.0007` from the marker coordinates, likely for
     *     visual adjustment to prevent the path from directly overlapping the center of the marker icon.
     * - Scrolling the map to an initial view that encompasses both origin and destination markers.
     *   The target center point is the average of origin/destination coordinates (calculated by [avr]),
     *   and the zoom scale is determined by [calculateScale].
     */
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
                addPoint(x = flightData.value.mapOriginX, y = flightData.value.mapOriginY - 0.0007)
                addPoint(
                    x = flightData.value.mapDestinationX,
                    y = flightData.value.mapDestinationY - 0.0007
                )
            }
            // Scroll map to show both origin and destination
            scrollTo(
                avr(flightData.value.mapOriginX, flightData.value.mapDestinationX), // Center X
                avr(flightData.value.mapOriginY, flightData.value.mapDestinationY), // Center Y
                calculateScale( // Calculated scale to fit points
                    flightData.value.mapOriginX,
                    flightData.value.mapOriginY,
                    flightData.value.mapDestinationX,
                    flightData.value.mapDestinationY
                )
            )
        }
    }

    /**
     * A factory class for creating instances of [DetailsViewModel].
     * This implements [ViewModelProvider.NewInstanceFactory] and is used to pass constructor
     * arguments to the ViewModel when it's being created by the Android ViewModel system.
     *
     * @property context The Android application context.
     * @property flightDataDao The Data Access Object for flight data.
     * @property id The unique identifier of the flight for which details are to be displayed.
     */
    class Factory(
        private val context: Context,
        private val flightDataDao: FlightDataDao,
        private val id: String,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailsViewModel(context, flightDataDao, id) as T
        }
    }

    /**
     * Calculates the total map size in pixels for a specific zoom level.
     * The current implementation hardcodes the zoom level to 5 for this calculation.
     * It assumes a tile size of 256 pixels. The formula is `tileSize * 2^zoomLevel`.
     *
     * @return The calculated map size in pixels.
     */
    private fun mapSizeAtLevel(): Int {
        return 256 * 2.0.pow(5).toInt() // Hardcoded zoom level 5 for map size calculation
    }

    /**
     * Calculates the average of two [Double] values.
     * @param a The first double value.
     * @param b The second double value.
     * @return The average of `a` and `b`.
     */
    private fun avr(a: Double, b: Double): Double {
        return (a + b) / 2
    }

    /**
     * Calculates an appropriate map scale (zoom factor) to fit two points (origin and destination)
     * within the view. The scale is inversely proportional to the distance between the points.
     *
     * @param x1 The x-coordinate of the first point.
     * @param y1 The y-coordinate of the first point.
     * @param x2 The x-coordinate of the second point.
     * @param y2 The y-coordinate of the second point.
     * @return A [Float] value representing the calculated map scale.
     */
    private fun calculateScale(x1: Double, y1: Double, x2: Double, y2: Double): Float {
        val zoomConstant = 12.0 // Empirically determined constant to adjust the overall zoom level.
        // A smaller value zooms out more, a larger value zooms in more.

        val a = (x2 - x1) * (x2 - x1) // Squared difference in x
        val b = (y2 - y1) * (y2 - y1) // Squared difference in y
        // The scale is inversely related to the distance.
        // sqrt(a+b) is the distance between the two points.
        return (1 / (sqrt(a + b) * zoomConstant)).toFloat()
    }
}
