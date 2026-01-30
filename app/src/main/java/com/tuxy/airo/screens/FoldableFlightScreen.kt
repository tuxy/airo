package com.tuxy.airo.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Airlines
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.viewmodel.MainFlightViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ExtraPaneTypes {
    Undefined,
    TicketInformation,
    AircraftInformation
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FoldableFlightScreen(
    navController: NavController,
    flightDataDao: FlightDataDao,
) {
    val viewModelFactory = MainFlightViewModel.Factory(LocalContext.current)
    val viewModel: MainFlightViewModel = viewModel(factory = viewModelFactory)
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    // val isDragged by interactionSource.collectIsDraggedAsState() // Future reference

    var currentExtraPaneType by remember { mutableStateOf(ExtraPaneTypes.Undefined) }

    LaunchedEffect(Unit) {
        viewModel.loadData(flightDataDao)
    }

    val containerWidth = LocalWindowInfo.current.containerSize.width
    println(containerWidth)
    val navigator = rememberListDetailPaneScaffoldNavigator<String>(
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
            maxHorizontalPartitions = if (containerWidth > 1200) 2 else 1
        )
    )
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
            PaneExpansionAnchor.Proportion(if (containerWidth > 2200) 0.3f else 0.45f), // For tablets
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
                            Modifier.fillMaxWidth()
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
                            id = displayedId!!,
                            flightDataDao = flightDataDao,
                            paneNavigator = navigator,
                            onFlightDelete = {
                                scope.launch {
                                    viewModel.loadData(flightDataDao) // Reload data
                                    navigator.navigateBack()
                                }
                            },
                            onShowTicket = {
                                currentExtraPaneType = ExtraPaneTypes.TicketInformation
                                scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Extra, displayedId!!) }
                            },
                            onShowAircraft = {
                                currentExtraPaneType = ExtraPaneTypes.AircraftInformation
                                scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Extra, displayedId!!) }
                            }
                        )
                    } else {
                        EmptyFlight()
                    }
                }
            },
            extraPane = {
                AnimatedPane {
                    when(currentExtraPaneType) {
                        ExtraPaneTypes.TicketInformation -> TicketInformationView(
                            paneNavigator = navigator,
                            id = currentId!!,
                            flightDataDao = flightDataDao,
                        )
                        ExtraPaneTypes.AircraftInformation -> AircraftInformationView(
                            paneNavigator = navigator,
                            id = currentId!!,
                            flightDataDao = flightDataDao
                        )
                        else -> EmptyFlight()
                    }
                }
            },
            paneExpansionState = paneExpansionState,
            paneExpansionDragHandle = { state ->
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
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Airlines,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.select_flight),
            overflow = TextOverflow.Visible,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
