package com.tuxy.airo.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata_rework.CaughtException
import com.tuxy.airo.data.flightdata_rework.Error
import com.tuxy.airo.data.flightdata_rework.FlightData
import com.tuxy.airo.data.flightdata_rework.FlightDataDao
import com.tuxy.airo.data.flightdata_rework.FlightDataRequest
import com.tuxy.airo.data.flightdata_rework.Success
import com.tuxy.airo.screens.ApiSettings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openapitools.client.models.FlightDirection
import org.openapitools.client.models.FlightSearchByEnum
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * A ViewModel for handling date and time related operations, and displaying toasts.
 *
 * @property preferencesInterface For accessing user preferences.
 * @property loading A mutable state to indicate if data is being loaded.
 * @param context The application context.
 */
@Suppress("UNCHECKED_CAST", "StaticFieldLeak") // Determined to (probably) not be an issue
class DateViewModel(
    private val context: Context
) : ViewModel() {
    // Initialise interface to preferences
    val preferencesInterface = PreferencesInterface(context)
    var loading by mutableStateOf(false)

    /**
     * Returns a Toast message based on the provided index.
     * @param idx The index of the toast message to be returned.
     * @return A Toast object.
     */
    fun toast(idx: Int): Toast {
        val toasts = arrayOf( // Toasts on error
            Toast.makeText(
                context,
                context.resources.getString(R.string.no_api),
                Toast.LENGTH_SHORT
            ),
            Toast.makeText(
                context,
                context.resources.getString(R.string.invalid_api_network),
                Toast.LENGTH_SHORT
            ),
            Toast.makeText(
                context,
                context.resources.getString(R.string.no_flight),
                Toast.LENGTH_SHORT
            ),
            Toast.makeText(
                context,
                context.resources.getString(R.string.flight_exists),
                Toast.LENGTH_SHORT
            ),
            Toast.makeText(
                context,
                context.resources.getString(R.string.error_unknown),
                Toast.LENGTH_SHORT
            ),
            Toast.makeText(
                context,
                context.resources.getString(R.string.update_error),
                Toast.LENGTH_SHORT
            ),
            Toast.makeText(
                context,
                context.resources.getString(R.string.no_flight),
                Toast.LENGTH_SHORT
            )
        )
        return toasts[idx]
    }

    /**
     * Converts a Long representing milliseconds since epoch to a ZonedDateTime.
     * @param time The time in milliseconds since epoch.
     * @return A ZonedDateTime object, or null if the conversion fails.
     */
    fun getDateAsString(time: Long): ZonedDateTime? {
        return LocalDateTime.ofEpochSecond(time / 1000L, 0, ZoneOffset.UTC)
            .atZone(ZoneOffset.systemDefault())
    }

    /**
     * Formats a flight number string by removing any spaces or hyphens.
     * @param string The flight number string to be formatted.
     * @return The formatted flight number.
     */
    fun formatFlightNumber(string: String): String {
        // Using " " or "-" as a space in between the carrier and flight number will be split either way
        val splitString = string.split("[- ]".toRegex())
        if (splitString.size == 2) {
            return "${splitString[0]}${splitString[1]}"
        }
        return string
    }

    /**
     * Adds a flight to the database.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun addFlight(
        navController: NavController,
        flightNumber: String,
        timeMillis: Long,
        flightDataDao: FlightDataDao,
        settings: ApiSettings
    ) { // TODO Settings
        viewModelScope.launch(Dispatchers.IO) {
            try {
                loading = true

                val urlChoice = when (settings.choice) {
                    "" -> "https://airoapi.tuxy.stream/" // When datastore hasn't initialised (user hasn't picked)
                    "0" -> "https://airoapi.tuxy.stream/"
                    "1" -> settings.server
                    "2" -> settings.adbEndpoint
                    else -> "https://airoapi.tuxy.stream/"
                } ?: "https://airoapi.tuxy.stream/"

                val request = FlightDataRequest(
                    baseUrl = urlChoice,
                    key = if (settings.choice == "2") settings.adbKey else null
                )

                val result = request.getFlightOnSpecificDate(
                    searchParam = formatFlightNumber(flightNumber),
                    dateLocal = getDateAsString(timeMillis)!!.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    searchBy = FlightSearchByEnum.Number,
                    dateLocalRole = FlightDirection.Departure,
                )

                when(result) {
                    is CaughtException -> {
                        println("exception ${result.exception}")
                    }
                    is Error -> {
                        println("error ${result.result}")
//                        Toast.makeText(
//                            context,
//                            result.result.message,
//                            Toast.LENGTH_LONG
//                        ).show()
                    }
                    is Success -> {
                        println("success ${result.result}")
                        val contract = result.result.firstOrNull()
                        if (contract != null) {
                            flightDataDao.addFlight(FlightData().from(contract))
                        }
                        else { toast(2).show() }
                    }
                }
            } finally {
                loading = false
                viewModelScope.launch(Dispatchers.Main) {
                    navController.navigateUp()
                    navController.navigateUp()
                }
            }
        }
    }

    /**
     * Factory for creating [DateViewModel] instances.
     */
    class Factory(
        private val context: Context,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DateViewModel(context) as T
        }
    }
}

