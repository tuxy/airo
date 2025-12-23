package com.tuxy.airo.screens

import android.content.Context
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.R
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.screens.settings.SettingSub

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    navController: NavController
) {
    val context = LocalContext.current
    val webpageString = stringResource(R.string.source_code)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar(stringResource(R.string.settings), navController) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
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
