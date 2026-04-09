package com.tuxy.airo.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.jamal.composeprefs3.ui.LocalPrefsDataStore
import com.tuxy.airo.R
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.data.database.PreferencesInterface
import com.tuxy.airo.dataStore
import com.tuxy.airo.screens.settings.SettingSub
import com.tuxy.airo.screens.settings.prefs.ButtonPref
import kotlinx.coroutines.launch

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsView(
    powerManager: PowerManager,
    paneNavigator: ThreePaneScaffoldNavigator<String>? = null,
    onNavigateToSubSetting: ((SettingsSubPaneTypes) -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as Activity
    val preferencesInterface = PreferencesInterface(context)
    LocalPrefsDataStore = staticCompositionLocalOf { context.dataStore }

    val webpageString = stringResource(R.string.source_code)
    val isIgnoringBatteryOptimizations =
        powerManager.isIgnoringBatteryOptimizations(context.packageName)

    val ignored = preferencesInterface.getValueBool("is_ignoring_optimisation")

    val intent = Intent()
    intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    intent.setData(("package:" + context.packageName).toUri())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (paneNavigator != null) {
                LargeAppBar(stringResource(R.string.settings), paneNavigator)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            key(ignored, isIgnoringBatteryOptimizations) {
                if (!(isIgnoringBatteryOptimizations || ignored)) {
                    PowerMessage(
                        activity = activity,
                        intent = intent,
                        paneNavigator = paneNavigator
                    )
                }
            }
            SettingSub(
                name = stringResource(R.string.api_settings),
                icon = Icons.Outlined.Link,
                description = stringResource(R.string.api_settings_desc),
                location = "",
                onClick = if (onNavigateToSubSetting != null) {
                    { onNavigateToSubSetting(SettingsSubPaneTypes.Api) }
                } else null
            )
//            SettingSub( // Currently unused & not ready
//                name = stringResource(R.string.notifications),
//                icon = Icons.Outlined.Notifications,
//                description = stringResource(R.string.notifications_desc),
//                location = "",
//                onClick = if (onNavigateToSubSetting != null) {{ onNavigateToSubSetting(SettingsSubPaneTypes.Notifications) }} else null
//            )
//            SettingSub( // Currently unused & not ready, only 24hr time utilised
//                name = stringResource(R.string.locale_settings),
//                icon = Icons.Outlined.LocationOn,
//                description = stringResource(R.string.locale_settings_desc),
//                location = "",
//                onClick = if (onNavigateToSubSetting != null) {{ onNavigateToSubSetting("locale") }} else null
//            )
            SettingSub(
                name = stringResource(R.string.backup_and_restore),
                icon = Icons.Outlined.Save,
                description = stringResource(R.string.backup_and_restore_desc),
                location = "",
                onClick = if (onNavigateToSubSetting != null) {
                    { onNavigateToSubSetting(SettingsSubPaneTypes.Backup) }
                } else null
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
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PowerMessage(
    activity: Activity,
    intent: Intent,
    paneNavigator: ThreePaneScaffoldNavigator<String>? = null
) {
    val scope = rememberCoroutineScope()

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        ListItem(
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 16.dp),
            headlineContent = { Text(stringResource(R.string.remove_background_optimisation)) },
            supportingContent = { Text(stringResource(R.string.remove_background_optimisation_extra)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.info)
                )
            },
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
                title = stringResource(R.string.ignore),
                modifier = Modifier.padding(end = 8.dp)
            )
            Button(onClick = {
                activity.startActivity(intent)
                paneNavigator?.let {
                    scope.launch { it.navigateBack() }
                }
            }) {
                Text(stringResource(R.string.allow))
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
