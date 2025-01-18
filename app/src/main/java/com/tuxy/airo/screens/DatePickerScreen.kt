package com.tuxy.airo.screens

import android.util.Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.tuxy.airo.composables.SmallAppBar
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.getData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
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
    var loading = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Picker)
    val timeMillis = maybe(datePickerState.selectedDateMillis)

    Scaffold(
        topBar = { SmallAppBar("", navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    loading.value = true
                    GlobalScope.launch(Dispatchers.Main) {
                        getData(flightNumber, data, getDateAsString(timeMillis))
                        joinAll()
                        loading.value = false
                        navController.navigateUp()
                        navController.navigateUp()
                    }

                },
                icon = { Icon(Icons.Filled.Check, "Add flight") },
                text = { Text(text = "Add Flight") },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            if(loading.value) {
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
    return time?.toLong() ?: 0
}

fun getDateAsString(time: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-DD", Locale.UK)
    return dateFormat.format(time)
}
