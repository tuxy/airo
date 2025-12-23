package com.tuxy.airo.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.viewmodel.MainFlightViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FoldableFlightScreen(
    navController: NavController,
    flightDataDao: FlightDataDao,
) {
    val viewModelFactory = MainFlightViewModel.Factory(LocalContext.current)
    val viewModel: MainFlightViewModel = viewModel(factory = viewModelFactory)
    viewModel.loadData(flightDataDao)

    // A state to hold the currently selected flight ID for the detail view
    var selectedFlightId by remember { mutableStateOf<Int?>(null) }

    // Find the closest flight initially
    val closestFlightId = viewModel.findClosestFlightId()

    // Initialize selectedFlightId with the closest flight if it hasn't been set yet
    LaunchedEffect(closestFlightId) {
        if (selectedFlightId == null) {
            selectedFlightId = closestFlightId
        }
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<String?>()
    val scope = rememberCoroutineScope()

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            MainFlightView(
                navController = navController,
                flightDataDao = flightDataDao,
                onFlightClick = { flightId ->
                    // Simply update the state. The navigator will react to this change.
                    selectedFlightId = flightId.toInt()
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.List, flightId)
                    }
                }
            )
        },
        detailPane = {
            AnimatedPane {
                if (selectedFlightId != null) {
                    FlightDetailsView(
                        navController = navController,
                        id = selectedFlightId.toString(),
                        flightDataDao = flightDataDao,
                    )
                } else {
                    EmptyFlight()
                }
            }
        }
    )
}

@Composable
fun EmptyFlight() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Select a flight to see details")
    }
}
