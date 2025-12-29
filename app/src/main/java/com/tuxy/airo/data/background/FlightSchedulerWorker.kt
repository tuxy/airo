package com.tuxy.airo.data.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightDataBase
import com.tuxy.airo.data.flightdata.getData
import com.tuxy.airo.screens.ApiSettings
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * A [CoroutineWorker] that periodically scans the list of flights, updates them from a remote source,
 * and schedules the necessary workers (FlightAlarm for departure time changes and FlightDataWorker for progress updates) for each flight. This ensures that alarms are not lost and that
 * flight data is kept up-to-date.
 * Please note that the usages and functions for FlightAlarm and FlightWorkers overlaps a lot and need to be worked around
 */
class FlightSchedulerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val flightDataDao = FlightDataBase.getDatabase(applicationContext).flightDataDao()
        val preferencesInterface = PreferencesInterface(applicationContext)
        val flightAlarmScheduler = FlightAlarmScheduler(applicationContext)

        // Get API settings from preferences
        val apiSettings = ApiSettings(
            choice = preferencesInterface.getValueFlowString("selected_api").first(),
            adbEndpoint = preferencesInterface.getValueFlowString("endpoint_adb").first(),
            adbKey = preferencesInterface.getValueFlowString("endpoint_adb_key").first(),
            server = preferencesInterface.getValueFlowString("endpoint_airoapi").first()
        )

        Log.d("FlightSchedulerWorker", "Starting flight data update.")

        val originalFlights = flightDataDao.readAll()
        for (oldFlight in originalFlights) {
            // Ignore flight if depart date has already passed
            if (oldFlight.departDate < ZonedDateTime.now()) {
                Log.d("FlightSchedulerWorker", "Ignored flight: ${oldFlight.callSign} (Past)")
                continue
            }

            // Ignore flight if it's more than a week away to save API calls
            if (oldFlight.departDate > ZonedDateTime.now().plusDays(7)) {
                continue
            }

            // Get updated flight data
            val result = getData(
                flightNumber = formatFlightNumber(oldFlight.callSign),
                flightDataDao = flightDataDao,
                date = oldFlight.departDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                settings = apiSettings,
                context = applicationContext,
                update = true
            )

            result.onSuccess { newFlightData ->
                // Notify the user if the flight time has changed. NOTE: this is the only change that occurs
                if (oldFlight.departDate != newFlightData.departDate) {
                    Log.d("FlightSchedulerWorker", "Flight time changed: ${oldFlight.callSign}")
                    flightAlarmScheduler.setAlarmOnChange(oldFlight, newFlightData)
                    // Update the database
                    flightDataDao.deleteFlight(oldFlight)
                    flightDataDao.addFlight(newFlightData)
                }
                Log.d("FlightSchedulerWorker", "Updated flight: ${newFlightData.callSign}")
            }.onFailure {
                Log.e("FlightSchedulerWorker", "Failed to update flight: ${oldFlight.callSign}", it)
            }
        }

        // After updating all flights, reset and schedule all alarms
        Log.d("FlightSchedulerWorker", "All flights updated. Resetting alarms.")
        flightAlarmScheduler.resetAll(flightDataDao)

        return Result.success()
    }
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