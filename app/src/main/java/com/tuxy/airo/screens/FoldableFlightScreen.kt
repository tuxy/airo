package com.tuxy.airo.screens

import android.os.PowerManager
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
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tuxy.airo.R
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.screens.settings.ApiSettingsView
import com.tuxy.airo.screens.settings.BackupSettingsView
import com.tuxy.airo.screens.settings.NotificationsSettingsView
import com.tuxy.airo.viewmodel.MainFlightViewModel
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FoldableFlightScreen(
    navController: NavController,
    flightDataDao: FlightDataDao,
    backup: RoomBackup,
    powerManager: PowerManager,
) {
    val viewModelFactory = MainFlightViewModel.Factory(LocalContext.current)
    val viewModel: MainFlightViewModel = viewModel(factory = viewModelFactory)
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    // val isDragged by interactionSource.collectIsDraggedAsState() // Future reference

    var currentExtraPaneType by remember { mutableStateOf(ExtraPaneTypes.Undefined) }
    var currentSettingsSubKey by remember { mutableStateOf<SettingsSubPaneTypes?>(null) }
    var isInSettingsFlow by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = {2})

    LaunchedEffect(Unit) {
        viewModel.startCollecting(flightDataDao)
    }

    val containerWidth = LocalWindowInfo.current.containerSize.width
    val navigator = rememberListDetailPaneScaffoldNavigator<String>(
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
            maxHorizontalPartitions = if (containerWidth > 1200) 2 else 1
        )
    )

    val flightData by viewModel.flightData.collectAsState()
    val closestFlightId = remember(flightData) {
        if (navigator.scaffoldDirective.maxHorizontalPartitions == 1) {
            flightData
                .filter { Duration.between(ZonedDateTime.now(), it.scheduledDepartDate).seconds >= 0 }
                .minByOrNull {
                    Duration.between(it.scheduledDepartDate, ZonedDateTime.now()).seconds
                }?.id?.toString()
        } else {
            null
        }
    }

    BackHandler(navigator.canNavigateBack()) {
        scope.launch {
            when {
                currentSettingsSubKey != null -> {
                    currentSettingsSubKey = null
                    navigator.navigateBack()
                }
                isInSettingsFlow -> {
                    isInSettingsFlow = false
                    navigator.navigateBack()
                }
                else -> navigator.navigateBack()
            }
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
    if (containerWidth > 2200) paneExpansionState.setFirstPaneProportion(0.3f) else paneExpansionState.setFirstPaneProportion(0.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    key(navigator.currentDestination?.contentKey) {
                        Row(
                            Modifier.fillMaxWidth()
                        ) {
                            MainFlightView(
                                navController = navController,
                                flightDataDao = flightDataDao,
                                viewModel = viewModel,
                                pagerState = pagerState,
                                onFlightClick = { id ->
                                    isInSettingsFlow = false
                                    currentSettingsSubKey = null
                                    scope.launch {
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                                    }
                                },
                                onNavigateToSettings = {
                                    isInSettingsFlow = true
                                    currentSettingsSubKey = null
                                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "settings") }
                                },
                            )
                        }
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    val flightData by viewModel.flightData.collectAsState()
                    key(flightData, isInSettingsFlow) {
                        if (isInSettingsFlow) {
                            SettingsView(
                                powerManager = powerManager,
                                paneNavigator = navigator,
                                onNavigateToSubSetting = { subKey ->
                                    currentSettingsSubKey = subKey
                                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Extra, subKey.name.lowercase()) }
                                }
                            )
                        } else {
                            val detailId = navigator.currentDestination?.contentKey ?: closestFlightId
                            if (detailId != null) {
                                FlightDetailsView(
                                    id = detailId,
                                    flightDataDao = flightDataDao,
                                    paneNavigator = navigator,
                                    onFlightDelete = {
                                        scope.launch {
                                            navigator.navigateBack()
                                        }
                                    },
                                    onShowTicket = {
                                        currentExtraPaneType = ExtraPaneTypes.TicketInformation
                                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Extra, detailId) }
                                    },
                                    onShowAircraft = {
                                        currentExtraPaneType = ExtraPaneTypes.AircraftInformation
                                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Extra, detailId) }
                                    }
                                )
                            } else {
                                EmptyFlight()
                            }
                        }
                    }
                }
            },
            extraPane = {
                AnimatedPane {
                    when (currentSettingsSubKey) {
                        SettingsSubPaneTypes.Api -> ApiSettingsView(paneNavigator = navigator)
                        SettingsSubPaneTypes.Backup -> BackupSettingsView(paneNavigator = navigator, backup = backup)
                        SettingsSubPaneTypes.Notifications -> NotificationsSettingsView(paneNavigator = navigator, flightDataDao = flightDataDao)
                        else -> {
                            when (currentExtraPaneType) {
                                ExtraPaneTypes.TicketInformation -> TicketInformationView(
                                    paneNavigator = navigator,
                                    id = navigator.currentDestination?.contentKey ?: "0",
                                    flightDataDao = flightDataDao,
                                )
                                ExtraPaneTypes.AircraftInformation -> AircraftInformationView(
                                    paneNavigator = navigator,
                                    id = navigator.currentDestination?.contentKey ?: "0",
                                    flightDataDao = flightDataDao
                                )
                                else -> { }
                            }
                        }
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
