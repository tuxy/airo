package com.tuxy.airo.screens

import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.R
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.screens.settings.ApiServerView
import com.tuxy.airo.screens.settings.CustomApiView
import com.tuxy.airo.viewmodel.SettingsViewModel

@Composable
fun SettingsView(
    navController: NavController
) {
    val viewModelFactory = SettingsViewModel.Factory(LocalContext.current)
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)

    // Set up SegmentedButtons
    val options =
        listOf(stringResource(R.string.airo_api_server), stringResource(R.string.direct_api))
    var selectedIndex by remember { mutableIntStateOf(0) }

    // Initialise settings (Don't get API key for security)
    viewModel.currentEndpoint = viewModel.getValue("ENDPOINT")
    viewModel.currentApiServer = viewModel.getValue("API_SERVER")

    val toast = Toast.makeText(
        LocalContext.current,
        stringResource(R.string.invalid_api_url),
        Toast.LENGTH_SHORT
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeAppBar(stringResource(R.string.settings), navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (selectedIndex) { // Parse and check for URL
                        0 -> {
                            if (!URLUtil.isValidUrl(viewModel.currentApiServer)) {
                                navController.navigateUp()
                                toast.show()
                                return@ExtendedFloatingActionButton
                            }
                        }

                        1 -> {
                            if (!URLUtil.isValidUrl(viewModel.currentApiKey)) {
                                navController.navigateUp()
                                toast.show()
                                return@ExtendedFloatingActionButton
                            }
                            if (!URLUtil.isValidUrl(viewModel.currentEndpoint)) {
                                navController.navigateUp()
                                toast.show()
                                return@ExtendedFloatingActionButton
                            }
                        }
                    }

                    viewModel.saveKey("API_CHOICE", selectedIndex.toString())
                    viewModel.saveKey("API_KEY", viewModel.currentApiKey)
                    viewModel.saveKey("ENDPOINT", viewModel.currentEndpoint)
                    viewModel.saveKey("API_SERVER", viewModel.currentApiServer)

                    navController.navigateUp()
                },
                icon = { Icon(Icons.Filled.Check, stringResource(R.string.apply_settings)) },
                text = { Text(stringResource(R.string.apply_settings)) },
            )
        }
    ) { innerPadding ->
        Column {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .fillMaxWidth()
            ) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index }
                    ) {
                        Text(label)
                    }
                }
            }
            AnimatedVisibility(selectedIndex == 0) {
                ApiServerView(navController, viewModel)
            }
            AnimatedVisibility(selectedIndex == 1) {
                CustomApiView(navController, viewModel)
            }
        }
    }
}

@Composable
fun Setting(name: String) {
    val value = remember { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontSize = 16.sp)
        Switch(
            onCheckedChange = { value.value = it },
            checked = value.value
        )
    }
}

@Composable
@Preview(showBackground = true)
fun SettingsPreview() {
    SettingsView(rememberNavController())
}
