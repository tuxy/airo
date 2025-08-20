package com.tuxy.airo.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jamal.composeprefs3.ui.PrefsScreen
import com.tuxy.airo.R
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.newDataStore
import com.tuxy.airo.screens.settings.prefs.SingleSegmentedListPref
import com.tuxy.airo.screens.settings.prefs.TextFieldPref
import com.tuxy.airo.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ApiSettingsView(
    navController: NavController
) {
    val viewModelFactory = SettingsViewModel.Factory()
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar("API Settings", navController) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            val selectedIndex = viewModel.getValueAsInt(LocalContext.current, "selected_api")

            PrefsScreen(
                dataStore = LocalContext.current.newDataStore,
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
                    )
                    AnimatedVisibility(selectedIndex == 1) {
                        TextFieldPref(
                            title = stringResource(R.string.airo_api_server),
                            key = "endpoint_airoapi",
                        )
                    }
                    AnimatedVisibility(selectedIndex == 2) {
                        Column {
                            Text(stringResource(R.string.adb_text))
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