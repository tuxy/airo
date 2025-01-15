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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.RouteBar
import com.tuxy.airo.composables.SmallAppBar

@Composable
fun FlightDetailsView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { SmallAppBar("KSFO to KATL", navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Column {
                LinearProgressIndicator(
                    progress = { 50F }
                )
                RouteBar()
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
                FlightInformationInteract(navController)
            }
        }
    }
}

@Composable
fun DepartureAndDestinationText(icao: String, fullName: String) {
    Column {
        Text(
            fullName,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
        Text(icao)
    }
}

@Composable
fun FlightInformationInteract(navController: NavController) {
    Column {
        ListItem(
            modifier = Modifier.clickable(onClick = { navController.navigate(Screen.TicketInformationScreen.route) }),
            headlineContent = { Text("Ticket") },
            supportingContent = { Text("Seat 32A - Economy") }
        )
        ListItem(
            modifier = Modifier.clickable(onClick = { navController.navigate(Screen.AircraftInformationScreen.route) }),
            headlineContent = { Text("Aircraft") },
            supportingContent = { Text("Boeing 777-300ER") }
        )
        ListItem(
            modifier = Modifier.clickable(onClick = {}),
            headlineContent = { Text("Airport") },
            supportingContent = { Text("KSFO - Gate 5 Terminal 2") },
            trailingContent = { Icon(Icons.Filled.Info, "Airport Information") }
        )
    }
}

@Composable
@Preview(showBackground = true)
fun FlightDetailsPreview() {
    FlightDetailsView(navController = rememberNavController())
}
