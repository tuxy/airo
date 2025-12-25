package com.tuxy.airo.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.viewmodel.MainFlightViewModel
import kotlinx.coroutines.delay
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

    LaunchedEffect(Unit) {
        viewModel.loadData(flightDataDao)
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<String>()

    var displayedId by remember { mutableStateOf<String?>(null) }
    val currentId = navigator.currentDestination?.contentKey

    LaunchedEffect(currentId) {
        if (currentId != null) {
            displayedId = currentId
        } else {
            delay(300L) // Delay to prevent EmptyFlight() from taking over too early
            displayedId = null
        }
    }

    BackHandler(navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
        }
    }

    val paneExpansionState = rememberPaneExpansionState(
        keyProvider = navigator.scaffoldValue,
        anchors = listOf(
            PaneExpansionAnchor.Proportion(0f),
            PaneExpansionAnchor.Proportion(0.3f),
            PaneExpansionAnchor.Proportion(0.45f),
            PaneExpansionAnchor.Proportion(0.5f),
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane(Modifier.clipToBounds()) {
                    key(viewModel.flightData) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentWidth(unbounded = true)
                        ) {
                            MainFlightView(
                                navController = navController,
                                flightDataDao = flightDataDao,
                                viewModel = viewModel,
                                paneNavigator = navigator,
                                onFlightClick = { id ->
                                    scope.launch {
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                                    }
                                },
                            )
                        }
                    }
                }
            },
            detailPane = {
                AnimatedPane(Modifier.clipToBounds()) {
                    if (displayedId != null) {
                        FlightDetailsView(
                            navController = navController,
                            id = displayedId!!,
                            flightDataDao = flightDataDao,
                            paneNavigator = navigator
                        )
                    } else {
                        EmptyFlight()
                    }
                }
            },
            paneExpansionState = paneExpansionState,
            paneExpansionDragHandle = { state ->
                val interactionSource = remember { MutableInteractionSource() }
                VerticalDragHandle(
                    modifier =
                    Modifier
                        .paneExpansionDraggable(
                            state,
                            LocalMinimumInteractiveComponentSize.current,
                            interactionSource

                        )
                        .width(2.dp),
                    interactionSource = interactionSource
                )
            }
        )
    }
}

@Composable
fun EmptyFlight() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentWidth(unbounded = true),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Select a flight to see details")
    }
}
