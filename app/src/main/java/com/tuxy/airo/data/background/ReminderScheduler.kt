package com.tuxy.airo.data.background

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tuxy.airo.MainActivity
import com.tuxy.airo.R
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.settings.AppPreferences
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Schedules the "prepare for your flight" reminder.
 *
 * Reminders are keyed by the database id (not the call sign), so two flights
 * with the same number on different dates no longer replace each other, and
 * deleting a flight can cancel exactly its own reminder.
 *
 * An exact alarm is used when the platform allows it (it is the only way to
 * survive Doze reliably). Otherwise a WorkManager one-time request is used as a
 * best-effort fallback.
 */
class ReminderScheduler(
    private val context: Context,
    private val preferences: AppPreferences,
) {
    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    suspend fun schedule(flight: FlightData) {
        if (flight.id == 0 || !preferences.isAlertsEnabled()) {
            cancel(flight)
            return
        }

        val departure = flight.effectiveDepartDate
        val now = ZonedDateTime.now()
        if (!departure.isAfter(now)) {
            cancel(flight)
            return
        }

        val timeFormat = preferences.timeFormat.first()
        val formatter = DateTimeFormatter.ofPattern(timeFormat)
        val title = context.getString(R.string.flight_alert_title, LEAD_HOURS.toString())
        val content = buildString {
            append(context.getString(R.string.get_ready))
            append(' ')
            append(flight.callSign)
            append(' ')
            append(context.getString(R.string.to))
            append(' ')
            append(flight.toName)
            append(' ')
            append(context.getString(R.string.at))
            append(' ')
            append(departure.format(formatter))
        }

        val trigger = departure.minus(Duration.ofHours(LEAD_HOURS.toLong()))
        val triggerAt = if (trigger.isAfter(now)) trigger else now.plusSeconds(5)

        if (canScheduleExactAlarms()) {
            scheduleExact(flight, triggerAt.toInstant(), title, content)
        } else {
            scheduleFallback(flight, triggerAt.toInstant(), title, content)
        }
    }

    fun cancel(flight: FlightData) {
        if (flight.id == 0) return
        alarmManager?.cancel(reminderIntent(flight))
        WorkManager.getInstance(context).cancelUniqueWork(workName(flight.id))
    }

    private fun scheduleExact(
        flight: FlightData,
        triggerAt: Instant,
        title: String,
        content: String,
    ) {
        val pendingIntent = reminderIntent(flight, title, content)
        val manager = alarmManager ?: return
        // If exact scheduling is not permitted for any reason, fall back rather
        // than letting the reminder disappear.
        runCatching {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt.toEpochMilli(),
                pendingIntent,
            )
        }.onFailure {
            scheduleFallback(flight, triggerAt, title, content)
        }
    }

    private fun scheduleFallback(
        flight: FlightData,
        triggerAt: Instant,
        title: String,
        content: String,
    ) {
        val delay = Duration.between(Instant.now(), triggerAt).toMillis().coerceAtLeast(0)
        val data = Data.Builder()
            .putInt(NotificationWorker.KEY_FLIGHT_ID, flight.id)
            .putString(NotificationWorker.KEY_TITLE, title)
            .putString(NotificationWorker.KEY_CONTENT, content)
            .build()
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(flight.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun reminderIntent(
        flight: FlightData,
        title: String? = null,
        content: String? = null,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_FLIGHT_ID, flight.id)
            if (title != null) putExtra(ReminderReceiver.EXTRA_TITLE, title)
            if (content != null) putExtra(ReminderReceiver.EXTRA_CONTENT, content)
        }
        return PendingIntent.getBroadcast(
            context,
            flight.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun workName(flightId: Int) = "flight-reminder-$flightId"

    companion object {
        const val LEAD_HOURS = 6
    }
}

/** Receives the exact alarm and posts the reminder notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val flightId = intent.getIntExtra(EXTRA_FLIGHT_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: return
        FlightNotifications.notifyReminder(context, flightId, title, content)
    }

    companion object {
        const val ACTION_REMINDER = "com.tuxy.airo.ACTION_FLIGHT_REMINDER"
        const val EXTRA_FLIGHT_ID = "flight_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONTENT = "content"
    }
}

/** Notification channels and the small set of notifications the app posts. */
object FlightNotifications {
    const val CHANNEL_REFRESH = "flight_refresh_channel"
    const val CHANNEL_ALERTS = "flight_alert_channel"

    private const val REFRESH_NOTIFICATION_ID = 1001
    private const val CHANGE_NOTIFICATION_BASE = 20_000

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REFRESH,
                context.getString(R.string.app_name) + " updates",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.refreshing_flight_data)
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.app_name) + " alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.enable_flight_alerts)
            }
        )
    }

    fun notifyReminder(context: Context, flightId: Int, title: String, content: String) {
        ensureChannels(context)
        val notification = baseBuilder(context, CHANNEL_ALERTS, title, content)
            .setAutoCancel(true)
            .build()
        notify(context, flightId.coerceAtLeast(1), notification)
    }

    fun notifyRefresh(context: Context, updated: Int, failed: Int) {
        if (updated == 0 && failed == 0) return
        ensureChannels(context)
        val title = if (updated > 0) {
            context.getString(R.string.flight_refresh_complete)
        } else {
            context.getString(R.string.update_error)
        }
        val text = if (updated > 0) {
            context.getString(R.string.flight_refresh_complete_desc, updated)
        } else {
            context.getString(R.string.update_error_desc)
        }
        val notification = baseBuilder(context, CHANNEL_REFRESH, title, text)
            .setAutoCancel(true)
            .build()
        notify(context, REFRESH_NOTIFICATION_ID, notification)
    }

    fun notifyChange(
        context: Context,
        flightId: Int,
        title: String,
        content: String,
    ) {
        ensureChannels(context)
        val notification = baseBuilder(context, CHANNEL_ALERTS, title, content)
            .setAutoCancel(true)
            .build()
        notify(context, CHANGE_NOTIFICATION_BASE + flightId, notification)
    }

    private fun baseBuilder(
        context: Context,
        channel: String,
        title: String,
        content: String,
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, channel)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}
