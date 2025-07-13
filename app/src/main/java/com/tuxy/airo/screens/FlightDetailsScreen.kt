package com.tuxy.airo.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.outlined.Desk
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.Screen
import com.tuxy.airo.cancelAlarm
import com.tuxy.airo.composables.RouteBar
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.getData
import com.tuxy.airo.viewmodel.DetailsViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.MapUI
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.time.toKotlinDuration

@OptIn(DelicateCoroutinesApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsView(
    navController: NavController,
    id: String,
    flightDataDao: FlightDataDao,
) {
    val context = LocalContext.current

    val viewModelFactory = DetailsViewModel.Factory(LocalContext.current, flightDataDao, id)
    val viewModel: DetailsViewModel = viewModel(factory = viewModelFactory)

    val settings = ApiSettings(
        viewModel.getValue("API_CHOICE"),
        viewModel.getValue("ENDPOINT"),
        viewModel.getValue("API_KEY"),
        viewModel.getValue("API_SERVER"),
    )

    val refreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    var isRefreshingFinished by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshingFinished) {
        if(isRefreshingFinished) {
            navController.navigateUp()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SmallAppBarWithDelete(
                "${viewModel.flightData.value.from} to ${viewModel.flightData.value.to}",
                navController,
                viewModel.openDialog
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)

        ) {
            Column {
                if (viewModel.openDialog.value) {
                    DeleteDialog(
                        viewModel.openDialog,
                        navController,
                        flightDataDao,
                        viewModel.flightData
                    )
                }
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = {
                        GlobalScope.launch {
                            val now = Duration.between(
                                LocalDateTime.now(),
                                viewModel.flightData.value.departDate
                            ).toMillis()

                            if (LocalDateTime.now() < viewModel.flightData.value.departDate) {
                                viewModel.progress.floatValue = 0.0F
                                return@launch
                            }

                            val duration = viewModel.flightData.value.duration.toMillis()

                            val current = now.toFloat() / duration.toFloat()

                            viewModel.progress.floatValue = current.absoluteValue
                            delay(10000) // Improve performance
                        }
                        viewModel.progress.floatValue
                    }
                )
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    state = refreshState,
                    onRefresh = {
                        isRefreshing = true
                        GlobalScope.launch(Dispatchers.IO) {
                            val collectedFlightData = getData(
                                flightNumber = viewModel.flightData.value.callSign.replace(" ", ""), // Whitespace removal
                                flightDataDao = flightDataDao,
                                date = viewModel.flightData.value.departDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                settings = settings,
                                context = context,
                                update = true
                            )

                            collectedFlightData.onSuccess { newFlight ->
                                Log.d("FlightUpdate", newFlight.toString())
                                flightDataDao.deleteFlight(viewModel.flightData.value)
                                flightDataDao.addFlight(
                                    newFlight.copy(ticketData = viewModel.flightData.value.ticketData) // Retain ticket information
                                )
                            }
                            isRefreshing = false
                            isRefreshingFinished = true
                        }

                    }
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxHeight()
                    ) {
                        RouteBar(viewModel.flightData.value)
                        FlightBoardCard(viewModel.flightData.value)
                        Card(
                            modifier = Modifier
                                .padding(
                                    top = 17.dp,
                                    start = 17.dp,
                                    end = 17.dp
                                ) // Not sure why, but map seems to pop out a bit more than the others
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Gray)
                                    .aspectRatio(1280f / 847f)
                            ) {
                                MapUI(
                                    state = viewModel.mapState
                                )
                            }
                        }
                        FlightStatusCard(viewModel)
                        FlightInformationInteract(navController, viewModel.flightData.value)
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "Last updated: ${
                                viewModel.flightData.value.lastUpdate.format(
                                    DateTimeFormatter.ISO_DATE_TIME
                                )
                            }",
                            color = Color.Gray
                        )

                    }
                }
            }
        }
    }
}

@Composable
fun FlightBoardCard(
    flightData: FlightData
) {
    Card(
        modifier = Modifier
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FlightBoard(
                code = flightData.from,
                name = flightData.fromName,
                terminal = flightData.terminal,
                gate = flightData.gate,
                baggageClaim = "",
                checkIn = flightData.checkInDesk,
                date = flightData.departDate,
                timeZone = flightData.departTimeZone
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.width(128.dp)
                )
                Spacer(Modifier.padding(4.dp))
                Text(
                    flightData.duration.toKotlinDuration().toComponents { hours, minutes, _, _ ->
                        if (hours < 1) {
                            return@toComponents "${minutes}m"
                        } else {
                            return@toComponents "${hours}hr ${minutes}m"
                        }
                    },
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(Modifier.padding(4.dp))
                HorizontalDivider(
                    modifier = Modifier.width(128.dp)
                )
            }

            FlightBoard(
                code = flightData.to,
                name = flightData.toName,
                terminal = flightData.toTerminal,
                gate = flightData.toGate,
                baggageClaim = flightData.toBaggageClaim,
                checkIn = "",
                date = flightData.arriveDate,
                timeZone = flightData.arriveTimeZone,
                to = true,
            )
        }
    }
}

@Composable
fun FlightBoard(
    code: String,
    name: String,
    terminal: String,
    gate: String,
    baggageClaim: String,
    checkIn: String,
    date: LocalDateTime,
    timeZone: ZoneId,
    to: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                code,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp
            )
            Text(
                name,
                fontWeight = FontWeight.W500,
                fontSize = 16.sp
            )
            Row {
                SmallCard(
                    Icons.Filled.FlightTakeoff,
                    stringResource(R.string.terminal),
                    terminal
                )
                SmallCard(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    stringResource(R.string.gate),
                    gate
                )
                if (to) { // Checks if the card is an arrival flight
                    SmallCard(
                        Icons.Filled.Luggage,
                        stringResource(R.string.baggage_claim),
                        baggageClaim // Then pick baggageClaim over checkInDesk
                    )
                } else {
                    SmallCard(
                        Icons.Outlined.Desk,
                        stringResource(R.string.check_in),
                        checkIn
                    )
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                date
                    .atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(timeZone)
                    .format(DateTimeFormatter.ofPattern("HH:mm")),
                fontWeight = FontWeight.W500,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SmallCard(
    imageVector: ImageVector,
    contentDescription: String,
    text: String
) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp, end = 8.dp)
            .background(
                color = Color.hsl(
                    hue = 53.0F,
                    saturation = 1F,
                    lightness = 0.46F,
                ),
                shape = RoundedCornerShape(4.dp)
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector,
                contentDescription,
                Modifier.size(16.dp),
                Color.Black
            )
            Spacer(Modifier.padding(4.dp))
            Text(
                text,
                color = Color.Black
            )
            Spacer(Modifier.padding(2.dp))
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun FlightStatusCard(viewModel: DetailsViewModel) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.now),
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                )
                Text(
                    viewModel.getEndTime(context),
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    viewModel.getStatus(context),
                    fontWeight = FontWeight.W500,
                    fontSize = 16.sp
                )
                Text(
                    viewModel.getDuration(context),
                    fontWeight = FontWeight.W500,
                    fontSize = 16.sp
                )
            }
            LinearProgressIndicator(
                progress = {
                    GlobalScope.launch {
                        delay(2000)
                        val now = Duration.between(
                            LocalDateTime.now(),
                            viewModel.flightData.value.departDate
                        ).toMillis()

                        if (LocalDateTime.now() < viewModel.flightData.value.departDate) {
                            viewModel.progress.floatValue = 0.0F
                            return@launch
                        }

                        val duration = viewModel.flightData.value.duration.toMillis()

                        val current = now.toFloat() / duration.toFloat()

                        viewModel.progress.floatValue = current.absoluteValue
                        delay(10000) // Improve performance
                    }
                    viewModel.progress.floatValue
                },
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallAppBarWithDelete(
    text: String,
    navController: NavController,
    openDialog: MutableState<Boolean>,
) {
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
        },
        actions = {
            IconButton(onClick = {
                openDialog.value = true
            }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}

@Composable
fun FlightInformationInteract(navController: NavController, flightData: FlightData) {
    Column {
        ListItem(
            modifier = Modifier.clickable(onClick = { navController.navigate("${Screen.TicketInformationScreen.route}/${flightData.id}") }),
            headlineContent = { Text(stringResource(R.string.ticket)) },
            supportingContent = { Text(flightData.callSign) }
        )
        ListItem(
            modifier = Modifier.clickable(onClick = { navController.navigate("${Screen.AircraftInformationScreen.route}/${flightData.id}") }),
            headlineContent = { Text(stringResource(R.string.aircraft)) },
            supportingContent = { Text(flightData.aircraftName) }
        )
//        No API or webpage really exists with good coverage for terminal maps. Please send me links to good ones
//        ListItem(
//            modifier = Modifier.clickable(onClick = {}),
//            headlineContent = { Text("Airport") },
//            supportingContent = { Text(flightData.fromName) },
//            trailingContent = { Icon(Icons.Filled.Info, "Airport Information") }
//        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun DeleteDialog(
    openDialog: MutableState<Boolean>,
    navController: NavController,
    flightDataDao: FlightDataDao,
    flightData: MutableState<FlightData>,
) {
    val context = LocalContext.current

    BasicAlertDialog(
        onDismissRequest = {
            openDialog.value = false
        }
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(R.string.delete_dialog),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    TextButton(
                        onClick = {
                            openDialog.value = false
                        },
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            GlobalScope.launch(Dispatchers.IO) {
                                flightDataDao.deleteFlight(flightData.value)
                            }
                            cancelAlarm(context, flightData.value)
                            openDialog.value = false
                            navController.navigateUp()
                        },
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}
