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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.SmallAppBarLegacy
import com.tuxy.airo.data.repository.FlightRepository
import com.tuxy.airo.viewmodel.AddFlightState
import com.tuxy.airo.viewmodel.DateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerView(
    navController: NavController,
    flightNumber: String,
    repository: FlightRepository,
) {
    val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Picker)
    val viewModel: DateViewModel = viewModel(
        factory = DateViewModel.Factory(repository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val selectedDateMillis = datePickerState.selectedDateMillis
    val snackbarHostState = remember { SnackbarHostState() }

    val duplicateMessage = stringResource(R.string.flight_exists)
    val noFlightMessage = stringResource(R.string.no_flight)
    val pickDateMessage = stringResource(R.string.pick_date_first)

    LaunchedEffect(state) {
        when (val current = state) {
            AddFlightState.Success -> {
                viewModel.resetState()
                navController.popBackStack(
                    route = Screen.MainFlightsScreen.route,
                    inclusive = false,
                )
            }

            AddFlightState.Duplicate -> {
                snackbarHostState.showSnackbar(duplicateMessage)
                viewModel.resetState()
            }

            AddFlightState.NoFlight -> {
                snackbarHostState.showSnackbar(noFlightMessage)
                viewModel.resetState()
            }

            is AddFlightState.Error -> {
                snackbarHostState.showSnackbar(current.message.ifBlank { pickDateMessage })
                viewModel.resetState()
            }

            AddFlightState.Idle,
            AddFlightState.Loading -> Unit
        }
    }

    Scaffold(
        topBar = { SmallAppBarLegacy("", navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("add_flight"),
                onClick = {
                    viewModel.addFlight(flightNumber, selectedDateMillis)
                },
                icon = { Icon(Icons.Filled.Check, stringResource(R.string.empty)) },
                text = { Text(stringResource(R.string.add_flight)) },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            if (state is AddFlightState.Loading) {
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
