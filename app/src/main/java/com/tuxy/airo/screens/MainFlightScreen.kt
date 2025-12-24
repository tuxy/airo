package com.tuxy.airo.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.tuxy.airo.R
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.BoldDepartureAndDestinationText
import com.tuxy.airo.composables.LargeTopSmallBottom
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.viewmodel.MainFlightViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainFlightView(
    navController: NavController,
    flightDataDao: FlightDataDao,
    onFlightClick: (String) -> Unit = {}
) {
    val viewModelFactory = MainFlightViewModel.Factory(LocalContext.current)
    val viewModel: MainFlightViewModel = viewModel(factory = viewModelFactory)

    val pagerState = rememberPagerState(pageCount = { 2 })
    val selectedTabIndex = remember { derivedStateOf { pagerState.currentPage } }
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf(viewModel.loadData(flightDataDao)) }

    LaunchedEffect(key) {
        viewModel.loadData(flightDataDao)
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { MainTopBar(navController, scrollBehavior, viewModel) },
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
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (viewModel.flightData.isEmpty()) {
                NoFlight(Modifier
                    .fillMaxSize()
                    .padding(96.dp))
            } else {
                TabRow(
                    selectedTabIndex.value,
                    pagerState,
                    scope,
                )
                FlightsList(
                    pagerState = pagerState,
                    viewModel = viewModel,
                    navController = navController,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    onFlightClick = onFlightClick
                )
            }
        }
    }
}

@Composable
fun TabRow(
    selectedTabIndex: Int,
    pagerState: PagerState,
    scope: CoroutineScope,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier
            .wrapContentHeight()
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(0)
                }
            },
            enabled = true,
            text = { Text(stringResource(R.string.upcoming_flights)) },
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(1)
                }
            },
            enabled = true,
            text = { Text(stringResource(R.string.past_flights)) },
        )
    }
}

@Composable
fun FlightsList(
    pagerState: PagerState,
    viewModel: MainFlightViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onFlightClick: (String) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            when (page) {
                0 -> {
                    if (viewModel.flightsUpcomingList.isEmpty()) {
                        NoFlight(Modifier
                            .fillMaxSize()
                            .padding(128.dp))
                    } else {
                        viewModel.flightsUpcomingList.forEach { flights ->
                            DateHeader(flights[0].departDate, flights.size)
                            flights.groupBy { flight ->
                                FlightCard(navController, flight, viewModel, onFlightClick)
                            }
                        }
                    }
                }
                1 -> {
                    if (viewModel.flightsPastList.isEmpty()) {
                        NoFlight(Modifier
                            .fillMaxSize()
                            .padding(128.dp))
                    } else {
                        viewModel.flightsPastList.forEach { flights ->
                            DateHeader(flights[0].departDate, flights.size)
                            flights.groupBy { flight ->
                                FlightCard(navController, flight, viewModel, onFlightClick)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(56.dp))
        }
    }
}

@Composable
fun NoFlight(
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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

@Composable
fun FlightCard(
    navController: NavController,
    flightData: FlightData,
    viewModel: MainFlightViewModel,
    onFlightClick: (String) -> Unit
) {
    val timeFormat = viewModel.preferencesInterface.getValueTimeFormatComposable("24_time")

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top main card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {
                onFlightClick(flightData.id.toString())
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
                        flightData.fromCountryCode,
                        flightData.fromName,
                        flightData.departDate
                            .atOffset(ZoneOffset.UTC)
                            .atZoneSameInstant(flightData.departTimeZone)
                            .format(DateTimeFormatter.ofPattern(timeFormat)),
                        Alignment.Start
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.to)
                    )
                    BoldDepartureAndDestinationText(
                        flightData.to,
                        flightData.toCountryCode,
                        flightData.toName,
                        flightData.arriveDate
                            .atOffset(ZoneOffset.UTC)
                            .atZoneSameInstant(flightData.arriveTimeZone)
                            .format(DateTimeFormatter.ofPattern(timeFormat)),
                        Alignment.End
                    )
                }
                TicketInformationCard(flightData)
                LinearProgressIndicator(
                    progress = {
                        val now = LocalDateTime
                            .now()
                            .atZone(flightData.departTimeZone)
                            .withZoneSameInstant(ZoneOffset.UTC)

                        val departTime = flightData.departDate
                            .atOffset(ZoneOffset.UTC)
                            .withOffsetSameInstant(ZoneOffset.UTC)

                        if (now < departTime.toZonedDateTime()) {
                            0.0F
                        } else {
                            val timeFromStart = Duration.between(now, departTime).toMillis()

                            val duration = flightData.duration.toMillis()

                            val current = timeFromStart.toFloat() / duration.toFloat()
                            current.absoluteValue
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
fun DateHeader(time: LocalDateTime, count: Int) {
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
        if (count > 1) {
            Text(
                "$count ${stringResource(R.string.flights)}",
                color = Color.Gray
            )
        }
    }
}

@SuppressLint("ShowToast")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: MainFlightViewModel,
) {
    val toast = Toast.makeText(LocalContext.current, R.string.no_flight, Toast.LENGTH_SHORT)

    LargeTopAppBar(
        title = { Text(stringResource(R.string.my_flights)) },
        colors = TopAppBarDefaults.topAppBarColors(),
        actions = {
            IconButton(onClick = {
                val closestFlight = viewModel.findClosestFlightId()

                if (closestFlight == null) {
                    toast.show()
                    return@IconButton
                } else closestFlight.let {
                    if (it > 0) {
                        navController.navigate("${Screen.FlightDetailsScreen.route}/${closestFlight}")
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.FlightTakeoff,
                    contentDescription = stringResource(R.string.upcoming_flights)
                )
            }
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

@Composable
@Preview
fun FlightCardPreview() {
    FlightCard(
        navController = rememberNavController(),
        flightData = FlightData(
            fromName = "Tan Son Nhat",
            toName = "Brisbane",
            from = "SGN",
            to = "BNE"
        ),
        viewModel = viewModel<MainFlightViewModel>(),
        onFlightClick = {}
    )
}
