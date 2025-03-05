package com.tuxy.airo.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tuxy.airo.R
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.BoldDepartureAndDestinationText
import com.tuxy.airo.composables.LargeTopSmallBottom
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.viewmodel.MainFlightViewModel
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainFlightView(
    navController: NavController,
    flightDataDao: FlightDataDao,
) {
    val viewModel = viewModel<MainFlightViewModel>()
    viewModel.loadData(flightDataDao)

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { MainTopBar(navController, scrollBehavior) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navController.navigate(Screen.NewFlightScreen.route)
                },
                icon = { Icon(Icons.Filled.Add, stringResource(R.string.add_flight)) },
                text = { Text(stringResource(R.string.add_flight)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()),
        ) {
            if (!viewModel.flights.isEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(max = 2000.dp)
                ) {
                    viewModel.flights.forEach { (header, flights) ->

                        val unsortedFlights = flights.groupBy { flight ->
                            flight.departDate.toEpochSecond(ZoneOffset.UTC).toDouble()
                        }.toSortedMap()

                        stickyHeader {
                            DateHeader(
                                LocalDateTime.ofEpochSecond(
                                    header.toLong(), // Grouped date
                                    0,
                                    ZoneOffset.UTC
                                )
                            )
                        }

                        unsortedFlights.forEach { (_, flights) -> // Another list, with un-rounded values (Performance?)
                            items(flights) { flight ->
                                FlightCard(navController, flight, viewModel)
                            }
                        }
                    }
                }
                Spacer(Modifier.padding(56.dp))
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(360.dp))
                    Icon(
                        modifier = Modifier.size(100.dp),
                        imageVector = Icons.Filled.FlightTakeoff,
                        contentDescription = stringResource(R.string.add_flight),
                        tint = Color.Gray
                    )
                    Text(
                        stringResource(R.string.no_flight_smile),
                        color = Color.Gray,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Currently not used
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabRow(
    index: MutableIntState
) {
    PrimaryTabRow(
        selectedTabIndex = index.intValue,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp)
    ) {
        Tab(
            modifier = Modifier.padding(12.dp),
            selected = index.intValue == 0,
            onClick = { index.intValue = 0 },
            content = { Text(stringResource(R.string.upcoming_flights)) }
        )
        Tab(
            modifier = Modifier.padding(12.dp),
            selected = index.intValue == 1,
            onClick = { index.intValue = 1 },
            content = { Text(stringResource(R.string.past_flights)) }
        )
    }
}

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoldDepartureAndDestinationText(
                        flightData.from,
                        flightData.fromName,
                        Alignment.Start
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.to)
                    )
                    BoldDepartureAndDestinationText(flightData.to, flightData.toName, Alignment.End)
                }
                TicketInformationCard(flightData)
                LinearProgressIndicator(
                    progress = {
                        val now = Duration.between(LocalDateTime.now(), flightData.departDate)
                            .toMillis()

                        if (LocalDateTime.now() < flightData.departDate) {
                            0.0F
                        } else {
                            val duration = flightData.duration.toMillis()
                            abs(now.toFloat() / duration.toFloat())
                        }
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
        LargeTopSmallBottom(stringResource(R.string.flight), flight.callSign)
        LargeTopSmallBottom(stringResource(R.string.terminal), flight.terminal)
        LargeTopSmallBottom(stringResource(R.string.gate), flight.gate)
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

@Composable
fun DateHeader(time: LocalDateTime) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            time.format(DateTimeFormatter.ofPattern("dd MMM")),
            fontSize = 24.sp,
            fontWeight = FontWeight.W500
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    LargeTopAppBar(
        title = { Text(stringResource(R.string.my_flights)) },
        colors = TopAppBarDefaults.topAppBarColors(),
        actions = {
            IconButton(onClick = {
                navController.navigate(route = Screen.SettingsScreen.route)
            }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
