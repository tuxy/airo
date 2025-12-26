package com.tuxy.airo

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tuxy.airo.data.background.FlightSchedulerWorker
import java.util.concurrent.TimeUnit

class MainApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        setupStartupWork()
        setupRecurringWork()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun setupRecurringWork() {
        // TODO read from PreferencesInterface for interval -> read MainActivity.kt

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<FlightSchedulerWorker>(
            24,
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

    private fun setupStartupWork() {
        val work = OneTimeWorkRequestBuilder<FlightSchedulerWorker>().build() // Force a recheck of all flights - basically a check for when the user is actively on the app
        WorkManager.getInstance(applicationContext).enqueue(work)
    }
}