package com.tuxy.airo.data.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tuxy.airo.MainApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Single owner of background scheduling.
 *
 * The old app had four independent refresh triggers (startup alarm, repeating
 * alarm, network callback foreground service, and WorkManager for reminders).
 * Everything now goes through WorkManager: a unique periodic refresh, unique
 * one-time refreshes (startup/network), and reminder rescheduling after boot.
 */
object FlightWorkScheduler {
    const val PERIODIC_WORK = "flight-refresh-periodic"
    const val ONE_TIME_WORK = "flight-refresh-now"
    private const val RESCHEDULE_WORK = "reschedule-reminders"

    fun enqueuePeriodic(context: Context, intervalHours: Long) {
        val request = PeriodicWorkRequestBuilder<FlightRefreshWorker>(
            intervalHours.coerceAtLeast(MIN_PERIODIC_HOURS),
            TimeUnit.HOURS,
        )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun enqueueOneTime(
        context: Context,
        uniqueName: String = ONE_TIME_WORK,
        force: Boolean = false,
        delaySeconds: Long = 30,
    ) {
        val request = OneTimeWorkRequestBuilder<FlightRefreshWorker>()
            .setConstraints(networkConstraints())
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setInputData(workDataOf(FlightRefreshWorker.KEY_FORCE to force))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueRescheduleReminders(context: Context) {
        val request = OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            RESCHEDULE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private const val MIN_PERIODIC_HOURS = 1L
}

/** Refreshes every tracked flight. Safe to retry; failures are isolated per flight. */
class FlightRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MainApplication ?: return Result.failure()
        val container = app.container
        val force = inputData.getBoolean(KEY_FORCE, false)

        if (!force) {
            val last = container.repository.lastRefreshInstant()
            if (last != null &&
                Duration.between(last, Instant.now()) < Duration.ofMinutes(MIN_REFRESH_AGE_MINUTES)
            ) {
                return Result.success()
            }
        }

        return try {
            val summary = container.repository.refreshAll()
            FlightNotifications.notifyRefresh(
                applicationContext,
                updated = summary.updated,
                failed = summary.failed,
            )
            summary.changes.forEach { change ->
                FlightNotifications.notifyChange(
                    applicationContext,
                    flightId = change.flightId,
                    title = change.title,
                    content = change.message,
                )
            }
            if (summary.failed > 0 && summary.updated == 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_FORCE = "force"
        private const val MIN_REFRESH_AGE_MINUTES = 15L
    }
}

/** Posts a pre-composed reminder when an exact alarm cannot be used. */
class NotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val content = inputData.getString(KEY_CONTENT) ?: return Result.failure()
        val flightId = inputData.getInt(KEY_FLIGHT_ID, 1)
        FlightNotifications.notifyReminder(applicationContext, flightId, title, content)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_CONTENT = "content"
        const val KEY_FLIGHT_ID = "flight_id"
    }
}

/** Re-creates exact alarms after a reboot or an app update. */
class RescheduleRemindersWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MainApplication ?: return Result.failure()
        val now = ZonedDateTime.now()
        val flights = app.container.repository.observeFlights().first()
        val enabled = app.container.preferences.isAlertsEnabled()
        flights
            .filter { it.id != 0 && it.effectiveDepartDate.isAfter(now) }
            .forEach { flight ->
                if (enabled) {
                    app.container.reminderScheduler.schedule(flight)
                } else {
                    app.container.reminderScheduler.cancel(flight)
                }
            }
        return Result.success()
    }
}
