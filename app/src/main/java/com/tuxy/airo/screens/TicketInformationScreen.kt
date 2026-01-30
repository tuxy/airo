package com.tuxy.airo.screens

import android.Manifest
import android.content.ClipData
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.journeyapps.barcodescanner.ScanContract
import com.simonsickle.compose.barcodes.Barcode
import com.simonsickle.compose.barcodes.BarcodeType
import com.tuxy.airo.R
import com.tuxy.airo.composables.BoldDepartureAndDestinationText
import com.tuxy.airo.composables.LargeTopSmallBottom
import com.tuxy.airo.data.flightdata.FlightData
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.data.flightdata.IataParserData
import com.tuxy.airo.viewmodel.TicketViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
    ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun TicketInformationView(
    paneNavigator: ThreePaneScaffoldNavigator<String>,
    id: String,
    flightDataDao: FlightDataDao
) {
    val viewModelFactory = TicketViewModel.Factory(flightDataDao, id, LocalContext.current)
    val viewModel: TicketViewModel = viewModel(
        factory = viewModelFactory,
        key = id
    )
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val barCodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.ticketString = result.contents
            viewModel.updateData(flightDataDao, context)
        }
    }

    val permissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA,
        onPermissionResult = { result ->
            viewModel.hasCameraPermission = result
            if (result) { viewModel.showCamera(barCodeLauncher, context) }
        }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(viewModel.flightData.value.callSign) },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch { paneNavigator.navigateBack() }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.openDialog.value = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                if (viewModel.hasCameraPermission) {
                    viewModel.showCamera(barCodeLauncher, context)
                } else {
                    if (!viewModel.hasCameraPermission) {
                        viewModel.toast.show()
                        permissionState.launchPermissionRequest()
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.QrCode,
                    contentDescription = stringResource(R.string.add_ticket)
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add_ticket))
            }
        }
    ) { innerPadding ->
        AnimatedVisibility(viewModel.isDataPopulated()) { // If there is no ticket, then show an empty screen
            Column(modifier = Modifier.padding(innerPadding)) {
                if (viewModel.openDialog.value) {
                    DeleteTicketDialog(
                        viewModel.openDialog,
                        paneNavigator,
                        viewModel,
                        flightDataDao
                    )
                }
                AnimatedVisibility(!viewModel.ticketData.eTicketIndicator) {
                    Card(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.padding(start = 16.dp),
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.info)
                            )
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.warning), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.not_e_ticket))
                            }
                        }
                    }
                }
                MainTicketView(viewModel.ticketData, viewModel.flightData.value, viewModel)
//                if(viewModel.ticketData != IataParserData()) {
//                    MainTicketView(viewModel.ticketData, viewModel.flightData.value)
//                } else {
//                    Toast.makeText(LocalContext.current, stringResource(R.string.invalid_pass), Toast.LENGTH_LONG).show()
//                }
            }
        }
        if (!viewModel.isDataPopulated()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(360.dp))
                Icon(
                    modifier = Modifier.size(100.dp),
                    imageVector = Icons.AutoMirrored.Filled.AirplaneTicket,
                    contentDescription = stringResource(R.string.add_ticket),
                    tint = Color.Gray
                )
                Text(
                    stringResource(R.string.no_ticket),
                    color = Color.Gray,
                    modifier = Modifier.padding(32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MainTicketView(
    iataParserData: IataParserData,
    flightData: FlightData,
    viewModel: TicketViewModel
) {
    val clipboard = LocalClipboard.current
    val timeFormat = viewModel.preferencesInterface.getValueTimeFormatComposable("24_time")
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            BoldDepartureAndDestinationText(
                flightData.from,
                flightData.fromCountryCode,
                flightData.fromName,
                flightData.departDate.format(DateTimeFormatter.ofPattern(timeFormat)),
                Alignment.Start
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.to)
            )
            BoldDepartureAndDestinationText(
                flightData.to,
                flightData.toCountryCode,
                flightData.fromName,
                flightData.arriveDate.format(DateTimeFormatter.ofPattern(timeFormat)),
                Alignment.End
            )
        }
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 52.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LargeTopSmallBottom(
                    stringResource(R.string.date), iataParserData.date.format(
                        DateTimeFormatter.ofPattern("dd MMM")
                    )
                )
                LargeTopSmallBottom(stringResource(R.string.gate), flightData.gate)
                LargeTopSmallBottom(stringResource(R.string.seat), iataParserData.seat)
                LargeTopSmallBottom(
                    stringResource(R.string.class_s),
                    iataParserData.flightClass
                )
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 52.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LargeTopSmallBottom(
                    stringResource(R.string.passenger_name),
                    iataParserData.passengerName
                )
                LargeTopSmallBottom(
                    stringResource(R.string.flight_number),
                    "${iataParserData.carrier} ${iataParserData.flightNumber}"
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(vertical = 20.dp)
                    .width(250.dp)
                    .height(250.dp)
                    .background(Color.White)
            ) {
                if (viewModel.isDataPopulated()) {
                    when (BarcodeType.AZTEC.isValueValid(viewModel.ticketString)) {
                        true -> {
                            Barcode(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(250.dp)
                                    .height(250.dp)
                                    .padding(32.dp),
                                resolutionFactor = 10,
                                type = BarcodeType.AZTEC,
                                value = viewModel.ticketString
                            )
                        }

                        false -> {} // Do nothing if the code cannot be processed
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipData
                            .newPlainText(
                                "",
                                AnnotatedString(iataParserData.bookingReference)
                            )
                            .toClipEntry()
                    )
                }
            }) {
                Row(
                    Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.copy_reference)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.copy_reference))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class,
    ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun DeleteTicketDialog(
    openDialog: MutableState<Boolean>,
    paneNavigator: ThreePaneScaffoldNavigator<String>,
    viewModel: TicketViewModel,
    flightDataDao: FlightDataDao
) {
    BasicAlertDialog(
        onDismissRequest = {
            openDialog.value = false
        }
    ) {
        val context = LocalContext.current

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
                    text = stringResource(R.string.delete_dialog_ticket),
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
                                viewModel.deleteData(flightDataDao, context)
                                paneNavigator.navigateBack()
                            }
                            openDialog.value = false
                        },
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}
