package com.tuxy.airo.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.BoldDepartureAndDestinationText
import com.tuxy.airo.composables.LargeTopSmallBottom
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.viewmodel.MainFlightViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.absoluteValue

@Composable
fun MainFlightView(
    navController: NavController,
    flightDataDao: FlightDataDao
) {
    val viewModel = viewModel<MainFlightViewModel>()
    viewModel.loadData(flightDataDao)

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
                items(viewModel.flightData) { flight ->
                    FlightCard(navController, flight, viewModel)
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun FlightCard(
    navController: NavController,
    flightData: FlightData,
    viewModel: MainFlightViewModel
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top main card
        ElevatedCard(
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
                    verticalAlignment = Alignment.Bottom
                ) {
                    BoldDepartureAndDestinationText(
                        flightData.from,
                        flightData.fromName,
                        Alignment.Start
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "To"
                    )
                    BoldDepartureAndDestinationText(flightData.to, flightData.toName, Alignment.End)
                }
                TicketInformationCard(flightData)
                LinearProgressIndicator(
                    progress = {
                        GlobalScope.launch {
                            val now = Duration.between(LocalDateTime.now(), flightData.departDate)
                                .toMillis()

                            if (LocalDateTime.now() < flightData.departDate) {
                                viewModel.progress.floatValue = 0.0F
                                return@launch
                            }

                            val duration = flightData.duration.toMillis()

                            val current = now.toFloat() / duration.toFloat()

                            viewModel.progress.floatValue = current.absoluteValue
                            delay(10000)
                        }
                        viewModel.progress.floatValue
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun TicketInformationCard(flight: FlightData) {
    Row(
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
        AsyncImage(
            model = "https://raw.githubusercontent.com/Jxck-S/airline-logos/main/radarbox_logos/${flight.airlineIcao}.png",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(navController: NavController) {
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
    )
}

@Composable
@Preview(showBackground = true)
fun TopBarPreview() {
    MainTopBar(rememberNavController())
}
