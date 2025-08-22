package com.tuxy.airo.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.jamal.composeprefs3.ui.PrefsScreen
import com.tuxy.airo.R
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.dataStore
import com.tuxy.airo.screens.settings.prefs.SwitchPref

@Composable
fun LocaleSettingsView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar(stringResource(R.string.locale_settings), navController) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            PrefsScreen(LocalContext.current.dataStore) {
                prefsItem {
                    SwitchPref(
                        key = "24_time",
                        title = stringResource(R.string.time_setting),
                        defaultChecked = false,
                    )
                    SwitchPref(
                        key = "american_date",
                        title = stringResource(R.string.american_date),
                        defaultChecked = false,
                        enabled = true
                    )
                    SwitchPref(
                        key = "temperature_f",
                        title = stringResource(R.string.temperature_setting),
                        defaultChecked = false,
                        enabled = false,
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun LocaleSettingsViewPreview() {
    LocaleSettingsView(rememberNavController())
}