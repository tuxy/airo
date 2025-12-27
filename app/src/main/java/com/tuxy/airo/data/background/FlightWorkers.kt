package com.tuxy.airo.data.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tuxy.airo.R

/**
 * A [CoroutineWorker] that displays a simple notification with a title and content.
 *
 * This worker retrieves the title and content from the input data and uses them to
 * create and display a standard notification. This is used for general flight alerts,
 * such as departure reminders and updates.
 */
class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE)
        val content = inputData.getString(KEY_CONTENT)

        if (title.isNullOrEmpty() || content.isNullOrEmpty()) {
            return Result.failure()
        }

        Notification(title, content).showNotification(applicationContext)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_CONTENT = "content"
    }
}

/**
 * A [CoroutineWorker] that displays a progress notification.
 *
 * This worker is triggered at the scheduled departure time of a flight to show a
 * notification with an indeterminate progress bar. This indicates to the user that
 * the flight is in progress.
 */
class ProgressWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(NotificationWorker.KEY_TITLE)
        val content = inputData.getString(NotificationWorker.KEY_CONTENT)

        if (title.isNullOrEmpty() || content.isNullOrEmpty()) {
            return Result.failure()
        }

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "progress_notification_channel"
        val channelName = "Progress Notifications"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, true) // Indeterminate progress
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(2, builder.build()) // Use a different ID

        return Result.success()
    }
}
