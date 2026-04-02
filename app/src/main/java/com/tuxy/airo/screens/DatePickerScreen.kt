package com.tuxy.airo.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.composables.SmallAppBarLegacy
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.viewmodel.DateViewModel
import kotlinx.coroutines.DelicateCoroutinesApi

data class ApiSettings(
    val choice: String,
    val adbEndpoint: String?,
    val adbKey: String?,
    val server: String?,
)

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun DatePickerView(
    navController: NavController,
    flightNumber: String,
    flightDataDao: FlightDataDao,
) {
    val context = LocalContext.current

    val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Picker)
    val viewModelFactory = DateViewModel.Factory(context)
    val viewModel: DateViewModel = viewModel(factory = viewModelFactory)

    val timeMillis = datePickerState.selectedDateMillis ?: 0

    // Server settings
    val settings = ApiSettings(
        viewModel.preferencesInterface.getValue("selected_api"),
        viewModel.preferencesInterface.getValue("endpoint_adb"),
        viewModel.preferencesInterface.getValue("endpoint_adb_key"),
        viewModel.preferencesInterface.getValue("endpoint_airoapi"),
    )

    Scaffold(
        topBar = { SmallAppBarLegacy("", navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("add_flight"),
                onClick = {
                    viewModel.addFlight(
                        navController,
                        flightNumber,
                        timeMillis,
                        flightDataDao,
                        settings
                    )
                    viewModel.loading = true
                },
                icon = { Icon(Icons.Filled.Check, stringResource(R.string.empty)) },
                text = { Text(stringResource(R.string.add_flight)) },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            if (viewModel.loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            DatePicker(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(28.dp)),
                state = datePickerState
            )
        }
    }
}
