package com.tuxy.airo

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.tuxy.airo.data.background.AlarmSchedulerHelper
import com.tuxy.airo.data.background.FlightAlarmScheduler
import com.tuxy.airo.data.background.NetworkCallbackHandler
import com.tuxy.airo.data.background.NetworkChangeReceiver
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Main application class that initializes app-wide components.
 *
 * Responsible for:
 * - Scheduling periodic flight refreshes using AlarmManager
 * - Performing immediate refresh on first launch
 * - Registering network change callbacks for real-time refresh triggers
 * - Cleaning up network callbacks on termination
 *
 * @see AlarmSchedulerHelper for periodic scheduling logic
 * @see NetworkCallbackHandler for network-based refresh triggers
 */
class MainApplication : Application(), Configuration.Provider {

    private var networkCallbackHandler: NetworkCallbackHandler? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        GlobalScope.launch(Dispatchers.IO) {
            runBlocking { setupStartupWork() }
            setupRecurringWork()
            registerNetworkCallback()
        }

        Log.d("MainApplication", "Application created and initialized")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    /**
     * Performs startup work when the application first launches.
     *
     * Tasks:
     * 1. Cancels all existing flight alarms (clean slate)
     * 2. Schedules an immediate refresh to fetch latest flight data
     */
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun setupStartupWork() {
        Log.d("MainApplication", "Running startup work - immediate refresh and alarm reset")

        val flightAlarmScheduler = FlightAlarmScheduler(applicationContext)
        flightAlarmScheduler.cancelAll()

        AlarmSchedulerHelper.scheduleImmediateRefresh(applicationContext)

        Log.d("MainApplication", "Startup work complete")
    }

    /**
     * Sets up recurring flight refresh using AlarmManager.
     *
     * Reads the update interval from preferences and schedules a periodic
     * alarm to trigger [FlightRefreshService] at regular intervals.
     */
    private suspend fun setupRecurringWork() {
        Log.d("MainApplication", "Setting up recurring work with AlarmManager")

        val preferencesInterface = com.tuxy.airo.data.database.PreferencesInterface(this)
        val intervalString = preferencesInterface.getValueFlowString("update_interval").first()
        val intervalHours = intervalString?.toLongOrNull() ?: 12

        AlarmSchedulerHelper.schedulePeriodicRefresh(applicationContext, intervalHours)

        Log.d("MainApplication", "Recurring work scheduled with ${intervalHours}hour interval")
    }

    /**
     * Registers network callback to trigger refreshes when network becomes available.
     */
    private fun registerNetworkCallback() {
        Log.d("MainApplication", "Registering network callback")
        networkCallbackHandler = NetworkCallbackHandler(applicationContext)
        NetworkChangeReceiver.registerNetworkCallback(applicationContext, networkCallbackHandler!!)
    }

    override fun onTerminate() {
        super.onTerminate()
        networkCallbackHandler?.let {
            NetworkChangeReceiver.unregisterNetworkCallback(applicationContext, it)
        }
        Log.d("MainApplication", "Application terminated")
    }
}