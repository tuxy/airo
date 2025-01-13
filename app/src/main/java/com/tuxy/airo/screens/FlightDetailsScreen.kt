package com.tuxy.airo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
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
            MainContent(navController)
        }
    }
}

@Composable
fun MainContent(navController: NavController) {
    Column {
        LinearProgressIndicator(
            progress = { 50F }
        )
        RouteBar()
        FlightInformationInteract(navController)
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
            modifier = Modifier.clickable(onClick = {}),
            headlineContent = { Text("Airport") },
            supportingContent = { Text("KSFO - Gate 5 Terminal 2") }
        )
        ListItem(
            modifier = Modifier.clickable(onClick = { navController.navigate(Screen.AircraftInformationScreen.route) }),
            headlineContent = { Text("Aircraft") },
            supportingContent = { Text("Boeing 777-300ER") }
        )
    }
}

@Composable
@Preview(showBackground = true)
fun FlightDetailsPreview() {
    FlightDetailsView(navController = rememberNavController())
}
