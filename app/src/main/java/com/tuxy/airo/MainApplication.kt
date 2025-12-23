package com.tuxy.airo

import android.app.Application
import androidx.work.Configuration
import com.tuxy.airo.data.background.ProgressNotification
import com.tuxy.airo.data.background.UpdateWorkerFactory

class MainApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        ProgressNotification.createNotificationChannel(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(UpdateWorkerFactory())
            .build()
}