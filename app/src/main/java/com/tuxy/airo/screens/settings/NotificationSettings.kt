package com.tuxy.airo.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.jamal.composeprefs3.ui.PrefsScreen
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.dataStore
import com.tuxy.airo.screens.settings.prefs.SwitchPref

@Composable
fun NotificationsSettingsView(navController: NavController) {
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
                        title = "Enable flight alerts (WIP)",
                        defaultChecked = false,
                        enabled = false
                    )
                    SwitchPref(
                        key = "alert_urgency",
                        title = "Increase urgency (WIP)",
                        defaultChecked = false,
                        enabled = false
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun NotificationsSettingsViewPreview() {
    NotificationsSettingsView(rememberNavController())
}