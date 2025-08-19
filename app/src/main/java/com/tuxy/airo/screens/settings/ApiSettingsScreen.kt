package com.tuxy.airo.screens.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ireward.htmlcompose.HtmlText
import com.tuxy.airo.R
import com.tuxy.airo.composables.LargeAppBar
import com.tuxy.airo.screens.settings.api.ApiServerView
import com.tuxy.airo.screens.settings.api.CustomApiView
import com.tuxy.airo.viewmodel.ApiSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsView(
    navController: NavController
) {
    val viewModelFactory = ApiSettingsViewModel.Factory(LocalContext.current)
    val viewModel: ApiSettingsViewModel = viewModel(factory = viewModelFactory)

    // Set up SegmentedButtons
    val options =
        listOf(
            stringResource(R.string.default_server),
            stringResource(R.string.airo_api_server),
            stringResource(R.string.direct_api)
        )
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
        topBar = { LargeAppBar("API Settings", navController) },
        floatingActionButton = {
            ApplyFAB(
                selectedIndex = selectedIndex,
                viewModel = viewModel,
                toast = toast,
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
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
            Spacer(modifier = Modifier.size(8.dp))
            AnimatedVisibility(selectedIndex == 0) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    HtmlText(
                        stringResource(R.string.default_server_extra),
                        style = TextStyle(color = Color.Gray)
                    )
                }
            }
            AnimatedVisibility(selectedIndex == 1) {
                ApiServerView(navController, viewModel)
            }
            AnimatedVisibility(selectedIndex == 2) {
                CustomApiView(navController, viewModel)
            }
        }
    }
}