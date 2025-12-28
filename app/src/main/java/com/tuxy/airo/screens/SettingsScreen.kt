package com.tuxy.airo.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.jamal.composeprefs3.ui.LocalPrefsDataStore
import com.tuxy.airo.R
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.dataStore
import com.tuxy.airo.screens.settings.SettingSub
import com.tuxy.airo.screens.settings.prefs.ButtonPref

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    navController: NavController,
    powerManager: PowerManager
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as Activity
    val preferencesInterface = PreferencesInterface(context)
    LocalPrefsDataStore = staticCompositionLocalOf { context.dataStore }

    val webpageString = stringResource(R.string.source_code)
    val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    // val ignored = preferencesInterface.getValueBool("is_ignoring_optimisation")
    val ignored = false

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar(stringResource(R.string.settings), navController) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            key(ignored) {
                if (!(isIgnoringBatteryOptimizations || ignored)) {
                    PowerMessage(
                        context,
                        activity
                    )
                }
            }
            SettingSub(
                stringResource(R.string.api_settings),
                Icons.Outlined.Link,
                stringResource(R.string.api_settings_desc),
                Screen.ApiSettingsScreen.route,
                navController
            )
            SettingSub(
                stringResource(R.string.notifications),
                Icons.Outlined.Notifications,
                stringResource(R.string.notifications_desc),
                Screen.NotificationsSettingsScreen.route,
                navController
            )
            SettingSub(
                stringResource(R.string.locale_settings),
                Icons.Outlined.LocationOn,
                stringResource(R.string.locale_settings_desc),
                Screen.LocaleSettingsScreen.route,
                navController
            )
            SettingSub(
                stringResource(R.string.backup_and_restore),
                Icons.Outlined.Save,
                stringResource(R.string.backup_and_restore_desc),
                Screen.BackupSettingsScreen.route,
                navController
            )
            ListItem(
                modifier = Modifier.clickable(onClick = {
                    openWebpage(context, webpageString)
                }),
                headlineContent = { Text(stringResource(R.string.about)) },
                supportingContent = { Text(stringResource(R.string.source_and_license)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.about)
                    )
                }
            )
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
fun PowerMessage(
    context: Context,
    activity: Activity
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        ListItem(
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 16.dp),
            headlineContent = { Text("Remove background optimisation") },
            supportingContent = { Text("Airo needs to remove background optimisations for its notifications to work properly.") },
            leadingContent = { Icon(imageVector = Icons.Outlined.Info, contentDescription = "test") },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            ButtonPref(
                key = "is_ignoring_optimisation",
                title = "Ignore",
                modifier = Modifier.padding(end = 8.dp)
            )
            Button(onClick = {
                // TODO Implement battery permission request
            }) {
                Text("Disable")
            }
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
