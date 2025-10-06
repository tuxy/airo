package com.tuxy.airo.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ireward.htmlcompose.HtmlText
import com.tuxy.airo.R
import com.tuxy.airo.composables.SmallAppBar
import com.tuxy.airo.data.flightdata.FlightDataDao
import com.tuxy.airo.viewmodel.StandardDataViewModel

@Composable
fun AircraftInformationView(
    navController: NavController,
    id: String,
    flightDataDao: FlightDataDao
) {
    val context = LocalContext.current
    val viewModelFactory = StandardDataViewModel.Factory(flightDataDao, id)
    val viewModel: StandardDataViewModel = viewModel(factory = viewModelFactory)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { SmallAppBar(stringResource(R.string.aircraft_information), navController) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text(viewModel.flightData.value.aircraftName) },
                supportingContent = { Text(viewModel.flightData.value.airline) }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
                    .clickable {
                        viewModel.openWebpage(context, viewModel.flightData.value.authorUri)
                    }
            ) {
                AsyncImage(
                    model = viewModel.flightData.value.aircraftUri,
                    contentDescription = stringResource(R.string.aircraft),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1280f / 847f)
                )
            }
            AircraftListView(context, viewModel)
        }
    }
}

@Composable
fun AircraftListView(
    context: Context,
    viewModel: StandardDataViewModel
) {

    Column {
        ListItem(
            modifier = Modifier
                .clickable {
                    viewModel.openWebpage(
                        context,
                        "https://www.aerolopa.com/${viewModel.flightData.value.callSign.split(" ")[0].lowercase()}"
                    )
                },
            headlineContent = { Text(stringResource(R.string.seat_maps)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, "") }
        )
        ListItem(
            modifier = Modifier
                .clickable {
                    viewModel.openWebpage(context, viewModel.flightData.value.authorUri)
                },
            headlineContent = { Text(stringResource(R.string.author)) },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    stringResource(R.string.empty)
                )
            }
        )
        Attribution(viewModel)
    }
}

@Composable
fun Attribution(
    viewModel: StandardDataViewModel
) {
    val expanded = remember { mutableStateOf(false) }
    val hasVisualOverflow = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .padding(16.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                expanded.value = !expanded.value
            }
    ) {
        HtmlText(
            text = viewModel.flightData.value.attribution,
            maxLines = if (expanded.value) Int.MAX_VALUE else 1,
            onTextLayout = { hasVisualOverflow.value = it.hasVisualOverflow },
            style = TextStyle(color = Color.Gray)
        )
    }
}
