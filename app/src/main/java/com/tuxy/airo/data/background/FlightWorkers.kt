package com.tuxy.airo.data.background

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

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
 * notification with an indeterminate progress bar through FlightProgressService. This indicates to the user that
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

        val serviceIntent = Intent(applicationContext, FlightProgressService::class.java).apply {
            action = FlightProgressService.ACTION_START
            putExtra(FlightProgressService.EXTRA_TITLE, title)
            putExtra(FlightProgressService.EXTRA_CONTENT, content)
            putExtra(FlightProgressService.EXTRA_FLIGHT_ID, "flight_id") // TODO add actual flight id for multiple flights
        }

        applicationContext.startForegroundService(serviceIntent)

        Log.d("ProgressWorker", "Started FlightProgressService")

        return Result.success()
    }
}
