package com.tuxy.airo.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tuxy.airo.Screen
import com.tuxy.airo.composables.LargeTopSmallBottom
import com.tuxy.airo.composables.RouteBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFlightView(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { MainTopBar(navController = navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navController.navigate(Screen.NewFlightScreen.route)
                },
                icon = { Icon(Icons.Filled.Add, "Add flight") },
                text = { Text(text = "Add Flight") },
            )
        }
    ) { innerPadding ->
        Column (
            modifier = Modifier
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = 0,
            ) {
                Tab(
                    selected = true,
                    onClick = {},
                    text = { Text("Upcoming") }
                )
                Tab(
                    selected = false,
                    onClick = {},
                    text = { Text("Past") }
                )
            }
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                FlightCard(navController)
            }
        }
    }
}

@Composable
fun FlightCard(navController: NavController) { // TODO Add parameters to create multiple seperate instances of flights
    ElevatedCard(
        onClick = {
            navController.navigate(route = Screen.FlightDetailsScreen.route)
        },
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp)
    ) {
        Column {
            RouteBar()
            TicketInformationCard()
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    onClick = {}
                ) {
                    Text("Show Ticket")
                }
                FilledTonalButton(onClick = {}) {
                    Text("Check-in")
                }
            }
            LinearProgressIndicator(
                progress = { 0F },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun TicketInformationCard() { // TODO How to get ticket information from ticket?
    Row (
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LargeTopSmallBottom("Terminal", "2")
        LargeTopSmallBottom("Gate", "5")
        LargeTopSmallBottom("Seat", "32A")
        Badge(
            modifier = Modifier.size(16.dp),
            containerColor = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(modifier: Modifier = Modifier, navController: NavController) {
    LargeTopAppBar(
        title = { Text("My Flights") },
        colors = TopAppBarDefaults.topAppBarColors(),
        actions = {
            IconButton(onClick = {
                navController.navigate(route = Screen.SettingsScreen.route)
            }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        modifier = modifier
    )
}
