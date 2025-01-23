package com.tuxy.airo.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.data.FlightData

// Used in Airport, Aircraft, Ticket and Flight information screens
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallAppBar(text: String, navController: NavController) {
    TopAppBar(
        title = { Text(text) },
        navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
            contentDescription = stringResource(R.string.to)
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
            fontSize = 12.sp,
            lineHeight = 2.sp
        )
        Text(
            bottom,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
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
fun BoldDepartureAndDestinationText(
    icao: String,
    fullName: String,
    alignment: Alignment.Horizontal
) {
    Column(
        modifier = Modifier.defaultMinSize(minWidth = 64.dp),
        horizontalAlignment = alignment
    ) {
        Text(
            fullName,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp
        )
        Text(
            icao,
            fontWeight = FontWeight.W500,
            fontSize = 24.sp
        )
    }
}
