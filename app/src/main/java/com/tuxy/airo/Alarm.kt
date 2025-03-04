package com.tuxy.airo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

data class Notification(
    val title: String,
    val desc: String,
    // TODO add image, map or flight details on bitmap
)

class Alarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            showNotification(context, "Test", "Notification")
        } catch (ex: Exception) {
            Log.d("Alarm", "onReceive: ${ex.printStackTrace()}")
        }
    }
}

private fun showNotification(context: Context, title: String, desc: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "message_channel"
    val channelName = "message_name"

    val channel =
        NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
    manager.createNotificationChannel(channel)

    val builder = NotificationCompat.Builder(context, channelId)
        .setContentTitle(title)
        .setContentText(desc)
        .setSmallIcon(R.drawable.ic_launcher_foreground)

    manager.notify(1, builder.build())
}


