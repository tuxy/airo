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
import androidx.compose.material3.LinearProgressIndicator
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
import com.tuxy.airo.composables.RouteBar
import com.tuxy.airo.composables.SmallAppBar
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.singleIntoMut

@Composable
fun FlightDetailsView(
    navController: NavController,
    id: String,
    flightDataDao: FlightDataDao
) {
    val flightData = remember { mutableStateOf(FlightData()) }
    singleIntoMut(flightData, flightDataDao, id)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { SmallAppBar("${flightData.value.from} to ${flightData.value.from}", navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Column {
                LinearProgressIndicator(
                    progress = { flightData.value.progress.toFloat() }
                )
                RouteBar(flightData.value)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Gray)
                ) {
                    AsyncImage(
                        model = "",
                        contentDescription = "Map view",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1280f/847f)
                    )
                }
                FlightInformationInteract(navController, flightData.value)
            }
        }
    }
}

@Composable
fun FlightInformationInteract(navController: NavController, flightData: FlightData) {
    Column {
        ListItem(
            modifier = Modifier.clickable(onClick = { navController.navigate(Screen.TicketInformationScreen.route) }),
            headlineContent = { Text("Ticket") },
            supportingContent = { Text(flightData.ticketSeat) }
        )
        ListItem(
            modifier = Modifier.clickable(onClick = { navController.navigate(Screen.AircraftInformationScreen.route) }),
            headlineContent = { Text("Aircraft") },
            supportingContent = { Text(flightData.aircraftName) }
        )
        ListItem(
            modifier = Modifier.clickable(onClick = {}),
            headlineContent = { Text("Airport") },
            supportingContent = { Text(flightData.fromName) },
            trailingContent = { Icon(Icons.Filled.Info, "Airport Information") }
        )
    }
}
