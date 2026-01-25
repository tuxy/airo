package com.tuxy.airo.data.background

import android.content.Context
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
