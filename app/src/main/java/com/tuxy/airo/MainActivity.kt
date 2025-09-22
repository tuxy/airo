package com.tuxy.airo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tuxy.airo.data.background.UpdateWorker
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightDataBase
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.ui.theme.AeroTheme
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import java.util.concurrent.TimeUnit

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    lateinit var data: FlightDataDao

    val preferencesInterface = PreferencesInterface(this)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val backup = RoomBackup(this)
        data = FlightDataBase.getDatabase(this).flightDataDao()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false)
            .build()

        enableEdgeToEdge()
        setContent {
            val current = preferencesInterface.getValue("selected_api")
            val interval = if (current != "0") preferencesInterface.getValueFloat("update_interval") else 48f // Try to enforce 48h on pre-provided api
            AeroTheme {
                // Attempts to set up notification permissions
                if (Build.VERSION.SDK_INT >= 33) {
                    if (ContextCompat.checkSelfPermission(
                            LocalContext.current,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101
                        )
                    } // If the user denies notifications, we ignore forever
                }

                val periodicWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(interval.toLong(), TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(this).enqueueUniquePeriodicWork( // Only create once, keep existing and don't replace
                    "download_work",
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWorkRequest
                )

                MainScreen(backup)
            }
        }
    }
}

