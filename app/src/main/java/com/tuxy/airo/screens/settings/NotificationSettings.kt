package com.tuxy.airo.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jamal.composeprefs3.ui.PrefsScreen
import com.tuxy.airo.R
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.PreferencesInterface
import com.tuxy.airo.dataStore
import com.tuxy.airo.screens.settings.prefs.SwitchPref
import com.tuxy.airo.viewmodel.settings.NotificationViewModel

@Composable
fun NotificationsSettingsView(
    navController: NavController,
    flightDataDao: FlightDataDao
) {
    val context = LocalContext.current
    val option = PreferencesInterface(context).getValueBool("enable_alerts")

    val viewModelFactory = NotificationViewModel.Factory(context, flightDataDao)
    val viewModel: NotificationViewModel = viewModel(factory = viewModelFactory)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar("Notification Settings", navController) },
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
                        onCheckedChange = { viewModel.check(option) }
                    )
                }
            }
        }
    }
}