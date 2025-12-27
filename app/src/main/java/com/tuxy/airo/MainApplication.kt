package com.tuxy.airo

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tuxy.airo.data.background.FlightAlarmScheduler
import com.tuxy.airo.data.background.FlightSchedulerWorker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        val startupJob = setupStartupWork()
        setupRecurringWork(startupJob)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    @OptIn(DelicateCoroutinesApi::class)
    private fun setupRecurringWork(startupJob: Job) {
        // TODO read from PreferencesInterface for interval -> read MainActivity.kt

        GlobalScope.launch(Dispatchers.IO) {
            startupJob.join()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val repeatingRequest = PeriodicWorkRequestBuilder<FlightSchedulerWorker>(
                12,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "flight-scheduler-worker",
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun setupStartupWork(): Job {
        val flightChangeWork = OneTimeWorkRequestBuilder<FlightSchedulerWorker>().build() // Force a recheck of all flights - basically a check for when the user is actively on the app
        val flightAlarmScheduler = FlightAlarmScheduler(applicationContext)
        WorkManager.getInstance(applicationContext).enqueue(flightChangeWork)

        return GlobalScope.launch(Dispatchers.IO) {
            flightAlarmScheduler.cancelAll()
        }
    }
}