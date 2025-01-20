package com.tuxy.airo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.SmallAppBar
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.singleIntoMut

@Composable
fun AircraftInformationView(
    navController: NavController,
    id: String,
    flightDataDao: FlightDataDao
) {
    val flightData = remember { mutableStateOf(FlightData()) }
    val loaded = remember { mutableStateOf(false) }

    if(!loaded.value) {
        singleIntoMut(flightData, flightDataDao, id)
        loaded.value = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column( modifier = Modifier.padding(innerPadding) ) {
            SmallAppBar("Aircraft Information", navController)
            ListItem(
                headlineContent = { Text(flightData.value.aircraftName) },
                supportingContent = { Text(flightData.value.airline) }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
            ) {
                AsyncImage(
                    model = flightData.value.aircraftUri,
                    contentDescription = "Aircraft",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1280f/847f)
                )
            }
            AircraftListView(navController, flightData.value)
        }
    }
}

@Composable
fun AircraftListView(navController: NavController, flightData: FlightData) {
    Column {
        ListItem(
            modifier = Modifier
                .clickable { navController.navigate("${Screen.WebViewScreen.route}/${flightData.airlineIata}") },
            headlineContent = { Text("Seat Maps") },
            trailingContent = { Icon(Icons.Filled.Info, "Seating information") }
        )
    }
}
