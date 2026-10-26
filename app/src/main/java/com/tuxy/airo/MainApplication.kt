package com.tuxy.airo

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.tuxy.airo.data.background.FlightNotifications
import com.tuxy.airo.data.background.FlightWorkScheduler
import com.tuxy.airo.data.background.NetworkCallbackHandler
import com.tuxy.airo.data.background.NetworkChangeReceiver
import com.tuxy.airo.data.settings.AppPreferences
import com.tuxy.airo.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * Owns the process-wide dependency container and the background schedule.
 *
 * There is exactly one refresh pipeline now: WorkManager. The application
 * observes the typed update interval and re-enqueues the unique periodic
 * worker when it changes. Network changes only enqueue a unique one-time
 * worker, so the app can never try to start a foreground service from the
 * background (which crashes on Android 12+).
 */
class MainApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var networkCallbackHandler: NetworkCallbackHandler? = null

    override fun onCreate() {
        super.onCreate()

        container = AppContainer(this)
        FlightNotifications.ensureChannels(this)

        applicationScope.launch {
            container.preferences.updateIntervalHours
                .distinctUntilChanged()
                .collect { hours ->
                    FlightWorkScheduler.enqueuePeriodic(this@MainApplication, hours)
                }
        }

        // Kick a refresh shortly after launch only when the data is stale. This
        // runs in WorkManager, so it never competes with the first frame and
        // never starts a foreground service from the background.
        applicationScope.launch {
            try {
                val intervalHours =
                    container.preferences.updateIntervalHours.first()
                val last = container.repository.lastRefreshInstant()
                val stale = last == null ||
                    Duration.between(last, Instant.now()).toHours() >= intervalHours
                if (stale) {
                    FlightWorkScheduler.enqueueOneTime(
                        this@MainApplication,
                        delaySeconds = STARTUP_REFRESH_DELAY_SECONDS,
                    )
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Startup refresh check failed", e)
            }
        }

        registerNetworkCallback()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    private fun registerNetworkCallback() {
        networkCallbackHandler = NetworkCallbackHandler(applicationContext)
        NetworkChangeReceiver.registerNetworkCallback(
            applicationContext,
            networkCallbackHandler!!,
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        networkCallbackHandler?.let {
            NetworkChangeReceiver.unregisterNetworkCallback(applicationContext, it)
        }
        applicationScope.cancel()
    }

    private companion object {
        const val LOG_TAG = "MainApplication"
        const val STARTUP_REFRESH_DELAY_SECONDS = 60L
    }
}
