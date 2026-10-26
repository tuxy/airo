package com.tuxy.airo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.jamal.composeprefs3.ui.LocalPrefsDataStore
import com.tuxy.airo.data.settings.dataStore
import com.tuxy.airo.di.AppContainer
import com.tuxy.airo.ui.theme.AeroTheme
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

class MainActivity : ComponentActivity() {

    private val powerManager: PowerManager by lazy {
        getSystemService(POWER_SERVICE) as PowerManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ComposePrefs3 exposes its DataStore as a global composition local and
        // requires it to be assigned once before any preference composable is
        // created.
        LocalPrefsDataStore = staticCompositionLocalOf { applicationContext.dataStore }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        requestNotificationPermissionIfNeeded()

        val container: AppContainer = (application as MainApplication).container
        val backup = RoomBackup(this)

        setContent {
            AeroTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        backup = backup,
                        powerManager = powerManager,
                        container = container,
                    )
                }
            }
        }
    }

    /**
     * Permission requests are a side effect and must not run from composition.
     * A denial is respected; Settings explains how to re-enable notifications.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST,
            )
        }
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 101
    }
}
