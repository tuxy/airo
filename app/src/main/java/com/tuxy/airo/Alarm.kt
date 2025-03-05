package com.tuxy.airo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class Alarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val callSign = intent.getStringExtra("title")!!
            showNotification(context, callSign)
        } catch (ex: Exception) {
            Log.d("Alarm", "onReceive: ${ex.printStackTrace()}")
        }
    }
}

private fun showNotification(context: Context, title: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "flight_alert_channel"
    val channelName = "Flight Alerts"

    val channel =
        NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
    manager.createNotificationChannel(channel)

    val builder = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Prepare for flight $title")
        .setContentText("Flight departing in 6 hours. Check your bags :)")
        .setSmallIcon(R.drawable.ic_launcher_foreground)

    manager.notify(1, builder.build())
}


