package com.tuxy.airo

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tuxy.airo.data.background.ProgressNotification
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataDao
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZoneOffset
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
class AlarmController(val context: Context) {
    @OptIn(DelicateCoroutinesApi::class)
    fun setAlarm(flightData: FlightData) {
        val preferencesInterface = PreferencesInterface(context)
        var timeFormatWait = ""

        GlobalScope.launch {
            val timeFormat = preferencesInterface.getValueTimeFormat("24_time")
            timeFormatWait = timeFormat
        }

        val depTime =
            flightData.departDate
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(flightData.departTimeZone).toEpochSecond()

        if (depTime > (System.currentTimeMillis() + 21600000) / 1000) { // If the flight is within 6 hours, don't set alarm
            val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, FlightAlarm::class.java)

            val time = flightData.departDate.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(timeFormatWait))

            val flight = context.getString(R.string.flight_alert_title)
            val content =
                "${context.getString(R.string.get_ready)} ${flightData.callSign} ${context.getString(R.string.to)} ${flightData.toName} ${
                    context.getString(R.string.at)
                } $time"

            intent.putExtra("title", flight)
            intent.putExtra("content", content)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                flightData.id,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                (depTime - 21600) * 1000,
                pendingIntent
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun setProgressAlarm(flightData: FlightData) {
        val preferencesInterface = PreferencesInterface(context)
        var timeFormatWait = ""

        GlobalScope.launch {
            val timeFormat = preferencesInterface.getValueTimeFormat("24_time")
            timeFormatWait = timeFormat
        }

        val depTime =
            flightData.departDate
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(flightData.departTimeZone).toEpochSecond()

        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ProgressAlarm::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            flightData.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val time = flightData.departDate.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(timeFormatWait))

        val flight = "${context.getString(R.string.flight)} ${flightData.callSign}"
        val content =
            "${context.getString(R.string.landing)} ${context.getString(R.string.at)} $time"

        intent.putExtra("title", flight)
        intent.putExtra("content", content)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            (depTime) * 1000,
            pendingIntent
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun setAlarmOnChange(previous: FlightData, new: FlightData) {
        val preferencesInterface = PreferencesInterface(context)
        var timeFormatWait = ""

        GlobalScope.launch {
            val timeFormat = preferencesInterface.getValueTimeFormat("24_time")
            timeFormatWait = timeFormat
        } // Quite delicate

        val oldDepTime =
            previous.departDate
                .atZone(previous.departTimeZone)

        val newDepTime =
            new.departDate
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(new.departTimeZone)

        if (newDepTime != oldDepTime) {
            val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, FlightAlarm::class.java)

            val oldTime = oldDepTime.format(DateTimeFormatter.ofPattern(timeFormatWait))
            val newTime = newDepTime.format(DateTimeFormatter.ofPattern(timeFormatWait))

            val title = context.getString(R.string.flight_update)
            val content =
                "${context.getString(R.string.flight)} ${previous.callSign} ${context.getString(R.string.has_updated)} $oldTime ${context.getString(R.string.to)} $newTime"

            intent.putExtra("title", title)
            intent.putExtra("content", content)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                new.id,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() - 1,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(flightData: FlightData) {
        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FlightAlarm::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            flightData.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAll(flightDataDao: FlightDataDao) {
        val flightList = flightDataDao.readAll()
        for (i in flightList) {
            cancelAlarm(i)
        }
    }

    fun resetAll(flightDataDao: FlightDataDao) {
        val flightList = flightDataDao.readAll()
        for (i in flightList) {
            setAlarm(i)
        }
    }
}


/**
 * A [BroadcastReceiver] that handles scheduled alarms.
 *
 * This receiver is triggered when an alarm goes off. It extracts notification
 * data (flight and content) from the incoming [Intent] and displays a system
 * notification using the [Notification.showNotification] method.
 */
class FlightAlarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val notification = Notification(
                title = intent.getStringExtra("title")!!,
                content = intent.getStringExtra("content")!!
            )
            notification.showNotification(context)
        } catch (ex: Exception) {
            Log.d("FlightAlarm", "onReceive: ${ex.printStackTrace()}")
        }
    }
}

class ProgressAlarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val title = intent.getStringExtra("title")!!
            val content = intent.getStringExtra("content")!!
            ProgressNotification.show(
                context,
                title,
                content,
                100,
                0
            )
        } catch (ex: Exception) {
            Log.d("ProgressAlarm", "onReceive: ${ex.printStackTrace()}")
        }
    }
}