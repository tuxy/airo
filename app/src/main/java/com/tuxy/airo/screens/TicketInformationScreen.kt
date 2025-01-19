package com.tuxy.airo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tuxy.airo.composables.LargeTopSmallBottom
import com.tuxy.airo.composables.RouteBar
import com.tuxy.airo.composables.SmallAppBar
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.singleIntoMut

@Composable
fun TicketInformationView(
    navController: NavController,
    id: String,
    flightDataDao: FlightDataDao
) {
    val flightData = remember { mutableStateOf(FlightData()) }
    singleIntoMut(flightData, flightDataDao, id)

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column( modifier = Modifier.padding(innerPadding) ) {
            SmallAppBar(flightData.value.callSign, navController)
            MainTicketView(flightData.value)
        }
    }
}

@Composable
fun MainTicketView(flightData: FlightData) {
    Column(
        modifier = Modifier,
    ) {
        RouteBar(flightData)
        Box(
            modifier = Modifier
                .padding(horizontal = 96.dp)
                .padding(vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
            ) {
                AsyncImage(
                    model = "",
                    contentDescription = "Ticket QR Code",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f/1f)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LargeTopSmallBottom("Terminal", flightData.ticketTerminal)
            LargeTopSmallBottom("Gate", flightData.ticketGate)
            LargeTopSmallBottom("Seat", flightData.ticketSeat)
        }
    }
}
