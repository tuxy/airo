package com.tuxy.airo.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ireward.htmlcompose.HtmlText
import com.jamal.composeprefs3.ui.PrefsScreen
import com.tuxy.airo.R
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.data.PreferencesInterface
import com.tuxy.airo.dataStore
import com.tuxy.airo.screens.settings.prefs.SingleSegmentedListPref
import com.tuxy.airo.screens.settings.prefs.TextFieldPref

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ApiSettingsView(
    navController: NavController
) {
    val preferencesInterface = PreferencesInterface(LocalContext.current)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar("API Settings", navController) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            val selectedIndex = preferencesInterface.getValueAsInt("selected_api")

            PrefsScreen(
                dataStore = LocalContext.current.dataStore,
                modifier = Modifier.height(400.dp)
            ) {
                prefsItem {
                    SingleSegmentedListPref(
                        key = "selected_api",
                        entries = mapOf(
                            "0" to stringResource(R.string.default_server),
                            "1" to stringResource(R.string.airo_api_server),
                            "2" to stringResource(R.string.direct_api)
                        ),
                        defaultValue = "0"
                    )
                    AnimatedVisibility(selectedIndex == 0) {
                        HtmlText(
                            stringResource(R.string.default_server_extra),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = TextStyle(color = Color.Gray)
                        )
                    }
                    AnimatedVisibility(selectedIndex == 1) {
                        TextFieldPref(
                            title = stringResource(R.string.airo_api_server),
                            key = "endpoint_airoapi",
                        )
                    }
                    AnimatedVisibility(selectedIndex == 2) {
                        Column {
                            HtmlText(
                                stringResource(R.string.adb_text),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = TextStyle(color = Color.Gray)
                            )
                            TextFieldPref(
                                title = stringResource(R.string.api_endpoint),
                                key = "endpoint_adb"
                            )
                            TextFieldPref(
                                title = stringResource(R.string.api_key),
                                key = "endpoint_adb_key"
                            )
                        }
                    }
                }
            }
        }
    }
}