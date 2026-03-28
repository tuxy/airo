package com.tuxy.airo.data.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tuxy.airo.MainActivity
import com.tuxy.airo.R
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata_rework.CaughtException
import com.tuxy.airo.data.flightdata_rework.FlightData
import com.tuxy.airo.data.flightdata_rework.FlightDataBase
import com.tuxy.airo.data.flightdata_rework.FlightDataRequest
import com.tuxy.airo.data.flightdata_rework.Success
import com.tuxy.airo.screens.ApiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openapitools.client.models.FlightDirection
import org.openapitools.client.models.FlightSearchByEnum
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * A foreground service that handles periodic flight data updates.
 *
 * This service runs in the foreground to ensure updates are not interrupted by Android's
 * battery optimization. It shows a progress notification while updating and a completion
 * notification when finished.
 *
 * ## Notification Behavior
 * - While running: Shows indeterminate progress with "Updating flights…"
 * - On completion: Shows success/failure notification with flight count
 * - On flight time change: Triggers separate alarm notification via FlightAlarmScheduler
 *
 * ## Service Actions
 * - [ACTION_REFRESH]: Scheduled periodic refresh (triggered by AlarmManager)
 * - [ACTION_NETWORK_CHANGE]: Immediate refresh when network becomes available
 *
 * ## Lifecycle
 * - Started as foreground service to prevent being killed
 * - Self-stops after completing updates and showing completion notification
 *
 * @see AlarmSchedulerHelper for scheduling logic
 * @see NetworkCallbackHandler for network-based triggers
 */
class FlightRefreshService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "flight_refresh_channel"
        const val NOTIFICATION_ID = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002
        const val ACTION_REFRESH = "com.tuxy.airo.ACTION_REFRESH"
        const val ACTION_NETWORK_CHANGE = "com.tuxy.airo.ACTION_NETWORK_CHANGE"

        private const val LOG_TAG = "FlightRefreshService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(LOG_TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand called with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_REFRESH -> {
                Log.d(LOG_TAG, "Starting scheduled refresh")
                startForeground(NOTIFICATION_ID, createProgressNotification())
                refreshFlights()
            }
            ACTION_NETWORK_CHANGE -> {
                Log.d(LOG_TAG, "Starting network change refresh")
                startForeground(NOTIFICATION_ID, createProgressNotification())
                refreshFlights()
            }
            else -> {
                Log.d(LOG_TAG, "Starting default refresh")
                startForeground(NOTIFICATION_ID, createProgressNotification())
                refreshFlights()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Creates the notification channel for flight update notifications.
     * Uses IMPORTANCE_LOW to show progress without causing interruption.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Flight Updates",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress when updating flight data"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Creates the progress notification shown while flights are being updated.
     * Uses indeterminate progress (spinning indicator) since we don't know how long it will take.
     *
     * @return Notification with indeterminate progress indicator
     */
    private fun createProgressNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.updating_flights))
            .setContentText(getString(R.string.refreshing_flight_data))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Creates the completion notification shown after flight updates finish.
     *
     * @param success True if updates completed successfully
     * @param flightCount Number of flights that had time changes
     */
    private fun createCompletionNotification(success: Boolean, flightCount: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (success) {
            getString(R.string.flight_refresh_complete)
        } else {
            getString(R.string.update_error)
        }

        val content = if (success) {
            getString(R.string.flight_refresh_complete_desc, flightCount)
        } else {
            getString(R.string.update_error_desc)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    /**
     * Core refresh logic that:
     * 1. Fetches all flights from database
     * 2. For each flight within 7 days, checks for time changes via API
     * 3. If departure time changed, triggers alarm notification and updates database
     * 4. Reschedules all alarms after processing
     * 5. Shows completion notification and stops the service
     */
    private fun refreshFlights() {
        serviceScope.launch {
            Log.d(LOG_TAG, "Starting flight refresh")

            val flightDataDao = FlightDataBase.getDatabase(applicationContext).flightDataDao()
            val preferencesInterface = PreferencesInterface(applicationContext)
            val flightAlarmScheduler = FlightAlarmScheduler(applicationContext)

            val apiSettings = ApiSettings(
                choice = preferencesInterface.getValueFlowString("selected_api").first(),
                adbEndpoint = preferencesInterface.getValueFlowString("endpoint_adb").first(),
                adbKey = preferencesInterface.getValueFlowString("endpoint_adb_key").first(),
                server = preferencesInterface.getValueFlowString("endpoint_airoapi").first()
            )

            val originalFlights = flightDataDao.readAll()
            var updatedCount = 0

            for (oldFlight in originalFlights) {
                if (oldFlight.scheduledDepartDate < ZonedDateTime.now()) {
                    Log.d(LOG_TAG, "Ignored flight: ${oldFlight.callSign} (Past)")
                    continue
                }

                if (oldFlight.scheduledDepartDate > ZonedDateTime.now().plusDays(7)) {
                    continue
                }

                val request = FlightDataRequest()
                val result = request.getFlightOnSpecificDate(
                    searchParam = formatFlightNumber(oldFlight.callSign),
                    dateLocal = oldFlight.scheduledDepartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    searchBy = FlightSearchByEnum.Number,
                    dateLocalRole = FlightDirection.Departure,
                    withAircraftImage = true,
                    withLocation = true,
                    withFlightPlan = true,
                )

                when(result) {
                    is Success -> {
                        val newFlightData = FlightData().from(result.result[0] ?: continue)

                        if ((oldFlight.revisedDepartDate ?: oldFlight.scheduledDepartDate) != (newFlightData.revisedDepartDate ?: newFlightData.scheduledDepartDate)) {
                            Log.d(LOG_TAG, "Flight time changed: ${oldFlight.callSign}")
                            flightAlarmScheduler.setAlarmOnChange(oldFlight, newFlightData)
                            flightDataDao.deleteFlight(oldFlight)
                            flightDataDao.addFlight(newFlightData)
                            updatedCount++
                        }
                        Log.d(LOG_TAG, "Updated flight: ${newFlightData.callSign}")
                    }
                    is Error -> {
                        Log.e(LOG_TAG, "Failed to update flight: ${oldFlight.callSign} error: ${result.message}")
                    }
                    is CaughtException -> {
                        Log.e(LOG_TAG, "Failed to update flight: ${oldFlight.callSign} exception: ${result.exception}")
                    }
                    else -> {
                        Log.e(LOG_TAG, "Failed to update flight: ${oldFlight.callSign} unknown error")
                    }
                }
            }

            Log.d(LOG_TAG, "All flights processed. Resetting alarms.")
            flightAlarmScheduler.resetAll(flightDataDao)

            Log.d(LOG_TAG, "Flight refresh complete. Updated: $updatedCount flights")
            createCompletionNotification(true, updatedCount)

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(LOG_TAG, "Service destroyed")
    }
}

/**
 * Formats a flight number string by removing any spaces or hyphens.
 *
 * @param string The flight number string to be formatted.
 * @return The formatted flight number.
 */
fun formatFlightNumber(string: String): String {
    val splitString = string.split("[- ]".toRegex())
    if (splitString.size == 2) {
        return "${splitString[0]}${splitString[1]}"
    }
    return string
}