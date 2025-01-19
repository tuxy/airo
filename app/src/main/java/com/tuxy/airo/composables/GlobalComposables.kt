package com.tuxy.airo.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tuxy.airo.data.FlightData

// Used in Airport, Aircraft, Ticket and Flight information screens
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallAppBar(text:String, navController: NavController) {
    TopAppBar(
        title = { Text(text) },
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

// Used in main flight screen and settings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeAppBar(text: String, navController: NavController) {
    LargeTopAppBar(
        title = { Text(text) },
        navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        }
    )
}

// Used in Flight, Ticket and main screen
@Composable
fun RouteBar(flightData: FlightData) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DepartureAndDestinationText(flightData.from, flightData.fromName)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "To"
        )
        DepartureAndDestinationText(flightData.to, flightData.toName)
    }
}

// Used in main flight and ticket screen
@Composable
fun LargeTopSmallBottom(top: String, bottom: String) {
    Column {
        Text(
            top,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
        Text(bottom)
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
