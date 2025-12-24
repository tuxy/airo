package com.tuxy.airo.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

    val scope = rememberCoroutineScope()

    val navigator = rememberListDetailPaneScaffoldNavigator<String?>()

    BackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            MainFlightView(
                navController = navController,
                flightDataDao = flightDataDao,
                onFlightClick = { flightId ->
                    // Simply update the state. The navigator will react to this change.
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, flightId)
                    }
                }
            )
        },
        detailPane = {
            AnimatedPane {
                navigator.currentDestination?.contentKey?.let { index ->
                    key(index) {
                        FlightDetailsView(
                            navController = navController,
                            id = index,
                            flightDataDao = flightDataDao
                        )
                    }
                } ?: EmptyFlight()
            }
        },
        paneExpansionState =
            rememberPaneExpansionState(
                keyProvider = navigator.scaffoldValue,
                anchors = listOf(
                    PaneExpansionAnchor.Proportion(0.45f),
                    PaneExpansionAnchor.Proportion(0.5f),
                    PaneExpansionAnchor.Proportion(0.55f),
                )
            ),
        paneExpansionDragHandle = { state ->
            val interactionSource = remember { MutableInteractionSource() }
            VerticalDragHandle(
                modifier =
                    Modifier.paneExpansionDraggable(
                        state,
                        LocalMinimumInteractiveComponentSize.current,
                        interactionSource
                    ),
                interactionSource = interactionSource
            )
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
