package com.tuxy.airo.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.journeyapps.barcodescanner.ScanContract
import com.tuxy.airo.R
import com.tuxy.airo.composables.BoldDepartureAndDestinationText
import com.tuxy.airo.composables.LargeTopSmallBottom
import com.tuxy.airo.data.FlightData
import com.tuxy.airo.data.FlightDataDao
import com.tuxy.airo.data.IataParserData
import com.tuxy.airo.viewmodel.TicketViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketInformationView(
    navController: NavController,
    id: String,
    flightDataDao: FlightDataDao
) {
    val viewModelFactory = TicketViewModel.Factory(flightDataDao, id, LocalContext.current)
    val viewModel: TicketViewModel = viewModel(factory = viewModelFactory)
    val context = LocalContext.current

    val barCodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.updateData(result.contents, flightDataDao, context)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(viewModel.flightData.value.callSign) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteData(flightDataDao, context)
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
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Create,
                    contentDescription = stringResource(R.string.add_ticket)
                )
                Text(stringResource(R.string.add_ticket))
            }
        }
    ) { innerPadding ->
        if (viewModel.isDataPopulated()) { // If there is no ticket, then show an empty screen
            Column(modifier = Modifier.padding(innerPadding)) {
                if (!viewModel.ticketData.eTicketIndicator) {
                    Card(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
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
                                Text("Warning", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("This ticket is not an E-ticket, so it may not be recognised")
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
    }
}

@Composable
fun MainTicketView(
    iataParserData: IataParserData,
    flightData: FlightData,
    viewModel: TicketViewModel
) {
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
                flightData.fromName,
                Alignment.Start
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.to)
            )
            BoldDepartureAndDestinationText(flightData.to, flightData.toName, Alignment.End)
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
        Box(
            modifier = Modifier
                .padding(horizontal = 64.dp)
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
            ) {
                AsyncImage(
                    model = viewModel.getQrCode(),
                    contentDescription = stringResource(R.string.qrcode_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / 1f)
                )
            }
        }
//        Row( // Right now, only aztec is used as the format
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceEvenly
//        ) {
//            OutlinedButton(onClick = {}) {
//                Text(stringResource(R.string.switch_ticket))
//            }
//        }
    }
}
