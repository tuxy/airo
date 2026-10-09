package com.tuxy.airo.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuxy.airo.R
import com.tuxy.airo.data.background.formatFlightNumber
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.CaughtException
import com.tuxy.airo.data.flightdata.Error
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.data.flightdata.FlightDataRequest
import com.tuxy.airo.data.flightdata.Success
import com.tuxy.airo.data.flightdata.bestFlight
import com.tuxy.airo.screens.ApiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.models.FlightDirection
import org.openapitools.client.models.FlightSearchByEnum
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

/**
 * ViewModel for the flight details screen.
 *
 * This ViewModel is responsible for fetching and holding the details of a specific flight,
 * calculating flight progress, handling flight deletion, and managing the map display.
 *
 * @property preferencesInterface For accessing user preferences.
 * @property flightData The flight data for the current flight.
 * @property openDialog A mutable state to control the visibility of the delete confirmation dialog.
 * @property progress The current progress of the flight (0.0 = not started, 1.0 = landed).
 * @param context The application context.
 * @param scheme The color scheme for the map.
 */
@Suppress("UNCHECKED_CAST")
class DetailsViewModel(
    context: Context,
    val preferencesInterface: PreferencesInterface,
    scheme: ColorScheme,
) : ViewModel() {
    private val contextForAssets = context

    var flightData = mutableStateOf(FlightData())
    var openDialog = mutableStateOf(false)

    private val _progress = MutableStateFlow(0.0F)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private var progressJob: Job? = null

    fun loadFlightById(id: String, dao: FlightDataDao) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = dao.readSingle(id.toInt()) ?: FlightData()
            withContext(Dispatchers.Main) {
                flightData.value = data
                // Recompute the flight progress immediately once the real flight is
                // loaded. Without this the progress indicator shows the value derived
                // from the placeholder FlightData() until the next 10s poll tick.
                _progress.value = calculateProgressFraction()
            }
        }
    }

    /** Starts periodic progress updates. Called when the flight data is first loaded. */
    fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = viewModelScope.launch {
            while (isActive) {
                _progress.value = calculateProgressFraction()
                delay(10.seconds)
            }
        }
    }

    /** Stops periodic progress updates. */
    fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * Calculates the flight's progress as a float between 0.0 and 1.0.
     */
    internal fun calculateProgressFraction(): Float {
        val now = ZonedDateTime.now()
        val departTime = flightData.value.revisedDepartDate
            ?: flightData.value.scheduledDepartDate

        if (now < departTime) return 0.0F

        val timeFromStart = Duration.between(departTime, now).toMillis()
        val duration = flightData.value.duration.toMillis()
        if (duration <= 0) return 0.0F

        return (timeFromStart.toFloat() / duration.toFloat()).coerceIn(0.0F, 1.0F)
    }

    /**
     * Calculates the time difference between the departure and arrival timezones.
     * @return A string representing the time difference, e.g., "+02:00".
     */
    fun getZoneDifference(): String {
        val departZoneSeconds = flightData.value.scheduledDepartDate.offset
        val arriveZoneSeconds = flightData.value.scheduledArriveDate.offset

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
     * Returns the flight's progress fraction. Used for the one-shot reading from UI.
     */
    fun getProgress(): Float = _progress.value

    /**
     * Deletes the current flight from the database.
     * @param flightDataDao The DAO for accessing flight data.
     */
    suspend fun deleteFlight(
        flightDataDao: FlightDataDao,
    ) {
        flightDataDao.deleteFlight(flightData.value)
        openDialog.value = false
    }

    /**
     * Refreshes the flight data from the API.
     * @param flightDataDao The DAO for accessing flight data.
     * @param settings The API settings.
     * @param isRefreshing A mutable state to indicate if the data is being refreshed.
     */
    fun refreshData(
        flightDataDao: FlightDataDao,
        settings: ApiSettings, // TODO Use the settings
        isRefreshing: MutableState<Boolean>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true

            val request = FlightDataRequest()
            val result = request.getFlightOnSpecificDate(
                searchParam = formatFlightNumber(flightData.value.callSign),
                dateLocal = flightData.value.scheduledDepartDate.format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
                ),
                searchBy = FlightSearchByEnum.Number,
                dateLocalRole = FlightDirection.Departure,
                withAircraftImage = true,
                withLocation = true,
                withFlightPlan = true,
            )

            when (result) {
                is Success -> {
                    val oldFlight = flightData.value
                    val newFlight = result.result.bestFlight()?.let { FlightData().from(it) }
                        ?.copy(id = oldFlight.id, ticketData = oldFlight.ticketData)

                    if (newFlight != null) {
                        flightData.value = newFlight
                        flightDataDao.updateFlight(newFlight)
                        Log.d("FlightDetails", "Refreshed flight: ${newFlight.callSign}")
                    } else {
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                contextForAssets,
                                contextForAssets.getString(R.string.refresh_no_data),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                is Error -> {
                    Log.e(
                        "FlightSchedulerWorker",
                        "Failed to refresh flight: ${flightData.value.callSign} error: ${result.result.message}"
                    )
                }

                is CaughtException -> {
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            contextForAssets,
                            contextForAssets.getString(R.string.refresh_network_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Log.e(
                        "FlightSchedulerWorker",
                        "Failed to refresh flight: ${flightData.value.callSign} exception: ${result.exception}"
                    )
                }
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
            flightData.value.revisedDepartDate ?: flightData.value.scheduledDepartDate
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
            flightData.value.revisedDepartDate ?: flightData.value.scheduledDepartDate
        )
        val seconds = duration.seconds
        val currentProgress = _progress.value

        if (seconds >= 3600) { // More than or equal to 1 hour until departure
            return context.getString(R.string.check_in) // Check-in
        } else { // Less than 1 hour until departure or already departed
            if (currentProgress in 0.9F..<1.0F) {
                return context.getString(R.string.landing) // About to land
            } else if (currentProgress >= 1.0F) {
                return context.getString(R.string.landed) // Already landed
            }
            return context.getString(R.string.flying) // Still flying
        }

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
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailsViewModel(context, PreferencesInterface(context), scheme) as T
        }
    }
}