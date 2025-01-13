package com.tuxy.airo.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.Screen

@Composable
fun NewFlightView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            FlightSearch(navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightSearch(navController: NavController) {

    val focusRequester = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }

    SearchBar(
        modifier = Modifier
            .focusRequester(focusRequester)
            .fillMaxSize(),
        query = query,
        onQueryChange = { query = it },
        onSearch = { // newQuery ->
            // TODO Apply logic and pass in newQuery
            navController.navigate(Screen.DatePickerScreen.route)
        },
        active = active,
        onActiveChange = { active = it },
        placeholder = { Text("Callsign") },
        leadingIcon = {
            IconButton(onClick = {
                navController.navigateUp()
            }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Back"
                )
            }
        },
    ) { }
}

@Composable
@Preview(showBackground = true)
fun NewFlightViewPreview() {
    NewFlightView(rememberNavController())
}
