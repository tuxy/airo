package com.tuxy.airo.screens

import android.widget.Toast
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.tuxy.airo.composables.SmallAppBar
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.UserPreferences
import com.tuxy.airo.data.getData
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
    val loading = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Picker)
    val timeMillis = maybe(datePickerState.selectedDateMillis)

    val dataStore = UserPreferences(LocalContext.current)
    val retrievedKey = dataStore.getApiKey.collectAsState(initial = "")

    val toasts = arrayOf(
        Toast.makeText(LocalContext.current, "API Key not found", Toast.LENGTH_SHORT),
        Toast.makeText(LocalContext.current, "Network error / Invalid API Key", Toast.LENGTH_SHORT),
        Toast.makeText(LocalContext.current, "Could not find flight", Toast.LENGTH_SHORT)
    )

    Scaffold(
        topBar = { SmallAppBar("", navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    loading.value = true
                    GlobalScope.launch(Dispatchers.Main) {
                        getData(flightNumber, data, getDateAsString(timeMillis), toasts, retrievedKey)
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
    return time ?: 0
}

fun getDateAsString(time: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-DD", Locale.UK)
    return dateFormat.format(time)
}
