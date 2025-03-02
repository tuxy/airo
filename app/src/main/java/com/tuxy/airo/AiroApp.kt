package com.tuxy.airo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AiroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Oreo version check?
        val channel = NotificationChannel(
            NotificationService.ALERT_CHANNEL_ID,
            "alert",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Used for flight time alerts"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

class NotificationService(
    private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showAlert() {
        val activityIntent = Intent(context, MainActivity::class.java)
        val activityPendingIntent =
            PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Testing")
            .setContentIntent(activityPendingIntent)
            .build()

        notificationManager.notify(1, notification)
    }

    companion object {
        const val ALERT_CHANNEL_ID = "alert_channel_id"
    }
}
