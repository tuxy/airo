package com.tuxy.airo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BasicAlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.RouteBar
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.singleIntoMut
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.minScale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun FlightDetailsView(
    navController: NavController,
    id: String,
    flightDataDao: FlightDataDao
) {
    val flightData = remember { mutableStateOf(FlightData()) } // Empty flight data
    val loaded = remember { mutableStateOf(false) } // has db has been loaded into flightData

    // Loaded?
    if(!loaded.value) {
        singleIntoMut(flightData, flightDataDao, id)
        loaded.value = true
    }

    // Dialog open?
    val openDialog = remember { mutableStateOf(false) }

    // Map data
    val context = LocalContext.current
    val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
        context.assets.open("tiles/${zoomLvl}/${col}/${row}.png")
    }

    val mapSize = mapSizeAtLevel(5, 256)
    val mapState = MapState(6, mapSize, mapSize).apply {
        addLayer(tileStreamProvider)
        addMarker("origin", x = flightData.value.mapOriginX, y = flightData.value.mapOriginY) {
            Badge(contentColor = Color.Black, containerColor = Color.Black)
        }
        addMarker("destination", x = flightData.value.mapDestinationX, y = flightData.value.mapDestinationY) {
            Badge(contentColor = Color.Black, containerColor = Color.Black)
        }
        GlobalScope.launch {
            scrollTo(
                avr(flightData.value.mapOriginX, flightData.value.mapDestinationX),
                avr(flightData.value.mapOriginY, flightData.value.mapDestinationY),
                calculateScale(flightData.value.mapOriginX, flightData.value.mapOriginY, flightData.value.mapDestinationX, flightData.value.mapDestinationY)
            )
            addPath("route", color = Color.Black, width = 2.dp) {
                addPoint(x = flightData.value.mapOriginX, y = flightData.value.mapOriginY - 0.0007)
                addPoint(x = flightData.value.mapDestinationX, y = flightData.value.mapDestinationY - 0.0007)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { SmallAppBarWithDelete("${flightData.value.from} to ${flightData.value.to}", navController, openDialog) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Column {
                if(openDialog.value) {
                    DeleteDialog(
                        openDialog,
                        navController,
                        flightDataDao,
                        flightData
                    )
                }
                LinearProgressIndicator(
                    progress = { flightData.value.progress.toFloat() }
                )
                RouteBar(flightData.value)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Gray)
                        .aspectRatio(1280f / 847f)
                ) {
                    MapUI(
                        state = mapState
                    )
                }
                FlightInformationInteract(navController, flightData.value)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallAppBarWithDelete(
    text:String,
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
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = {
                openDialog.value = true
            }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Back"
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
            headlineContent = { Text("Ticket") },
            supportingContent = { Text(flightData.ticketSeat) }
        )
        ListItem(
            modifier = Modifier.clickable(onClick = { navController.navigate("${Screen.AircraftInformationScreen.route}/${flightData.id}") }),
            headlineContent = { Text("Aircraft") },
            supportingContent = { Text(flightData.aircraftName) }
        )
        ListItem(
            modifier = Modifier.clickable(onClick = {}),
            headlineContent = { Text("Airport") },
            supportingContent = { Text(flightData.fromName) },
            trailingContent = { Icon(Icons.Filled.Info, "Airport Information") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun DeleteDialog(
    openDialog: MutableState<Boolean>,
    navController: NavController,
    flightDataDao: FlightDataDao,
    flightData: MutableState<FlightData>
) {
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
                    text = "Delete flight? This CANNOT be undone.",
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    TextButton(
                        onClick = {
                            openDialog.value = false
                        },
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                        GlobalScope.launch(Dispatchers.IO) {
                            flightDataDao.deleteFlight(flightData.value)
                        }
                            openDialog.value = false
                            navController.navigateUp()
                        },
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }

}

private fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}

private fun avr(a: Double, b: Double): Double {
    return (a + b) / 2
}

private fun calculateScale(x1: Double, y1: Double, x2: Double, y2: Double): Float {
    val zoomConstant = 12.0 // Trial and error

    val a = (x2 - x1) * (x2 - x1)
    val b = (y2 - y1) * (y2 - y1)
    return (1/(sqrt(a + b) * zoomConstant)).toFloat()
}
