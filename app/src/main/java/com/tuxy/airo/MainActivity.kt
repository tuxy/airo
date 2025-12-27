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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.data.flightdata.FlightDataBase
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.ui.theme.AeroTheme
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    lateinit var data: FlightDataDao
    val preferencesInterface = PreferencesInterface(this)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val backup = RoomBackup(this)
        data = FlightDataBase.getDatabase(this).flightDataDao()

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

                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(backup)
                }
            }
        }
    }
}