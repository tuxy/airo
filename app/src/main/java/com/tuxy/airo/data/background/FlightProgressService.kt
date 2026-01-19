package com.tuxy.airo.data.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tuxy.airo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlightProgressService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var notificationManager: NotificationManager

    companion object {
        // Notification Constants
        const val PROGRESS_NOTIFICATION_ID = 2
        const val CHANNEL_ID = "progress_notification_channel"
        const val CHANNEL_NAME = "Progress Notifications"

        // Intent Actions & Extras
        const val ACTION_START = "com.tuxy.airo.data.background.ACTION_START"
        const val ACTION_STOP = "com.tuxy.airo.data.background.ACTION_STOP"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_FLIGHT_ID = "extra_flight_id" // Use a unique ID for the flight
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Flight In Progress"
                val content = intent.getStringExtra(EXTRA_CONTENT) ?: "Tracking your flight."
                val flightId = intent.getStringExtra(EXTRA_FLIGHT_ID)

                // Start the service in the foreground
                startForeground(PROGRESS_NOTIFICATION_ID, createNotification(title, content, true))

                // Launch a coroutine to handle flight progress updates
                trackFlightProgress(flightId)
            }
            ACTION_STOP -> {
                // You can call this from your app when the flight has landed
                stopSelf()
            }
        }
        return START_STICKY // If the service is killed, it will restart
    }

    private fun trackFlightProgress(flightId: String?) {
        if (flightId == null) {
            stopSelf()
            return
        }

        scope.launch {
            while (isActive) {
                delay(60_000) // Placeholder
            }
            stopForeground(STOP_FOREGROUND_REMOVE) // Stop foreground and remove notification
            stopSelf() // Stop the service
        }
    }

    private fun createNotification(title: String, content: String, isOngoing: Boolean, progress: Int? = null) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Use LOW to avoid sound/vibration on update
            .setOngoing(isOngoing)
            .setOnlyAlertOnce(true)
            .apply {
                if (progress != null && isOngoing) {
                    setProgress(100, progress, false) // Determinate progress
                } else if (isOngoing) {
                    setProgress(100, 0, true) // Indeterminate progress
                } else {
                    setProgress(0, 0, false) // Remove progress bar
                }
            }
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    // Unused update function
    private fun updateNotification(title: String, content: String, isOngoing: Boolean, progress: Int? = null) {
        notificationManager.notify(PROGRESS_NOTIFICATION_ID, createNotification(title, content, isOngoing, progress))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        channel.description = "Shows notifications for flights currently in progress."
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all coroutines when completed
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }
}
