package com.tuxy.airo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun FlightDetailsView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { FlightDetailsAppBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            MainContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsAppBar(navController: NavController) {
    TopAppBar(
        title = { Text("KSFO to KATL") },
        navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}

@Composable
fun MainContent() {
    Column {
        LinearProgressIndicator(
            progress = { 50F }
        )
        RouteBar()
        FlightInformationInteract()
    }
}

@Composable
fun RouteBar() {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
       horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DepartureAndDestinationText("KSFO", "San Francisco")
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "To"
        )
        DepartureAndDestinationText("KATL", "Atlanta")
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
fun FlightInformationInteract() {
    Column {
        ListItem(
            modifier = Modifier.clickable(onClick = {}),
            headlineContent = { Text("Ticket") },
            supportingContent = { Text("Seat 32A - Economy") }
        )
        ListItem(
            modifier = Modifier.clickable(onClick = {}),
            headlineContent = { Text("Airport") },
            supportingContent = { Text("KSFO - Gate 5 Terminal 2") }
        )
        ListItem(
            modifier = Modifier.clickable(onClick = {}),
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
