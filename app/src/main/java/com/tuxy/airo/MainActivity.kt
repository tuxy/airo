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
import com.tuxy.airo.data.FlightDataBase
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.ui.theme.AeroTheme
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    lateinit var data: FlightDataDao

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val backup = RoomBackup(this)
        data = FlightDataBase.getDatabase(this).flightDataDao()

        enableEdgeToEdge()
        setContent {
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

                MainScreen(backup)
            }
        }
    }
}

