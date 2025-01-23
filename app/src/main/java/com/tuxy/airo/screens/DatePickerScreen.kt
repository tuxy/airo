package com.tuxy.airo.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.composables.SmallAppBar
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.getData
import com.tuxy.airo.viewmodel.DateViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun DatePickerView(
    navController: NavController,
    flightNumber: String,
    data: FlightDataDao
) {
    val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Picker)
    val timeMillis = maybe(datePickerState.selectedDateMillis)

    val viewModelFactory = DateViewModel.Factory(LocalContext.current)
    val viewModel: DateViewModel = viewModel(factory = viewModelFactory)

    viewModel.GetKey() // Get key from data store

    Scaffold(
        topBar = { SmallAppBar("", navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.loading = true
                    GlobalScope.launch(Dispatchers.Main) {
                        getData(
                            flightNumber,
                            data,
                            getDateAsString(timeMillis),
                            viewModel.toasts,
                            viewModel.key
                        )
                        joinAll()
                        viewModel.loading = false
                        navController.navigateUp()
                        navController.navigateUp()
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
                state = datePickerState
            )
        }
    }
}

fun maybe(time: Long?): Long {
    return time ?: 0
}

fun getDateAsString(time: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-DD", Locale.UK)
    return dateFormat.format(time)
}
