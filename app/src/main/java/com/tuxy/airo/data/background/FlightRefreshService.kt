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
import com.tuxy.airo.data.flightdata.CaughtException
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataBase
import com.tuxy.airo.data.flightdata.FlightDataRequest
import com.tuxy.airo.data.flightdata.Success
import com.tuxy.airo.screens.ApiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openapitools.client.models.FlightDirection
import org.openapitools.client.models.FlightSearchByEnum
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Foreground service that handles flight data updates.
 * Shows progress notification while updating, completion notification when done.
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

        @Volatile
        private var isRefreshing = false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(LOG_TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand called with action: ${intent?.action}")

        if (isRefreshing) {
            Log.d(LOG_TAG, "Refresh already in progress, skipping")
            stopSelf()
            return START_NOT_STICKY
        }

        isRefreshing = true

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

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

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

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun refreshFlights() {
        serviceScope.launch {
            val preferencesInterface = PreferencesInterface(applicationContext)

            val lastRefreshString =
                preferencesInterface.getValueFlowString("last_refresh_time").first()
            val intervalString = preferencesInterface.getValueFlowString("update_interval").first()
            val intervalHours = intervalString?.toLongOrNull() ?: 12

            val shouldRefresh = lastRefreshString.isEmpty() ||
                    runCatching {
                        val lastRefresh = ZonedDateTime.parse(lastRefreshString)
                        val hoursSinceRefresh =
                            Duration.between(lastRefresh, ZonedDateTime.now()).toHours()
                        hoursSinceRefresh >= intervalHours
                    }.getOrDefault(true)

            if (!shouldRefresh) {
                Log.d(LOG_TAG, "Skipping refresh (last refresh was < ${intervalHours}h ago)")
                isRefreshing = false
                stopSelf()
                return@launch
            }

            Log.d(LOG_TAG, "Starting flight refresh")

            val flightDataDao = FlightDataBase.getDatabase(applicationContext).flightDataDao()
            val flightAlarmScheduler = FlightAlarmScheduler(applicationContext)

            val apiSettings = ApiSettings(
                choice = preferencesInterface.getValueFlowString("selected_api").first(),
                adbEndpoint = preferencesInterface.getValueFlowString("endpoint_adb").first(),
                adbKey = preferencesInterface.getValueFlowString("endpoint_adb_key").first(),
                server = preferencesInterface.getValueFlowString("endpoint_airoapi").first()
            )

            val originalFlights = flightDataDao.readAll().first()
            var processedCount = 0

            for (oldFlight in originalFlights) {
                val departDate = oldFlight.revisedDepartDate ?: oldFlight.scheduledDepartDate
                val arriveDate = oldFlight.revisedArriveDate ?: oldFlight.scheduledArriveDate
                val now = ZonedDateTime.now()

                if (arriveDate.isBefore(now)) {
                    Log.d(LOG_TAG, "Ignored flight: ${oldFlight.callSign} (Already arrived)")
                    continue
                }

                if (departDate.isAfter(now.plusDays(7))) {
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

                when (result) {
                    is Success -> {
                        val newFlightData = FlightData().from(result.result[0] ?: continue)

                        val oldTime = oldFlight.revisedDepartDate ?: oldFlight.scheduledDepartDate
                        val newTime =
                            newFlightData.revisedDepartDate ?: newFlightData.scheduledDepartDate

                        if (oldTime != newTime) {
                            Log.d(LOG_TAG, "Flight time changed: ${oldFlight.callSign}")
                            flightAlarmScheduler.setAlarmOnChange(oldFlight, newFlightData)
                        }

                        flightDataDao.deleteFlight(oldFlight)
                        flightDataDao.addFlight(newFlightData)
                        processedCount++
                        Log.d(LOG_TAG, "Processed flight: ${newFlightData.callSign}")
                    }

                    is Error -> {
                        Log.e(
                            LOG_TAG,
                            "Failed to update flight: ${oldFlight.callSign} error: ${result.message}"
                        )
                    }

                    is CaughtException -> {
                        Log.e(
                            LOG_TAG,
                            "Failed to update flight: ${oldFlight.callSign} exception: ${result.exception}"
                        )
                    }

                    else -> {
                        Log.e(
                            LOG_TAG,
                            "Failed to update flight: ${oldFlight.callSign} unknown error"
                        )
                    }
                }
            }

            Log.d(LOG_TAG, "All flights processed. Resetting alarms.")
            flightAlarmScheduler.resetAll(flightDataDao)

            Log.d(LOG_TAG, "Flight refresh complete. Processed: $processedCount flights")
            createCompletionNotification(true, processedCount)

            preferencesInterface.saveValue("last_refresh_time", ZonedDateTime.now().toString())

            isRefreshing = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRefreshing = false
        serviceScope.cancel()
        Log.d(LOG_TAG, "Service destroyed")
    }
}

fun formatFlightNumber(string: String): String {
    val splitString = string.split("[- ]".toRegex())
    if (splitString.size == 2) {
        return "${splitString[0]}${splitString[1]}"
    }
    return string
}