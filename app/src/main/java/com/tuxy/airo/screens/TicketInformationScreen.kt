package com.tuxy.airo.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.tuxy.airo.viewmodel.TicketViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun TicketInformationView(
    navController: NavController,
    id: String,
    flightDataDao: FlightDataDao
) {
    val viewModelFactory = TicketViewModel.Factory(flightDataDao, id, LocalContext.current)
    val viewModel: TicketViewModel = viewModel(factory = viewModelFactory)

    val barCodeLauncher = rememberLauncherForActivityResult(ScanContract()) {
            result ->
        if( result.contents == null ) {
            Log.d("Camera", "Cancelled")
        } else {
            viewModel.flightData.value.ticketData = result.contents
            GlobalScope.launch(Dispatchers.IO) {
                flightDataDao.updateFlight(viewModel.flightData.value)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(
            title = { Text("UA45") },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = {  }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            }
        ) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                if(viewModel.hasCameraPermission) {
//                    navController.navigate("${Screen.CameraScreen.route}/${id}")
                    viewModel.showCamera(barCodeLauncher)
                } else {
                    if(!viewModel.hasCameraPermission) {
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
        Column(modifier = Modifier.padding(innerPadding)) {
            MainTicketView(viewModel.flightData.value)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainTicketView(flightData: FlightData) {
    Column(
        modifier = Modifier,
    ) {
        Row(
            modifier = Modifier
                .padding(28.dp)
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
                LargeTopSmallBottom(stringResource(R.string.terminal), flightData.ticketTerminal)
                LargeTopSmallBottom(stringResource(R.string.gate), flightData.ticketGate)
                LargeTopSmallBottom(stringResource(R.string.seat), "Placeholder")
                LargeTopSmallBottom(stringResource(R.string.class_s), "Business")
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 52.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LargeTopSmallBottom(stringResource(R.string.passenger_name), "John Doe")
            }
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 64.dp)
                .padding(vertical = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
            ) {
                AsyncImage(
                    model = "https://docs.zebra.com/content/dam/techpubs/media/scanners/common/1d-barcodes/sample-aztec.svg/_jcr_content/renditions/original",
                    contentDescription = "Ticket QR Code",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / 1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = {}) {
                Text(stringResource(R.string.switch_ticket))
            }
        }
    }
}
