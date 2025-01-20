package com.tuxy.airo.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tuxy.airo.R
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.BoldDepartureAndDestinationText
import com.tuxy.airo.composables.DepartureAndDestinationText
import com.tuxy.airo.composables.LargeTopSmallBottom
import com.tuxy.airo.composables.RouteBar
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.dataIntoMut
import com.tuxy.airo.data.singleIntoMut

@Composable
fun MainFlightView(
    navController: NavController,
    flightDataDao: FlightDataDao
) {
    val flightData = remember { mutableStateOf(emptyList<FlightData>()) }
    val loaded = remember { mutableStateOf(false) } // has db has been loaded into flightData

    if(!loaded.value) {
        dataIntoMut(flightData, flightDataDao)
        loaded.value = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { MainTopBar(navController = navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navController.navigate(Screen.NewFlightScreen.route)
                },
                icon = { Icon(Icons.Filled.Add, "Add flight") },
                text = { Text(text = "Add Flight") },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .height(2000.dp)
                .verticalScroll(rememberScrollState())
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = 2000.dp)
            ) {
                items(flightData.value) { flight ->
                    FlightCard(navController, flight)
                }
            }
        }
    }
}

@Composable
fun FlightCard(
    navController: NavController,
    flightData: FlightData
) { // TODO Add parameters to create multiple separate instances of flights
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top main card
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {
                navController.navigate(
                    "${Screen.FlightDetailsScreen.route}/${flightData.id}"
                )
            }
        ) {
            Column(
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoldDepartureAndDestinationText(flightData.from, flightData.fromName, Alignment.Start)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "To"
                    )
                    BoldDepartureAndDestinationText(flightData.to, flightData.toName, Alignment.End)
                }
                TicketInformationCard(flightData)
                LinearProgressIndicator(
                    progress = { flightData.progress.toFloat() },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun FlightCardPreview() {
    FlightCard(
        rememberNavController(),
        FlightData(
            fromName = "Atlanta",
            from = "ATL",
            toName = "New York",
            callSign = "UA45",
            to = "JFK",
            ticketGate = "5",
            ticketTerminal = "2",
            localDepartDate = "29 Mar",
            localDepartTime = "11:50",
            localArriveTime = "16:52",
        )
    )
}

@Composable
fun TicketInformationCard(flight: FlightData) { // TODO How to get ticket information from ticket?
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LargeTopSmallBottom("Flight", flight.callSign) // TODO
        LargeTopSmallBottom("Terminal", flight.ticketTerminal)
        LargeTopSmallBottom("Gate", flight.ticketGate)
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = "avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(modifier: Modifier = Modifier, navController: NavController) {
    LargeTopAppBar(
        title = { Text("My Flights") },
        colors = TopAppBarDefaults.topAppBarColors(),
        actions = {
            IconButton(onClick = {
                navController.navigate(route = Screen.SettingsScreen.route)
            }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        modifier = modifier
    )
}
