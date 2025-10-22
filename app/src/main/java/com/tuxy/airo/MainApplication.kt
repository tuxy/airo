package com.tuxy.airo

import android.app.Application
import androidx.work.Configuration
import com.tuxy.airo.data.background.UpdateWorkerFactory

class MainApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(UpdateWorkerFactory())
            .build()
}