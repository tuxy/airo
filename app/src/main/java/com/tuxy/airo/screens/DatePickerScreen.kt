package com.tuxy.airo.screens

import android.widget.Toast
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
import com.tuxy.airo.composables.SmallAppBar
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.FlightDataError
import com.tuxy.airo.data.FlightDataFetchException
import com.tuxy.airo.data.getData
import com.tuxy.airo.viewmodel.DateViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

data class ApiSettings(
    val choice: String,
    val endpoint: String?,
    val key: String?,
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

    val timeMillis = viewModel.maybe(datePickerState.selectedDateMillis)

    // Server settings
    val settings = ApiSettings(
        viewModel.preferencesInterface.getValue("selected_api"),
        viewModel.preferencesInterface.getValue("endpoint_adb"),
        viewModel.preferencesInterface.getValue("endpoint_adb_key"),
        viewModel.preferencesInterface.getValue("endpoint_airoapi"),
    )

    Scaffold(
        topBar = { SmallAppBar("", navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("add_flight"),
                onClick = {
                    viewModel.loading = true

                    GlobalScope.launch(Dispatchers.Main) {
                        viewModel.loading = true
                        try {
                            val result = getData( // FlightRequest.kt
                                viewModel.formatFlightNumber(flightNumber),
                                flightDataDao,
                                viewModel
                                    .getDateAsString(timeMillis)!! // Unless the API is broken, should work fine.
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                // viewModel.toasts, // REMOVED
                                settings,
                                context,
                                false
                            )

                            if (result.isSuccess) {
                                // Optional: Toast.makeText(context, "Flight added successfully!", Toast.LENGTH_SHORT).show()
                                navController.navigateUp()
                                navController.navigateUp()
                            } else {
                                val exception = result.exceptionOrNull()
                                if (exception is FlightDataFetchException) {
                                    when (exception.errorType) {
                                        is FlightDataError.ApiKeyMissing -> viewModel.toasts[0].show()
                                        is FlightDataError.NetworkError -> viewModel.toasts[1].show()
                                        is FlightDataError.ParsingError -> viewModel.toasts[2].show()
                                        is FlightDataError.IncompleteDataError -> viewModel.toasts[2].show()
                                        is FlightDataError.FlightAlreadyExists -> viewModel.toasts[3].show()
                                        is FlightDataError.UnknownError -> viewModel.toasts[4].show()
                                        is FlightDataError.UpdateError -> viewModel.toasts[5].show()
                                        // Realistically, this won't happen, but needed to complete case
                                    }
                                } else {
                                    // Generic error for other unexpected exceptions
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.error_unknown),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                // No navigation on failure
                            }
                        } finally {
                            viewModel.loading = false
                        }
                    }
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
