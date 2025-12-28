package com.tuxy.airo

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tuxy.airo.data.background.FlightAlarmScheduler
import com.tuxy.airo.data.background.FlightSchedulerWorker
import com.tuxy.airo.data.database.PreferencesInterface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class MainApplication : Application(), Configuration.Provider {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        val preferencesInterface = PreferencesInterface(this)

        super.onCreate()
        GlobalScope.launch(Dispatchers.IO) {
            preferencesInterface.getValueFlowBool("enable_alerts").collect { enabled ->
                if (enabled) {
                    runBlocking { setupStartupWork() }
                    setupRecurringWork()
                } else {
                    Log.d("FlightSchedulerWorker", "NOTE: Alerts have been disabled")
                }
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    @OptIn(DelicateCoroutinesApi::class)
    private fun setupRecurringWork() {
        // TODO read from PreferencesInterface for interval -> read MainActivity.kt
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun setupStartupWork() {
        val flightChangeWork = OneTimeWorkRequestBuilder<FlightSchedulerWorker>().build() // Force a recheck of all flights - basically a check for when the user is actively on the app
        val flightAlarmScheduler = FlightAlarmScheduler(applicationContext)
        WorkManager.getInstance(applicationContext).enqueue(flightChangeWork)

        flightAlarmScheduler.cancelAll()
    }
}