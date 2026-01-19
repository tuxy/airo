package com.tuxy.airo.data.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tuxy.airo.R
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataDao
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Represents the data required for a flight-related notification.
 *
 * This data class holds the title (flight identifier) and the main content (message)
 * for a notification that will be displayed to the user.
 *
 * @property title The flight identifier, used as the notification's title.
 * @property content The detailed message of the notification, used as its content text.
 */
data class Notification(
    val title: String,
    val content: String,
) {
    /**
     * Shows a notification to the user.
     *
     * This function creates a notification channel (if it doesn't already exist)
     * and then builds and displays a notification with the provided flight information
     * and content.
     *
     * @param context The context from which to access system services.
     */
    fun showNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "flight_alert_channel"
        val channelName = "Flight Alerts"

        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(this.title)
            .setContentText(this.content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)

        manager.notify(1, builder.build())
    }
}

/**
 * Manages the scheduling and cancellation of flight departure alarms.
 *
 * This class provides methods to set an alarm for a specific flight and to cancel it.
 * Alarms are scheduled to trigger a notification a set amount of time before the flight's
 * departure.
 */
class FlightAlarmScheduler(val context: Context) {
    /**
     * Schedules a pre-flight reminder and a departure progress notification for a specific flight.
     *
     * A `FlightDataWorker` is scheduled to trigger a notification 6 hours before the flight's
     * departure. This only happens if the flight is more than 6 hours away. A `ProgressWorker`
     * is also scheduled via a private helper to show a notification at the time of departure.
     *
     * This uses `enqueueUniqueWork` with `ExistingWorkPolicy.REPLACE` to ensure that
     * only one alarm and one progress worker exist for each flight, using the flight's
     * call sign to create a unique name.
     *
     * @param flightData The flight for which to schedule the alarms.
     */
    suspend fun setAlarm(flightData: FlightData) {
        val preferencesInterface = PreferencesInterface(context)
        val timeFormatWait = preferencesInterface.getValueTimeFormat("24_time")

        val depTime = flightData.departDate
        val targetTime = depTime.toEpochSecond() - 21600 // 6 hours before

        val delay = targetTime - Instant.now().epochSecond

        if (delay > 0) { // If the flight is in the past, don't schedule an alarm
            val time = flightData.departDate.format(DateTimeFormatter.ofPattern(timeFormatWait))

            val flight = context.getString(R.string.flight_alert_title, "6")
            val content =
                "${context.getString(R.string.get_ready)} ${flightData.callSign} ${
                    context.getString(
                        R.string.to
                    )
                } ${flightData.toName} ${
                    context.getString(R.string.at)
                } $time"

            val data = Data.Builder()
                .putString(NotificationWorker.KEY_TITLE, flight)
                .putString(NotificationWorker.KEY_CONTENT, content)
                .build()

            val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(Duration.ofSeconds(delay)) // Sets delay for notification
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${flightData.callSign}-alarm",
                ExistingWorkPolicy.REPLACE,
                notificationWork
            ) // Starts work for 6-hour alarm

            Log.d(
                "FlightAlarmScheduler",
                "Scheduled alarm for flight: ${flightData.callSign} at ${flightData.departDate}"
            )
        }
    }

    /**
     * Schedules a progress notification for the exact departure time of a flight.
     *
     * A `ProgressWorker` is scheduled to display a notification with an indeterminate progress
     * bar when the flight is scheduled to depart. If the departure time is in the past,
     * no worker is scheduled.
     *
     * This uses `enqueueUniqueWork` with `ExistingWorkPolicy.REPLACE` to ensure that
     * only one progress worker exists for each flight, using the flight's call sign
     * to create a unique name.
     *
     * @param newFlightData The flight for which to schedule the progress notification.
     */
     suspend fun setProgressAlarm(oldFlightData: FlightData, newFlightData: FlightData) {
        val preferencesInterface = PreferencesInterface(context)
        val timeFormatWait = preferencesInterface.getValueTimeFormat("24_time")

        val depTime = newFlightData.departDate.toEpochSecond()

        val delay = depTime - Instant.now().epochSecond
        if (delay > 0) {
            val format = DateTimeFormatter.ofPattern(timeFormatWait)
            val arriveTime = newFlightData.arriveDate.format(format)


            val flight = "${context.getString(R.string.flight)} ${newFlightData.callSign}"
            val content =
                "${context.getString(R.string.landing)} ${context.getString(R.string.at)} $arriveTime"

            val data = Data.Builder()
                .putString(NotificationWorker.KEY_TITLE, flight)
                .putString(NotificationWorker.KEY_CONTENT, content)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ProgressWorker>()
                .setInitialDelay(Duration.ofSeconds(delay))
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${oldFlightData.callSign}-progress", // In case there is an old one
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Log.d("FlightAlarmScheduler", "Scheduled progress notification for flight: ${newFlightData.callSign}")
        }
    }

    /**
     * Schedules an immediate notification if a flight's departure time has changed.
     *
     * It compares the departure times of the old and new flight data. If they differ,
     * it enqueues a `FlightDataWorker` to notify the user about the change instantly.
     *
     * This uses `enqueueUniqueWork` with `ExistingWorkPolicy.REPLACE` to avoid
     * sending multiple notifications for the same change.
     *
     * @param previous The flight data before the change.
     * @param new The updated flight data.
     */
    suspend fun setAlarmOnChange(previous: FlightData, new: FlightData) {
        val preferencesInterface = PreferencesInterface(context)
        val timeFormatWait = preferencesInterface.getValueTimeFormat("24_time")

        val oldDepTime = previous.departDate
        val newDepTime = new.departDate

        if (newDepTime != oldDepTime) {
            val oldTime = oldDepTime.format(DateTimeFormatter.ofPattern(timeFormatWait))
            val newTime = newDepTime.format(DateTimeFormatter.ofPattern(timeFormatWait))

            val title = context.getString(R.string.flight_update)
            val content =
                "${context.getString(R.string.flight)} ${previous.callSign} ${context.getString(R.string.has_updated)} $oldTime ${context.getString(
                    R.string.to)} $newTime"

            val data = Data.Builder()
                .putString(NotificationWorker.KEY_TITLE, title)
                .putString(NotificationWorker.KEY_CONTENT, content)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${previous.callSign}-change",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    /**
     * Cancels all scheduled flight-related workers.
     *
     * This function calls `WorkManager.cancelAllWork()` to remove all pending and
     * active work requests managed by `WorkManager` for this app.
     */
    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWork()
    }

    /**
     * Re-schedules alarms for all flights currently stored in the database.
     *
     * This function iterates through all flights from the `FlightDataDao` and calls
     * `setAlarm` for each one. Due to the `ExistingWorkPolicy.REPLACE` policy in `setAlarm`,
     * this will update any existing alarms for those flights.
     *
     * Note: This does not cancel alarms for flights that may have been deleted from the database.
     * To cancel all alarms, use `cancelAll()`.
     *
     * @param flightDataDao The DAO used to retrieve the list of all flights.
     */
    suspend fun resetAll(flightDataDao: FlightDataDao) {
        val flightList = flightDataDao.readAll()
        for (i in flightList) {
            setAlarm(i)
        }
    }
}
