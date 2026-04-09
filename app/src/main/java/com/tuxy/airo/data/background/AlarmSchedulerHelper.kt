package com.tuxy.airo.data.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tuxy.airo.data.background.AlarmSchedulerHelper.LOG_TAG
import com.tuxy.airo.data.database.PreferencesInterface
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Helper object for scheduling flight refresh alarms using AlarmManager.
 *
 * Uses Android's AlarmManager for reliable periodic scheduling that survives
 * battery optimization and device reboots.
 *
 * ## Scheduling Types
 * - **Periodic**: Repeating alarms at configurable intervals (default 12 hours)
 * - **Immediate**: One-time alarm for immediate refresh
 *
 * ## Debugging
 * All operations log with tag [LOG_TAG]. Key log messages include:
 * - "Scheduling periodic refresh in Xhours at Y"
 * - "Periodic refresh scheduled successfully"
 * - "Scheduling immediate refresh"
 * - "Cancelling periodic refresh"
 *
 * @see FlightRefreshService for the service that handles alarm triggers
 * @see BootReceiver for rescheduling after device reboot
 */
object AlarmSchedulerHelper {

    private const val LOG_TAG = "AlarmSchedulerHelper"
    private const val REQUEST_CODE_REFRESH = 1001
    const val ACTION_ALARM_REFRESH = "com.tuxy.airo.ACTION_ALARM_REFRESH"

    /**
     * Schedules a periodic flight refresh using inexact repeating alarm.
     *
     * Uses [AlarmManager.setInexactRepeating] with [AlarmManager.RTC_WAKEUP] to wake
     * the device at approximately the specified interval. This is battery-efficient
     * as Android batches inexact alarms together.
     *
     * @param context Application context for accessing AlarmManager
     * @param intervalHours Interval between refreshes in hours (default 12)
     */
    fun schedulePeriodicRefresh(context: Context, intervalHours: Long = 12) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FlightRefreshService::class.java).apply {
            action = FlightRefreshService.ACTION_REFRESH
        }
        val pendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE_REFRESH,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = TimeUnit.HOURS.toMillis(intervalHours)
        val triggerTime = System.currentTimeMillis() + intervalMillis

        Log.d(LOG_TAG, "Scheduling periodic refresh in ${intervalHours}hours at ${triggerTime}")

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            intervalMillis,
            pendingIntent
        )

        Log.d(LOG_TAG, "Periodic refresh scheduled successfully")
    }

    /**
     * Schedules an immediate one-time refresh.
     *
     * Uses [AlarmManager.set] with [AlarmManager.RTC_WAKEUP] to trigger
     * a refresh immediately or as soon as possible.
     *
     * @param context Application context for accessing AlarmManager
     */
    fun scheduleImmediateRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FlightRefreshService::class.java).apply {
            action = FlightRefreshService.ACTION_REFRESH
        }
        val pendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE_REFRESH,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(LOG_TAG, "Scheduling immediate refresh")

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            pendingIntent
        )

        Log.d(LOG_TAG, "Immediate refresh scheduled")
    }

    /**
     * Cancels the periodic refresh alarm.
     *
     * @param context Application context for accessing AlarmManager
     */
    fun cancelPeriodicRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FlightRefreshService::class.java).apply {
            action = FlightRefreshService.ACTION_REFRESH
        }
        val pendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE_REFRESH,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(LOG_TAG, "Cancelling periodic refresh")

        alarmManager.cancel(pendingIntent)

        Log.d(LOG_TAG, "Periodic refresh cancelled")
    }

    /**
     * Reads the update interval from preferences and reschedules the periodic refresh.
     *
     * @param context Application context for accessing preferences
     */
    suspend fun rescheduleFromPreferences(context: Context) {
        val preferencesInterface = PreferencesInterface(context)
        val intervalString = preferencesInterface.getValueFlowString("update_interval").first()
        val intervalHours = intervalString?.toLongOrNull() ?: 12

        Log.d(LOG_TAG, "Rescheduling with interval: ${intervalHours}hours")

        cancelPeriodicRefresh(context)
        schedulePeriodicRefresh(context, intervalHours)
    }
}