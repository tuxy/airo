package com.tuxy.airo.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.R
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.screens.settings.SettingSub
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    navController: NavController
) {
    val context = LocalContext.current

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
            ListItem(
                modifier = Modifier.clickable(onClick = {
                    openWebpage(context, "https://github.com/tuxy/airo")
                }),
                headlineContent = { Text("About") },
                supportingContent = { Text("Source code") },
                leadingContent = { Icon(imageVector = Icons.Outlined.Info, contentDescription = stringResource(R.string.about)) }
            )
        }
    }
}

fun openWebpage(context: Context, url: String) {
    try {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true) //
            .setUrlBarHidingEnabled(true)
            .build()
        intent.launchUrl(context, url.toUri())
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(
            context,
            context.resources.getString(R.string.not_avail),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
@Preview(showBackground = true)
fun SettingsPreview() {
    SettingsView(rememberNavController())
}
