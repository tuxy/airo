package com.tuxy.airo.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.jamal.composeprefs3.ui.PrefsScreen
import com.tuxy.airo.R
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.data.settings.AppPreferences
import com.tuxy.airo.data.background.FlightWorkScheduler
import com.tuxy.airo.data.settings.dataStore
import com.tuxy.airo.screens.settings.prefs.SwitchPref

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NotificationsSettingsView(
    preferences: AppPreferences,
    paneNavigator: ThreePaneScaffoldNavigator<String>? = null,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar("Notification Settings", paneNavigator) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            PrefsScreen(LocalContext.current.dataStore) {
                prefsItem {
                    SwitchPref(
                        key = "enable_alerts",
                        title = stringResource(R.string.enable_flight_alerts),
                        defaultChecked = true,
                        enabled = true,
                        onCheckedChange = {
                            FlightWorkScheduler.enqueueRescheduleReminders(context)
                        }
                    )
                }
            }
        }
    }
}