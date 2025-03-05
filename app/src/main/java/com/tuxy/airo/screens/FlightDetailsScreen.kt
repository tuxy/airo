package com.tuxy.airo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.tuxy.airo.viewmodel.DetailsViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.MapUI
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.absoluteValue

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun FlightDetailsView(
    navController: NavController,
    id: String,
    flightDataDao: FlightDataDao,
) {
    val viewModelFactory = DetailsViewModel.Factory(LocalContext.current, flightDataDao, id)
    val viewModel: DetailsViewModel = viewModel(factory = viewModelFactory)

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
                RouteBar(viewModel.flightData.value)
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
                FlightStatusCard(viewModel)
                FlightInformationInteract(navController, viewModel.flightData.value)
            }
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
