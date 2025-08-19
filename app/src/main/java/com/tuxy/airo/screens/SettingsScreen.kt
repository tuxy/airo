package com.tuxy.airo.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.screens.settings.SettingSub

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    navController: NavController
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar("Settings", navController) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            SettingSub(
                "API Settings",
                Icons.Outlined.Link,
                "Select API to use",
                Screen.ApiSettingsScreen.route,
                navController
            )
            SettingSub(
                "Notifications",
                Icons.Outlined.Notifications,
                "Set alert & notification offsets",
                Screen.NotificationsSettingsScreen.route,
                navController
            )
            SettingSub(
                "Locale settings",
                Icons.Outlined.LocationOn,
                "Date, time and temperature formats",
                Screen.LocaleSettingsScreen.route,
                navController
            )
            SettingSub(
                "Backup & restore",
                Icons.Outlined.Save,
                "Manage backup location & app data",
                Screen.BackupSettingsScreen.route,
                navController
            )
            SettingSub(
                "About",
                Icons.Outlined.Info,
                "App related information",
                Screen.AboutSettingsScreen.route,
                navController
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SettingsPreview() {
    // SettingsView(rememberNavController())
    SettingSub("Test", Icons.Filled.NetworkWifi, "Test description", "", rememberNavController())
}
