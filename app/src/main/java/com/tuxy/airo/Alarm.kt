package com.tuxy.airo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

data class Notification(
    val flight: String,
    val content: String,
)

class Alarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val notification = Notification(
                flight = intent.getStringExtra("flight")!!,
                content = intent.getStringExtra("content")!!
            )
            showNotification(context, notification)
        } catch (ex: Exception) {
            Log.d("Alarm", "onReceive: ${ex.printStackTrace()}")
        }
    }
}

/**
 * Shows a notification to the user.
 *
 * This function creates a notification channel (if it doesn't already exist)
 * and then builds and displays a notification with the provided flight information
 * and content.
 *
 * @param context The context from which to access system services.
 * @param notification The [Notification] data object containing the flight and content
 *                     for the notification.
 */
private fun showNotification(context: Context, notification: Notification) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "flight_alert_channel"
    val channelName = "Flight Alerts"

    val channel =
        NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
    manager.createNotificationChannel(channel)

    val builder = NotificationCompat.Builder(context, channelId)
        .setContentTitle(notification.flight)
        .setContentText(notification.content)
        .setSmallIcon(R.drawable.ic_launcher_foreground)

    manager.notify(1, builder.build())
}


